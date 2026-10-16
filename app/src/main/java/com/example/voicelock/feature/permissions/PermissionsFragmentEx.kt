package com.example.voicelock.feature.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.voicelock.R
import com.example.voicelock.data.model.permissions.PermissionsUiState
import com.example.voicelock.util.hasOverlayPermission
import com.example.voicelock.util.hasUsageAccess
import com.example.voicelock.util.setPreventDoubleClick

fun PermissionsFragment.initView() {
    // Basic setup if any
}

fun PermissionsFragment.settingEvent() {
    binding.btnUsageAccess.setPreventDoubleClick {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    binding.btnOverlayAccess.setPreventDoubleClick {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireContext().packageName}")),
        )
    }
}

fun PermissionsFragment.refreshPermissions() {
    viewModel.updatePermissionState(
        isUsageAccessGranted = requireContext().hasUsageAccess(),
        isOverlayPermissionGranted = requireContext().hasOverlayPermission(),
    )
}

fun PermissionsFragment.updateUiState(state: PermissionsUiState) {
    binding.btnUsageAccess.isEnabled = !state.isUsageAccessGranted
    binding.btnUsageAccess.text =
        if (state.isUsageAccessGranted) getString(R.string.app_lock_granted) else getString(R.string.app_lock_open_settings)

    binding.btnOverlayAccess.isEnabled = !state.isOverlayPermissionGranted
    binding.btnOverlayAccess.text =
        if (state.isOverlayPermissionGranted) getString(R.string.app_lock_granted) else getString(R.string.app_lock_open_settings)
}
