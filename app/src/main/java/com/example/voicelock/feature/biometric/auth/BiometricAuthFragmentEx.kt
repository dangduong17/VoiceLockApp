package com.example.voicelock.feature.biometric.auth

import android.view.View
import androidx.core.content.ContextCompat
import com.example.voicelock.R
import com.example.voicelock.data.model.biometric.BiometricAuthStage
import com.example.voicelock.data.model.biometric.BiometricAuthStatusInfo
import com.example.voicelock.data.model.biometric.BiometricAuthUiEvent
import com.example.voicelock.util.displayToast
import com.example.voicelock.util.isBiometricCancel
import com.example.voicelock.util.setPreventDoubleClickScaleView
import com.example.voicelock.util.showBiometricPrompt

fun BiometricAuthFragment.initView() {
    // Basic setup if any
}

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
    tvBiometricAuthStatus.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

    tvBiometricAuthError.visibility = if (errorMessage.isNullOrBlank()) View.GONE else View.VISIBLE
    tvBiometricAuthError.text = errorMessage.orEmpty()

    val isBusy = stage == BiometricAuthStage.PROMPTING || stage == BiometricAuthStage.SUCCESS
    btnBiometricAuthAction.isEnabled = !isBusy
    btnBiometricAuthAction.alpha = if (isBusy) 0.5f else 1f
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
            if (event.isAppUnlock) {
                displayToast(getString(R.string.biometric_auth_stage_success))
                requireActivity().finish()
            }
    }
}
