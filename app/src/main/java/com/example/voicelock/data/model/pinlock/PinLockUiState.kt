package com.example.voicelock.data.model.pinlock

data class PinLockUiState(
    val mode: PinLockMode = PinLockMode.SETUP,
    val stage: PinLockStage = PinLockStage.ENTER,
    val pinLength: Int = 6,
    val enteredDigits: Int = 0,
    val isError: Boolean = false,
    val isLocked: Boolean = false,
    val lockoutRemainingSeconds: Int = 0,
)

/**
 * The keypad is disabled only while the lockout countdown is running.
 */
fun PinLockUiState.isKeypadEnabled(): Boolean = !isLocked

/**
 * Switching between a 4- and 6-digit PIN is only offered during first-time setup,
 * before any digit of the first entry has been typed.
 */
fun PinLockUiState.canToggleLength(): Boolean = mode == PinLockMode.SETUP && stage == PinLockStage.ENTER
