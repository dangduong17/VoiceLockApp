package com.example.voicelock.feature.face.auth

import android.graphics.Bitmap
import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.face.BlinkTracker
import com.example.voicelock.data.model.face.FaceAuthStage
import com.example.voicelock.data.model.face.FaceAuthUiEvent
import com.example.voicelock.data.model.face.FaceAuthUiState
import com.example.voicelock.data.model.face.FaceEngineStatus
import com.example.voicelock.data.model.face.FaceFrameAnalysis
import com.example.voicelock.data.model.face.FaceHint
import com.example.voicelock.data.model.face.FaceSettingsDtoModel
import com.example.voicelock.data.model.face.FaceVerifyResult
import com.example.voicelock.data.model.face.canStartScan
import com.example.voicelock.data.model.face.toHint
import com.example.voicelock.domain.usecase.applock.MarkAppUnlockedUseCase
import com.example.voicelock.domain.usecase.face.AnalyzeFaceFrameUseCase
import com.example.voicelock.domain.usecase.face.DeleteFaceProfileUseCase
import com.example.voicelock.domain.usecase.face.GetFaceSettingsUseCase
import com.example.voicelock.domain.usecase.face.InitFaceEngineUseCase
import com.example.voicelock.domain.usecase.face.VerifyOwnerFaceUseCase
import com.example.voicelock.util.Constant
import com.example.voicelock.util.Result
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.util.getDataOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-app face verification against the owner's enrolled embeddings. A scan succeeds once, on the
 * same continuously visible face, both happened: an open-eye frame matched the owner
 * ([Constant.FACE_MATCH_THRESHOLD]) and the user blinked (a photo cannot). Losing the face or
 * seeing several faces starts both checks over. A scan gives up after [Constant.FACE_AUTH_TIMEOUT_MS].
 *
 * Opened from Settings / Home it is a test screen; hosted by LockOverlayActivity with a target
 * package it marks that app unlocked, like the PIN / pattern / voice screens.
 */
