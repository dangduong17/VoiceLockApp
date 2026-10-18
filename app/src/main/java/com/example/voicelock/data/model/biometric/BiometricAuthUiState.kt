package com.example.voicelock.data.model.biometric

enum class BiometricAuthStage {
    /** Waiting: the prompt was cancelled or not shown yet. */
    IDLE,
    PROMPTING,
    SUCCESS,

    /** The system gave up (lockout, sensor error, ...); [BiometricAuthUiState.errorMessage] says why. */
    FAILED,
}

data class BiometricAuthUiState(
    val stage: BiometricAuthStage = BiometricAuthStage.IDLE,
    /** True when started by LockOverlayActivity to unlock another app. */
    val isAppUnlock: Boolean = false,
    /** System-provided, already localized error text of the last failed prompt. */
    val errorMessage: String? = null,
)

sealed class BiometricAuthUiEvent {
    data object ShowPrompt : BiometricAuthUiEvent()

    /** [isAppUnlock]: the screen was opened by the lock overlay and should close it. */
    data class Authenticated(
        val isAppUnlock: Boolean,
    ) : BiometricAuthUiEvent()
}
