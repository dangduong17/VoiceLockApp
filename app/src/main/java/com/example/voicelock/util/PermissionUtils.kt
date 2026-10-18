package com.example.voicelock.util

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.voicelock.service.VoiceLockDeviceAdminReceiver

@Suppress("DEPRECATION")
fun Context.hasUsageAccess(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode =
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName,
        )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun Context.hasOverlayPermission(): Boolean = Settings.canDrawOverlays(this)

/** F1: VoiceLock may lock the screen (device admin with the force-lock policy). */
fun Context.isDeviceAdminActive(): Boolean = getSystemService(DevicePolicyManager::class.java).isAdminActive(deviceAdminComponent())

fun Context.deviceAdminComponent(): ComponentName = ComponentName(this, VoiceLockDeviceAdminReceiver::class.java)

fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.packageUri(): Uri = Uri.fromParts(URI_SCHEME_PACKAGE, packageName, null)

/** This app's page in Android settings (permissions, auto-start fallback). */
fun Context.appDetailsSettingsIntent(): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri())

/** "Display over other apps" for this app. */
fun Context.overlaySettingsIntent(): Intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri())

private const val URI_SCHEME_PACKAGE = "package"
