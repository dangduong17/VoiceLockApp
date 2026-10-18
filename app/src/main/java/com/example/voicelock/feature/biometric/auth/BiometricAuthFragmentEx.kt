package com.example.voicelock.feature.biometric.auth

import com.example.voicelock.R
import com.example.voicelock.data.model.biometric.BiometricAuthStage
import com.example.voicelock.data.model.biometric.BiometricAuthStatusInfo
import com.example.voicelock.data.model.biometric.BiometricAuthUiEvent
import com.example.voicelock.util.isBiometricCancel
import com.example.voicelock.util.setPreventDoubleClickScaleView
import com.example.voicelock.util.showBiometricPrompt
import androidx.core.view.isVisible
import com.example.voicelock.util.setEnabledWithAlpha
import com.example.voicelock.util.setTextColorRes

fun BiometricAuthFragment.backEvent() {
    navigator.navigateUp()
}

fun BiometricAuthFragment.settingEvent() {
    binding.btnBiometricAuthAction.setPreventDoubleClickScaleView { authEvent() }
}

fun BiometricAuthFragment.authEvent() {
    viewModel.requestPrompt()
}

fun BiometricAuthFragment.updateStatus(info: BiometricAuthStatusInfo) = with(binding) {
    val stage = info.stage
    val errorMessage = info.errorMessage
    tvBiometricAuthStatus.text =
        getString(
            when (stage) {
                BiometricAuthStage.IDLE -> R.string.biometric_auth_stage_idle
                BiometricAuthStage.PROMPTING -> R.string.biometric_auth_stage_prompting
                BiometricAuthStage.SUCCESS -> R.string.biometric_auth_stage_success
                BiometricAuthStage.FAILED -> R.string.biometric_auth_stage_failed
            },
        )
    val colorRes = if (stage == BiometricAuthStage.FAILED) R.color.error_red else R.color.text_primary
    tvBiometricAuthStatus.setTextColorRes(colorRes)

    tvBiometricAuthError.isVisible = !errorMessage.isNullOrBlank()
    tvBiometricAuthError.text = errorMessage.orEmpty()

    val isBusy = stage == BiometricAuthStage.PROMPTING || stage == BiometricAuthStage.SUCCESS
    btnBiometricAuthAction.setEnabledWithAlpha(!isBusy)
}

fun BiometricAuthFragment.handleUiEvent(event: BiometricAuthUiEvent) {
    when (event) {
        is BiometricAuthUiEvent.ShowPrompt ->
            showBiometricPrompt(
                title = getString(R.string.biometric_prompt_unlock_title),
                subtitle = getString(R.string.biometric_prompt_unlock_subtitle),
                onSuccess = { viewModel.onAuthenticationSucceeded() },
                onError = { errorCode, message ->
                    viewModel.onAuthenticationError(message = message, isCancel = isBiometricCancel(errorCode))
                },
            )

        is BiometricAuthUiEvent.Authenticated ->
            // The unlocked app coming to the front is the feedback; the overlay just closes.
            if (event.isAppUnlock) requireActivity().finish()
    }
}
