package com.example.voicelock.util

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.provider.Settings
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
