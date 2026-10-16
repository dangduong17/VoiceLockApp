package com.example.voicelock.feature.applock

import android.content.Intent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.voicelock.R
import com.example.voicelock.data.model.applock.AppLockUiState
import com.example.voicelock.data.model.applock.isMonitoringReady
import com.example.voicelock.feature.applock.adapter.AppLockAdapter
import com.example.voicelock.service.AppLockMonitorService
import com.example.voicelock.util.UiState
import com.example.voicelock.util.handleUiState
import com.example.voicelock.util.hasOverlayPermission
import com.example.voicelock.util.hasUsageAccess
import com.example.voicelock.util.setPreventDoubleClick

fun AppLockFragment.initView() {
    binding.rvApps.adapter = adapter
}

fun AppLockFragment.settingEvent() {
    adapter.setListener(
        object : AppLockAdapter.Listener {
            override fun onToggleLocked(
                packageName: String,
                isLocked: Boolean,
            ) {
                viewModel.onToggleLocked(packageName, isLocked)
            }
        },
    )

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

fun AppLockFragment.updateUiState(state: AppLockUiState) {
    val isMissingPermission = !state.isUsageAccessGranted || !state.isOverlayPermissionGranted
    binding.btnPermissionWarning.visibility = if (isMissingPermission) View.VISIBLE else View.GONE

    state.itemsUiState.handleUiState(
        onSuccess = { items -> adapter.submitList(items) },
    )

    syncMonitorService(state)
}

private fun AppLockFragment.syncMonitorService(state: AppLockUiState) {
    if (state.itemsUiState !is UiState.Success) return
    val serviceIntent = Intent(requireContext(), AppLockMonitorService::class.java)
    if (state.isMonitoringReady()) {
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    } else {
        requireContext().stopService(serviceIntent)
    }
}
