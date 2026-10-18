package com.example.voicelock.data.model.applock

import com.example.voicelock.data.model.lock.LockMethod

data class LockOverlayUiState(
    val isResolving: Boolean = true,
    val availableMethods: List<LockMethod> = emptyList(),
    val currentMethodIndex: Int = 0,
    val pinLength: Int = 6,
)

fun LockOverlayUiState.currentMethod(): LockMethod? = availableMethods.getOrNull(currentMethodIndex)

/** Voice unlock is configured, so the volume keys mean "listen now" on this lock screen (F0). */
fun LockOverlayUiState.isVoiceAvailable(): Boolean = LockMethod.VOICE in availableMethods

/**
 * Whether a different, already-configured method (e.g. the backup PIN while Pattern is shown)
 * exists for the user to fall back to on the lock screen.
 */
fun LockOverlayUiState.hasFallbackMethod(): Boolean = availableMethods.size > 1
