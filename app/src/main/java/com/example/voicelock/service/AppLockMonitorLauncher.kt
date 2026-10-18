package com.example.voicelock.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.voicelock.domain.usecase.applock.GetLockedPackagesUseCase
import com.example.voicelock.domain.usecase.quicklock.GetFloatingLockEnabledUseCase
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.util.hasOverlayPermission
import com.example.voicelock.util.hasUsageAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Single place that (re)starts [AppLockMonitorService] from the saved settings, so the app lock
 * comes back by itself after a reboot, an app update or a force stop (F0). The service only runs
 * when at least one app is locked and both required permissions are granted.
 */
class AppLockMonitorLauncher(
    private val context: Context,
    private val getLockedPackagesUseCase: GetLockedPackagesUseCase,
    private val getFloatingLockEnabledUseCase: GetFloatingLockEnabledUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fire-and-forget variant for callers without a coroutine (e.g. MainActivity.onCreate). */
    fun syncWithSavedSettingsAsync() {
        scope.launch { syncWithSavedSettings() }
    }

    /** Starts the monitor if the saved settings need it; never stops it (AppLockFragment does). */
    suspend fun syncWithSavedSettings() {
        val lockedPackages = getLockedPackagesUseCase().getDataOrDefault(emptySet())
        val isReady = lockedPackages.isNotEmpty() && context.hasUsageAccess() && context.hasOverlayPermission()
        Timber.tag(TAG).d("Sync app lock monitor: locked=%d ready=%s", lockedPackages.size, isReady)
        if (isReady) start()
        // The floating lock button comes back the same way (it needs the overlay permission only).
        if (getFloatingLockEnabledUseCase().getDataOrDefault(false) && context.hasOverlayPermission()) {
            FloatingLockService.start(context)
        }
    }

    private fun start() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, AppLockMonitorService::class.java))
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException (Android 12+) when started from the background
            // without an exemption; the next app launch retries.
            Timber.tag(TAG).e(e, "App lock monitor could not be started")
        }
    }

    private companion object {
        const val TAG = "AppLockMonitorLauncher"
    }
}
