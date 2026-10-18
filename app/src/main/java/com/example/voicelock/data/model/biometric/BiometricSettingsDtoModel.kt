package com.example.voicelock.data.model.biometric

/**
 * Stored on/off switch plus what the device currently supports. The switch only takes effect
 * while the biometric is [BiometricAvailability.AVAILABLE] (e.g. the user may remove every
 * fingerprint from Android later), so screens and the lock overlay read the "active" helpers.
 */
data class BiometricSettingsDtoModel(
    val fingerprintAvailability: BiometricAvailability = BiometricAvailability.NO_HARDWARE,
    val isFingerprintPreferenceOn: Boolean = false,
)

fun BiometricSettingsDtoModel.getAvailability(type: BiometricType): BiometricAvailability =
    when (type) {
        BiometricType.FINGERPRINT -> fingerprintAvailability
    }

fun BiometricSettingsDtoModel.isActive(type: BiometricType): Boolean =
    getAvailability(type) == BiometricAvailability.AVAILABLE &&
        when (type) {
            BiometricType.FINGERPRINT -> isFingerprintPreferenceOn
        }

fun BiometricSettingsDtoModel.withPreference(
    type: BiometricType,
    isEnabled: Boolean,
): BiometricSettingsDtoModel =
    when (type) {
        BiometricType.FINGERPRINT -> copy(isFingerprintPreferenceOn = isEnabled)
    }

fun BiometricSettingsDtoModel.isBiometricUnlockActive(): Boolean = isActive(BiometricType.FINGERPRINT)
