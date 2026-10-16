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
    val recognizedText: String = "",
    val scores: VoiceScores? = null,
    val failure: AuthFailure = AuthFailure.NONE,
)

fun TwoStageAuthUiState.canStart(): Boolean =
    stage == AuthStage.IDLE || stage == AuthStage.SUCCESS || stage == AuthStage.FAILED

sealed interface TwoStageAuthUiEvent {
    data object Authenticated : TwoStageAuthUiEvent
}
