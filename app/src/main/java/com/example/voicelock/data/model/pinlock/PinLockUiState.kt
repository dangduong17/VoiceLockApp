package com.example.voicelock.data.model.pinlock

import com.example.voicelock.util.Constant

data class PinLockUiState(
    val mode: PinLockMode = PinLockMode.SETUP,
    val stage: PinLockStage = PinLockStage.ENTER,
    val pinLength: Int = Constant.PIN_LENGTH_DEFAULT,
    val enteredDigits: Int = 0,
    val isError: Boolean = false,
    val lockoutRemainingSeconds: Int = 0,
    /** A security question exists, so a forgotten PIN can be reset from the CHANGE screen. */
    val isSecurityQuestionSet: Boolean = false,
)

fun PinLockUiState.isLocked(): Boolean = lockoutRemainingSeconds > 0

/**
 * The keypad is disabled only while the lockout countdown is running.
 */
fun PinLockUiState.isKeypadEnabled(): Boolean = !isLocked()

/** "Quên PIN?" is offered only while verifying the current PIN before changing it. */
fun PinLockUiState.canResetWithSecurityQuestion(): Boolean =
    mode == PinLockMode.CHANGE && stage == PinLockStage.VERIFY && isSecurityQuestionSet

/**
 * Switching between a 4- and 6-digit PIN is only offered during first-time setup,
 * before any digit of the first entry has been typed.
 */
fun PinLockUiState.canToggleLength(): Boolean = mode == PinLockMode.SETUP && stage == PinLockStage.ENTER

/** The other length the toggle switches to. */
fun PinLockUiState.getToggledLength(): Int =
    if (pinLength == Constant.PIN_LENGTH_SHORT) Constant.PIN_LENGTH_DEFAULT else Constant.PIN_LENGTH_SHORT

/** Title + subtitle. */
data class PinLockHeaderInfo(
    val mode: PinLockMode,
    val stage: PinLockStage,
    val pinLength: Int,
)

fun PinLockUiState.getHeaderInfo(): PinLockHeaderInfo = PinLockHeaderInfo(mode = mode, stage = stage, pinLength = pinLength)

data class PinLockDotsInfo(
    val pinLength: Int,
    val enteredDigits: Int,
    val isError: Boolean,
)

fun PinLockUiState.getDotsInfo(): PinLockDotsInfo = PinLockDotsInfo(pinLength = pinLength, enteredDigits = enteredDigits, isError = isError)

/** The 4 / 6 digit toggle: visible before the first digit, labelled with the length it switches to. */
data class PinLengthToggleInfo(
    val isVisible: Boolean,
    val toggledLength: Int,
)

fun PinLockUiState.getLengthToggleInfo(): PinLengthToggleInfo =
    PinLengthToggleInfo(isVisible = canToggleLength() && enteredDigits == 0, toggledLength = getToggledLength())
