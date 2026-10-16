package com.example.voicelock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.voicelock.R
import com.example.voicelock.domain.usecase.applock.GetLockedPackagesUseCase
import com.example.voicelock.domain.usecase.applock.IsAppUnlockedUseCase
import com.example.voicelock.feature.applock.LockOverlayActivity
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.Constant
import com.example.voicelock.util.Result
import com.example.voicelock.util.getDataOrDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Plain Service (outside the BaseFragment/BaseViewModel mandate, same justification as
 * LockOverlayActivity) that keeps running in the background with an ongoing notification,
 * polling UsageStatsManager for the current foreground app and triggering LockOverlayActivity
 * when a locked, not-session-unlocked, non-self package comes to the foreground.
 */
class AppLockMonitorService : Service() {

    private val getLockedPackagesUseCase: GetLockedPackagesUseCase by inject()
    private val isAppUnlockedUseCase: IsAppUnlockedUseCase by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    @Volatile
    private var lockedPackages: Set<String> = emptySet()
    private var lastForegroundPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startMonitoringIfNeeded()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startMonitoringIfNeeded() {
        if (monitorJob?.isActive == true) return
        monitorJob =
            serviceScope.launch {
                launch { observeLockedPackages() }
                pollForegroundApp()
            }
    }

    private suspend fun observeLockedPackages() {
        getLockedPackagesUseCase().collect { result ->
            if (result is Result.Success) {
                lockedPackages = result.data
            }
        }
    }

    private suspend fun pollForegroundApp() {
        while (true) {
            delay(Constant.APP_LOCK_POLL_INTERVAL_MS)
            val currentPackage = resolveForegroundPackage() ?: continue
            if (currentPackage == lastForegroundPackage) continue
            lastForegroundPackage = currentPackage
            maybeShowLockOverlay(currentPackage)
        }
    }

    private fun resolveForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - QUERY_WINDOW_MS, now)
        val event = UsageEvents.Event()
        var resolvedPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (isForegroundEvent(event)) {
                resolvedPackage = event.packageName
            }
        }
        return resolvedPackage
    }

    private fun isForegroundEvent(event: UsageEvents.Event): Boolean {
        if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) return true
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
    }

    private fun maybeShowLockOverlay(packageName: String) {
        if (packageName == applicationContext.packageName || packageName !in lockedPackages) return
        serviceScope.launch {
            val isUnlocked = isAppUnlockedUseCase(packageName).getDataOrDefault(false)
            if (isUnlocked) return@launch
            val overlayIntent =
                Intent(this@AppLockMonitorService, LockOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(BundleKey.KEY_LOCK_TARGET_PACKAGE, packageName)
                }
            startActivity(overlayIntent)
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.app_lock_notification_channel_name),
                    NotificationManager.IMPORTANCE_MIN,
                )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_lock_notification_title))
            .setContentText(getString(R.string.app_lock_notification_text))
            .setSmallIcon(R.drawable.ic_shield_check)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "app_lock_monitor_channel"
        private const val NOTIFICATION_ID = 1001
        private const val QUERY_WINDOW_MS = 10_000L
    }
}
