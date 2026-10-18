package com.example.voicelock.data.model.home

import com.example.voicelock.data.model.biometric.BiometricSettings
import com.example.voicelock.data.model.face.FaceSettings

data class HomeUiState(
    val isProtectionEnabled: Boolean = true,
    val isVoiceEnrolled: Boolean = false,
    val voiceSamplesCount: Int = 0,
    val isBackupPinEnabled: Boolean = true,
    val isVoiceUnlockEnabled: Boolean = false,
    val isVoiceLockEnabled: Boolean = false,
    val isPinSet: Boolean = false,
    val pinLength: Int = 6,
    val isPatternSet: Boolean = false,
    val biometricSettings: BiometricSettings = BiometricSettings(),
    val faceSettings: FaceSettings = FaceSettings(),
    val pinStatusText: String = "6 chữ số • Đã thiết lập",
    val patternStatusText: String = "Thiết lập",
)
