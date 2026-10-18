package com.example.voicelock.feature.applock

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.data.model.applock.getMonitorRunState
import com.example.voicelock.data.model.applock.isMissingPermission
import com.example.voicelock.databinding.FragmentAppLockBinding
import com.example.voicelock.feature.applock.adapter.AppLockAdapter
import com.example.voicelock.util.collectFlowOnView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AppLockFragment : BaseFragment<FragmentAppLockBinding, AppLockViewModel>(
    FragmentAppLockBinding::inflate,
    AppLockViewModel::class,
),
    AppLockAdapter.Listener {
    val adapter = AppLockAdapter()

    override fun init(view: View, savedInstanceState: Bundle?) {
        initView()
        settingEvent()
        refreshPermissions()
        viewModel.loadApps()
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState.map { it.isMissingPermission() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updatePermissionWarning(it)
        }
        viewModel.uiState.map { it.itemsUiState }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateItemsUiState(it) }
        viewModel.uiState.map { it.getMonitorRunState() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            syncMonitorService(it)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    override fun onToggleLocked(
        packageName: String,
        isLocked: Boolean,
    ) {
        viewModel.onToggleLocked(packageName = packageName, isLocked = isLocked)
    }
}
