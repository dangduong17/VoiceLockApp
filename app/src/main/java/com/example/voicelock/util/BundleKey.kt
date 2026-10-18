package com.example.voicelock.util

object BundleKey {
    const val KEY_BOOKMARK_ID = "KEY_BOOKMARK_ID"
    const val KEY_LOCK_MODE = "KEY_LOCK_MODE"
    const val KEY_PIN_LENGTH = "KEY_PIN_LENGTH"
    const val KEY_LOCK_TARGET_PACKAGE = "KEY_LOCK_TARGET_PACKAGE"
    /** VoiceVerificationMode name: which voice phrase (unlock passphrase / F1 lock command). */
    const val KEY_VOICE_MODE = "KEY_VOICE_MODE"
    /** F1: TwoStageAuthFragment opened as the voice lock command (locks the phone on success). */
    const val KEY_VOICE_LOCK_COMMAND = "KEY_VOICE_LOCK_COMMAND"
}
