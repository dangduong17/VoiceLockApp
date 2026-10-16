package com.example.voicelock.feature.home

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.databinding.FragmentHomeBinding
import com.example.voicelock.util.collectFlowOnView

class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>(
    FragmentHomeBinding::inflate,
    HomeViewModel::class,
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
