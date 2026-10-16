package com.example.voicelock.feature.settings

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.settings.SettingsUiEvent
import com.example.voicelock.data.model.settings.SettingsUiState
import com.example.voicelock.domain.usecase.settings.GetSettingsUseCase
import com.example.voicelock.util.handleApiCall

class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
) : BaseViewModel<SettingsUiState, SettingsUiEvent>(SettingsUiState()) {

    fun initData() {
        handleApiCall(
            apiCall = { getSettingsUseCase() },
            onSuccess = { state -> setState { state } },
            onError = { throwable ->
                launchIO {
                    setEvent(SettingsUiEvent.ShowToast(throwable.message ?: "An error occurred"))
                }
            },
        )
    }

    fun toggleVoiceUnlock(isEnabled: Boolean) {
        setState { copy(isVoiceUnlockEnabled = isEnabled) }
    }

    fun toggleVoiceLock(isEnabled: Boolean) {
        setState { copy(isVoiceLockEnabled = isEnabled) }
    }

    fun onHomeTabClicked() {
        launchIO { setEvent(SettingsUiEvent.NavigateToHome) }
    }

    fun onVoiceCommandsClicked() {
        launchIO { setEvent(SettingsUiEvent.NavigateToVoiceCommands) }
    }

    fun onPermissionsClicked() {
        launchIO { setEvent(SettingsUiEvent.NavigateToPermissions) }
    }

    fun onPinClicked() {
        val state = getCurrentState()
        val mode = if (state.isPinSet) PinLockMode.CHANGE else PinLockMode.SETUP
        launchIO { setEvent(SettingsUiEvent.NavigateToPinLock(mode = mode, pinLength = state.pinLength)) }
    }

    fun onPatternClicked() {
        val state = getCurrentState()
        val mode = if (state.isPatternSet) PatternLockMode.CHANGE else PatternLockMode.SETUP
        launchIO { setEvent(SettingsUiEvent.NavigateToPatternLock(mode)) }
    }

    fun onFingerprintClicked() {
        launchIO { setEvent(SettingsUiEvent.NavigateToFingerprintSetup) }
    }

    fun onQuestionsClicked() {
        launchIO { setEvent(SettingsUiEvent.NavigateToSecurityQuestions) }
    }

    fun onThemeLanguageClicked() {
        launchIO { setEvent(SettingsUiEvent.NavigateToThemeLanguage) }
    }

    fun onHelpFeedbackClicked() {
        launchIO { setEvent(SettingsUiEvent.NavigateToHelpFeedback) }
    }

    fun onAboutClicked() {
        launchIO { setEvent(SettingsUiEvent.NavigateToAbout) }
    }

    fun onAppLockClicked() {
        launchIO { setEvent(SettingsUiEvent.NavigateToAppLock) }
    }
}
