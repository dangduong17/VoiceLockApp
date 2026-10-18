package com.example.voicelock.data.model.lock

enum class DeviceLockResult {
    /** Device admin active: the phone screen was locked (DevicePolicyManager.lockNow()). */
    SCREEN_LOCKED,

    /** No device admin: only the apps locked by VoiceLock were re-locked (their unlock sessions cleared). */
    APPS_LOCKED,
}
