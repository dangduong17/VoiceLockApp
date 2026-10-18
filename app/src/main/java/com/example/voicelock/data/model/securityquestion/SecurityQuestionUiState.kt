package com.example.voicelock.data.model.securityquestion

enum class SecurityQuestionMode {
    /** First-time setup from Home / Settings. */
    SETUP,

    /** Change from Settings: answer the current question first. */
    CHANGE,

    /** Lock overlay fallback method: a correct answer unlocks the target app. */
    UNLOCK,

    /** "Quên PIN?" on the PIN change screen: a correct answer allows setting a new PIN. */
    RESET_PIN,
}

enum class SecurityQuestionStage {
    LOADING,
    VERIFY,
    SETUP,

    /** Verification was asked for but no question exists (never set up a new one here: that would bypass the lock). */
    NOT_SET,
}

enum class SecurityQuestionInputError {
    NONE,
    QUESTION_EMPTY,
    ANSWER_EMPTY,
    ANSWER_TOO_SHORT,
    ANSWER_MISMATCH,
}

data class SecurityQuestionUiState(
    val mode: SecurityQuestionMode = SecurityQuestionMode.SETUP,
    val stage: SecurityQuestionStage = SecurityQuestionStage.LOADING,
    /** The stored question, shown in the VERIFY stage. */
    val question: String = "",
    val inputError: SecurityQuestionInputError = SecurityQuestionInputError.NONE,
    val isBusy: Boolean = false,
    val isLocked: Boolean = false,
    val lockoutRemainingSeconds: Int = 0,
)

fun SecurityQuestionUiState.isInputEnabled(): Boolean = !isBusy && !isLocked

/** Title, visible section and button text all follow mode + stage + stored question. */
data class SecurityQuestionScreenInfo(
    val mode: SecurityQuestionMode,
    val stage: SecurityQuestionStage,
    val question: String,
)

fun SecurityQuestionUiState.getScreenInfo(): SecurityQuestionScreenInfo =
    SecurityQuestionScreenInfo(mode = mode, stage = stage, question = question)

data class SecurityQuestionLockout(
    val isLocked: Boolean,
    val remainingSeconds: Int,
)

fun SecurityQuestionUiState.getLockout(): SecurityQuestionLockout =
    SecurityQuestionLockout(isLocked = isLocked, remainingSeconds = lockoutRemainingSeconds)

sealed class SecurityQuestionUiEvent {
    data class VerifyFailed(
        val remainingAttempts: Int,
    ) : SecurityQuestionUiEvent()

    data class Saved(
        val mode: SecurityQuestionMode,
    ) : SecurityQuestionUiEvent()

    /** UNLOCK mode: the target app is marked unlocked, the overlay should close. */
    data object Unlocked : SecurityQuestionUiEvent()

    /** RESET_PIN mode: the answer was right, open PIN setup. */
    data object NavigateToPinSetup : SecurityQuestionUiEvent()
}
