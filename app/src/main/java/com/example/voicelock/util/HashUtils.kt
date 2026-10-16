package com.example.voicelock.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object HashUtils {
    private const val SALT_BYTES = 16

    fun generateSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hash(
        value: String,
        salt: String,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + value).toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
