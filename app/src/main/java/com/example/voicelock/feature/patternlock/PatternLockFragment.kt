package com.example.voicelock.feature.patternlock

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.databinding.FragmentPatternLockBinding
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.collectFlowOnView

class PatternLockFragment : BaseFragment<FragmentPatternLockBinding, PatternLockViewModel>(
    FragmentPatternLockBinding::inflate,
    PatternLockViewModel::class,
) {
    override fun init(view: View, savedInstanceState: Bundle?) {
        initView()
        settingEvent()
        onSystemBack { backEvent() }

        val mode =
            arguments?.getString(BundleKey.KEY_LOCK_MODE)?.let {
                runCatching { PatternLockMode.valueOf(it) }.getOrDefault(PatternLockMode.SETUP)
            } ?: PatternLockMode.SETUP
        val targetPackageName = arguments?.getString(BundleKey.KEY_LOCK_TARGET_PACKAGE)
        viewModel.initData(mode = mode, targetPackageName = targetPackageName)
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
