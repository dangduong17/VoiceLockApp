package com.example.voicelock.feature.permissions

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.databinding.FragmentPermissionsBinding
import com.example.voicelock.util.collectFlowOnView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PermissionsFragment : BaseFragment<FragmentPermissionsBinding, PermissionsViewModel>(
    FragmentPermissionsBinding::inflate,
    PermissionsViewModel::class,
) {
    override fun init(view: View, savedInstanceState: Bundle?) {
        initView()
        settingEvent()
        refreshPermissions()
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState.map { it.isUsageAccessGranted }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateUsageAccess(it)
        }
        viewModel.uiState.map { it.isDeviceAdminActive }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateDeviceAdmin(it)
        }
        viewModel.uiState.map { it.isOverlayPermissionGranted }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateOverlayAccess(it)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }
}
