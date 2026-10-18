package com.example.voicelock.feature.home

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.data.model.home.getLockInfo
import com.example.voicelock.data.model.home.getVoiceInfo
import com.example.voicelock.databinding.FragmentHomeBinding
import com.example.voicelock.util.collectFlowOnView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>(
    FragmentHomeBinding::inflate,
    HomeViewModel::class,
) {
    override fun init(view: View, savedInstanceState: Bundle?) {
        settingEvent()
    }

    override fun onResume() {
        super.onResume()
        // Also refreshes after returning from voice / face enrollment / PIN setup screens.
        viewModel.initData()
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState.map { it.isProtectionEnabled }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateProtection(it)
        }
        viewModel.uiState.map { it.getVoiceInfo() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateVoice(it) }
        viewModel.uiState.map { it.getLockInfo() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateLockMethods(it) }
        viewModel.uiState.map { it.biometricSettings }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateFingerprint(it)
        }
        viewModel.uiState.map { it.faceSettings }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateFace(it) }
        viewModel.uiEvent.collectFlowOnView(viewLifecycleOwner) { handleUiEvent(it) }
    }
}
