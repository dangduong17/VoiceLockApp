package com.example.voicelock.data.model.pinlock

sealed class PinLockUiEvent {
    data class VerifyFailed(
        val remainingAttempts: Int,
    ) : PinLockUiEvent()

    data object ConfirmMismatch : PinLockUiEvent()

    data object Success : PinLockUiEvent()
}
