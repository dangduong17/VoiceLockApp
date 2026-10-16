package com.example.voicelock.data.model.home

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
    val pinStatusText: String = "6 chữ số • Đã thiết lập",
    val patternStatusText: String = "Thiết lập",
    val fingerprintStatusText: String = "Chưa bật",
)
