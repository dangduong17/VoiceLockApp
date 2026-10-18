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
