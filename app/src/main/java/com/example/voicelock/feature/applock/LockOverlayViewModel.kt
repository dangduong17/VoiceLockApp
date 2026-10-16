package com.example.voicelock.feature.applock

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.applock.LockOverlayUiEvent
import com.example.voicelock.data.model.applock.LockOverlayUiState
import com.example.voicelock.data.model.lock.LockMethod
import com.example.voicelock.data.model.pinlock.PinInfo
import com.example.voicelock.domain.usecase.patternlock.GetPatternInfoUseCase
import com.example.voicelock.data.model.voice.VoiceSettings
import com.example.voicelock.data.model.voice.isVoiceUnlockActive
import com.example.voicelock.domain.usecase.pinlock.GetPinInfoUseCase
import com.example.voicelock.domain.usecase.voice.GetVoiceSettingsUseCase
import com.example.voicelock.util.getDataOrDefault

/**
 * Voice is the primary lock method (when enrolled and switched on), then Pattern, then PIN as
 * the backup — [buildAvailableMethods] is the single place that encodes this priority order.
 * Adding a future method (fingerprint, face, ...) only means appending it here;
 * [onTryAnotherMethod] already cycles through whatever the user has configured.
 */
class LockOverlayViewModel(
    private val getPinInfoUseCase: GetPinInfoUseCase,
    private val getPatternInfoUseCase: GetPatternInfoUseCase,
    private val getVoiceSettingsUseCase: GetVoiceSettingsUseCase,
) : BaseViewModel<LockOverlayUiState, LockOverlayUiEvent>(LockOverlayUiState()) {

    fun decideVerificationMethod() {
        launchIO {
            val pinInfo = getPinInfoUseCase().getDataOrDefault(PinInfo(isPinSet = false, pinLength = 6))
            val isPatternSet = getPatternInfoUseCase().getDataOrDefault(false)
            val isVoiceActive = getVoiceSettingsUseCase().getDataOrDefault(VoiceSettings()).isVoiceUnlockActive()
            val methods =
                buildAvailableMethods(isVoiceActive = isVoiceActive, isPatternSet = isPatternSet, isPinSet = pinInfo.isPinSet)

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
        isPatternSet: Boolean,
        isPinSet: Boolean,
    ): List<LockMethod> =
        buildList {
            if (isVoiceActive) add(LockMethod.VOICE)
            if (isPatternSet) add(LockMethod.PATTERN)
            if (isPinSet) add(LockMethod.PIN)
        }
}
