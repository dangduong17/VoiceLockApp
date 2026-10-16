package com.example.voicelock.data.model.settings

data class SettingsUiState(
    val isVoiceUnlockEnabled: Boolean = true,
    val isVoiceLockEnabled: Boolean = false,
    val voiceSamplesCount: Int = 3,
    val isPinSet: Boolean = false,
    val pinLength: Int = 6,
    val isPatternSet: Boolean = false,
    val pinStatusText: String = "6 chữ số • Đã thiết lập",
    val patternStatusText: String = "Chưa thiết lập",
    val fingerprintStatusText: String = "Chưa bật • Xác minh qua Android",
    val questionStatusText: String = "Đã lưu • Dùng để khôi phục PIN",
    val themeLanguageText: String = "Sáng • Tiếng Việt",
    val appVersionText: String = "Phiên bản 1.0.0 • Quyền riêng tư",
)
