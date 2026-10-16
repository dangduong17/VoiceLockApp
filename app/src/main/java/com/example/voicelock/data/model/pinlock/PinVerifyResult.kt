package com.example.voicelock.data.model.pinlock

sealed class PinVerifyResult {
    data object Success : PinVerifyResult()

    data class Failed(
        val remainingAttempts: Int,
    ) : PinVerifyResult()

    data class Locked(
        val remainingSeconds: Int,
    ) : PinVerifyResult()
}
