package com.example.voicelock.data.model.settings

import com.example.voicelock.data.model.lock.LockMethodUiEvent

sealed class SettingsUiEvent {
    data object ShowLoadError : SettingsUiEvent()
    data object NavigateToPermissions : SettingsUiEvent()
    /** Start / stop the floating lock button service to match the saved switch. */
    data class SyncFloatingLock(val isEnabled: Boolean) : SettingsUiEvent()
    /** The floating button needs "Display over other apps" first. */
    data object ShowFloatingLockNeedsOverlay : SettingsUiEvent()

    /** A lock-method row effect shared with Home. */
    data class LockMethod(val event: LockMethodUiEvent) : SettingsUiEvent()
}
