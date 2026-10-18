package com.example.voicelock.feature.pinlock

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.pinlock.canResetWithSecurityQuestion
import com.example.voicelock.data.model.pinlock.getDotsInfo
import com.example.voicelock.data.model.pinlock.getHeaderInfo
import com.example.voicelock.data.model.pinlock.getLengthToggleInfo
import com.example.voicelock.data.model.pinlock.isKeypadEnabled
import com.example.voicelock.databinding.FragmentPinLockBinding
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.Constant
import com.example.voicelock.util.collectFlowOnView
import com.example.voicelock.util.getEnum
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PinLockFragment : BaseFragment<FragmentPinLockBinding, PinLockViewModel>(
    FragmentPinLockBinding::inflate,
    PinLockViewModel::class,
) {
    override fun init(view: View, savedInstanceState: Bundle?) {
        settingEvent()
        onSystemBack { backEvent() }

        viewModel.initData(
            mode = arguments.getEnum(BundleKey.KEY_LOCK_MODE, PinLockMode.SETUP),
            knownPinLength = arguments?.getInt(BundleKey.KEY_PIN_LENGTH, Constant.PIN_LENGTH_DEFAULT) ?: Constant.PIN_LENGTH_DEFAULT,
            targetPackageName = arguments?.getString(BundleKey.KEY_LOCK_TARGET_PACKAGE),
        )
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState.map { it.getHeaderInfo() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateHeader(it) }
        viewModel.uiState.map { it.getDotsInfo() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateDots(it) }
        viewModel.uiState.map { it.getLengthToggleInfo() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateLengthToggle(it)
        }
        viewModel.uiState.map { it.canResetWithSecurityQuestion() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateForgotPin(it)
        }
        viewModel.uiState.map { it.lockoutRemainingSeconds }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateLockout(it)
        }
        viewModel.uiState.map { it.isKeypadEnabled() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateKeypadEnabled(it)
        }
        viewModel.uiEvent.collectFlowOnView(viewLifecycleOwner) { handleUiEvent(it) }
    }
}
