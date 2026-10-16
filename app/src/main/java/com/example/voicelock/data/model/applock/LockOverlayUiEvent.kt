package com.example.voicelock.data.model.applock

import com.example.voicelock.data.model.lock.LockMethod

sealed class LockOverlayUiEvent {
    data class ShowMethod(
        val method: LockMethod,
        val pinLength: Int,
    ) : LockOverlayUiEvent()

    data object NothingConfigured : LockOverlayUiEvent()
}
