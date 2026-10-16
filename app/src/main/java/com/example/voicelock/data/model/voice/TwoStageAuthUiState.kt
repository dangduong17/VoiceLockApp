package com.example.voicelock.data.model.voice

enum class AuthStage {
    CHECKING_ENROLLMENT,
    NOT_ENROLLED,
    IDLE,
    CAPTURING_VOICE, // single AudioRecord capture of the passphrase
    VERIFYING, // keyword (SpeechRecognizer) + Sherpa-ONNX + DTW on that capture
    SUCCESS,
    FAILED,
}

enum class AuthFailure {
    NONE,
    KEYWORD_MISMATCH,
    RECOGNIZER_ERROR,
    NO_SPEECH,
    SPEAKER_MISMATCH,
    PHRASE_MISMATCH,
    SPEAKER_AND_PHRASE_MISMATCH,
    MODEL_MISSING,
    UNEXPECTED_ERROR,
}

data class TwoStageAuthUiState(
    val stage: AuthStage = AuthStage.CHECKING_ENROLLMENT,
    /** True when started by LockOverlayActivity to unlock another app (mode is then fixed to UNLOCK). */
    val isAppUnlock: Boolean = false,
    val mode: VoiceVerificationMode = VoiceVerificationMode.UNLOCK,
    /** What the recognizer heard in the last attempt: null = not recognized yet, "" = nothing understood. */
    val heardText: String? = null,
    val scores: VoiceScores? = null,
    val failure: AuthFailure = AuthFailure.NONE,
)

fun TwoStageAuthUiState.canStart(): Boolean =
    stage == AuthStage.IDLE || stage == AuthStage.SUCCESS || stage == AuthStage.FAILED

/** Capturing or verifying (or still loading): the start button and mode toggle are locked. */
fun AuthStage.isBusy(): Boolean =
    when (this) {
        AuthStage.CHECKING_ENROLLMENT, AuthStage.CAPTURING_VOICE, AuthStage.VERIFYING -> true
        AuthStage.NOT_ENROLLED, AuthStage.IDLE, AuthStage.SUCCESS, AuthStage.FAILED -> false
    }

/** Stage and failure always render together (the failure text replaces the stage text). */
data class AuthStageInfo(
    val stage: AuthStage,
    val failure: AuthFailure,
)

fun TwoStageAuthUiState.getStageInfo(): AuthStageInfo = AuthStageInfo(stage = stage, failure = failure)

sealed class TwoStageAuthUiEvent {
    /** [isAppUnlock]: the screen was opened by the lock overlay and should close it. */
    data class Authenticated(
        val isAppUnlock: Boolean,
    ) : TwoStageAuthUiEvent()
}
