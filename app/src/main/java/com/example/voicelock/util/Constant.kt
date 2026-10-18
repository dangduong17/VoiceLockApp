package com.example.voicelock.util

import androidx.biometric.BiometricManager

object Constant {
    const val DEFAULT_PAGE_SIZE = 20

    const val LOCK_MAX_ATTEMPTS = 5
    const val LOCK_LOCKOUT_DURATION_SECONDS = 30

    const val APP_LOCK_SESSION_TTL_MS = 10_000L
    const val APP_LOCK_POLL_INTERVAL_MS = 500L

    const val VOICE_ENROLLMENT_SAMPLES = 3

    /** Floating voice-lock button: this many taps on it within the window open the voice lock command. */
    const val FLOATING_LOCK_TAP_COUNT = 3
    const val FLOATING_LOCK_TAP_WINDOW_MS = 1_500L
    const val FLOATING_LOCK_IDLE_DIM_MS = 3_000L


    /** F1: how long "apps locked" stays on the lock command screen before it closes. */
    const val VOICE_LOCK_CLOSE_DELAY_MS = 1_500L

    /** Minimum length of a security answer after normalization (see normalizeForComparison). */
    const val SECURITY_ANSWER_MIN_LENGTH = 2

    /** Stage-1 phrases for voice profiles enrolled before the owner's own phrase was transcribed. */
    val VOICE_UNLOCK_KEYWORDS = listOf("unlock", "mở khóa")

    /** Class 3 biometrics only (fingerprint on most phones); in-app face unlock is separate. */
    const val BIOMETRIC_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG

    /** MobileFaceNet (BSD-3-Clause, github.com/MCarlomagno/FaceRecognitionAuth), see assets. */
    const val FACE_MODEL_ASSET = "mobilefacenet.tflite"
    const val FACE_ENROLLMENT_SAMPLES = 5

    /** Gap between accepted enrollment frames, so the samples are not near-identical copies. */
    const val FACE_ENROLLMENT_SAMPLE_INTERVAL_MS = 300L

    /** Cosine similarity between L2-normalized MobileFaceNet embeddings; tune on real devices. */
    const val FACE_MATCH_THRESHOLD = 0.65f
    const val FACE_MAX_ANGLE_DEGREES = 20f

    /** Minimum face width relative to the frame width; smaller faces give unreliable embeddings. */
    const val FACE_MIN_WIDTH_RATIO = 0.25f
    const val FACE_EYE_OPEN_PROBABILITY = 0.6f
    const val FACE_EYE_CLOSED_PROBABILITY = 0.25f
    const val FACE_AUTH_TIMEOUT_MS = 15_000L
}
