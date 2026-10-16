package com.example.voicelock.feature.pinlock

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.lock.LockType
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.pinlock.PinLockStage
import com.example.voicelock.data.model.pinlock.PinLockUiEvent
import com.example.voicelock.data.model.pinlock.PinLockUiState
import com.example.voicelock.data.model.pinlock.PinVerifyResult
import com.example.voicelock.data.model.pinlock.canToggleLength
import com.example.voicelock.data.model.pinlock.isKeypadEnabled
import com.example.voicelock.domain.usecase.lock.GetLockoutStateUseCase
import com.example.voicelock.domain.usecase.pinlock.SetPinUseCase
import com.example.voicelock.domain.usecase.pinlock.VerifyPinUseCase
import com.example.voicelock.util.handleApiCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class PinLockViewModel(
    private val setPinUseCase: SetPinUseCase,
    private val verifyPinUseCase: VerifyPinUseCase,
    private val getLockoutStateUseCase: GetLockoutStateUseCase,
) : BaseViewModel<PinLockUiState, PinLockUiEvent>(PinLockUiState()) {

    private val currentInput = StringBuilder()
    private var firstPin: String = ""
    private var lockoutJob: Job? = null

    fun initData(
        mode: PinLockMode,
        knownPinLength: Int,
    ) {
        val initialStage = if (mode == PinLockMode.CHANGE) PinLockStage.VERIFY else PinLockStage.ENTER
        val length = if (mode == PinLockMode.CHANGE) knownPinLength else DEFAULT_LENGTH
        currentInput.clear()
        firstPin = ""
        setState {
            copy(
                mode = mode,
                stage = initialStage,
                pinLength = length,
                enteredDigits = 0,
                isError = false,
            )
        }
        if (initialStage == PinLockStage.VERIFY) {
            checkExistingLockout()
        }
    }

    fun onDigitPressed(digit: Int) {
        val state = getCurrentState()
        if (!state.isKeypadEnabled() || currentInput.length >= state.pinLength) return
        currentInput.append(digit)
        setState { copy(enteredDigits = currentInput.length, isError = false) }
        if (currentInput.length == state.pinLength) {
            processCompletedInput(state.stage)
        }
    }

    fun onDeletePressed() {
        val state = getCurrentState()
        if (!state.isKeypadEnabled() || currentInput.isEmpty()) return
        currentInput.deleteCharAt(currentInput.length - 1)
        setState { copy(enteredDigits = currentInput.length, isError = false) }
    }

    fun onToggleLength() {
        val state = getCurrentState()
        if (!state.canToggleLength() || currentInput.isNotEmpty()) return
        setState { copy(pinLength = if (state.pinLength == 4) 6 else 4) }
    }

    private fun processCompletedInput(stage: PinLockStage) {
        val pin = currentInput.toString()
        when (stage) {
            PinLockStage.VERIFY -> verifyExistingPin(pin)
            PinLockStage.ENTER -> {
                firstPin = pin
                currentInput.clear()
                setState { copy(stage = PinLockStage.CONFIRM, enteredDigits = 0) }
            }
            PinLockStage.CONFIRM -> {
                currentInput.clear()
                if (pin == firstPin) {
                    savePin(pin)
                } else {
                    firstPin = ""
                    setState { copy(stage = PinLockStage.ENTER, enteredDigits = 0, isError = true) }
                    launchIO { setEvent(PinLockUiEvent.ConfirmMismatch) }
                }
            }
        }
    }

    private fun verifyExistingPin(pin: String) {
        currentInput.clear()
        handleApiCall(
            apiCall = { verifyPinUseCase(pin) },
            onSuccess = { result ->
                when (result) {
                    is PinVerifyResult.Success -> {
                        setState { copy(stage = PinLockStage.ENTER, enteredDigits = 0, isError = false) }
                    }

                    is PinVerifyResult.Failed -> {
                        setState { copy(enteredDigits = 0, isError = true) }
                        launchIO { setEvent(PinLockUiEvent.VerifyFailed(remainingAttempts = result.remainingAttempts)) }
                    }

                    is PinVerifyResult.Locked -> startLockout(result.remainingSeconds)
                }
            },
        )
    }

    private fun savePin(pin: String) {
        handleApiCall(
            apiCall = { setPinUseCase(pin = pin, length = getCurrentState().pinLength) },
            onSuccess = {
                launchIO { setEvent(PinLockUiEvent.Success) }
            },
        )
    }

    private fun checkExistingLockout() {
        handleApiCall(
            apiCall = { getLockoutStateUseCase(LockType.PIN) },
            onSuccess = { lockoutState ->
                if (lockoutState.isLocked) startLockout(lockoutState.remainingSeconds)
            },
        )
    }

    private fun startLockout(remainingSeconds: Int) {
        lockoutJob?.cancel()
        currentInput.clear()
        setState { copy(isLocked = true, lockoutRemainingSeconds = remainingSeconds, enteredDigits = 0) }
        lockoutJob =
            launchIO {
                var remaining = remainingSeconds
                while (remaining > 0) {
                    delay(1000)
                    remaining--
                    setState { copy(lockoutRemainingSeconds = remaining) }
                }
                setState { copy(isLocked = false, lockoutRemainingSeconds = 0) }
            }
    }

    override fun onCleared() {
        lockoutJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val DEFAULT_LENGTH = 6
    }
}
