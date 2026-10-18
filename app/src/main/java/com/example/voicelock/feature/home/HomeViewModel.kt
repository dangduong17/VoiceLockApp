package com.example.voicelock.feature.home

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.biometric.BiometricAvailability
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.biometric.getAvailability
import com.example.voicelock.data.model.biometric.isActive
import com.example.voicelock.data.model.biometric.withPreference
import com.example.voicelock.data.model.home.HomeUiEvent
import com.example.voicelock.data.model.home.HomeUiState
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.domain.usecase.biometric.SetBiometricEnabledUseCase
import com.example.voicelock.domain.usecase.home.GetHomeStatusUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceLockEnabledUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceUnlockEnabledUseCase
import com.example.voicelock.util.handleApiCall

class HomeViewModel(
    private val getHomeStatusUseCase: GetHomeStatusUseCase,
    private val setVoiceUnlockEnabledUseCase: SetVoiceUnlockEnabledUseCase,
    private val setVoiceLockEnabledUseCase: SetVoiceLockEnabledUseCase,
    private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
) : BaseViewModel<HomeUiState, HomeUiEvent>(HomeUiState()) {

    fun initData() {
        handleApiCall(
            apiCall = { getHomeStatusUseCase() },
            onSuccess = { status ->
                setState { status }
            },
            onError = { throwable ->
                launchIO {
                    setEvent(HomeUiEvent.ShowLoadError(throwable.message))
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

    /** Without a face profile this opens enrollment; otherwise the face test screen (re-enroll / delete). */
    fun onFaceClicked() {
        val event =
            if (getCurrentState().faceSettings.isEnrolled) HomeUiEvent.NavigateToFaceTest else HomeUiEvent.NavigateToFaceEnrollment
        launchIO { setEvent(event) }
    }

    /**
     * Fingerprint row: once on, a tap tests the system prompt; otherwise it turns the
     * method on (confirmed with the prompt, see [onBiometricConfirmed]) or opens Android's
     * enrollment screen when nothing is enrolled. Turning it off is done from Settings.
     */
    fun onBiometricRowClicked(type: BiometricType) {
        val settings = getCurrentState().biometricSettings
        val event =
            if (settings.isActive(type)) {
                HomeUiEvent.TestBiometric(type)
            } else {
                when (val availability = settings.getAvailability(type)) {
                    BiometricAvailability.AVAILABLE -> HomeUiEvent.ConfirmBiometric(type)
                    BiometricAvailability.NOT_ENROLLED -> HomeUiEvent.OpenBiometricEnrollment(type)
                    BiometricAvailability.NO_HARDWARE, BiometricAvailability.UNAVAILABLE ->
                        HomeUiEvent.ShowBiometricUnavailable(type, availability)
                }
            }
        launchIO { setEvent(event) }
    }

    fun onBiometricConfirmed(type: BiometricType) {
        setState { copy(biometricSettings = biometricSettings.withPreference(type = type, isEnabled = true)) }
        handleApiCall(apiCall = { setBiometricEnabledUseCase(type = type, isEnabled = true) })
    }

    fun onQuestionsClicked() {
        launchIO { setEvent(HomeUiEvent.NavigateToSecurityQuestions) }
    }

    fun onAppLockClicked() {
        launchIO { setEvent(HomeUiEvent.NavigateToAppLock) }
    }
}
