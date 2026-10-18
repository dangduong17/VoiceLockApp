package com.example.voicelock.feature.face.enrollment

import android.graphics.Bitmap
import android.os.SystemClock
import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.face.BlinkTracker
import com.example.voicelock.data.model.face.FaceEngineStatus
import com.example.voicelock.data.model.face.FaceEnrollmentError
import com.example.voicelock.data.model.face.FaceEnrollmentPhase
import com.example.voicelock.data.model.face.FaceEnrollmentResult
import com.example.voicelock.data.model.face.FaceEnrollmentUiState
import com.example.voicelock.data.model.face.FaceFrameAnalysis
import com.example.voicelock.data.model.face.FaceHint
import com.example.voicelock.data.model.face.toHint
import com.example.voicelock.domain.usecase.face.AnalyzeFaceFrameUseCase
import com.example.voicelock.domain.usecase.face.EnrollOwnerFaceUseCase
import com.example.voicelock.domain.usecase.face.InitFaceEngineUseCase
import com.example.voicelock.domain.usecase.face.SetFaceUnlockEnabledUseCase
import com.example.voicelock.util.Constant
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.util.getDataOrNull
import com.example.voicelock.util.onError
import com.example.voicelock.util.onSuccess
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-app face enrollment: while the camera runs, every frame with exactly one frontal,
 * close-enough face with open eyes yields an embedding (taken a little apart in time). Once
 * [FaceEnrollmentUiState.requiredCount] samples are in and the user has blinked once (so the
 * profile is not built from a photo), the samples are checked against each other and saved via
 * [EnrollOwnerFaceUseCase].
 *
 * Embeddings are kept here rather than in the UiState: they are large and must not be diffed.
 */
class FaceEnrollmentViewModel(
    private val initFaceEngineUseCase: InitFaceEngineUseCase,
    private val analyzeFaceFrameUseCase: AnalyzeFaceFrameUseCase,
    private val enrollOwnerFaceUseCase: EnrollOwnerFaceUseCase,
    private val setFaceUnlockEnabledUseCase: SetFaceUnlockEnabledUseCase,
) : BaseViewModel<FaceEnrollmentUiState, Nothing>(FaceEnrollmentUiState()) {

    private val templates = mutableListOf<FloatArray>()
    private val blinkTracker = BlinkTracker()
    private val isAnalyzing = AtomicBoolean(false)
    private var lastSampleAtMs = 0L

    @Volatile
    private var isEngineReady = false

    @Volatile
    private var isCameraGranted = false

    init {
        launchIO {
            val status = initFaceEngineUseCase().getDataOrDefault(FaceEngineStatus.INIT_FAILED)
            if (status != FaceEngineStatus.READY) {
                setState { copy(phase = FaceEnrollmentPhase.ENGINE_ERROR, engineStatus = status) }
                return@launchIO
            }
            isEngineReady = true
            updateReadiness()
        }
    }

    fun onCameraPermissionResult(isGranted: Boolean) {
        isCameraGranted = isGranted
        updateReadiness()
    }

    /** Called on the camera thread before a frame is converted: drops frames while busy. */
    fun isAcceptingFrames(): Boolean = getCurrentState().phase == FaceEnrollmentPhase.CAPTURING && !isAnalyzing.get()

    fun onCameraFrame(bitmap: Bitmap) {
        if (getCurrentState().phase != FaceEnrollmentPhase.CAPTURING) return
        if (!isAnalyzing.compareAndSet(false, true)) return
        // Every frame feeds the blink check; an embedding is only computed when a sample is due.
        val isSampleDue =
            templates.size < getCurrentState().requiredCount &&
                SystemClock.elapsedRealtime() - lastSampleAtMs >= Constant.FACE_ENROLLMENT_SAMPLE_INTERVAL_MS
        launchIO(onError = { isAnalyzing.set(false) }) {
            try {
                analyzeFaceFrameUseCase(bitmap = bitmap, extractTemplate = isSampleDue).getDataOrNull()?.let { handleAnalysis(it) }
            } finally {
                isAnalyzing.set(false)
            }
        }
    }

    fun restartEnrollment() {
        val phase = getCurrentState().phase
        if (phase == FaceEnrollmentPhase.CAPTURING || phase == FaceEnrollmentPhase.COMPLETED) {
            resetSamples(FaceEnrollmentError.NONE)
        }
    }

    private fun updateReadiness() {
        if (!isEngineReady) return
        setState {
            when (phase) {
                FaceEnrollmentPhase.INITIALIZING, FaceEnrollmentPhase.CAMERA_PERMISSION_REQUIRED ->
                    copy(phase = if (isCameraGranted) FaceEnrollmentPhase.CAPTURING else FaceEnrollmentPhase.CAMERA_PERMISSION_REQUIRED)
                FaceEnrollmentPhase.ENGINE_ERROR, FaceEnrollmentPhase.CAPTURING, FaceEnrollmentPhase.PROCESSING,
                FaceEnrollmentPhase.COMPLETED,
                -> this
            }
        }
    }

    private suspend fun handleAnalysis(analysis: FaceFrameAnalysis) {
        if (getCurrentState().phase != FaceEnrollmentPhase.CAPTURING) return
        blinkTracker.onFrame(analysis)
        if (analysis !is FaceFrameAnalysis.Face) {
            setState { copy(hint = analysis.toHint()) }
            return
        }
        analysis.template?.let {
            templates += it
            lastSampleAtMs = SystemClock.elapsedRealtime()
        }
        val count = templates.size
        val hasAllSamples = count >= getCurrentState().requiredCount
        val hint = if (hasAllSamples && !blinkTracker.hasBlinked) FaceHint.BLINK else FaceHint.HOLD_STILL
        setState { copy(capturedCount = count, hint = hint, error = FaceEnrollmentError.NONE) }
        if (hasAllSamples && blinkTracker.hasBlinked) enroll()
    }

    private suspend fun enroll() {
        setState { copy(phase = FaceEnrollmentPhase.PROCESSING, hint = FaceHint.NONE) }
        enrollOwnerFaceUseCase(templates.toList()).first()
            .onSuccess { result ->
                when (result) {
                    is FaceEnrollmentResult.Success -> {
                        // Enrolling is how the user opts in, so face unlock becomes active right away.
                        setFaceUnlockEnabledUseCase(true).first()
                        setState { copy(phase = FaceEnrollmentPhase.COMPLETED) }
                    }
                    FaceEnrollmentResult.InconsistentSamples -> resetSamples(FaceEnrollmentError.INCONSISTENT_SAMPLES)
                }
            }.onError { throwable ->
                Timber.tag(TAG).e(throwable, "Face enrollment failed")
                resetSamples(FaceEnrollmentError.PROCESSING_FAILED)
            }
    }

    private fun resetSamples(error: FaceEnrollmentError) {
        templates.clear()
        blinkTracker.reset()
        lastSampleAtMs = 0L
        setState { copy(phase = FaceEnrollmentPhase.CAPTURING, capturedCount = 0, hint = FaceHint.NONE, error = error) }
    }

    private companion object {
        const val TAG = "FaceEnrollmentViewModel"
    }
}
