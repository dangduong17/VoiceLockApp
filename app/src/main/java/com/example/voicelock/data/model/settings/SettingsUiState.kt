package com.example.voicelock.data.model.settings

import com.example.voicelock.data.model.biometric.BiometricSettingsDtoModel
import com.example.voicelock.data.model.face.FaceSettingsDtoModel
import com.example.voicelock.data.model.lock.ProtectionState
import com.example.voicelock.data.model.lock.ProtectionStatusDtoModel
import com.example.voicelock.data.model.voice.isVoiceLockActive
import com.example.voicelock.data.model.voice.isVoiceUnlockActive
import com.example.voicelock.util.Constant

/** Source data only; every text the screen shows is built from it in SettingsFragmentEx. */
data class SettingsUiState(
    override val isVoiceEnrolled: Boolean = false,
    override val isVoiceUnlockEnabled: Boolean = false,
    override val isVoiceLockEnabled: Boolean = false,
    override val isVoiceLockCommandEnrolled: Boolean = false,
    val voiceLockPhrase: String = "",
    val voiceSamplesCount: Int = 0,
    override val biometricSettings: BiometricSettingsDtoModel? = null,
    override val faceSettings: FaceSettingsDtoModel = FaceSettingsDtoModel(),
    override val isPinSet: Boolean = false,
    override val pinLength: Int = Constant.PIN_LENGTH_DEFAULT,
    override val isPatternSet: Boolean = false,
    override val isSecurityQuestionSet: Boolean = false,
    val isFloatingLockEnabled: Boolean = false,
) : ProtectionState

fun ProtectionStatusDtoModel.toSettingsUiState(): SettingsUiState =
    SettingsUiState(
        isVoiceEnrolled = voiceSettings.isEnrolled,
        isVoiceUnlockEnabled = voiceSettings.isVoiceUnlockActive(),
        isVoiceLockEnabled = voiceSettings.isVoiceLockActive(),
        isVoiceLockCommandEnrolled = voiceSettings.isLockCommandEnrolled,
        voiceLockPhrase = voiceSettings.lockCommandPhrase,
        voiceSamplesCount = voiceSettings.sampleCount,
        biometricSettings = biometricSettings,
        faceSettings = faceSettings,
        isPinSet = pinInfo.isPinSet,
        pinLength = pinInfo.pinLength,
        isPatternSet = isPatternSet,
        isSecurityQuestionSet = isSecurityQuestionSet,
        isFloatingLockEnabled = isFloatingLockEnabled,
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
