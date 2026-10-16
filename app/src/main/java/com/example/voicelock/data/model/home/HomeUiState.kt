package com.example.voicelock.data.model.home

data class HomeUiState(
    val userName: String = "Minh",
    val isProtectionEnabled: Boolean = true,
    val voiceSamplesCount: Int = 3,
    val isBackupPinEnabled: Boolean = true,
    val isVoiceUnlockEnabled: Boolean = true,
    val isVoiceLockEnabled: Boolean = false,
    val isPinSet: Boolean = false,
    val pinLength: Int = 6,
    val isPatternSet: Boolean = false,
    val pinStatusText: String = "6 chữ số • Đã thiết lập",
    val patternStatusText: String = "Thiết lập",
    val fingerprintStatusText: String = "Chưa bật",
)
