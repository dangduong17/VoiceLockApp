package com.example.voicelock.feature.securityquestion

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.data.model.securityquestion.SecurityQuestionMode
import com.example.voicelock.data.model.securityquestion.getLockout
import com.example.voicelock.data.model.securityquestion.getScreenInfo
import com.example.voicelock.data.model.securityquestion.isInputEnabled
import com.example.voicelock.databinding.FragmentSecurityQuestionBinding
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.collectFlowOnView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * F5 Security question (see [SecurityQuestionViewModel]). The mode comes from
 * [BundleKey.KEY_LOCK_MODE]; LockOverlayActivity also passes [BundleKey.KEY_LOCK_TARGET_PACKAGE].
 */
class SecurityQuestionFragment : BaseFragment<FragmentSecurityQuestionBinding, SecurityQuestionViewModel>(
    FragmentSecurityQuestionBinding::inflate,
    SecurityQuestionViewModel::class,
) {
    /** Index in the preset list; the last entry means "type my own question". */
    var selectedQuestionIndex = 0

    override fun init(view: View, savedInstanceState: Bundle?) {
        initView()
        settingEvent()
        onSystemBack { backEvent() }
        val mode =
            arguments?.getString(BundleKey.KEY_LOCK_MODE)
                ?.let { runCatching { SecurityQuestionMode.valueOf(it) }.getOrNull() }
                ?: SecurityQuestionMode.SETUP
        viewModel.initData(mode = mode, targetPackageName = arguments?.getString(BundleKey.KEY_LOCK_TARGET_PACKAGE))
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState.map { it.getScreenInfo() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateScreen(it) }
        viewModel.uiState.map { it.inputError }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateInputError(it) }
        viewModel.uiState.map { it.getLockout() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateLockout(it) }
        viewModel.uiState.map { it.isInputEnabled() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateInputEnabled(it)
        }
        viewModel.uiEvent.collectFlowOnView(viewLifecycleOwner) { handleUiEvent(it) }
    }
}
