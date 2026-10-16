package com.example.voicelock.feature.voice.auth

import android.annotation.SuppressLint
import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.voice.AuthFailure
import com.example.voicelock.data.model.voice.AuthStage
import com.example.voicelock.data.model.voice.TwoStageAuthUiEvent
import com.example.voicelock.data.model.voice.TwoStageAuthUiState
import com.example.voicelock.data.model.voice.VoiceScores
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.model.voice.VoiceVerifyResult
import com.example.voicelock.data.model.voice.canStart
import com.example.voicelock.domain.usecase.applock.MarkAppUnlockedUseCase
import com.example.voicelock.domain.usecase.voice.IsVoiceEnrolledUseCase
import com.example.voicelock.domain.usecase.voice.MatchVoiceKeywordUseCase
import com.example.voicelock.domain.usecase.voice.VerifyOwnerVoiceUseCase
import com.example.voicelock.domain.usecase.voice.WarmUpVoiceModelUseCase
import com.example.voicelock.util.Result
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.voice.AudioRecorderHelper
import com.example.voicelock.voice.KeywordAudioRecognizer
import com.example.voicelock.voice.VoiceModelMissingException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Two-stage check on ONE recording — the user says the passphrase once:
 *  1. [KeywordAudioRecognizer] feeds the clip to Android's SpeechRecognizer and
 *     [MatchVoiceKeywordUseCase] validates the text ("Unlock" / "Mở khóa").
 *  2. [VerifyOwnerVoiceUseCase] runs Sherpa-ONNX speaker verification + MFCC/DTW phrase match.
 * Both stages run in parallel on the same samples; access needs both to pass. On devices that
 * cannot recognize pre-recorded audio, Stage 1 is skipped and Stage 2 alone decides.
 */
