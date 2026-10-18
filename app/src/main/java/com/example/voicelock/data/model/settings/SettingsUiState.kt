package com.example.voicelock.data.model.settings

import com.example.voicelock.data.model.biometric.BiometricSettingsDtoModel
import com.example.voicelock.data.model.face.FaceSettingsDtoModel

/** Source data only; every text the screen shows is built from it in SettingsFragmentEx. */
data class SettingsUiState(
    val isVoiceEnrolled: Boolean = false,
    val isVoiceUnlockEnabled: Boolean = false,
    val isVoiceLockEnabled: Boolean = false,
    /** F1: the lock command has its own 3 samples. */
    val isVoiceLockCommandEnrolled: Boolean = false,
    val voiceLockPhrase: String = "",
    val voiceSamplesCount: Int = 0,
    /** Null until loaded, so the fingerprint row is never shown on a device without a sensor (F4). */
    val biometricSettings: BiometricSettingsDtoModel? = null,
    val faceSettings: FaceSettingsDtoModel = FaceSettingsDtoModel(),
    val isPinSet: Boolean = false,
    val pinLength: Int = 6,
    val isPatternSet: Boolean = false,
    val isSecurityQuestionSet: Boolean = false,
    val isFloatingLockEnabled: Boolean = false,
)

/** Everything the voice rows render, so it always changes together. */
data class SettingsVoiceInfo(
    val isVoiceEnrolled: Boolean,
    val isVoiceUnlockEnabled: Boolean,
    val isVoiceLockEnabled: Boolean,
    val isVoiceLockCommandEnrolled: Boolean,
    val voiceLockPhrase: String,
    val voiceSamplesCount: Int,
)

fun SettingsUiState.getVoiceInfo(): SettingsVoiceInfo =
    SettingsVoiceInfo(
        isVoiceEnrolled = isVoiceEnrolled,
        isVoiceUnlockEnabled = isVoiceUnlockEnabled,
        isVoiceLockEnabled = isVoiceLockEnabled,
        isVoiceLockCommandEnrolled = isVoiceLockCommandEnrolled,
        voiceLockPhrase = voiceLockPhrase,
        voiceSamplesCount = voiceSamplesCount,
    )

/** PIN / pattern rows. */
data class SettingsLockInfo(
    val isPinSet: Boolean,
    val pinLength: Int,
    val isPatternSet: Boolean,
    val isSecurityQuestionSet: Boolean,
)

fun SettingsUiState.getLockInfo(): SettingsLockInfo = SettingsLockInfo(isPinSet = isPinSet, pinLength = pinLength, isPatternSet = isPatternSet, isSecurityQuestionSet = isSecurityQuestionSet)
