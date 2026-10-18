package com.example.voicelock.feature.applock

import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.voicelock.R
import com.example.voicelock.data.model.applock.AppLockItemUiModel
import com.example.voicelock.service.AppLockMonitorService
import com.example.voicelock.util.UiState
import com.example.voicelock.util.handleUiState
import com.example.voicelock.util.hasOverlayPermission
import com.example.voicelock.util.hasUsageAccess
import com.example.voicelock.util.setPreventDoubleClick

fun AppLockFragment.initView() {
    adapter.setListener(this)
    binding.rvApps.adapter = adapter
}

fun AppLockFragment.settingEvent() {
    binding.btnPermissionWarning.setPreventDoubleClick {
        navigator.navigateTo(R.id.action_appLockFragment_to_permissionsFragment)
    }
}

fun AppLockFragment.refreshPermissions() {
    viewModel.updatePermissionState(
        isUsageAccessGranted = requireContext().hasUsageAccess(),
        isOverlayPermissionGranted = requireContext().hasOverlayPermission(),
    )
}

fun AppLockFragment.updatePermissionWarning(isMissingPermission: Boolean) {
    binding.btnPermissionWarning.isVisible = isMissingPermission
}

fun AppLockFragment.updateItemsUiState(itemsUiState: UiState<List<AppLockItemUiModel>>) {
    itemsUiState.handleUiState(onSuccess = { items -> adapter.submitList(items) })
}

/** Starts / stops the monitor only when the decision changes, not on every state emission. */
fun AppLockFragment.syncMonitorService(shouldRun: Boolean?) {
    if (shouldRun == null) return
    val serviceIntent = Intent(requireContext(), AppLockMonitorService::class.java)
    if (shouldRun) {
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    } else {
        requireContext().stopService(serviceIntent)
    }
}