class TwoStageAuthViewModel(
    private val audioRecorderHelper: AudioRecorderHelper,
    private val keywordAudioRecognizer: KeywordAudioRecognizer,
    private val isVoiceEnrolledUseCase: IsVoiceEnrolledUseCase,
    private val warmUpVoiceModelUseCase: WarmUpVoiceModelUseCase,
    private val matchVoiceKeywordUseCase: MatchVoiceKeywordUseCase,
    private val verifyOwnerVoiceUseCase: VerifyOwnerVoiceUseCase,
    private val markAppUnlockedUseCase: MarkAppUnlockedUseCase,
) : BaseViewModel<TwoStageAuthUiState, TwoStageAuthUiEvent>(TwoStageAuthUiState()) {

    private var authJob: Job? = null
    private var targetPackageName: String? = null

    /** [targetPackageName] is set when unlocking a locked app from the lock overlay. */
    fun initData(targetPackageName: String?) {
        this.targetPackageName = targetPackageName?.takeIf { it.isNotBlank() }
        if (this.targetPackageName != null) {
            setState { copy(isAppUnlock = true, mode = VoiceVerificationMode.UNLOCK) }
        }
        checkEnrollment()
    }

    fun checkEnrollment() {
        launchIO {
            val isEnrolled = isVoiceEnrolledUseCase().getDataOrDefault(false)
            setState { copy(stage = if (isEnrolled) AuthStage.IDLE else AuthStage.NOT_ENROLLED) }
            if (isEnrolled) warmUpVoiceModelUseCase().first()
        }
    }

    fun setMode(mode: VoiceVerificationMode) {
        val state = getCurrentState()
        if (state.canStart() && !state.isAppUnlock) setState { copy(mode = mode) }
    }

    /** The caller must hold RECORD_AUDIO (TwoStageAuthActivity checks it before calling). */
    @SuppressLint("MissingPermission")
    fun startAuthentication() {
        if (!getCurrentState().canStart()) return
        setState { copy(stage = AuthStage.CAPTURING_VOICE, recognizedText = "", scores = null, failure = AuthFailure.NONE) }

        authJob =
            launchIO(onError = { throwable -> fail(AuthFailure.UNEXPECTED_ERROR, throwable) }) {
                val samples = audioRecorderHelper.record(durationMs = CAPTURE_MS)
                setState { copy(stage = AuthStage.VERIFYING) }

                val mode = getCurrentState().mode
                val (keywordOutcome, verifyResult) =
                    coroutineScope {
                        val keyword = async { keywordAudioRecognizer.recognize(samples) }
                        val verify = async { verifyOwnerVoiceUseCase(samples, mode).first() }
                        keyword.await() to verify.await()
                    }

                val keywordFailure = checkKeyword(keywordOutcome)
                when {
                    verifyResult is Result.Error -> fail(AuthFailure.UNEXPECTED_ERROR, verifyResult.error)
                    keywordFailure != null -> {
                        // Scores are still shown so thresholds can be tuned from real attempts.
                        val scores = (verifyResult as Result.Success).data.scoresOrNull()
                        setState { copy(stage = AuthStage.FAILED, failure = keywordFailure, scores = scores) }
                    }
                    else -> handleVerifyResult((verifyResult as Result.Success).data)
                }
            }
    }

    /** Called when the screen goes to the background: a half-finished attempt is abandoned. */
    fun cancelAuthentication() {
        authJob?.cancel()
        authJob = null
        val stage = getCurrentState().stage
        if (stage == AuthStage.CAPTURING_VOICE || stage == AuthStage.VERIFYING) {
            setState { copy(stage = AuthStage.IDLE) }
        }
    }

    /** Returns null when Stage 1 passes (or is unavailable on this device). */
    private suspend fun checkKeyword(outcome: KeywordAudioRecognizer.Outcome): AuthFailure? =
        when (outcome) {
            is KeywordAudioRecognizer.Outcome.Candidates -> {
                setState { copy(recognizedText = outcome.texts.first()) }
                val isMatch = matchVoiceKeywordUseCase(outcome.texts).getDataOrDefault(false)
                if (isMatch) null else AuthFailure.KEYWORD_MISMATCH
            }
            KeywordAudioRecognizer.Outcome.NoSpeech -> AuthFailure.KEYWORD_MISMATCH
            is KeywordAudioRecognizer.Outcome.Failed -> AuthFailure.RECOGNIZER_ERROR
            KeywordAudioRecognizer.Outcome.Unsupported -> {
                Timber.w("Keyword stage unavailable on this device; deciding on speaker + phrase only")
                null
            }
        }

    private fun VoiceVerifyResult.scoresOrNull(): VoiceScores? =
        when (this) {
            is VoiceVerifyResult.Verified -> scores
            is VoiceVerifyResult.Rejected -> scores
            else -> null
        }

    private suspend fun handleVerifyResult(result: VoiceVerifyResult) {
        when (result) {
            is VoiceVerifyResult.Verified -> {
                // Same session bookkeeping as a PIN/pattern unlock, so the monitor lets the app through.
                targetPackageName?.let { markAppUnlockedUseCase(it).first() }
                setState { copy(stage = AuthStage.SUCCESS, scores = result.scores) }
                setEvent(TwoStageAuthUiEvent.Authenticated)
            }

            is VoiceVerifyResult.Rejected -> {
                val failure =
                    when {
                        !result.isSpeakerMatch && !result.isPhraseMatch -> AuthFailure.SPEAKER_AND_PHRASE_MISMATCH
                        !result.isSpeakerMatch -> AuthFailure.SPEAKER_MISMATCH
                        else -> AuthFailure.PHRASE_MISMATCH
                    }
                setState { copy(stage = AuthStage.FAILED, scores = result.scores, failure = failure) }
            }

            VoiceVerifyResult.NoSpeechDetected -> fail(AuthFailure.NO_SPEECH)
            VoiceVerifyResult.NotEnrolled -> setState { copy(stage = AuthStage.NOT_ENROLLED) }
        }
    }

    private fun fail(
        failure: AuthFailure,
        throwable: Throwable? = null,
    ) {
        throwable?.let { Timber.e(it, "Two-stage voice authentication failed") }
        val resolvedFailure = if (throwable is VoiceModelMissingException) AuthFailure.MODEL_MISSING else failure
        setState { copy(stage = AuthStage.FAILED, failure = resolvedFailure) }
    }

    private companion object {
        /** A little longer than enrollment so a late start does not cut the phrase off. */
        const val CAPTURE_MS = 3_000L
    }
}
