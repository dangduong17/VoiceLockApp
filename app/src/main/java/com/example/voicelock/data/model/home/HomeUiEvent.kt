package com.example.voicelock.data.model.home

import com.example.voicelock.data.model.biometric.BiometricAvailability
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode

sealed class HomeUiEvent {
    data object LockNow : HomeUiEvent()
    data class ShowToast(val message: String) : HomeUiEvent()
    data class NavigateToPinLock(val mode: PinLockMode, val pinLength: Int) : HomeUiEvent()
    data class NavigateToPatternLock(val mode: PatternLockMode) : HomeUiEvent()
    data object NavigateToSecurityQuestions : HomeUiEvent()
    data object NavigateToAppLock : HomeUiEvent()
    data object NavigateToVoiceEnrollment : HomeUiEvent()
    data object NavigateToVoiceTest : HomeUiEvent()
    data object NavigateToFaceEnrollment : HomeUiEvent()
    data object NavigateToFaceTest : HomeUiEvent()

    /** Turning a biometric on: confirm it once with the system prompt. */
    data class ConfirmBiometric(val type: BiometricType) : HomeUiEvent()
    data class TestBiometric(val type: BiometricType) : HomeUiEvent()
    data class OpenBiometricEnrollment(val type: BiometricType) : HomeUiEvent()
    data class ShowBiometricUnavailable(val type: BiometricType, val availability: BiometricAvailability) : HomeUiEvent()
}
