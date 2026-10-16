package com.example.voicelock.feature.home

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.home.HomeUiEvent
import com.example.voicelock.data.model.home.HomeUiState
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.domain.usecase.home.GetHomeStatusUseCase
import com.example.voicelock.util.handleApiCall

class HomeViewModel(
    private val getHomeStatusUseCase: GetHomeStatusUseCase,
) : BaseViewModel<HomeUiState, HomeUiEvent>(HomeUiState()) {

    fun initData() {
        handleApiCall(
            apiCall = { getHomeStatusUseCase() },
            onSuccess = { status ->
                setState { status }
            },
            onError = { throwable ->
                launchIO {
                    setEvent(HomeUiEvent.ShowToast(throwable.message ?: "An error occurred"))
                }
            },
        )
    }

    fun onLockNowClicked() {
        launchIO {
            setEvent(HomeUiEvent.LockNow)
        }
    }

    fun toggleVoiceUnlock(isEnabled: Boolean) {
        setState { copy(isVoiceUnlockEnabled = isEnabled) }
    }

    fun toggleVoiceLock(isEnabled: Boolean) {
        setState { copy(isVoiceLockEnabled = isEnabled) }
    }

    fun onPinClicked() {
        val state = getCurrentState()
        val mode = if (state.isPinSet) PinLockMode.CHANGE else PinLockMode.SETUP
        launchIO { setEvent(HomeUiEvent.NavigateToPinLock(mode = mode, pinLength = state.pinLength)) }
    }

    fun onPatternClicked() {
        val state = getCurrentState()
        val mode = if (state.isPatternSet) PatternLockMode.CHANGE else PatternLockMode.SETUP
        launchIO { setEvent(HomeUiEvent.NavigateToPatternLock(mode)) }
    }

    fun onFingerprintClicked() {
        launchIO { setEvent(HomeUiEvent.NavigateToFingerprintSetup) }
    }

    fun onQuestionsClicked() {
        launchIO { setEvent(HomeUiEvent.NavigateToSecurityQuestions) }
    }

    fun onAppLockClicked() {
        launchIO { setEvent(HomeUiEvent.NavigateToAppLock) }
    }
}
