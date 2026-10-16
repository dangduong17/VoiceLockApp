package com.example.voicelock.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings

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
