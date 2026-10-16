package com.example.voicelock.data.model.home

import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode

sealed class HomeUiEvent {
    data object LockNow : HomeUiEvent()
    data class ShowToast(val message: String) : HomeUiEvent()
    data class NavigateToPinLock(val mode: PinLockMode, val pinLength: Int) : HomeUiEvent()
    data class NavigateToPatternLock(val mode: PatternLockMode) : HomeUiEvent()
    data object NavigateToFingerprintSetup : HomeUiEvent()
    data object NavigateToSecurityQuestions : HomeUiEvent()
    data object NavigateToAppLock : HomeUiEvent()
    data object NavigateToVoiceEnrollment : HomeUiEvent()
    data object NavigateToVoiceTest : HomeUiEvent()
}
