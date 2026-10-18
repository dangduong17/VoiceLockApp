package com.example.voicelock.feature.settings

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.model.securityquestion.SecurityQuestionMode
import com.example.voicelock.data.model.settings.SettingsUiEvent
import com.example.voicelock.data.model.settings.SettingsUiState
import com.example.voicelock.data.model.biometric.BiometricAvailability
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.biometric.getAvailability
import com.example.voicelock.data.model.biometric.isActive
import com.example.voicelock.data.model.biometric.withPreference
import com.example.voicelock.data.model.face.isFaceUnlockActive
import com.example.voicelock.domain.usecase.biometric.SetBiometricEnabledUseCase
import com.example.voicelock.domain.usecase.face.SetFaceUnlockEnabledUseCase
import com.example.voicelock.domain.usecase.settings.GetSettingsUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceLockEnabledUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceUnlockEnabledUseCase
import com.example.voicelock.util.handleApiCall

class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val setVoiceUnlockEnabledUseCase: SetVoiceUnlockEnabledUseCase,
    private val setVoiceLockEnabledUseCase: SetVoiceLockEnabledUseCase,
    private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
    private val setFaceUnlockEnabledUseCase: SetFaceUnlockEnabledUseCase,
) : BaseViewModel<SettingsUiState, SettingsUiEvent>(SettingsUiState()) {

    fun initData() {
        handleApiCall(
            apiCall = { getSettingsUseCase() },
            onSuccess = { state -> setState { state } },
            onError = { throwable ->
                launchIO {
                    setEvent(SettingsUiEvent.ShowLoadError(throwable.message))
                }
            },
        )
    }

    /** Same rule as HomeViewModel: turning a voice switch on before enrolling opens enrollment. */
    fun toggleVoiceUnlock(isEnabled: Boolean) {
        val state = getCurrentState()
        if (isEnabled == state.isVoiceUnlockEnabled) return
        if (!state.isVoiceEnrolled) {
            launchIO { setEvent(SettingsUiEvent.NavigateToVoiceEnrollment(VoiceVerificationMode.UNLOCK)) }
            return
        }
        setState { copy(isVoiceUnlockEnabled = isEnabled) }
        handleApiCall(apiCall = { setVoiceUnlockEnabledUseCase(isEnabled) })
    }

    fun toggleVoiceLock(isEnabled: Boolean) {
        val state = getCurrentState()
        if (isEnabled == state.isVoiceLockEnabled) return
        if (!state.isVoiceLockCommandEnrolled) {
            // F1: the lock command is its own recording, separate from the unlock passphrase.
            launchIO { setEvent(SettingsUiEvent.NavigateToVoiceEnrollment(VoiceVerificationMode.LOCK)) }
            return
        }
        setState { copy(isVoiceLockEnabled = isEnabled) }
        handleApiCall(
            apiCall = { setVoiceLockEnabledUseCase(isEnabled) },
            onSuccess = { if (isEnabled) launchIO { setEvent(SettingsUiEvent.OfferVoiceLockTile) } },
        )
    }

    /** F1: tapping the voice lock row records (or re-records) the lock command. */
    fun onVoiceLockRowClicked() {
        launchIO { setEvent(SettingsUiEvent.NavigateToVoiceEnrollment(VoiceVerificationMode.LOCK)) }
    }

    fun onVoiceUnlockRowClicked() {
        val event =
            if (getCurrentState().isVoiceEnrolled) SettingsUiEvent.NavigateToVoiceTest else SettingsUiEvent.NavigateToVoiceEnrollment(VoiceVerificationMode.UNLOCK)
        launchIO { setEvent(event) }
    }

    /** Same rule as voice: turning face unlock on before enrolling opens face enrollment. */
    fun toggleFaceUnlock(isEnabled: Boolean) {
        val face = getCurrentState().faceSettings
        if (isEnabled == face.isFaceUnlockActive()) return
        if (!face.isEnrolled) {
            launchIO { setEvent(SettingsUiEvent.NavigateToFaceEnrollment) }
            return
        }
        setState { copy(faceSettings = faceSettings.copy(isUnlockPreferenceOn = isEnabled)) }
        handleApiCall(apiCall = { setFaceUnlockEnabledUseCase(isEnabled) })
    }

    /** Enrolled: test screen (re-enroll / delete live there); otherwise enrollment. */
    fun onFaceUnlockRowClicked() {
        val event =
            if (getCurrentState().faceSettings.isEnrolled) SettingsUiEvent.NavigateToFaceTest else SettingsUiEvent.NavigateToFaceEnrollment
        launchIO { setEvent(event) }
    }

    /**
     * Turning the fingerprint on is confirmed with the system prompt first ([onBiometricConfirmed]);
     * without an enrolled biometric it opens the Android enrollment screen instead.
     */
    fun toggleBiometric(
        type: BiometricType,
        isEnabled: Boolean,
    ) {
        val settings = getCurrentState().biometricSettings ?: return
        if (isEnabled == settings.isActive(type)) return
        if (!isEnabled) {
            saveBiometric(type = type, isEnabled = false)
            return
        }
        val event =
            when (val availability = settings.getAvailability(type)) {
                BiometricAvailability.AVAILABLE -> SettingsUiEvent.ConfirmBiometric(type)
                BiometricAvailability.NOT_ENROLLED -> SettingsUiEvent.OpenBiometricEnrollment(type)
                BiometricAvailability.NO_HARDWARE, BiometricAvailability.UNAVAILABLE ->
                    SettingsUiEvent.ShowBiometricUnavailable(type, availability)
            }
        launchIO { setEvent(event) }
    }

    /** Once on, tapping the row just tests the prompt; otherwise it starts turning it on. */
    fun onBiometricRowClicked(type: BiometricType) {
        if (getCurrentState().biometricSettings?.isActive(type) == true) {
            launchIO { setEvent(SettingsUiEvent.TestBiometric(type)) }
        } else {
            toggleBiometric(type = type, isEnabled = true)
        }
    }

    fun onBiometricConfirmed(type: BiometricType) {
        saveBiometric(type = type, isEnabled = true)
    }

    private fun saveBiometric(
        type: BiometricType,
        isEnabled: Boolean,
    ) {
        setState { copy(biometricSettings = biometricSettings?.withPreference(type = type, isEnabled = isEnabled)) }
        handleApiCall(apiCall = { setBiometricEnabledUseCase(type = type, isEnabled = isEnabled) })
    }

    fun onHomeTabClicked() {
        launchIO { setEvent(SettingsUiEvent.NavigateToHome) }
    }

    fun onVoiceCommandsClicked() {
        // "Voice commands & samples" = (re-)record the voice password.
        launchIO { setEvent(SettingsUiEvent.NavigateToVoiceEnrollment(VoiceVerificationMode.UNLOCK)) }
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

    fun onQuestionsClicked() {
        // Changing an existing question first asks it (CHANGE mode), like changing the PIN.
        val mode = if (getCurrentState().isSecurityQuestionSet) SecurityQuestionMode.CHANGE else SecurityQuestionMode.SETUP
        launchIO { setEvent(SettingsUiEvent.NavigateToSecurityQuestion(mode)) }
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
