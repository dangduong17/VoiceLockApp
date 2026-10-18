package com.example.voicelock.feature.permissions

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.provider.Settings
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.voicelock.R
import com.example.voicelock.util.deviceAdminComponent
import com.example.voicelock.util.hasOverlayPermission
import com.example.voicelock.util.hasUsageAccess
import com.example.voicelock.util.isAutoStartPermissionRequired
import com.example.voicelock.util.isDeviceAdminActive
import com.example.voicelock.util.openAutoStartSettings
import com.example.voicelock.util.overlaySettingsIntent
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.startActivitySafely

fun PermissionsFragment.initView() {
    // Xiaomi / HyperOS only: Android cannot report this setting, so the row is a shortcut, not a status.
    val isAutoStartRequired = isAutoStartPermissionRequired()
    binding.rowAutoStart.isVisible = isAutoStartRequired
    binding.dividerAutoStart.isVisible = isAutoStartRequired
}

fun PermissionsFragment.settingEvent() {
    binding.btnUsageAccess.setPreventDoubleClick { usageAccessEvent() }

    binding.btnAutoStart.setPreventDoubleClick { autoStartEvent() }

    binding.btnDeviceAdmin.setPreventDoubleClick { deviceAdminEvent() }

    binding.btnOverlayAccess.setPreventDoubleClick { overlayAccessEvent() }
}

fun PermissionsFragment.usageAccessEvent() {
    startActivitySafely(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
}

fun PermissionsFragment.overlayAccessEvent() {
    startActivitySafely(requireContext().overlaySettingsIntent())
}

/** F1: Android's own "activate device admin" screen, with our explanation of the single force-lock policy. */
fun PermissionsFragment.deviceAdminEvent() {
    val intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, requireContext().deviceAdminComponent())
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.permission_device_admin_explanation))
    startActivitySafely(intent)
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

fun PermissionsFragment.updateUsageAccess(isGranted: Boolean) {
    binding.btnUsageAccess.updatePermissionButton(isGranted)
}

fun PermissionsFragment.updateDeviceAdmin(isActive: Boolean) {
    binding.btnDeviceAdmin.updatePermissionButton(isActive)
}

fun PermissionsFragment.updateOverlayAccess(isGranted: Boolean) {
    binding.btnOverlayAccess.updatePermissionButton(isGranted)
}

/** A granted permission's button is disabled and says so; otherwise it opens the settings page. */
private fun TextView.updatePermissionButton(isGranted: Boolean) {
    isEnabled = !isGranted
    setText(if (isGranted) R.string.app_lock_granted else R.string.app_lock_open_settings)
}
