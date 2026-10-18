package com.example.voicelock.feature.face.auth

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.data.model.face.getStatusInfo
import com.example.voicelock.databinding.FragmentFaceAuthBinding
import com.example.voicelock.feature.face.camera.FaceCameraController
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.collectFlowOnView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Face verification (see [FaceAuthViewModel]).
 *
 * Opened from Settings it is a test screen that can also re-enroll or delete the face profile.
 * Hosted by LockOverlayActivity with [BundleKey.KEY_LOCK_TARGET_PACKAGE] it unlocks that app: it
 * scans right away, marks the app unlocked and closes the overlay; the overlay's own
 * "try another method" button switches to PIN / pattern / voice.
 */
class FaceAuthFragment : BaseFragment<FragmentFaceAuthBinding, FaceAuthViewModel>(
    FragmentFaceAuthBinding::inflate,
    FaceAuthViewModel::class,
) {
    val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> onCameraPermissionResult(isGranted) }

    var cameraController: FaceCameraController? = null

    override fun init(view: View, savedInstanceState: Bundle?) {
        initView()
        settingEvent()
        onSystemBack { backEvent() }
        viewModel.initData(targetPackageName = arguments?.getString(BundleKey.KEY_LOCK_TARGET_PACKAGE))
        requestCamera()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onScreenStarted()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onScreenStopped()
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState.map { it.getStatusInfo() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateStatus(it) }
        viewModel.uiState.map { it.isAppUnlock }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateAppUnlock(it) }
        viewModel.uiEvent.collectFlowOnView(viewLifecycleOwner) { handleUiEvent(it) }
    }

    override fun onDestroyView() {
        releaseCamera()
        super.onDestroyView()
    }
}
