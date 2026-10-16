package com.example.voicelock.data.model.settings

sealed class SettingsUiEvent {
    data class ShowToast(val message: String) : SettingsUiEvent()
    data object NavigateToHome : SettingsUiEvent()
    data object NavigateToVoiceCommands : SettingsUiEvent()
    data object NavigateToPermissions : SettingsUiEvent()
    data object NavigateToPinSetup : SettingsUiEvent()
    data object NavigateToPatternSetup : SettingsUiEvent()
    data object NavigateToFingerprintSetup : SettingsUiEvent()
    data object NavigateToSecurityQuestions : SettingsUiEvent()
    data object NavigateToThemeLanguage : SettingsUiEvent()
    data object NavigateToHelpFeedback : SettingsUiEvent()
    data object NavigateToAbout : SettingsUiEvent()
}
