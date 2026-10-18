package com.example.voicelock.feature.face.enrollment

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.data.model.face.getMessage
import com.example.voicelock.data.model.face.getProgress
import com.example.voicelock.databinding.FragmentFaceEnrollmentBinding
import com.example.voicelock.feature.face.camera.FaceCameraController
import com.example.voicelock.util.collectFlowOnView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Face enrollment: the camera runs while the user looks at it; live, frontal frames are captured
 * automatically until enough samples are saved (see [FaceEnrollmentViewModel]).
 */
class FaceEnrollmentFragment : BaseFragment<FragmentFaceEnrollmentBinding, FaceEnrollmentViewModel>(
    FragmentFaceEnrollmentBinding::inflate,
    FaceEnrollmentViewModel::class,
) {
    val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> onCameraPermissionResult(isGranted) }

    var cameraController: FaceCameraController? = null

    override fun init(view: View, savedInstanceState: Bundle?) {
        initView()
        settingEvent()
        onSystemBack { backEvent() }
        requestCamera()
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState.map { it.getProgress() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateProgress(it) }
        viewModel.uiState.map { it.getMessage() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateMessage(it) }
    }

    override fun onDestroyView() {
        releaseCamera()
        super.onDestroyView()
    }
}
