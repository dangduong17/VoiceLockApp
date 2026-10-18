package com.example.voicelock.data.model.home

import com.example.voicelock.data.model.lock.DeviceLockResult
import com.example.voicelock.data.model.lock.LockMethodUiEvent

sealed class HomeUiEvent {
    data class LockNowDone(val result: DeviceLockResult) : HomeUiEvent()
    data object ShowLoadError : HomeUiEvent()

    /** A lock-method row effect shared with Settings. */
    data class LockMethod(val event: LockMethodUiEvent) : HomeUiEvent()
}
