package com.example.voicelock.feature.biometric

import android.view.View
import androidx.fragment.app.Fragment
import com.example.voicelock.R
import com.example.voicelock.data.model.biometric.BiometricAvailability
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.util.MessageType
import com.example.voicelock.util.openBiometricEnrollment
import com.example.voicelock.util.showBiometricPrompt
import com.example.voicelock.util.showMessage

/** Fingerprint row helpers shared by Home and Settings. */
private fun Fragment.getBiometricName(type: BiometricType): String =
    when (type) {
        BiometricType.FINGERPRINT -> getString(R.string.fingerprint)
    }

/** Two-line rows (Settings): the subtitle under the title. */
fun Fragment.getBiometricSubText(
    availability: BiometricAvailability,
    isActive: Boolean,
): String =
    getString(
        when (availability) {
            BiometricAvailability.AVAILABLE -> if (isActive) R.string.biometric_sub_enabled else R.string.biometric_sub_disabled
            BiometricAvailability.NOT_ENROLLED -> R.string.biometric_sub_not_enrolled
            BiometricAvailability.NO_HARDWARE -> R.string.biometric_sub_no_hardware
            BiometricAvailability.UNAVAILABLE -> R.string.biometric_sub_unavailable
        },
    )

/** Compact rows (Home): the short status on the right. */
fun Fragment.getBiometricActionText(
    availability: BiometricAvailability,
    isActive: Boolean,
): String =
    getString(
        when (availability) {
            BiometricAvailability.AVAILABLE -> if (isActive) R.string.biometric_action_enabled else R.string.not_enabled
            BiometricAvailability.NOT_ENROLLED -> R.string.setup
            BiometricAvailability.NO_HARDWARE -> R.string.biometric_action_no_hardware
            BiometricAvailability.UNAVAILABLE -> R.string.biometric_action_unavailable
        },
    )

/** Turning a biometric on is confirmed once with the system prompt, so it is known to work. */
fun Fragment.confirmBiometric(
    type: BiometricType,
    anchorView: View?,
    onConfirmed: () -> Unit,
) {
    showBiometricPrompt(
        title = getString(R.string.biometric_prompt_enable_title, getBiometricName(type)),
        subtitle = getString(R.string.biometric_prompt_enable_subtitle),
        onSuccess = {
            onConfirmed()
            showMessage(
                message = getString(R.string.biometric_enabled_toast, getBiometricName(type)),
                type = MessageType.SUCCESS,
                anchorView = anchorView,
            )
        },
    )
}

fun Fragment.testBiometric(
    type: BiometricType,
    anchorView: View?,
) {
    showBiometricPrompt(
        title = getString(R.string.biometric_prompt_test_title, getBiometricName(type)),
        subtitle = getString(R.string.biometric_prompt_enable_subtitle),
        onSuccess = {
            showMessage(message = getString(R.string.biometric_test_success), type = MessageType.SUCCESS, anchorView = anchorView)
        },
    )
}

fun Fragment.startBiometricEnrollment(
    type: BiometricType,
    anchorView: View?,
) {
    showMessage(message = getString(R.string.biometric_enroll_hint, getBiometricName(type)), anchorView = anchorView)
    openBiometricEnrollment()
}

fun Fragment.showBiometricUnavailable(
    type: BiometricType,
    availability: BiometricAvailability,
    anchorView: View?,
) {
    val messageRes =
        if (availability == BiometricAvailability.NO_HARDWARE) R.string.biometric_no_hardware_toast else R.string.biometric_unavailable_toast
    showMessage(message = getString(messageRes, getBiometricName(type)), type = MessageType.ERROR, anchorView = anchorView)
}
