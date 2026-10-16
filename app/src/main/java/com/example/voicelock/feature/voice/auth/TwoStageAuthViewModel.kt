package com.example.voicelock.feature.voice.auth

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.voice.AuthFailure
import com.example.voicelock.data.model.voice.AuthStage
import com.example.voicelock.data.model.voice.SpeechRecognitionOutcome
import com.example.voicelock.data.model.voice.TwoStageAuthUiEvent
import com.example.voicelock.data.model.voice.TwoStageAuthUiState
import com.example.voicelock.data.model.voice.VoiceKeywordProfile
import com.example.voicelock.data.model.voice.VoiceModelMissingException
import com.example.voicelock.data.model.voice.VoiceScores
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.model.voice.VoiceVerifyResult
import com.example.voicelock.data.model.voice.canStart
import com.example.voicelock.data.model.voice.getTexts
import com.example.voicelock.domain.usecase.applock.MarkAppUnlockedUseCase
import com.example.voicelock.domain.usecase.voice.GetVoiceKeywordProfileUseCase
import com.example.voicelock.domain.usecase.voice.IsSpeechToTextAvailableUseCase
import com.example.voicelock.domain.usecase.voice.IsVoiceEnrolledUseCase
import com.example.voicelock.domain.usecase.voice.MatchVoiceKeywordUseCase
import com.example.voicelock.domain.usecase.voice.RecognizeSpeechUseCase
import com.example.voicelock.domain.usecase.voice.RecordVoiceSampleUseCase
import com.example.voicelock.domain.usecase.voice.VerifyOwnerVoiceUseCase
import com.example.voicelock.domain.usecase.voice.WarmUpVoiceModelUseCase
import com.example.voicelock.util.Constant
import com.example.voicelock.util.Result
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.util.getDataOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.Locale

/**
 * Two-stage check on ONE recording — the user says the passphrase once:
 *  1. [RecognizeSpeechUseCase] turns the clip into text in the language the owner enrolled in, and
 *     [MatchVoiceKeywordUseCase] fuzzy-matches it against what was heard from the owner's own
 *     enrollment samples ([VoiceKeywordProfile]).
 *  2. [VerifyOwnerVoiceUseCase] runs Sherpa-ONNX speaker verification + MFCC/DTW phrase match.
 * Both stages run in parallel on the same samples; access needs both to pass. Stage 1 is skipped
 * only for profiles without an enrolled transcript on devices that cannot transcribe recorded audio;
 * Stage 2 then applies a stricter personal phrase threshold. A Stage-1 failure at unlock time never
 * downgrades to Stage 2 alone.
 */
