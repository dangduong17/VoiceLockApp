package com.example.voicelock.data.model.voice

/**
 * Stored voice preferences. The switches only take effect once a voice profile exists, so the
 * effective values below are what screens and the lock overlay should read.
 */
data class VoiceSettingsDtoModel(
    val isEnrolled: Boolean = false,
    val sampleCount: Int = 0,
    val isUnlockPreferenceOn: Boolean = true,
    val isLockPreferenceOn: Boolean = false,
)

fun VoiceSettingsDtoModel.isVoiceUnlockActive(): Boolean = isEnrolled && isUnlockPreferenceOn

fun VoiceSettingsDtoModel.isVoiceLockActive(): Boolean = isEnrolled && isLockPreferenceOn
