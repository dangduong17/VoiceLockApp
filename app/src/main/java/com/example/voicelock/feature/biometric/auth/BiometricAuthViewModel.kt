package com.example.voicelock.feature.biometric.auth

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.biometric.BiometricAuthStage
import com.example.voicelock.data.model.biometric.BiometricAuthUiEvent
import com.example.voicelock.data.model.biometric.BiometricAuthUiState
import com.example.voicelock.domain.usecase.applock.MarkAppUnlockedUseCase
import kotlinx.coroutines.flow.first

/**
 * Lock-overlay step for system biometrics (fingerprint / face via BiometricPrompt). The prompt is
 * shown whenever the screen comes to the front; a success marks the target app unlocked, like
 * the PIN / pattern / voice screens. Retries and lockout are handled by Android itself, and the
 * overlay's "try another method" button falls back to the other configured methods.
 */
class BiometricAuthViewModel(
    private val markAppUnlockedUseCase: MarkAppUnlockedUseCase,
) : BaseViewModel<BiometricAuthUiState, BiometricAuthUiEvent>(BiometricAuthUiState()) {

    private var targetPackageName: String? = null

    /** [targetPackageName] is set when unlocking a locked app from the lock overlay. */
    fun initData(targetPackageName: String?) {
        this.targetPackageName = targetPackageName?.takeIf { it.isNotBlank() }
        setState { copy(isAppUnlock = this@BiometricAuthViewModel.targetPackageName != null) }
    }

    /** The overlay was (re)opened: the user wants in, so the prompt shows by itself. */
    fun onScreenStarted() {
        val stage = getCurrentState().stage
        if (stage == BiometricAuthStage.IDLE || stage == BiometricAuthStage.FAILED) requestPrompt()
    }

    fun requestPrompt() {
        val stage = getCurrentState().stage
        if (stage == BiometricAuthStage.PROMPTING || stage == BiometricAuthStage.SUCCESS) return
        setState { copy(stage = BiometricAuthStage.PROMPTING, errorMessage = null) }
        launchIO { setEvent(BiometricAuthUiEvent.ShowPrompt) }
    }

    fun onAuthenticationSucceeded() {
        launchIO {
            // Same session bookkeeping as a PIN/pattern unlock, so the monitor lets the app through.
            targetPackageName?.let { markAppUnlockedUseCase(it).first() }
            setState { copy(stage = BiometricAuthStage.SUCCESS, errorMessage = null) }
            setEvent(BiometricAuthUiEvent.Authenticated(isAppUnlock = targetPackageName != null))
        }
    }

    fun onAuthenticationError(
        message: String,
        isCancel: Boolean,
    ) {
        setState {
            if (isCancel) {
                copy(stage = BiometricAuthStage.IDLE, errorMessage = null)
            } else {
                copy(stage = BiometricAuthStage.FAILED, errorMessage = message)
            }
        }
    }
}
