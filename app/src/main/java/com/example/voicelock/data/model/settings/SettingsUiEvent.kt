package com.example.voicelock.data.model.settings

import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode

sealed class SettingsUiEvent {
    data class ShowToast(val message: String) : SettingsUiEvent()
    data object NavigateToHome : SettingsUiEvent()
    data object NavigateToVoiceCommands : SettingsUiEvent()
    data object NavigateToPermissions : SettingsUiEvent()
    data class NavigateToPinLock(val mode: PinLockMode, val pinLength: Int) : SettingsUiEvent()
    data class NavigateToPatternLock(val mode: PatternLockMode) : SettingsUiEvent()
    data object NavigateToFingerprintSetup : SettingsUiEvent()
    data object NavigateToSecurityQuestions : SettingsUiEvent()
    data object NavigateToThemeLanguage : SettingsUiEvent()
    data object NavigateToHelpFeedback : SettingsUiEvent()
    data object NavigateToAbout : SettingsUiEvent()
    data object NavigateToAppLock : SettingsUiEvent()
}
