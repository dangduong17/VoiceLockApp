package com.example.voicelock.data.model.settings

import com.example.voicelock.data.model.biometric.BiometricAvailability
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.model.securityquestion.SecurityQuestionMode

sealed class SettingsUiEvent {
    /** [message] null: the screen shows its generic error text. */
    data class ShowLoadError(val message: String?) : SettingsUiEvent()
    data object NavigateToHome : SettingsUiEvent()
    data object NavigateToPermissions : SettingsUiEvent()
    data class NavigateToPinLock(val mode: PinLockMode, val pinLength: Int) : SettingsUiEvent()
    data class NavigateToPatternLock(val mode: PatternLockMode) : SettingsUiEvent()
    data class NavigateToSecurityQuestion(val mode: SecurityQuestionMode) : SettingsUiEvent()
    data object NavigateToThemeLanguage : SettingsUiEvent()
    data object NavigateToHelpFeedback : SettingsUiEvent()
    data object NavigateToAbout : SettingsUiEvent()
    data object NavigateToAppLock : SettingsUiEvent()
    /** [mode] UNLOCK: the voice passphrase; LOCK: the F1 lock command. */
    data class NavigateToVoiceEnrollment(val mode: VoiceVerificationMode) : SettingsUiEvent()
    /** F1: voice lock was just turned on; Android 13+ can offer to add the Quick Settings tile. */
    data object OfferVoiceLockTile : SettingsUiEvent()
    data object NavigateToVoiceTest : SettingsUiEvent()
    data object NavigateToFaceEnrollment : SettingsUiEvent()
    data object NavigateToFaceTest : SettingsUiEvent()

    /** Turning a biometric on: confirm it once with the system prompt. */
    data class ConfirmBiometric(val type: BiometricType) : SettingsUiEvent()
    data class TestBiometric(val type: BiometricType) : SettingsUiEvent()
    data class OpenBiometricEnrollment(val type: BiometricType) : SettingsUiEvent()
    data class ShowBiometricUnavailable(val type: BiometricType, val availability: BiometricAvailability) : SettingsUiEvent()
}
