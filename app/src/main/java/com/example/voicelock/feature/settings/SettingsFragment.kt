package com.example.voicelock.feature.settings

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.databinding.FragmentSettingsBinding
import com.example.voicelock.util.collectFlowOnView

class SettingsFragment : BaseFragment<FragmentSettingsBinding, SettingsViewModel>(
    FragmentSettingsBinding::inflate,
    SettingsViewModel::class,
) {
    override fun init(view: View, savedInstanceState: Bundle?) {
        initView()
        settingEvent()
        viewModel.initData()
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState
            .collectFlowOnView(viewLifecycleOwner) { state ->
                updateUiState(state)
            }

        viewModel.uiEvent
            .collectFlowOnView(viewLifecycleOwner) { event ->
                handleUiEvent(event)
            }
    }
}
