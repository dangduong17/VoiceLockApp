package com.example.voicelock.feature.permissions

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.databinding.FragmentPermissionsBinding
import com.example.voicelock.util.collectFlowOnView

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
