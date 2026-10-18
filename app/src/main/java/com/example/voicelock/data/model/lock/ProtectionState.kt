package com.example.voicelock.data.model.lock

import com.example.voicelock.data.model.biometric.BiometricAvailability
import com.example.voicelock.data.model.biometric.BiometricSettingsDtoModel
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.biometric.getAvailability
import com.example.voicelock.data.model.biometric.isActive
import com.example.voicelock.data.model.face.FaceSettingsDtoModel
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.securityquestion.SecurityQuestionMode
import com.example.voicelock.data.model.voice.VoiceVerificationMode

/**
 * The lock-method fields Home and Settings both store, so the rules for what a row tap or a
 * switch does live in one place (the extensions below) instead of in each ViewModel.
 */
interface ProtectionState {
    val isVoiceEnrolled: Boolean
    val isVoiceUnlockEnabled: Boolean
    val isVoiceLockEnabled: Boolean

    /** F1: the lock command has its own 3 samples. */
    val isVoiceLockCommandEnrolled: Boolean
    val isPinSet: Boolean
    val pinLength: Int
    val isPatternSet: Boolean
    val isSecurityQuestionSet: Boolean

    /** Null until loaded, so the fingerprint row is never shown on a device without a sensor (F4). */
    val biometricSettings: BiometricSettingsDtoModel?
    val faceSettings: FaceSettingsDtoModel
}

fun ProtectionState.getPinLockEvent(): LockMethodUiEvent =
    LockMethodUiEvent.NavigateToPinLock(mode = if (isPinSet) PinLockMode.CHANGE else PinLockMode.SETUP, pinLength = pinLength)

fun ProtectionState.getPatternLockEvent(): LockMethodUiEvent =
    LockMethodUiEvent.NavigateToPatternLock(if (isPatternSet) PatternLockMode.CHANGE else PatternLockMode.SETUP)

/** Changing an existing question first asks it (CHANGE mode), like changing the PIN. */
fun ProtectionState.getSecurityQuestionEvent(): LockMethodUiEvent =
    LockMethodUiEvent.NavigateToSecurityQuestion(if (isSecurityQuestionSet) SecurityQuestionMode.CHANGE else SecurityQuestionMode.SETUP)

/** Enrolled: test the voice; otherwise record it. */
fun ProtectionState.getVoiceUnlockRowEvent(): LockMethodUiEvent =
    if (isVoiceEnrolled) {
        LockMethodUiEvent.NavigateToVoiceTest
    } else {
        LockMethodUiEvent.NavigateToVoiceEnrollment(VoiceVerificationMode.UNLOCK)
    }

/**
 * Voice switches only make sense with an enrolled voice: turning one on before enrolling opens
 * enrollment instead (the screen snaps the switch back). Null: the switch may change.
 */
fun ProtectionState.getVoiceUnlockEnrollmentEvent(): LockMethodUiEvent? =
    if (isVoiceEnrolled) null else LockMethodUiEvent.NavigateToVoiceEnrollment(VoiceVerificationMode.UNLOCK)

/** F1: the lock command is its own recording, separate from the unlock passphrase. */
fun ProtectionState.getVoiceLockEnrollmentEvent(): LockMethodUiEvent? =
    if (isVoiceLockCommandEnrolled) null else LockMethodUiEvent.NavigateToVoiceEnrollment(VoiceVerificationMode.LOCK)

/** Without a face profile this opens enrollment; otherwise the face test screen (re-enroll / delete). */
fun ProtectionState.getFaceRowEvent(): LockMethodUiEvent =
    if (faceSettings.isEnrolled) LockMethodUiEvent.NavigateToFaceTest else LockMethodUiEvent.NavigateToFaceEnrollment

/** Same rule as voice: turning face unlock on before enrolling opens face enrollment. */
fun ProtectionState.getFaceEnrollmentEvent(): LockMethodUiEvent? =
    if (faceSettings.isEnrolled) null else LockMethodUiEvent.NavigateToFaceEnrollment

/**
 * Turning a biometric on is confirmed with the system prompt first; without an enrolled
 * biometric it opens the Android enrollment screen instead.
 */
fun BiometricSettingsDtoModel.getEnableBiometricEvent(type: BiometricType): LockMethodUiEvent =
    when (val availability = getAvailability(type)) {
        BiometricAvailability.AVAILABLE -> LockMethodUiEvent.ConfirmBiometric(type)
        BiometricAvailability.NOT_ENROLLED -> LockMethodUiEvent.OpenBiometricEnrollment(type)
        BiometricAvailability.NO_HARDWARE, BiometricAvailability.UNAVAILABLE ->
            LockMethodUiEvent.ShowBiometricUnavailable(type = type, availability = availability)
    }

/** Once on, tapping the row just tests the prompt; otherwise it starts turning it on. */
fun BiometricSettingsDtoModel.getBiometricRowEvent(type: BiometricType): LockMethodUiEvent =
    if (isActive(type)) LockMethodUiEvent.TestBiometric(type) else getEnableBiometricEvent(type)
