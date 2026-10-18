package com.example.voicelock.util

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.fragment.app.Fragment
import java.util.Locale

private val XIAOMI_BRANDS = setOf("xiaomi", "redmi", "poco")

/**
 * Xiaomi / Redmi / POCO (MIUI, HyperOS) block BOOT_COMPLETED for apps the user has not allowed
 * to auto-start, so the app lock cannot come back after a reboot without it.
 */
fun isAutoStartPermissionRequired(): Boolean =
    Build.MANUFACTURER.lowercase(Locale.ROOT) in XIAOMI_BRANDS || Build.BRAND.lowercase(Locale.ROOT) in XIAOMI_BRANDS

/** Opens the Xiaomi auto-start manager, or this app's settings page when it is not available. */
fun Fragment.openAutoStartSettings() {
    val autoStartIntent =
        Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        )
    val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireContext().packageName, null))
    runCatching { startActivity(autoStartIntent) }
        .recoverCatching { startActivity(appDetailsIntent) }
}
