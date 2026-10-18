package com.example.voicelock.data.model.biometric

/**
 * System biometric rows, verified by Android's BiometricPrompt. Face is not here: most phones do
 * not expose their face unlock to apps, so face unlock is the app's own (see data/model/face).
 */
enum class BiometricType {
    FINGERPRINT,
}

enum class BiometricAvailability {
    AVAILABLE,

    /** The sensor exists but nothing is enrolled in the Android settings yet. */
    NOT_ENROLLED,
    NO_HARDWARE,

    /** Temporarily unusable (sensor busy, security update required, ...). */
    UNAVAILABLE,
}

/**
 * Stored on/off switch plus what the device currently supports. The switch only takes effect
 * while the biometric is [BiometricAvailability.AVAILABLE] (e.g. the user may remove every
 * fingerprint from Android later), so screens and the lock overlay read the "active" helpers.
 */
data class BiometricSettings(
    val fingerprintAvailability: BiometricAvailability = BiometricAvailability.NO_HARDWARE,
    val isFingerprintPreferenceOn: Boolean = false,
)

fun BiometricSettings.getAvailability(type: BiometricType): BiometricAvailability =
    when (type) {
        BiometricType.FINGERPRINT -> fingerprintAvailability
    }

fun BiometricSettings.isActive(type: BiometricType): Boolean =
    getAvailability(type) == BiometricAvailability.AVAILABLE &&
        when (type) {
            BiometricType.FINGERPRINT -> isFingerprintPreferenceOn
        }

fun BiometricSettings.withPreference(
    type: BiometricType,
    isEnabled: Boolean,
): BiometricSettings =
    when (type) {
        BiometricType.FINGERPRINT -> copy(isFingerprintPreferenceOn = isEnabled)
    }

fun BiometricSettings.isBiometricUnlockActive(): Boolean = isActive(BiometricType.FINGERPRINT)
