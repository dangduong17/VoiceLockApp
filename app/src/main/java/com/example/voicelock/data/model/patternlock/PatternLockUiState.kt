package com.example.voicelock.data.model.patternlock

data class PatternLockUiState(
    val mode: PatternLockMode = PatternLockMode.SETUP,
    val stage: PatternLockStage = PatternLockStage.ENTER,
    val isLocked: Boolean = false,
    val lockoutRemainingSeconds: Int = 0,
)

/**
 * The pattern grid is disabled only while the lockout countdown is running.
 */
fun PatternLockUiState.isInputEnabled(): Boolean = !isLocked
