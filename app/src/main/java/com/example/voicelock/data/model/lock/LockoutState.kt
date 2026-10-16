package com.example.voicelock.data.model.lock

data class LockoutState(
    val failedAttempts: Int,
    val isLocked: Boolean,
    val remainingSeconds: Int,
)
