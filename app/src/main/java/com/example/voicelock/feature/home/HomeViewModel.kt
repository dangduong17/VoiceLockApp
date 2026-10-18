package com.example.voicelock.feature.home

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.biometric.withPreference
import com.example.voicelock.data.model.home.HomeUiEvent
import com.example.voicelock.data.model.home.HomeUiState
import com.example.voicelock.data.model.home.toHomeUiState
import com.example.voicelock.data.model.lock.DeviceLockResult
import com.example.voicelock.data.model.lock.LockMethodUiEvent
import com.example.voicelock.data.model.lock.getBiometricRowEvent
import com.example.voicelock.data.model.lock.getFaceRowEvent
import com.example.voicelock.data.model.lock.getPatternLockEvent
import com.example.voicelock.data.model.lock.getPinLockEvent
import com.example.voicelock.data.model.lock.getSecurityQuestionEvent
import com.example.voicelock.data.model.lock.getVoiceLockEnrollmentEvent
import com.example.voicelock.data.model.lock.getVoiceUnlockEnrollmentEvent
import com.example.voicelock.data.model.lock.getVoiceUnlockRowEvent
import com.example.voicelock.domain.usecase.biometric.SetBiometricEnabledUseCase
import com.example.voicelock.domain.usecase.lock.GetProtectionStatusUseCase
import com.example.voicelock.domain.usecase.lock.LockDeviceUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceLockEnabledUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceUnlockEnabledUseCase
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.util.handleApiCall

class HomeViewModel(
    private val getProtectionStatusUseCase: GetProtectionStatusUseCase,
    private val setVoiceUnlockEnabledUseCase: SetVoiceUnlockEnabledUseCase,
    private val setVoiceLockEnabledUseCase: SetVoiceLockEnabledUseCase,
    private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
    private val lockDeviceUseCase: LockDeviceUseCase,
) : BaseViewModel<HomeUiState, HomeUiEvent>(HomeUiState()) {

    fun initData() {
        handleApiCall(
            apiCall = { getProtectionStatusUseCase() },
            onSuccess = { status -> setState { status.toHomeUiState() } },
            onError = { launchIO { setEvent(HomeUiEvent.ShowLoadError) } },
        )
    }

    /** F1 "Khóa ngay": locks the phone (device admin) or at least every locked app, no voice needed. */
    fun onLockNowClicked() {
        launchIO {
            val result = lockDeviceUseCase().getDataOrDefault(DeviceLockResult.APPS_LOCKED)
            setEvent(HomeUiEvent.LockNowDone(result))
        }
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

    fun onPinClicked() {
        sendLockMethodEvent(getCurrentState().getPinLockEvent())
    }

    fun onPatternClicked() {
        sendLockMethodEvent(getCurrentState().getPatternLockEvent())
    }

    fun onFaceClicked() {
        sendLockMethodEvent(getCurrentState().getFaceRowEvent())
    }

    /** Fingerprint row: tests the prompt once on, otherwise turns it on. Turning it off is done from Settings. */
    fun onBiometricRowClicked(type: BiometricType) {
        val settings = getCurrentState().biometricSettings ?: return
        sendLockMethodEvent(settings.getBiometricRowEvent(type))
    }

    fun onBiometricConfirmed(type: BiometricType) {
        setState { copy(biometricSettings = biometricSettings?.withPreference(type = type, isEnabled = true)) }
        handleApiCall(apiCall = { setBiometricEnabledUseCase(type = type, isEnabled = true) })
    }

    fun onQuestionsClicked() {
        sendLockMethodEvent(getCurrentState().getSecurityQuestionEvent())
    }

    private fun sendLockMethodEvent(event: LockMethodUiEvent) {
        launchIO { setEvent(HomeUiEvent.LockMethod(event)) }
    }
}
