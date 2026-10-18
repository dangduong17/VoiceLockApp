package com.example.voicelock.data.model.home

import com.example.voicelock.data.model.biometric.BiometricAvailability
import com.example.voicelock.data.model.lock.DeviceLockResult
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.model.securityquestion.SecurityQuestionMode

sealed class HomeUiEvent {
    data class LockNowDone(val result: DeviceLockResult) : HomeUiEvent()
    /** [message] null: the screen shows its generic error text. */
    data class ShowLoadError(val message: String?) : HomeUiEvent()
    data class NavigateToPinLock(val mode: PinLockMode, val pinLength: Int) : HomeUiEvent()
    data class NavigateToPatternLock(val mode: PatternLockMode) : HomeUiEvent()
    data class NavigateToSecurityQuestion(val mode: SecurityQuestionMode) : HomeUiEvent()
    data object NavigateToAppLock : HomeUiEvent()
    /** [mode] UNLOCK: the voice passphrase; LOCK: the F1 lock command. */
    data class NavigateToVoiceEnrollment(val mode: VoiceVerificationMode) : HomeUiEvent()
    /** F1: voice lock was just turned on; Android 13+ can offer to add the Quick Settings tile. */
    data object OfferVoiceLockTile : HomeUiEvent()
    data object NavigateToVoiceTest : HomeUiEvent()
    data object NavigateToFaceEnrollment : HomeUiEvent()
    data object NavigateToFaceTest : HomeUiEvent()

    /** Turning a biometric on: confirm it once with the system prompt. */
    data class ConfirmBiometric(val type: BiometricType) : HomeUiEvent()
    data class TestBiometric(val type: BiometricType) : HomeUiEvent()
    data class OpenBiometricEnrollment(val type: BiometricType) : HomeUiEvent()
    data class ShowBiometricUnavailable(val type: BiometricType, val availability: BiometricAvailability) : HomeUiEvent()
}
