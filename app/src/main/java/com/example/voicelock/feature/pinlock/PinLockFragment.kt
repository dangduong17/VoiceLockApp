package com.example.voicelock.feature.pinlock

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.databinding.FragmentPinLockBinding
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.collectFlowOnView

class PinLockFragment : BaseFragment<FragmentPinLockBinding, PinLockViewModel>(
    FragmentPinLockBinding::inflate,
    PinLockViewModel::class,
) {
    override fun init(view: View, savedInstanceState: Bundle?) {
        initView()
        settingEvent()
        onSystemBack { backEvent() }

        val mode =
            arguments?.getString(BundleKey.KEY_LOCK_MODE)?.let {
                runCatching { PinLockMode.valueOf(it) }.getOrDefault(PinLockMode.SETUP)
            } ?: PinLockMode.SETUP
        val pinLength = arguments?.getInt(BundleKey.KEY_PIN_LENGTH) ?: 6
        viewModel.initData(mode = mode, knownPinLength = pinLength)
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