class FaceAuthViewModel(
    private val initFaceEngineUseCase: InitFaceEngineUseCase,
    private val getFaceSettingsUseCase: GetFaceSettingsUseCase,
    private val analyzeFaceFrameUseCase: AnalyzeFaceFrameUseCase,
    private val verifyOwnerFaceUseCase: VerifyOwnerFaceUseCase,
    private val deleteFaceProfileUseCase: DeleteFaceProfileUseCase,
    private val markAppUnlockedUseCase: MarkAppUnlockedUseCase,
) : BaseViewModel<FaceAuthUiState, FaceAuthUiEvent>(FaceAuthUiState()) {

    private var targetPackageName: String? = null
    private val isAnalyzing = AtomicBoolean(false)
    private val blinkTracker = BlinkTracker()
    private var timeoutJob: Job? = null

    /** An open-eye frame of the current face matched the owner during this scan. */
    @Volatile
    private var hasMatched = false

    /** Model loaded and a face profile exists. */
    @Volatile
    private var isPrepared = false

    @Volatile
    private var isCameraGranted = false

    @Volatile
    private var isScreenStarted = false

    /** [targetPackageName] is set when unlocking a locked app from the lock overlay. */
    fun initData(targetPackageName: String?) {
        this.targetPackageName = targetPackageName?.takeIf { it.isNotBlank() }
        if (this.targetPackageName != null) setState { copy(isAppUnlock = true) }
        prepare()
    }

    fun onCameraPermissionResult(isGranted: Boolean) {
        isCameraGranted = isGranted
        onReadinessChanged()
    }

    fun onScreenStarted() {
        isScreenStarted = true
        onReadinessChanged()
    }

    /** The screen went to the background: a running scan is abandoned and restarts on return. */
    fun onScreenStopped() {
        isScreenStarted = false
        timeoutJob?.cancel()
        setState { if (stage == FaceAuthStage.SCANNING) copy(stage = FaceAuthStage.IDLE, hint = FaceHint.NONE) else this }
    }

    fun startScan() {
        if (!isPrepared || !isCameraGranted || !getCurrentState().canStartScan()) return
        resetChecks()
        setState { copy(stage = FaceAuthStage.SCANNING, hint = FaceHint.NONE, bestSimilarity = null) }
        timeoutJob?.cancel()
        timeoutJob =
            launchIO {
                delay(Constant.FACE_AUTH_TIMEOUT_MS)
                setState { if (stage == FaceAuthStage.SCANNING) copy(stage = FaceAuthStage.FAILED) else this }
            }
    }

    /** Called on the camera thread before a frame is converted: drops frames while busy. */
    fun isAcceptingFrames(): Boolean = getCurrentState().stage == FaceAuthStage.SCANNING && !isAnalyzing.get()

    fun onCameraFrame(bitmap: Bitmap) {
        if (getCurrentState().stage != FaceAuthStage.SCANNING) return
        if (!isAnalyzing.compareAndSet(false, true)) return
        launchIO(onError = { isAnalyzing.set(false) }) {
            try {
                analyzeAndVerify(bitmap)
            } finally {
                isAnalyzing.set(false)
            }
        }
    }

    fun deleteProfile() {
        timeoutJob?.cancel()
        launchIO {
            when (val result = deleteFaceProfileUseCase().first()) {
                is Result.Success -> {
                    isPrepared = false
                    setState { copy(stage = FaceAuthStage.NOT_ENROLLED, hint = FaceHint.NONE, bestSimilarity = null) }
                    setEvent(FaceAuthUiEvent.ProfileDeleted)
                }
                is Result.Error -> Timber.tag(TAG).e(result.error, "Deleting the face profile failed")
            }
        }
    }

    override fun onCleared() {
        timeoutJob?.cancel()
        super.onCleared()
    }

    private fun prepare() {
        // Back from (re-)enrollment: a previous "not enrolled" / engine error must not stick.
        setState {
            if (stage == FaceAuthStage.NOT_ENROLLED || stage == FaceAuthStage.ENGINE_ERROR) copy(stage = FaceAuthStage.INITIALIZING) else this
        }
        launchIO {
            val settings = getFaceSettingsUseCase().getDataOrDefault(FaceSettingsDtoModel())
            if (!settings.isEnrolled) {
                setState { copy(stage = FaceAuthStage.NOT_ENROLLED) }
                return@launchIO
            }
            val status = initFaceEngineUseCase().getDataOrDefault(FaceEngineStatus.INIT_FAILED)
            if (status != FaceEngineStatus.READY) {
                setState { copy(stage = FaceAuthStage.ENGINE_ERROR, engineStatus = status) }
                return@launchIO
            }
            isPrepared = true
            onReadinessChanged()
        }
    }

    /** Moves INITIALIZING / permission states to IDLE once everything is ready, then auto-scans. */
    private fun onReadinessChanged() {
        if (!isPrepared) return
        val waitingStages = setOf(FaceAuthStage.INITIALIZING, FaceAuthStage.CAMERA_PERMISSION_REQUIRED, FaceAuthStage.IDLE)
        setState {
            when {
                stage !in waitingStages -> this
                isCameraGranted -> copy(stage = FaceAuthStage.IDLE)
                else -> copy(stage = FaceAuthStage.CAMERA_PERMISSION_REQUIRED)
            }
        }
        // The user opened this screen (or a locked app) to be recognized, so scanning starts by itself.
        if (isScreenStarted && getCurrentState().stage == FaceAuthStage.IDLE) startScan()
    }

    private suspend fun analyzeAndVerify(bitmap: Bitmap) {
        // Once matched, only the blink is still needed: skip the embedding to keep frames fast.
        val analysis = analyzeFaceFrameUseCase(bitmap = bitmap, extractTemplate = !hasMatched).getDataOrNull() ?: return
        blinkTracker.onFrame(analysis)
        if (analysis !is FaceFrameAnalysis.Face) {
            if (analysis == FaceFrameAnalysis.NoFace || analysis == FaceFrameAnalysis.MultipleFaces) hasMatched = false
            setState { if (stage == FaceAuthStage.SCANNING) copy(hint = analysis.toHint()) else this }
            return
        }

        analysis.template?.let { template ->
            when (val result = verifyOwnerFaceUseCase(template).getDataOrNull()) {
                is FaceVerifyResult.Verified -> {
                    hasMatched = true
                    updateBestSimilarity(result.similarity)
                }
                is FaceVerifyResult.Rejected -> {
                    updateBestSimilarity(result.similarity)
                    setState { if (stage == FaceAuthStage.SCANNING) copy(hint = FaceHint.FACE_MISMATCH) else this }
                    return
                }
                FaceVerifyResult.NotEnrolled -> {
                    timeoutJob?.cancel()
                    isPrepared = false
                    setState { copy(stage = FaceAuthStage.NOT_ENROLLED) }
                    return
                }
                null -> return
            }
        }

        when {
            hasMatched && blinkTracker.hasBlinked -> onVerified()
            hasMatched -> setState { if (stage == FaceAuthStage.SCANNING) copy(hint = FaceHint.BLINK) else this }
            else -> setState { if (stage == FaceAuthStage.SCANNING) copy(hint = FaceHint.HOLD_STILL) else this }
        }
    }

    private fun updateBestSimilarity(similarity: Float) {
        setState {
            if (stage != FaceAuthStage.SCANNING) this else copy(bestSimilarity = maxOf(bestSimilarity ?: -1f, similarity))
        }
    }

    private suspend fun onVerified() {
        if (getCurrentState().stage != FaceAuthStage.SCANNING) return
        timeoutJob?.cancel()
        // Same session bookkeeping as a PIN/pattern unlock, so the monitor lets the app through.
        markAppUnlockedUseCase(targetPackageName).getDataOrNull()
        setState { copy(stage = FaceAuthStage.SUCCESS, hint = FaceHint.NONE) }
        setEvent(FaceAuthUiEvent.Authenticated(isAppUnlock = targetPackageName != null))
    }

    private fun resetChecks() {
        hasMatched = false
        blinkTracker.reset()
    }

    private companion object {
        const val TAG = "FaceAuthViewModel"
    }
}
