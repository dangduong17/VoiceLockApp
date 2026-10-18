package com.example.voicelock.feature.settings

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.biometric.isActive
import com.example.voicelock.data.model.biometric.withPreference
import com.example.voicelock.data.model.face.isFaceUnlockActive
import com.example.voicelock.data.model.lock.LockMethodUiEvent
import com.example.voicelock.data.model.lock.getBiometricRowEvent
import com.example.voicelock.data.model.lock.getEnableBiometricEvent
import com.example.voicelock.data.model.lock.getFaceEnrollmentEvent
import com.example.voicelock.data.model.lock.getFaceRowEvent
import com.example.voicelock.data.model.lock.getPatternLockEvent
import com.example.voicelock.data.model.lock.getPinLockEvent
import com.example.voicelock.data.model.lock.getSecurityQuestionEvent
import com.example.voicelock.data.model.lock.getVoiceLockEnrollmentEvent
import com.example.voicelock.data.model.lock.getVoiceUnlockEnrollmentEvent
import com.example.voicelock.data.model.lock.getVoiceUnlockRowEvent
import com.example.voicelock.data.model.settings.SettingsUiEvent
import com.example.voicelock.data.model.settings.SettingsUiState
import com.example.voicelock.data.model.settings.toSettingsUiState
import com.example.voicelock.domain.usecase.biometric.SetBiometricEnabledUseCase
import com.example.voicelock.domain.usecase.face.SetFaceUnlockEnabledUseCase
import com.example.voicelock.domain.usecase.lock.GetProtectionStatusUseCase
import com.example.voicelock.domain.usecase.quicklock.SetFloatingLockEnabledUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceLockEnabledUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceUnlockEnabledUseCase
import com.example.voicelock.util.handleApiCall

class SettingsViewModel(
    private val getProtectionStatusUseCase: GetProtectionStatusUseCase,
    private val setVoiceUnlockEnabledUseCase: SetVoiceUnlockEnabledUseCase,
    private val setVoiceLockEnabledUseCase: SetVoiceLockEnabledUseCase,
    private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
    private val setFaceUnlockEnabledUseCase: SetFaceUnlockEnabledUseCase,
    private val setFloatingLockEnabledUseCase: SetFloatingLockEnabledUseCase,
) : BaseViewModel<SettingsUiState, SettingsUiEvent>(SettingsUiState()) {

    fun initData() {
        handleApiCall(
            apiCall = { getProtectionStatusUseCase() },
            onSuccess = { status -> setState { status.toSettingsUiState() } },
            onError = { launchIO { setEvent(SettingsUiEvent.ShowLoadError) } },
        )
    }

    fun toggleVoiceUnlock(isEnabled: Boolean) {
        val state = getCurrentState()
        if (isEnabled == state.isVoiceUnlockEnabled) return
        state.getVoiceUnlockEnrollmentEvent()?.let { event ->
            sendLockMethodEvent(event)
            return
        }
        setState { copy(isVoiceUnlockEnabled = isEnabled) }
        handleApiCall(apiCall = { setVoiceUnlockEnabledUseCase(isEnabled) })
    }

    fun toggleVoiceLock(isEnabled: Boolean) {
        val state = getCurrentState()
        if (isEnabled == state.isVoiceLockEnabled) return
        state.getVoiceLockEnrollmentEvent()?.let { event ->
            sendLockMethodEvent(event)
            return
        }
        setState { copy(isVoiceLockEnabled = isEnabled) }
        handleApiCall(
            apiCall = { setVoiceLockEnabledUseCase(isEnabled) },
            onSuccess = { if (isEnabled) sendLockMethodEvent(LockMethodUiEvent.OfferVoiceLockTile) },
        )
    }

    fun onVoiceUnlockRowClicked() {
        sendLockMethodEvent(getCurrentState().getVoiceUnlockRowEvent())
    }

    fun toggleFaceUnlock(isEnabled: Boolean) {
        val state = getCurrentState()
        if (isEnabled == state.faceSettings.isFaceUnlockActive()) return
        state.getFaceEnrollmentEvent()?.let { event ->
            sendLockMethodEvent(event)
            return
        }
        setState { copy(faceSettings = faceSettings.copy(isUnlockPreferenceOn = isEnabled)) }
        handleApiCall(apiCall = { setFaceUnlockEnabledUseCase(isEnabled) })
    }

    fun onFaceUnlockRowClicked() {
        sendLockMethodEvent(getCurrentState().getFaceRowEvent())
    }

    /** Turning off saves right away; turning on goes through [getEnableBiometricEvent] first. */
    fun toggleBiometric(
        type: BiometricType,
        isEnabled: Boolean,
    ) {
        val settings = getCurrentState().biometricSettings ?: return
        if (isEnabled == settings.isActive(type)) return
        if (isEnabled) {
            sendLockMethodEvent(settings.getEnableBiometricEvent(type))
        } else {
            saveBiometric(type = type, isEnabled = false)
        }
    }

    fun onBiometricRowClicked(type: BiometricType) {
        val settings = getCurrentState().biometricSettings ?: return
        sendLockMethodEvent(settings.getBiometricRowEvent(type))
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

    fun onPinClicked() {
        sendLockMethodEvent(getCurrentState().getPinLockEvent())
    }

    fun onPatternClicked() {
        sendLockMethodEvent(getCurrentState().getPatternLockEvent())
    }

    fun onQuestionsClicked() {
        sendLockMethodEvent(getCurrentState().getSecurityQuestionEvent())
    }

    /** [canDrawOverlays] comes from the screen (a system permission, checked where the Context is). */
    fun toggleFloatingLock(
        isEnabled: Boolean,
        canDrawOverlays: Boolean,
    ) {
        if (isEnabled == getCurrentState().isFloatingLockEnabled) return
        if (isEnabled && !canDrawOverlays) {
            launchIO {
                setEvent(SettingsUiEvent.ShowFloatingLockNeedsOverlay)
                setEvent(SettingsUiEvent.NavigateToPermissions)
            }
            return
        }
        setState { copy(isFloatingLockEnabled = isEnabled) }
        handleApiCall(
            apiCall = { setFloatingLockEnabledUseCase(isEnabled) },
            onSuccess = { launchIO { setEvent(SettingsUiEvent.SyncFloatingLock(isEnabled)) } },
        )
    }

    private fun sendLockMethodEvent(event: LockMethodUiEvent) {
        launchIO { setEvent(SettingsUiEvent.LockMethod(event)) }
    }
}
