package com.example.voicelock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Plain BroadcastReceiver (framework entry point, like AppLockMonitorService) that rebuilds the
 * app lock after the phone restarts or the app is updated (F0). The saved settings are read
 * asynchronously within goAsync()'s time budget.
 *
 * BOOT_COMPLETED arrives after the first unlock, when the credential-encrypted prefs / DataStore
 * holding the locked apps are readable. Xiaomi / HyperOS only delivers it when the app is allowed
 * to auto-start (see PermissionsFragment).
 */
class AppLockRestartReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val appLockMonitorLauncher: AppLockMonitorLauncher by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in RESTART_ACTIONS) return
        Timber.tag(TAG).d("Restart trigger: %s", intent.action)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                appLockMonitorLauncher.syncWithSavedSettings()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AppLockRestartReceiver"

        val RESTART_ACTIONS =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                // Fast-boot variants sent instead of BOOT_COMPLETED by some OEM builds (Xiaomi, HTC).
                "android.intent.action.QUICKBOOT_POWERON",
                "com.htc.intent.action.QUICKBOOT_POWERON",
            )
    }
}
