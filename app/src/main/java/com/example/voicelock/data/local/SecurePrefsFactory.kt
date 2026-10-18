package com.example.voicelock.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.security.KeyStore

private const val SECURE_PREFS_FILE_NAME = "voicelock_secure_prefs"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val TAG = "SecurePrefsFactory"

/**
 * When the encrypted file can no longer be decrypted (typically an Auto Backup restore or a
 * reinstall: the file came back but its Keystore master key did not), the unreadable data is
 * dropped and a fresh file is created, instead of crashing on every launch.
 */
fun createSecurePrefs(context: Context): SharedPreferences =
    try {
        openSecurePrefs(context)
    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Secure prefs unreadable, resetting them")
        context.deleteSharedPreferences(SECURE_PREFS_FILE_NAME)
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }
        openSecurePrefs(context)
    }

private fun openSecurePrefs(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedSharedPreferences.create(
        context,
        SECURE_PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
