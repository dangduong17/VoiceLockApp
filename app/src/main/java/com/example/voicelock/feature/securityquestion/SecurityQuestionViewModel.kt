package com.example.voicelock.feature.securityquestion

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.lock.LockType
import com.example.voicelock.data.model.securityquestion.SecurityAnswerVerifyResult
import com.example.voicelock.data.model.securityquestion.SecurityQuestionInputError
import com.example.voicelock.data.model.securityquestion.SecurityQuestionMode
import com.example.voicelock.data.model.securityquestion.SecurityQuestionStage
import com.example.voicelock.data.model.securityquestion.SecurityQuestionUiEvent
import com.example.voicelock.data.model.securityquestion.SecurityQuestionUiState
import com.example.voicelock.data.model.securityquestion.isInputEnabled
import com.example.voicelock.domain.usecase.applock.MarkAppUnlockedUseCase
import com.example.voicelock.domain.usecase.lock.GetLockoutStateUseCase
import com.example.voicelock.domain.usecase.securityquestion.GetSecurityQuestionUseCase
import com.example.voicelock.domain.usecase.securityquestion.SetSecurityQuestionUseCase
import com.example.voicelock.domain.usecase.securityquestion.VerifySecurityAnswerUseCase
import com.example.voicelock.util.Constant
import com.example.voicelock.util.getDataOrNull
import com.example.voicelock.util.handleApiCall
import com.example.voicelock.util.normalizeForComparison
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * F5 Security question. SETUP saves a question + answer; CHANGE first asks the current question;
 * UNLOCK (lock overlay) and RESET_PIN ("Quên PIN?") only verify. Wrong answers count towards the
 * same 5-attempt / 30-second lockout as the PIN, on their own counter.
 */
