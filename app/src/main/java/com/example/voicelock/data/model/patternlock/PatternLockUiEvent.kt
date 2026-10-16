package com.example.voicelock.data.model.patternlock

sealed class PatternLockUiEvent {
    data class VerifyFailed(
        val remainingAttempts: Int,
    ) : PatternLockUiEvent()

    data object ConfirmMismatch : PatternLockUiEvent()

    data object TooShort : PatternLockUiEvent()

    data class Success(
        val mode: PatternLockMode,
    ) : PatternLockUiEvent()
}
