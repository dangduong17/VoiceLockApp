package com.example.voicelock.data.model.applock

import com.example.voicelock.util.UiState

data class AppLockUiState(
    val itemsUiState: UiState<List<AppLockItemUiModel>> = UiState.None,
    val isUsageAccessGranted: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
)

fun AppLockUiState.hasLockedApps(): Boolean =
    (itemsUiState as? UiState.Success)?.data?.any { it.isLocked } == true

fun AppLockUiState.isMonitoringReady(): Boolean = isUsageAccessGranted && isOverlayPermissionGranted && hasLockedApps()

/** The monitor cannot work without usage access + overlay, so the screen points to the permissions. */
fun AppLockUiState.isMissingPermission(): Boolean = !isUsageAccessGranted || !isOverlayPermissionGranted

/** Whether the monitor service should run; null until the app list is loaded (nothing to decide yet). */
fun AppLockUiState.getMonitorRunState(): Boolean? = if (itemsUiState is UiState.Success) isMonitoringReady() else null
