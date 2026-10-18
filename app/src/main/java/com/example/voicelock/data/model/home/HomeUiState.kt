package com.example.voicelock.data.model.home

import com.example.voicelock.data.model.biometric.BiometricSettingsDtoModel
import com.example.voicelock.data.model.face.FaceSettingsDtoModel
import com.example.voicelock.data.model.lock.ProtectionState
import com.example.voicelock.data.model.lock.ProtectionStatusDtoModel
import com.example.voicelock.data.model.voice.isVoiceLockActive
import com.example.voicelock.data.model.voice.isVoiceUnlockActive
import com.example.voicelock.util.Constant

/** Source data only; every text the screen shows is built from it in HomeFragmentEx. */
data class HomeUiState(
    val isProtectionEnabled: Boolean = true,
    override val isVoiceEnrolled: Boolean = false,
    val voiceSamplesCount: Int = 0,
    val isBackupPinEnabled: Boolean = true,
    override val isVoiceUnlockEnabled: Boolean = false,
    override val isVoiceLockEnabled: Boolean = false,
    override val isVoiceLockCommandEnrolled: Boolean = false,
    val voiceLockPhrase: String = "",
    override val isPinSet: Boolean = false,
    override val pinLength: Int = Constant.PIN_LENGTH_DEFAULT,
    override val isPatternSet: Boolean = false,
    override val isSecurityQuestionSet: Boolean = false,
    override val biometricSettings: BiometricSettingsDtoModel? = null,
    override val faceSettings: FaceSettingsDtoModel = FaceSettingsDtoModel(),
) : ProtectionState

fun ProtectionStatusDtoModel.toHomeUiState(): HomeUiState =
    HomeUiState(
        isVoiceEnrolled = voiceSettings.isEnrolled,
        voiceSamplesCount = voiceSettings.sampleCount,
        isBackupPinEnabled = pinInfo.isPinSet,
        isVoiceUnlockEnabled = voiceSettings.isVoiceUnlockActive(),
        isVoiceLockEnabled = voiceSettings.isVoiceLockActive(),
        isVoiceLockCommandEnrolled = voiceSettings.isLockCommandEnrolled,
        voiceLockPhrase = voiceSettings.lockCommandPhrase,
        isPinSet = pinInfo.isPinSet,
        pinLength = pinInfo.pinLength,
        isPatternSet = isPatternSet,
        isSecurityQuestionSet = isSecurityQuestionSet,
        biometricSettings = biometricSettings,
        faceSettings = faceSettings,
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
