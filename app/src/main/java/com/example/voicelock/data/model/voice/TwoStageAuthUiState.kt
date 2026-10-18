package com.example.voicelock.data.model.voice

import com.example.voicelock.data.model.lock.DeviceLockResult

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
    /** F1: opened by VoiceCommandActivity; mode is fixed to LOCK and success locks the phone. */
    val isLockCommand: Boolean = false,
    val mode: VoiceVerificationMode = VoiceVerificationMode.UNLOCK,
    /** What the recognizer heard in the last attempt: null = not recognized yet, "" = nothing understood. */
    val heardText: String? = null,
    val scores: VoiceScores? = null,
    val failure: AuthFailure = AuthFailure.NONE,
)

/** Which variant of the screen is shown (test screen, lock overlay, F1 lock command). */
enum class AuthScreenKind { TEST, APP_UNLOCK, LOCK_COMMAND }

fun TwoStageAuthUiState.getScreenKind(): AuthScreenKind =
    when {
        isLockCommand -> AuthScreenKind.LOCK_COMMAND
        isAppUnlock -> AuthScreenKind.APP_UNLOCK
        else -> AuthScreenKind.TEST
    }

/** The lock overlay and the lock command start listening by themselves. */
fun TwoStageAuthUiState.isAutoListen(): Boolean = isAppUnlock || isLockCommand

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
    /** LOCK: texts talk about the F1 lock command instead of the unlock passphrase. */
    val mode: VoiceVerificationMode,
)

fun TwoStageAuthUiState.getStageInfo(): AuthStageInfo = AuthStageInfo(stage = stage, failure = failure, mode = mode)

sealed class TwoStageAuthUiEvent {
    /** [isAppUnlock]: the screen was opened by the lock overlay and should close it. */
    data class Authenticated(
        val isAppUnlock: Boolean,
    ) : TwoStageAuthUiEvent()

    /** F1: the owner's lock command was verified and the phone (or the locked apps) got locked. */
    data class DeviceLocked(
        val result: DeviceLockResult,
    ) : TwoStageAuthUiEvent()
}
