package com.example.voicelock.feature.applock

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.databinding.FragmentAppLockBinding
import com.example.voicelock.feature.applock.adapter.AppLockAdapter
import com.example.voicelock.util.collectFlowOnView

class AppLockFragment : BaseFragment<FragmentAppLockBinding, AppLockViewModel>(
    FragmentAppLockBinding::inflate,
    AppLockViewModel::class,
) {
    val adapter = AppLockAdapter()

    override fun init(view: View, savedInstanceState: Bundle?) {
        initView()
        settingEvent()
        refreshPermissions()
        viewModel.loadApps()
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState
            .collectFlowOnView(viewLifecycleOwner) { state ->
                updateUiState(state)
            }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }
}
