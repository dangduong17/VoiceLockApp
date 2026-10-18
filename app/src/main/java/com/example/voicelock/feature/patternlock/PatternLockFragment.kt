package com.example.voicelock.feature.patternlock

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.patternlock.getHeaderInfo
import com.example.voicelock.data.model.patternlock.isInputEnabled
import com.example.voicelock.databinding.FragmentPatternLockBinding
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.collectFlowOnView
import com.example.voicelock.util.getEnum
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PatternLockFragment : BaseFragment<FragmentPatternLockBinding, PatternLockViewModel>(
    FragmentPatternLockBinding::inflate,
    PatternLockViewModel::class,
) {
    override fun init(view: View, savedInstanceState: Bundle?) {
        settingEvent()
        onSystemBack { backEvent() }

        viewModel.initData(
            mode = arguments.getEnum(BundleKey.KEY_LOCK_MODE, PatternLockMode.SETUP),
            targetPackageName = arguments?.getString(BundleKey.KEY_LOCK_TARGET_PACKAGE),
        )
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState.map { it.getHeaderInfo() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateHeader(it) }
        viewModel.uiState.map { it.lockoutRemainingSeconds }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateLockout(it)
        }
        viewModel.uiState.map { it.isInputEnabled() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateInputEnabled(it)
        }
        viewModel.uiEvent.collectFlowOnView(viewLifecycleOwner) { handleUiEvent(it) }
    }
}
