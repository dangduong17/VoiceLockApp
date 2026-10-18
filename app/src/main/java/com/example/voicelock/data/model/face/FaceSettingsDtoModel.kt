package com.example.voicelock.data.model.face

/**
 * Stored face preferences. Like voice, the switch only takes effect once a face profile exists,
 * so [isFaceUnlockActive] is what screens and the lock overlay should read.
 */
data class FaceSettingsDtoModel(
    val isEnrolled: Boolean = false,
    val sampleCount: Int = 0,
    val isUnlockPreferenceOn: Boolean = true,
)

fun FaceSettingsDtoModel.isFaceUnlockActive(): Boolean = isEnrolled && isUnlockPreferenceOn
