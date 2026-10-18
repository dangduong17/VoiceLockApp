package com.example.voicelock.feature.biometric.auth

import android.os.Bundle
import android.view.View
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.databinding.FragmentBiometricAuthBinding
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.collectFlowOnView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Fingerprint / face unlock through Android's BiometricPrompt (see [BiometricAuthViewModel]).
 * Hosted by LockOverlayActivity with [BundleKey.KEY_LOCK_TARGET_PACKAGE].
 */
class BiometricAuthFragment : BaseFragment<FragmentBiometricAuthBinding, BiometricAuthViewModel>(
    FragmentBiometricAuthBinding::inflate,
    BiometricAuthViewModel::class,
) {
    override fun init(view: View, savedInstanceState: Bundle?) {
        initView()
        settingEvent()
        onSystemBack { backEvent() }
        viewModel.initData(targetPackageName = arguments?.getString(BundleKey.KEY_LOCK_TARGET_PACKAGE))
    }

    override fun onStart() {
        super.onStart()
        viewModel.onScreenStarted()
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState
            .map { it.stage to it.errorMessage }
            .distinctUntilChanged()
            .collectFlowOnView(viewLifecycleOwner) { (stage, errorMessage) -> updateStage(stage, errorMessage) }
        viewModel.uiEvent.collectFlowOnView(viewLifecycleOwner) { handleUiEvent(it) }
    }
}
