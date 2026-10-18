package com.example.voicelock.util

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.voicelock.R
import timber.log.Timber

private const val TAG = "BiometricPromptEx"

/**
 * Shows Android's BiometricPrompt (fingerprint / face, whatever the user enrolled). The system
 * handles retries and lockout itself; [onError] receives the final error, including the user
 * cancelling ([BiometricPrompt.ERROR_USER_CANCELED], [BiometricPrompt.ERROR_NEGATIVE_BUTTON]).
 */
fun Fragment.showBiometricPrompt(
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (errorCode: Int, message: String) -> Unit = { _, _ -> },
) {
    val prompt =
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    onError(errorCode, errString.toString())
                }
            },
        )
    val promptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(getString(R.string.biometric_prompt_cancel))
            .setAllowedAuthenticators(Constant.BIOMETRIC_AUTHENTICATORS)
            .build()
    prompt.authenticate(promptInfo)
}

fun isBiometricCancel(errorCode: Int): Boolean =
    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
        errorCode == BiometricPrompt.ERROR_CANCELED

/** Opens the Android screen where fingerprints / a face are enrolled. */
fun Fragment.openBiometricEnrollment() {
    val enrollIntent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                .putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, Constant.BIOMETRIC_AUTHENTICATORS)
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    try {
        startActivity(enrollIntent)
    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Biometric enrollment screen unavailable")
        runCatching { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
    }
}
