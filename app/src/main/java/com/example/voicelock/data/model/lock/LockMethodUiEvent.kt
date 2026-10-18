package com.example.voicelock.data.model.lock

import com.example.voicelock.data.model.biometric.BiometricAvailability
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.securityquestion.SecurityQuestionMode
import com.example.voicelock.data.model.voice.VoiceVerificationMode

/**
 * One-shot effects of the lock-method rows shared by Home and Settings (open a setup / test
 * screen, run the biometric prompt). Each screen wraps it in its own UiEvent.
 */
sealed class LockMethodUiEvent {
    data class NavigateToPinLock(val mode: PinLockMode, val pinLength: Int) : LockMethodUiEvent()
    data class NavigateToPatternLock(val mode: PatternLockMode) : LockMethodUiEvent()
    data class NavigateToSecurityQuestion(val mode: SecurityQuestionMode) : LockMethodUiEvent()

    /** [mode] UNLOCK: the voice passphrase; LOCK: the F1 lock command. */
    data class NavigateToVoiceEnrollment(val mode: VoiceVerificationMode) : LockMethodUiEvent()
    data object NavigateToVoiceTest : LockMethodUiEvent()

    /** F1: voice lock was just turned on; Android 13+ can offer to add the Quick Settings tile. */
    data object OfferVoiceLockTile : LockMethodUiEvent()
    data object NavigateToFaceEnrollment : LockMethodUiEvent()
    data object NavigateToFaceTest : LockMethodUiEvent()

    /** Turning a biometric on: confirm it once with the system prompt. */
    data class ConfirmBiometric(val type: BiometricType) : LockMethodUiEvent()
    data class TestBiometric(val type: BiometricType) : LockMethodUiEvent()
    data class OpenBiometricEnrollment(val type: BiometricType) : LockMethodUiEvent()
    data class ShowBiometricUnavailable(val type: BiometricType, val availability: BiometricAvailability) : LockMethodUiEvent()
}
