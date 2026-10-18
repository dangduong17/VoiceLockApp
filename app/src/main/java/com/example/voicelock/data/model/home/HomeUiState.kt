package com.example.voicelock.data.model.home

import com.example.voicelock.data.model.biometric.BiometricSettingsDtoModel
import com.example.voicelock.data.model.face.FaceSettingsDtoModel

/** Source data only; every text the screen shows is built from it in HomeFragmentEx. */
data class HomeUiState(
    val isProtectionEnabled: Boolean = true,
    val isVoiceEnrolled: Boolean = false,
    val voiceSamplesCount: Int = 0,
    val isBackupPinEnabled: Boolean = true,
    val isVoiceUnlockEnabled: Boolean = false,
    val isVoiceLockEnabled: Boolean = false,
    /** F1: the lock command has its own 3 samples. */
    val isVoiceLockCommandEnrolled: Boolean = false,
    val voiceLockPhrase: String = "",
    val isPinSet: Boolean = false,
    val pinLength: Int = 6,
    val isPatternSet: Boolean = false,
    val isSecurityQuestionSet: Boolean = false,
    /** Null until loaded, so the fingerprint row is never shown on a device without a sensor (F4). */
    val biometricSettings: BiometricSettingsDtoModel? = null,
    val faceSettings: FaceSettingsDtoModel = FaceSettingsDtoModel(),
)

/** Everything the voice card and the voice rows render, so it always changes together. */
data class HomeVoiceInfo(
    val isVoiceEnrolled: Boolean,
    val voiceSamplesCount: Int,
    val isBackupPinEnabled: Boolean,
    val isVoiceUnlockEnabled: Boolean,
    val isVoiceLockEnabled: Boolean,
    val isVoiceLockCommandEnrolled: Boolean,
    val voiceLockPhrase: String,
)

fun HomeUiState.getVoiceInfo(): HomeVoiceInfo =
    HomeVoiceInfo(
        isVoiceEnrolled = isVoiceEnrolled,
        voiceSamplesCount = voiceSamplesCount,
        isBackupPinEnabled = isBackupPinEnabled,
        isVoiceUnlockEnabled = isVoiceUnlockEnabled,
        isVoiceLockEnabled = isVoiceLockEnabled,
        isVoiceLockCommandEnrolled = isVoiceLockCommandEnrolled,
        voiceLockPhrase = voiceLockPhrase,
    )

/** PIN / pattern rows. */
data class HomeLockInfo(
    val isPinSet: Boolean,
    val pinLength: Int,
    val isPatternSet: Boolean,
    val isSecurityQuestionSet: Boolean,
)

fun HomeUiState.getLockInfo(): HomeLockInfo = HomeLockInfo(isPinSet = isPinSet, pinLength = pinLength, isPatternSet = isPatternSet, isSecurityQuestionSet = isSecurityQuestionSet)
