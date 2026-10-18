package com.example.voicelock.feature.face.enrollment

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import androidx.core.content.ContextCompat
import com.example.voicelock.R
import com.example.voicelock.data.model.face.FaceEnrollmentError
import com.example.voicelock.data.model.face.FaceEnrollmentMessage
import com.example.voicelock.data.model.face.FaceEnrollmentPhase
import com.example.voicelock.data.model.face.FaceEnrollmentProgress
import com.example.voicelock.feature.face.camera.FaceCameraController
import com.example.voicelock.feature.face.getFaceHintText
import com.example.voicelock.feature.face.getFaceEngineErrorText
import com.example.voicelock.util.Constant
import com.example.voicelock.util.displayToast
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.setPreventDoubleClickScaleView

fun FaceEnrollmentFragment.initView() {
    binding.tvFaceEnrollSubtitle.text = getString(R.string.face_enroll_subtitle, Constant.FACE_ENROLLMENT_SAMPLES)
}

fun FaceEnrollmentFragment.backEvent() {
    navigator.navigateUp()
}

fun FaceEnrollmentFragment.settingEvent() {
    binding.btnFaceEnrollAction.setPreventDoubleClickScaleView { actionEvent() }
    binding.btnFaceEnrollRestart.setPreventDoubleClick { viewModel.restartEnrollment() }
}

/** The main button asks for the camera, or closes once enrollment is done (or impossible). */
fun FaceEnrollmentFragment.actionEvent() {
    when (viewModel.uiState.value.phase) {
        FaceEnrollmentPhase.COMPLETED, FaceEnrollmentPhase.ENGINE_ERROR -> navigator.navigateUp()
        FaceEnrollmentPhase.CAMERA_PERMISSION_REQUIRED -> requestCamera()
        FaceEnrollmentPhase.INITIALIZING, FaceEnrollmentPhase.CAPTURING, FaceEnrollmentPhase.PROCESSING -> {
            // Busy: the button is disabled
        }
    }
}

fun FaceEnrollmentFragment.requestCamera() {
    val isGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    if (isGranted) {
        onCameraPermissionResult(true)
    } else {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
}

fun FaceEnrollmentFragment.onCameraPermissionResult(isGranted: Boolean) {
    viewModel.onCameraPermissionResult(isGranted)
    if (isGranted) {
        startCamera()
    } else {
        displayToast(getString(R.string.face_camera_permission_denied))
    }
}

private fun FaceEnrollmentFragment.startCamera() {
    if (cameraController != null) return
    cameraController =
        FaceCameraController(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            previewView = binding.previewFace,
            shouldAnalyze = { viewModel.isAcceptingFrames() },
            onFrame = { bitmap -> viewModel.onCameraFrame(bitmap) },
        ).also { it.start() }
}

fun FaceEnrollmentFragment.releaseCamera() {
    cameraController?.release()
    cameraController = null
}

fun FaceEnrollmentFragment.updateProgress(progress: FaceEnrollmentProgress) = with(binding) {
    progressFaceSamples.max = progress.requiredCount
    progressFaceSamples.setProgressCompat(progress.capturedCount, true)
    tvFaceSampleCount.text = getString(R.string.face_enroll_sample_count, progress.capturedCount, progress.requiredCount)
    tvFaceEnrollStatus.text = getStatusText(progress)

    val phase = progress.phase
    val isBusy =
        phase == FaceEnrollmentPhase.INITIALIZING || phase == FaceEnrollmentPhase.CAPTURING || phase == FaceEnrollmentPhase.PROCESSING
    progressFaceScan.visibility = if (isBusy) View.VISIBLE else View.GONE
    btnFaceEnrollAction.isEnabled = !isBusy
    btnFaceEnrollAction.alpha = if (isBusy) 0.5f else 1f
    btnFaceEnrollAction.text = getString(getActionTextRes(phase))
    val canRestart =
        phase == FaceEnrollmentPhase.COMPLETED || (phase == FaceEnrollmentPhase.CAPTURING && progress.capturedCount > 0)
    btnFaceEnrollRestart.visibility = if (canRestart) View.VISIBLE else View.INVISIBLE
}

private fun FaceEnrollmentFragment.getStatusText(progress: FaceEnrollmentProgress): String =
    when (progress.phase) {
        FaceEnrollmentPhase.INITIALIZING -> getString(R.string.face_status_initializing)
        FaceEnrollmentPhase.ENGINE_ERROR -> getFaceEngineErrorText(progress.engineStatus)
        FaceEnrollmentPhase.CAMERA_PERMISSION_REQUIRED -> getString(R.string.face_status_camera_permission)
        FaceEnrollmentPhase.CAPTURING -> getString(R.string.face_enroll_status_capturing)
        FaceEnrollmentPhase.PROCESSING -> getString(R.string.face_enroll_status_processing)
        FaceEnrollmentPhase.COMPLETED -> getString(R.string.face_enroll_status_completed)
    }

private fun getActionTextRes(phase: FaceEnrollmentPhase): Int =
    when (phase) {
        FaceEnrollmentPhase.COMPLETED -> R.string.face_enroll_action_done
        FaceEnrollmentPhase.ENGINE_ERROR -> R.string.face_action_back
        FaceEnrollmentPhase.CAMERA_PERMISSION_REQUIRED -> R.string.face_action_grant_camera
        FaceEnrollmentPhase.INITIALIZING, FaceEnrollmentPhase.CAPTURING, FaceEnrollmentPhase.PROCESSING ->
            R.string.face_enroll_action_capturing
    }

fun FaceEnrollmentFragment.updateMessage(message: FaceEnrollmentMessage) {
    val errorText =
        when (message.error) {
            FaceEnrollmentError.NONE -> null
            FaceEnrollmentError.INCONSISTENT_SAMPLES -> getString(R.string.face_enroll_error_inconsistent)
            FaceEnrollmentError.PROCESSING_FAILED -> getString(R.string.face_enroll_error_processing)
        }
    val text = errorText ?: getFaceHintText(message.hint)
    val colorRes = if (errorText != null) R.color.error_red else R.color.text_secondary
    binding.tvFaceEnrollMessage.visibility = if (text == null) View.INVISIBLE else View.VISIBLE
    binding.tvFaceEnrollMessage.text = text.orEmpty()
    binding.tvFaceEnrollMessage.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
}
