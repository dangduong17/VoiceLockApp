package com.example.voicelock.feature.patternlock

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.lock.LockType
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.patternlock.PatternLockStage
import com.example.voicelock.data.model.patternlock.PatternLockUiEvent
import com.example.voicelock.data.model.patternlock.PatternLockUiState
import com.example.voicelock.data.model.patternlock.PatternVerifyResult
import com.example.voicelock.data.model.patternlock.isInputEnabled
import com.example.voicelock.domain.usecase.lock.GetLockoutStateUseCase
import com.example.voicelock.domain.usecase.patternlock.SetPatternUseCase
import com.example.voicelock.domain.usecase.patternlock.VerifyPatternUseCase
import com.example.voicelock.util.handleApiCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class PatternLockViewModel(
    private val setPatternUseCase: SetPatternUseCase,
    private val verifyPatternUseCase: VerifyPatternUseCase,
    private val getLockoutStateUseCase: GetLockoutStateUseCase,
) : BaseViewModel<PatternLockUiState, PatternLockUiEvent>(PatternLockUiState()) {

    private var firstPattern: List<Int> = emptyList()
    private var lockoutJob: Job? = null

    fun initData(mode: PatternLockMode) {
        val initialStage = if (mode == PatternLockMode.CHANGE) PatternLockStage.VERIFY else PatternLockStage.ENTER
        firstPattern = emptyList()
        setState { copy(mode = mode, stage = initialStage) }
        if (initialStage == PatternLockStage.VERIFY) {
            checkExistingLockout()
        }
    }

    fun onPatternTooShort() {
        launchIO { setEvent(PatternLockUiEvent.TooShort) }
    }

    fun onPatternDrawn(cells: List<Int>) {
        val state = getCurrentState()
        if (!state.isInputEnabled()) return
        when (state.stage) {
            PatternLockStage.VERIFY -> verifyExistingPattern(cells)
            PatternLockStage.ENTER -> {
                firstPattern = cells
                setState { copy(stage = PatternLockStage.CONFIRM) }
            }
            PatternLockStage.CONFIRM -> {
                if (cells == firstPattern) {
                    savePattern(cells)
                } else {
                    firstPattern = emptyList()
                    setState { copy(stage = PatternLockStage.ENTER) }
                    launchIO { setEvent(PatternLockUiEvent.ConfirmMismatch) }
                }
            }
        }
    }

    private fun verifyExistingPattern(cells: List<Int>) {
        handleApiCall(
            apiCall = { verifyPatternUseCase(cells) },
            onSuccess = { result ->
                when (result) {
                    is PatternVerifyResult.Success -> {
                        setState { copy(stage = PatternLockStage.ENTER) }
                    }

                    is PatternVerifyResult.Failed -> {
                        launchIO { setEvent(PatternLockUiEvent.VerifyFailed(remainingAttempts = result.remainingAttempts)) }
                    }

                    is PatternVerifyResult.Locked -> startLockout(result.remainingSeconds)
                }
            },
        )
    }

    private fun savePattern(cells: List<Int>) {
        handleApiCall(
            apiCall = { setPatternUseCase(cells) },
            onSuccess = {
                launchIO { setEvent(PatternLockUiEvent.Success) }
            },
        )
    }

    private fun checkExistingLockout() {
        handleApiCall(
            apiCall = { getLockoutStateUseCase(LockType.PATTERN) },
            onSuccess = { lockoutState ->
                if (lockoutState.isLocked) startLockout(lockoutState.remainingSeconds)
            },
        )
    }

    private fun startLockout(remainingSeconds: Int) {
        lockoutJob?.cancel()
        setState { copy(isLocked = true, lockoutRemainingSeconds = remainingSeconds) }
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
}