class SecurityQuestionViewModel(
    private val getSecurityQuestionUseCase: GetSecurityQuestionUseCase,
    private val setSecurityQuestionUseCase: SetSecurityQuestionUseCase,
    private val verifySecurityAnswerUseCase: VerifySecurityAnswerUseCase,
    private val getLockoutStateUseCase: GetLockoutStateUseCase,
    private val markAppUnlockedUseCase: MarkAppUnlockedUseCase,
) : BaseViewModel<SecurityQuestionUiState, SecurityQuestionUiEvent>(SecurityQuestionUiState()) {

    private var targetPackageName: String? = null
    private var lockoutJob: Job? = null

    fun initData(
        mode: SecurityQuestionMode,
        targetPackageName: String?,
    ) {
        this.targetPackageName = targetPackageName?.takeIf { it.isNotBlank() }
        setState { copy(mode = mode, stage = SecurityQuestionStage.LOADING, inputError = SecurityQuestionInputError.NONE) }
        launchIO {
            val existing = getSecurityQuestionUseCase().getDataOrNull()
            val stage =
                when (mode) {
                    SecurityQuestionMode.SETUP -> SecurityQuestionStage.SETUP
                    SecurityQuestionMode.CHANGE -> if (existing == null) SecurityQuestionStage.SETUP else SecurityQuestionStage.VERIFY
                    SecurityQuestionMode.UNLOCK, SecurityQuestionMode.RESET_PIN ->
                        if (existing == null) SecurityQuestionStage.NOT_SET else SecurityQuestionStage.VERIFY
                }
            setState { copy(stage = stage, question = existing?.question.orEmpty()) }
            if (stage == SecurityQuestionStage.VERIFY) checkExistingLockout()
        }
    }

    fun onInputChanged() {
        if (getCurrentState().inputError != SecurityQuestionInputError.NONE) {
            setState { copy(inputError = SecurityQuestionInputError.NONE) }
        }
    }

    fun verifyAnswer(answer: String) {
        val state = getCurrentState()
        if (state.stage != SecurityQuestionStage.VERIFY || !state.isInputEnabled()) return
        if (answer.normalizeForComparison().isEmpty()) {
            setState { copy(inputError = SecurityQuestionInputError.ANSWER_EMPTY) }
            return
        }
        setState { copy(isBusy = true) }
        handleApiCall(
            apiCall = { verifySecurityAnswerUseCase(answer) },
            onSuccess = { result ->
                setState { copy(isBusy = false) }
                when (result) {
                    SecurityAnswerVerifyResult.Success -> onVerified()
                    is SecurityAnswerVerifyResult.Failed ->
                        launchIO { setEvent(SecurityQuestionUiEvent.VerifyFailed(remainingAttempts = result.remainingAttempts)) }
                    is SecurityAnswerVerifyResult.Locked -> startLockout(result.remainingSeconds)
                    SecurityAnswerVerifyResult.NotSet -> setState { copy(stage = SecurityQuestionStage.NOT_SET) }
                }
            },
            onError = { setState { copy(isBusy = false) } },
        )
    }

    fun saveQuestion(
        question: String,
        answer: String,
        confirmAnswer: String,
    ) {
        val state = getCurrentState()
        if (state.stage != SecurityQuestionStage.SETUP || !state.isInputEnabled()) return
        val normalizedAnswer = answer.normalizeForComparison()
        val inputError =
            when {
                question.isBlank() -> SecurityQuestionInputError.QUESTION_EMPTY
                normalizedAnswer.length < Constant.SECURITY_ANSWER_MIN_LENGTH -> SecurityQuestionInputError.ANSWER_TOO_SHORT
                normalizedAnswer != confirmAnswer.normalizeForComparison() -> SecurityQuestionInputError.ANSWER_MISMATCH
                else -> SecurityQuestionInputError.NONE
            }
        if (inputError != SecurityQuestionInputError.NONE) {
            setState { copy(inputError = inputError) }
            return
        }
        setState { copy(isBusy = true) }
        handleApiCall(
            apiCall = { setSecurityQuestionUseCase(question = question, answer = answer) },
            onSuccess = {
                setState { copy(isBusy = false) }
                launchIO { setEvent(SecurityQuestionUiEvent.Saved(mode = getCurrentState().mode)) }
            },
            onError = { setState { copy(isBusy = false) } },
        )
    }

    override fun onCleared() {
        lockoutJob?.cancel()
        super.onCleared()
    }

    private fun onVerified() {
        when (getCurrentState().mode) {
            SecurityQuestionMode.CHANGE -> setState { copy(stage = SecurityQuestionStage.SETUP, inputError = SecurityQuestionInputError.NONE) }
            SecurityQuestionMode.UNLOCK ->
                launchIO {
                    // Same session bookkeeping as a PIN/pattern unlock, so the monitor lets the app through.
                    targetPackageName?.let { markAppUnlockedUseCase(it).first() }
                    setEvent(SecurityQuestionUiEvent.Unlocked)
                }
            SecurityQuestionMode.RESET_PIN -> launchIO { setEvent(SecurityQuestionUiEvent.NavigateToPinSetup) }
            SecurityQuestionMode.SETUP -> Unit
        }
    }

    private fun checkExistingLockout() {
        handleApiCall(
            apiCall = { getLockoutStateUseCase(LockType.SECURITY_QUESTION) },
            onSuccess = { lockoutState -> if (lockoutState.isLocked) startLockout(lockoutState.remainingSeconds) },
        )
    }

    private fun startLockout(remainingSeconds: Int) {
        lockoutJob?.cancel()
        setState { copy(isLocked = true, lockoutRemainingSeconds = remainingSeconds) }
        lockoutJob =
            launchIO {
                var remaining = remainingSeconds
                while (remaining > 0) {
                    delay(LOCKOUT_TICK_MS)
                    remaining--
                    setState { copy(lockoutRemainingSeconds = remaining) }
                }
                setState { copy(isLocked = false, lockoutRemainingSeconds = 0) }
            }
    }

    private companion object {
        const val LOCKOUT_TICK_MS = 1_000L
    }
}
