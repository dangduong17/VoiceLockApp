package com.example.voicelock.feature.applock

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.applock.LockOverlayUiEvent
import com.example.voicelock.data.model.applock.LockOverlayUiState
import com.example.voicelock.data.model.lock.LockMethod
import com.example.voicelock.data.model.pinlock.PinInfo
import com.example.voicelock.domain.usecase.patternlock.GetPatternInfoUseCase
import com.example.voicelock.domain.usecase.pinlock.GetPinInfoUseCase
import com.example.voicelock.util.getDataOrDefault

/**
 * Pattern is the primary lock method, PIN is the backup — [buildAvailableMethods] is the single
 * place that encodes this priority order. Adding a future method (fingerprint, face, voice, ...)
 * only means appending it here; [onTryAnotherMethod] already cycles through whatever the user
 * has configured without any other change.
 */
class LockOverlayViewModel(
    private val getPinInfoUseCase: GetPinInfoUseCase,
    private val getPatternInfoUseCase: GetPatternInfoUseCase,
) : BaseViewModel<LockOverlayUiState, LockOverlayUiEvent>(LockOverlayUiState()) {

    fun decideVerificationMethod() {
        launchIO {
            val pinInfo = getPinInfoUseCase().getDataOrDefault(PinInfo(isPinSet = false, pinLength = 6))
            val isPatternSet = getPatternInfoUseCase().getDataOrDefault(false)
            val methods = buildAvailableMethods(isPatternSet = isPatternSet, isPinSet = pinInfo.isPinSet)

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
        isPatternSet: Boolean,
        isPinSet: Boolean,
    ): List<LockMethod> =
        buildList {
            if (isPatternSet) add(LockMethod.PATTERN)
            if (isPinSet) add(LockMethod.PIN)
        }
}
