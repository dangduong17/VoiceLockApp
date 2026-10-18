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
import com.example.voicelock.domain.usecase.applock.MarkAppUnlockedUseCase
import com.example.voicelock.domain.usecase.lock.GetLockoutStateUseCase
import com.example.voicelock.domain.usecase.patternlock.SetPatternUseCase
import com.example.voicelock.domain.usecase.patternlock.VerifyPatternUseCase
import com.example.voicelock.util.countdownFlow
import com.example.voicelock.util.getDataOrNull
import com.example.voicelock.util.handleApiCall
import kotlinx.coroutines.Job

class PatternLockViewModel(
    private val setPatternUseCase: SetPatternUseCase,
    private val verifyPatternUseCase: VerifyPatternUseCase,
    private val getLockoutStateUseCase: GetLockoutStateUseCase,
    private val markAppUnlockedUseCase: MarkAppUnlockedUseCase,
) : BaseViewModel<PatternLockUiState, PatternLockUiEvent>(PatternLockUiState()) {

    private var firstPattern: List<Int> = emptyList()
    private var targetPackageName: String? = null
    private var lockoutJob: Job? = null

    fun initData(
        mode: PatternLockMode,
        targetPackageName: String? = null,
    ) {
        val initialStage = if (mode == PatternLockMode.CHANGE || mode == PatternLockMode.UNLOCK) {
            PatternLockStage.VERIFY
        } else {
            PatternLockStage.ENTER
        }
        this.targetPackageName = targetPackageName
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
                    is PatternVerifyResult.Success -> onVerifySuccess()

                    is PatternVerifyResult.Failed -> {
                        launchIO { setEvent(PatternLockUiEvent.VerifyFailed(remainingAttempts = result.remainingAttempts)) }
                    }

                    is PatternVerifyResult.Locked -> startLockout(result.remainingSeconds)
                }
            },
        )
    }

    private fun onVerifySuccess() {
        val mode = getCurrentState().mode
        if (mode == PatternLockMode.UNLOCK) {
            launchIO {
                // Same session bookkeeping as every unlock method, so the monitor lets the app through.
                markAppUnlockedUseCase(targetPackageName).getDataOrNull()
                setEvent(PatternLockUiEvent.Success(mode))
            }
        } else {
            setState { copy(stage = PatternLockStage.ENTER) }
        }
    }

    private fun savePattern(cells: List<Int>) {
        handleApiCall(
            apiCall = { setPatternUseCase(cells) },
            onSuccess = {
                launchIO { setEvent(PatternLockUiEvent.Success(getCurrentState().mode)) }
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
        lockoutJob =
            launchIO {
                countdownFlow(remainingSeconds).collect { remaining -> setState { copy(lockoutRemainingSeconds = remaining) } }
            }
    }

    override fun onCleared() {
        lockoutJob?.cancel()
        super.onCleared()
    }
}
