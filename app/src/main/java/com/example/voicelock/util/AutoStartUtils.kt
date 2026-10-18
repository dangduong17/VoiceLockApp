package com.example.voicelock.util

import android.content.ComponentName
import android.content.Intent
import android.os.Build
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
    val autoStartIntent = Intent().setComponent(ComponentName(MIUI_SECURITY_PACKAGE, MIUI_AUTO_START_ACTIVITY))
    if (!startActivitySafely(autoStartIntent)) startActivitySafely(requireContext().appDetailsSettingsIntent())
}

private const val MIUI_SECURITY_PACKAGE = "com.miui.securitycenter"
private const val MIUI_AUTO_START_ACTIVITY = "com.miui.permcenter.autostart.AutoStartManagementActivity"