class TwoStageAuthViewModel(
    private val recordVoiceSampleUseCase: RecordVoiceSampleUseCase,
    private val recognizeSpeechUseCase: RecognizeSpeechUseCase,
    private val isSpeechToTextAvailableUseCase: IsSpeechToTextAvailableUseCase,
    private val isVoiceEnrolledUseCase: IsVoiceEnrolledUseCase,
    private val warmUpVoiceModelUseCase: WarmUpVoiceModelUseCase,
    private val getVoiceKeywordProfileUseCase: GetVoiceKeywordProfileUseCase,
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

    /** The caller must hold RECORD_AUDIO (the screen checks it before calling). */
    fun startAuthentication() {
        if (!getCurrentState().canStart()) return
        setState { copy(stage = AuthStage.CAPTURING_VOICE, heardText = null, scores = null, failure = AuthFailure.NONE) }

        authJob?.cancel()
        authJob =
            launchIO(onError = { throwable -> fail(failure = AuthFailure.UNEXPECTED_ERROR, throwable = throwable) }) {
                val samples =
                    when (val recording = recordVoiceSampleUseCase(maxDurationMs = CAPTURE_MS).first()) {
                        is Result.Success -> recording.data
                        is Result.Error -> return@launchIO fail(failure = AuthFailure.UNEXPECTED_ERROR, throwable = recording.error)
                    }
                setState { copy(stage = AuthStage.VERIFYING) }

                val mode = getCurrentState().mode
                val keywordProfile = getVoiceKeywordProfileUseCase().getDataOrNull() ?: legacyKeywordProfile()
                val isSpeechToTextAvailable = isSpeechToTextAvailableUseCase().getDataOrDefault(false)
                // Decided up front from enrollment, never from a runtime failure: if the phrase was
                // transcribed at enrollment, Stage 1 must pass now (a recognizer error rejects).
                val isKeywordChecked = keywordProfile.transcripts.isNotEmpty() && isSpeechToTextAvailable
                val (keywordOutcome, verifyResult) =
                    coroutineScope {
                        // Also run when Stage 1 does not gate the attempt, so the user always sees what
                        // was heard; the outcome is then shown but not used for the decision.
                        val keyword = async { if (isSpeechToTextAvailable) recognizeKeyword(samples = samples, profile = keywordProfile) else null }
                        val verify =
                            async { verifyOwnerVoiceUseCase(samples = samples, mode = mode, isKeywordChecked = isKeywordChecked).first() }
                        keyword.await() to verify.await()
                    }

                keywordOutcome?.let { outcome -> setState { copy(heardText = outcome.getTexts().firstOrNull().orEmpty()) } }
                val keywordFailure =
                    if (isKeywordChecked && keywordOutcome != null) checkKeyword(outcome = keywordOutcome, profile = keywordProfile) else null
                when (verifyResult) {
                    is Result.Error -> fail(failure = AuthFailure.UNEXPECTED_ERROR, throwable = verifyResult.error)
                    is Result.Success ->
                        if (keywordFailure != null) {
                            // Scores are still shown so thresholds can be tuned from real attempts.
                            setState { copy(stage = AuthStage.FAILED, failure = keywordFailure, scores = verifyResult.data.getScores()) }
                        } else {
                            handleVerifyResult(verifyResult.data)
                        }
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

    override fun onCleared() {
        authJob?.cancel()
        super.onCleared()
    }

    /**
     * Profiles enrolled before the phrase was transcribed: system language + the built-in
     * "Unlock" / "Mở khóa" keywords. Re-enrolling records the owner's own phrase and language.
     */
    private fun legacyKeywordProfile() =
        VoiceKeywordProfile(languageTag = Locale.getDefault().toLanguageTag(), transcripts = Constant.VOICE_UNLOCK_KEYWORDS)

    private suspend fun recognizeKeyword(
        samples: FloatArray,
        profile: VoiceKeywordProfile,
    ): SpeechRecognitionOutcome =
        recognizeSpeechUseCase(samples = samples, languageTag = profile.languageTag, biasingStrings = profile.transcripts)
            .getDataOrDefault(SpeechRecognitionOutcome.Failed(errorCode = RECOGNIZER_CALL_FAILED))

    /** Returns null only when Stage 1 passes; any recognizer problem fails closed. */
    private suspend fun checkKeyword(
        outcome: SpeechRecognitionOutcome,
        profile: VoiceKeywordProfile,
    ): AuthFailure? =
        when (outcome) {
            is SpeechRecognitionOutcome.Candidates -> {
                val isMatch =
                    matchVoiceKeywordUseCase(candidates = outcome.texts, expectedPhrases = profile.transcripts).getDataOrDefault(false)
                if (isMatch) null else AuthFailure.KEYWORD_MISMATCH
            }
            is SpeechRecognitionOutcome.NoSpeech -> AuthFailure.KEYWORD_MISMATCH
            is SpeechRecognitionOutcome.Failed -> AuthFailure.RECOGNIZER_ERROR
            SpeechRecognitionOutcome.Unsupported -> {
                Timber.tag(TAG).w("Keyword stage expected but the recognizer is unavailable: rejecting")
                AuthFailure.RECOGNIZER_ERROR
            }
        }

    private fun VoiceVerifyResult.getScores(): VoiceScores? =
        when (this) {
            is VoiceVerifyResult.Verified -> scores
            is VoiceVerifyResult.Rejected -> scores
            VoiceVerifyResult.NoSpeechDetected -> null
            VoiceVerifyResult.NotEnrolled -> null
        }

    private suspend fun handleVerifyResult(result: VoiceVerifyResult) {
        when (result) {
            is VoiceVerifyResult.Verified -> {
                // Same session bookkeeping as a PIN/pattern unlock, so the monitor lets the app through.
                targetPackageName?.let { markAppUnlockedUseCase(it).first() }
                setState { copy(stage = AuthStage.SUCCESS, scores = result.scores) }
                setEvent(TwoStageAuthUiEvent.Authenticated(isAppUnlock = targetPackageName != null))
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

            VoiceVerifyResult.NoSpeechDetected -> fail(failure = AuthFailure.NO_SPEECH)
            VoiceVerifyResult.NotEnrolled -> setState { copy(stage = AuthStage.NOT_ENROLLED) }
        }
    }

    private fun fail(
        failure: AuthFailure,
        throwable: Throwable? = null,
    ) {
        throwable?.let { Timber.tag(TAG).e(it, "Two-stage voice authentication failed") }
        val resolvedFailure = if (throwable is VoiceModelMissingException) AuthFailure.MODEL_MISSING else failure
        setState { copy(stage = AuthStage.FAILED, failure = resolvedFailure) }
    }

    private companion object {
        const val TAG = "TwoStageAuthViewModel"

        /** A little longer than enrollment so a late start does not cut the phrase off. */
        const val CAPTURE_MS = 3_000L

        /** Error code used when the speech-to-text call itself fails (not an engine error code). */
        const val RECOGNIZER_CALL_FAILED = -1
    }
}
