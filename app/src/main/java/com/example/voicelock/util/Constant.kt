package com.example.voicelock.util

object Constant {
    const val DEFAULT_PAGE_SIZE = 20

    const val LOCK_MAX_ATTEMPTS = 5
    const val LOCK_LOCKOUT_DURATION_SECONDS = 30

    const val APP_LOCK_SESSION_TTL_MS = 10_000L
    const val APP_LOCK_POLL_INTERVAL_MS = 500L
}
