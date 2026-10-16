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
