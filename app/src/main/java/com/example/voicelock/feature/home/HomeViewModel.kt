package com.example.voicelock.feature.home

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.home.HomeUiEvent
import com.example.voicelock.data.model.home.HomeUiState
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.domain.usecase.home.GetHomeStatusUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceLockEnabledUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceUnlockEnabledUseCase
import com.example.voicelock.util.handleApiCall

class HomeViewModel(
    private val getHomeStatusUseCase: GetHomeStatusUseCase,
    private val setVoiceUnlockEnabledUseCase: SetVoiceUnlockEnabledUseCase,
    private val setVoiceLockEnabledUseCase: SetVoiceLockEnabledUseCase,
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

    /**
     * Voice switches only make sense with an enrolled voice: turning one on before enrolling
     * opens enrollment instead (the fragment snaps the switch back to the saved state).
     */
    fun toggleVoiceUnlock(isEnabled: Boolean) {
        val state = getCurrentState()
        if (isEnabled == state.isVoiceUnlockEnabled) return
        if (!state.isVoiceEnrolled) {
            launchIO { setEvent(HomeUiEvent.NavigateToVoiceEnrollment) }
            return
        }
        setState { copy(isVoiceUnlockEnabled = isEnabled) }
        handleApiCall(apiCall = { setVoiceUnlockEnabledUseCase(isEnabled) })
    }

    fun toggleVoiceLock(isEnabled: Boolean) {
        val state = getCurrentState()
        if (isEnabled == state.isVoiceLockEnabled) return
        if (!state.isVoiceEnrolled) {
            launchIO { setEvent(HomeUiEvent.NavigateToVoiceEnrollment) }
            return
        }
        setState { copy(isVoiceLockEnabled = isEnabled) }
        handleApiCall(apiCall = { setVoiceLockEnabledUseCase(isEnabled) })
    }

    fun onVoiceUnlockRowClicked() {
        val event = if (getCurrentState().isVoiceEnrolled) HomeUiEvent.NavigateToVoiceTest else HomeUiEvent.NavigateToVoiceEnrollment
        launchIO { setEvent(event) }
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
