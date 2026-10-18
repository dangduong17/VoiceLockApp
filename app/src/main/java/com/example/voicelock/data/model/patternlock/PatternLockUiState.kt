package com.example.voicelock.data.model.patternlock

data class PatternLockUiState(
    val mode: PatternLockMode = PatternLockMode.SETUP,
    val stage: PatternLockStage = PatternLockStage.ENTER,
    val lockoutRemainingSeconds: Int = 0,
)

fun PatternLockUiState.isLocked(): Boolean = lockoutRemainingSeconds > 0

/**
 * The pattern grid is disabled only while the lockout countdown is running.
 */
fun PatternLockUiState.isInputEnabled(): Boolean = !isLocked()

/** Title + subtitle. */
data class PatternLockHeaderInfo(
    val mode: PatternLockMode,
    val stage: PatternLockStage,
)

fun PatternLockUiState.getHeaderInfo(): PatternLockHeaderInfo = PatternLockHeaderInfo(mode = mode, stage = stage)
