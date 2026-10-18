package com.example.voicelock.feature.applock

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.applock.LockOverlayUiEvent
import com.example.voicelock.data.model.applock.LockOverlayUiState
import com.example.voicelock.data.model.biometric.BiometricSettingsDtoModel
import com.example.voicelock.data.model.biometric.isBiometricUnlockActive
import com.example.voicelock.data.model.face.FaceSettingsDtoModel
import com.example.voicelock.data.model.face.isFaceUnlockActive
import com.example.voicelock.data.model.lock.LockMethod
import com.example.voicelock.domain.usecase.biometric.GetBiometricSettingsUseCase
import com.example.voicelock.domain.usecase.face.GetFaceSettingsUseCase
import com.example.voicelock.data.model.pinlock.PinInfo
import com.example.voicelock.domain.usecase.patternlock.GetPatternInfoUseCase
import com.example.voicelock.data.model.voice.VoiceSettingsDtoModel
import com.example.voicelock.data.model.voice.isVoiceUnlockActive
import com.example.voicelock.domain.usecase.pinlock.GetPinInfoUseCase
import com.example.voicelock.domain.usecase.voice.GetVoiceSettingsUseCase
import com.example.voicelock.util.getDataOrDefault

/**
 * Voice is the primary lock method (when enrolled and switched on), then the app's face
 * unlock, then the system fingerprint, then Pattern, then PIN as
 * the backup — [buildAvailableMethods] is the single place that encodes this priority order.
 * Adding a future method (fingerprint, face, ...) only means appending it here;
 * [onTryAnotherMethod] already cycles through whatever the user has configured.
 */
class LockOverlayViewModel(
    private val getPinInfoUseCase: GetPinInfoUseCase,
    private val getPatternInfoUseCase: GetPatternInfoUseCase,
    private val getVoiceSettingsUseCase: GetVoiceSettingsUseCase,
    private val getBiometricSettingsUseCase: GetBiometricSettingsUseCase,
    private val getFaceSettingsUseCase: GetFaceSettingsUseCase,
) : BaseViewModel<LockOverlayUiState, LockOverlayUiEvent>(LockOverlayUiState()) {

    fun decideVerificationMethod() {
        launchIO {
            val pinInfo = getPinInfoUseCase().getDataOrDefault(PinInfo(isPinSet = false, pinLength = 6))
            val isPatternSet = getPatternInfoUseCase().getDataOrDefault(false)
            val isVoiceActive = getVoiceSettingsUseCase().getDataOrDefault(VoiceSettingsDtoModel()).isVoiceUnlockActive()
            val isFaceActive = getFaceSettingsUseCase().getDataOrDefault(FaceSettingsDtoModel()).isFaceUnlockActive()
            val isBiometricActive = getBiometricSettingsUseCase().getDataOrDefault(BiometricSettingsDtoModel()).isBiometricUnlockActive()
            val methods =
                buildAvailableMethods(
                    isVoiceActive = isVoiceActive,
                    isFaceActive = isFaceActive,
                    isBiometricActive = isBiometricActive,
                    isPatternSet = isPatternSet,
                    isPinSet = pinInfo.isPinSet,
                )

            setState {
                copy(
                    isResolving = false,
                    availableMethods = methods,
                    currentMethodIndex = 0,
                    pinLength = pinInfo.pinLength,
                )
            }

            val firstMethod = methods.firstOrNull()
            if (firstMethod == null) {
                setEvent(LockOverlayUiEvent.NothingConfigured)
            } else {
                setEvent(LockOverlayUiEvent.ShowMethod(method = firstMethod, pinLength = pinInfo.pinLength))
            }
        }
    }

    fun onTryAnotherMethod() {
        val state = getCurrentState()
        if (state.availableMethods.size <= 1) return
        val nextIndex = (state.currentMethodIndex + 1) % state.availableMethods.size
        setState { copy(currentMethodIndex = nextIndex) }
        launchIO {
            setEvent(LockOverlayUiEvent.ShowMethod(method = state.availableMethods[nextIndex], pinLength = state.pinLength))
        }
    }

    private fun buildAvailableMethods(
        isVoiceActive: Boolean,
        isFaceActive: Boolean,
        isBiometricActive: Boolean,
        isPatternSet: Boolean,
        isPinSet: Boolean,
    ): List<LockMethod> =
        buildList {
            if (isVoiceActive) add(LockMethod.VOICE)
            if (isFaceActive) add(LockMethod.FACE)
            if (isBiometricActive) add(LockMethod.BIOMETRIC)
            if (isPatternSet) add(LockMethod.PATTERN)
            if (isPinSet) add(LockMethod.PIN)
        }
}
