package com.example.voicelock.feature.applock

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.applock.AppLockUiState
import com.example.voicelock.data.model.applock.toAppLockItem
import com.example.voicelock.data.model.applock.toPresentation
import com.example.voicelock.domain.usecase.applock.GetInstalledAppsUseCase
import com.example.voicelock.domain.usecase.applock.GetLockedPackagesUseCase
import com.example.voicelock.domain.usecase.applock.SetAppLockedUseCase
import com.example.voicelock.util.UiState
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.util.getDataOrNull

class AppLockViewModel(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val getLockedPackagesUseCase: GetLockedPackagesUseCase,
    private val setAppLockedUseCase: SetAppLockedUseCase,
) : BaseViewModel<AppLockUiState, Nothing>(AppLockUiState()) {

    fun loadApps() {
        val current = getCurrentState().itemsUiState
        if (current is UiState.Loading || current is UiState.Success) return
        setState { copy(itemsUiState = UiState.Loading) }
        launchIO(onError = { onLoadFailed(it) }) {
            val installedApps = getInstalledAppsUseCase().getDataOrNull()
            if (installedApps == null) {
                onLoadFailed(IllegalStateException("Không tải được danh sách ứng dụng"))
                return@launchIO
            }
            val lockedPackages = getLockedPackagesUseCase().getDataOrDefault(emptySet())
            val items = installedApps.map { it.toPresentation().toAppLockItem(lockedPackages) }
            setState { copy(itemsUiState = UiState.Success(items)) }
        }
    }

    fun onToggleLocked(
        packageName: String,
        isLocked: Boolean,
    ) {
        launchIO {
            setAppLockedUseCase(packageName = packageName, isLocked = isLocked).getDataOrNull()
            val updatedLockedPackages = getLockedPackagesUseCase().getDataOrDefault(emptySet())
            val currentItems = (getCurrentState().itemsUiState as? UiState.Success)?.data ?: return@launchIO
            val updatedItems = currentItems.map { it.copy(isLocked = it.packageName in updatedLockedPackages) }
            setState { copy(itemsUiState = UiState.Success(updatedItems)) }
        }
    }

    fun updatePermissionState(
        isUsageAccessGranted: Boolean,
        isOverlayPermissionGranted: Boolean,
    ) {
        setState { copy(isUsageAccessGranted = isUsageAccessGranted, isOverlayPermissionGranted = isOverlayPermissionGranted) }
    }

    private fun onLoadFailed(throwable: Throwable) {
        setState { copy(itemsUiState = UiState.Error(throwable)) }
    }
}
