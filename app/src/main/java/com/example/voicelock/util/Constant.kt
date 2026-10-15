package com.example.voicelock.util

object Constant {
    const val DEFAULT_PAGE_SIZE = 20

    // Network (base scaffold)
    const val API_BASE_URL = "https://api.example.com/"
    const val HTTP_CACHE_DIR = "http_cache"
    const val HTTP_CACHE_SIZE_BYTES = 10L * 1024L * 1024L
    const val NETWORK_TIMEOUT_SECONDS = 30L

    /** Speaker-embedding model in app/src/main/assets. If missing, the MFCC fallback is used. */
    const val SPEAKER_MODEL_ASSET = "speaker_embedding.tflite"

    /**
     * Cosine-similarity acceptance threshold for the TFLite model. Must be calibrated for the
     * specific model (measure genuine vs. impostor score distributions and pick the EER point).
     */
    const val TFLITE_THRESHOLD = 0.60f

    /** Acceptance threshold for the MFCC-statistics fallback embedder. */
    const val MFCC_FALLBACK_THRESHOLD = 0.90f

    // Recording
    const val SAMPLE_RATE_HZ = 16_000
    const val ENROLL_SAMPLE_COUNT = 3

    /** Recording length after the "speak now" cue. */
    const val RECORD_DURATION_MS = 3_000L

    /** The cue appears after a random delay in this range (liveness check). */
    const val CUE_DELAY_MIN_MS = 600L
    const val CUE_DELAY_MAX_MS = 1_500L

    /** dBFS mapped to 0% on the input level meter (0 dBFS = 100%). */
    const val LEVEL_FLOOR_DBFS = -60f
    const val LEVEL_MAX_PERCENT = 100

    // Sample quality
    const val MIN_SPEECH_MS = 1_000L
    const val MIN_PEAK_DBFS = -40f

    // Lock screen
    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_MS = 30_000L
    const val UNLOCK_ANIMATION_MS = 300L
    const val MILLIS_PER_SECOND = 1_000L

    // Backup PIN
    const val PIN_LENGTH = 6

    /** Short per-key debounce: stops accidental double taps without dropping repeated digits. */
    const val PIN_KEY_DEBOUNCE_MS = 80L
}
