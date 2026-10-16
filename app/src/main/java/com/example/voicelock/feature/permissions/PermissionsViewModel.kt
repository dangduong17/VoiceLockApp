package com.example.voicelock.feature.permissions

import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.data.model.permissions.PermissionsUiState

class PermissionsViewModel : BaseViewModel<PermissionsUiState, Nothing>(PermissionsUiState()) {

    fun updatePermissionState(
        isUsageAccessGranted: Boolean,
        isOverlayPermissionGranted: Boolean,
    ) {
        setState { copy(isUsageAccessGranted = isUsageAccessGranted, isOverlayPermissionGranted = isOverlayPermissionGranted) }
    }
}
