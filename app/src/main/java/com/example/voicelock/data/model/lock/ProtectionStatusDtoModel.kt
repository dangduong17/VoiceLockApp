package com.example.voicelock.data.model.lock

import com.example.voicelock.data.model.biometric.BiometricSettingsDtoModel
import com.example.voicelock.data.model.face.FaceSettingsDtoModel
import com.example.voicelock.data.model.pinlock.PinInfo
import com.example.voicelock.data.model.voice.VoiceSettingsDtoModel

/** Every configured lock method at once: what Home and Settings both render. */
data class ProtectionStatusDtoModel(
    val pinInfo: PinInfo,
    val isPatternSet: Boolean,
    val isSecurityQuestionSet: Boolean,
    val voiceSettings: VoiceSettingsDtoModel,
    val biometricSettings: BiometricSettingsDtoModel,
    val faceSettings: FaceSettingsDtoModel,
    val isFloatingLockEnabled: Boolean,
)
