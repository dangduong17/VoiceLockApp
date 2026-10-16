package com.example.voicelock.data.model.patternlock

sealed class PatternVerifyResult {
    data object Success : PatternVerifyResult()

    data class Failed(
        val remainingAttempts: Int,
    ) : PatternVerifyResult()

    data class Locked(
        val remainingSeconds: Int,
    ) : PatternVerifyResult()
}
