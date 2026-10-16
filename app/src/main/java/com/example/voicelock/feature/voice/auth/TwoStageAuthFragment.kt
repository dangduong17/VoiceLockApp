package com.example.voicelock.feature.voice.auth

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.data.model.voice.getStageInfo
import com.example.voicelock.databinding.FragmentTwoStageAuthBinding
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.collectFlowOnView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Two-stage voice check on a single utterance (see [TwoStageAuthViewModel]).
 *
 * Opened from Home/Settings it is a test screen (Lock/Unlock thresholds selectable). Hosted by
 * LockOverlayActivity with [BundleKey.KEY_LOCK_TARGET_PACKAGE] it unlocks that app: it listens right
 * away, marks the app unlocked and closes the overlay; the overlay's own "try another method"
 * button switches to PIN/pattern.
 */
class TwoStageAuthFragment : BaseFragment<FragmentTwoStageAuthBinding, TwoStageAuthViewModel>(
    FragmentTwoStageAuthBinding::inflate,
    TwoStageAuthViewModel::class,
) {
    val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> onMicPermissionResult(isGranted) }

    /** The lock overlay starts listening once by itself; afterwards only on return to the screen. */
    var hasAutoStarted = false

    override fun init(view: View, savedInstanceState: Bundle?) {
        initView()
        settingEvent()
        onSystemBack { backEvent() }
        viewModel.initData(targetPackageName = arguments?.getString(BundleKey.KEY_LOCK_TARGET_PACKAGE))
    }

    override fun onStart() {
        super.onStart()
        onScreenStarted()
    }

    override fun onStop() {
        super.onStop()
        viewModel.cancelAuthentication()
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState.map { it.getStageInfo() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateStage(it) }
        viewModel.uiState.map { it.heardText }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateHeardText(it) }
        viewModel.uiState.map { it.scores }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateScores(it) }
        viewModel.uiState.map { it.mode }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateMode(it) }
        viewModel.uiState.map { it.isAppUnlock }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateAppUnlock(it) }
        viewModel.uiEvent.collectFlowOnView(viewLifecycleOwner) { handleUiEvent(it) }
    }
}
