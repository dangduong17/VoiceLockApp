package com.example.voicelock.data.model.securityquestion

/** The stored question (the answer itself is only kept as a salted hash). */
data class SecurityQuestionDtoModel(
    val question: String,
)

sealed class SecurityAnswerVerifyResult {
    data object Success : SecurityAnswerVerifyResult()

    data class Failed(
        val remainingAttempts: Int,
    ) : SecurityAnswerVerifyResult()

    data class Locked(
        val remainingSeconds: Int,
    ) : SecurityAnswerVerifyResult()

    data object NotSet : SecurityAnswerVerifyResult()
}
