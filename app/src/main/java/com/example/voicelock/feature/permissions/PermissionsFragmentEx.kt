package com.example.voicelock.feature.permissions

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import com.example.voicelock.R
import com.example.voicelock.data.model.permissions.PermissionsUiState
import com.example.voicelock.util.deviceAdminComponent
import com.example.voicelock.util.hasOverlayPermission
import com.example.voicelock.util.isDeviceAdminActive
import com.example.voicelock.util.isAutoStartPermissionRequired
import com.example.voicelock.util.openAutoStartSettings
import com.example.voicelock.util.hasUsageAccess
import com.example.voicelock.util.setPreventDoubleClick

fun PermissionsFragment.initView() {
    // Xiaomi / HyperOS only: Android cannot report this setting, so the row is a shortcut, not a status.
    val isAutoStartRequired = isAutoStartPermissionRequired()
    binding.rowAutoStart.visibility = if (isAutoStartRequired) View.VISIBLE else View.GONE
    binding.dividerAutoStart.visibility = if (isAutoStartRequired) View.VISIBLE else View.GONE
}

fun PermissionsFragment.settingEvent() {
    binding.btnUsageAccess.setPreventDoubleClick {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    binding.btnAutoStart.setPreventDoubleClick { autoStartEvent() }

    binding.btnDeviceAdmin.setPreventDoubleClick { deviceAdminEvent() }

    binding.btnOverlayAccess.setPreventDoubleClick {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireContext().packageName}")),
        )
    }
}

/** F1: Android's own "activate device admin" screen, with our explanation of the single force-lock policy. */
fun PermissionsFragment.deviceAdminEvent() {
    val intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, requireContext().deviceAdminComponent())
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.permission_device_admin_explanation))
    runCatching { startActivity(intent) }
}

fun PermissionsFragment.autoStartEvent() {
    openAutoStartSettings()
}

fun PermissionsFragment.refreshPermissions() {
    viewModel.updatePermissionState(
        isUsageAccessGranted = requireContext().hasUsageAccess(),
        isOverlayPermissionGranted = requireContext().hasOverlayPermission(),
        isDeviceAdminActive = requireContext().isDeviceAdminActive(),
    )
}

fun PermissionsFragment.updateUiState(state: PermissionsUiState) {
    binding.btnUsageAccess.isEnabled = !state.isUsageAccessGranted
    binding.btnUsageAccess.text =
        if (state.isUsageAccessGranted) getString(R.string.app_lock_granted) else getString(R.string.app_lock_open_settings)

    binding.btnDeviceAdmin.isEnabled = !state.isDeviceAdminActive
    binding.btnDeviceAdmin.text =
        if (state.isDeviceAdminActive) getString(R.string.app_lock_granted) else getString(R.string.app_lock_open_settings)

    binding.btnOverlayAccess.isEnabled = !state.isOverlayPermissionGranted
    binding.btnOverlayAccess.text =
        if (state.isOverlayPermissionGranted) getString(R.string.app_lock_granted) else getString(R.string.app_lock_open_settings)
}
