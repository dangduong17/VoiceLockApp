package com.example.voicelock.data.model.settings

import com.example.voicelock.data.model.biometric.BiometricAvailability
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode

sealed class SettingsUiEvent {
    /** [message] null: the screen shows its generic error text. */
    data class ShowLoadError(val message: String?) : SettingsUiEvent()
    data object NavigateToHome : SettingsUiEvent()
    data object NavigateToPermissions : SettingsUiEvent()
    data class NavigateToPinLock(val mode: PinLockMode, val pinLength: Int) : SettingsUiEvent()
    data class NavigateToPatternLock(val mode: PatternLockMode) : SettingsUiEvent()
    data object NavigateToSecurityQuestions : SettingsUiEvent()
    data object NavigateToThemeLanguage : SettingsUiEvent()
    data object NavigateToHelpFeedback : SettingsUiEvent()
    data object NavigateToAbout : SettingsUiEvent()
    data object NavigateToAppLock : SettingsUiEvent()
    data object NavigateToVoiceEnrollment : SettingsUiEvent()
    data object NavigateToVoiceTest : SettingsUiEvent()
    data object NavigateToFaceEnrollment : SettingsUiEvent()
    data object NavigateToFaceTest : SettingsUiEvent()

    /** Turning a biometric on: confirm it once with the system prompt. */
    data class ConfirmBiometric(val type: BiometricType) : SettingsUiEvent()
    data class TestBiometric(val type: BiometricType) : SettingsUiEvent()
    data class OpenBiometricEnrollment(val type: BiometricType) : SettingsUiEvent()
    data class ShowBiometricUnavailable(val type: BiometricType, val availability: BiometricAvailability) : SettingsUiEvent()
}
