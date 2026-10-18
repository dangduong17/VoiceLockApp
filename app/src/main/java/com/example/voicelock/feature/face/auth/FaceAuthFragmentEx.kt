package com.example.voicelock.feature.face.auth

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import androidx.core.content.ContextCompat
import com.example.voicelock.R
import com.example.voicelock.data.model.face.FaceAuthStage
import com.example.voicelock.data.model.face.FaceAuthStatusInfo
import com.example.voicelock.data.model.face.FaceAuthUiEvent
import com.example.voicelock.feature.face.camera.FaceCameraController
import com.example.voicelock.feature.face.getFaceEngineErrorText
import com.example.voicelock.feature.face.getFaceHintText
import com.example.voicelock.util.Constant
import com.example.voicelock.util.MessageType
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.setPreventDoubleClickScaleView
import com.example.voicelock.util.showMessage
import com.example.voicelock.util.showPermissionDeniedMessage

fun FaceAuthFragment.initView() {
    // Basic setup if any
}

fun FaceAuthFragment.backEvent() {
    navigator.navigateUp()
}

fun FaceAuthFragment.settingEvent() {
    binding.btnFaceAuthAction.setPreventDoubleClickScaleView { actionEvent() }
    binding.btnFaceReEnroll.setPreventDoubleClick { reEnrollEvent() }
    binding.btnFaceDelete.setPreventDoubleClick { deleteEvent() }
}

fun FaceAuthFragment.reEnrollEvent() {
    navigator.navigateTo(R.id.action_faceAuthFragment_to_faceEnrollmentFragment)
}

fun FaceAuthFragment.deleteEvent() {
    viewModel.deleteProfile()
}

/** Without a face profile the button opens enrollment; otherwise it asks for the camera or (re)scans. */
fun FaceAuthFragment.actionEvent() {
    val state = viewModel.uiState.value
    when (state.stage) {
        FaceAuthStage.NOT_ENROLLED ->
            if (!state.isAppUnlock) navigator.navigateTo(R.id.action_faceAuthFragment_to_faceEnrollmentFragment)
        FaceAuthStage.CAMERA_PERMISSION_REQUIRED -> requestCamera()
        FaceAuthStage.ENGINE_ERROR -> if (!state.isAppUnlock) navigator.navigateUp()
        FaceAuthStage.IDLE, FaceAuthStage.SUCCESS, FaceAuthStage.FAILED -> viewModel.startScan()
        FaceAuthStage.INITIALIZING, FaceAuthStage.SCANNING -> {
            // Busy: the button is disabled
        }
    }
}

fun FaceAuthFragment.requestCamera() {
    val isGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    if (isGranted) {
        onCameraPermissionResult(true)
    } else {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
}

fun FaceAuthFragment.onCameraPermissionResult(isGranted: Boolean) {
    viewModel.onCameraPermissionResult(isGranted)
    if (isGranted) {
        startCamera()
    } else {
        showPermissionDeniedMessage(getString(R.string.face_camera_permission_denied))
    }
}

private fun FaceAuthFragment.startCamera() {
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

fun FaceAuthFragment.releaseCamera() {
    cameraController?.release()
    cameraController = null
}

fun FaceAuthFragment.updateStatus(info: FaceAuthStatusInfo) = with(binding) {
    val stage = info.stage
    tvFaceAuthStatus.text = getStageText(info)
    tvFaceAuthStatus.setTextColor(ContextCompat.getColor(requireContext(), getStageColorRes(stage)))

    val hintText = if (stage == FaceAuthStage.SCANNING) getFaceHintText(info.hint) else null
    tvFaceAuthHint.visibility = if (hintText == null) View.INVISIBLE else View.VISIBLE
    tvFaceAuthHint.text = hintText.orEmpty()

    tvFaceSimilarity.visibility = if (info.bestSimilarity == null) View.GONE else View.VISIBLE
    info.bestSimilarity?.let { tvFaceSimilarity.text = getString(R.string.face_auth_similarity, it, Constant.FACE_MATCH_THRESHOLD) }

    val isBusy = stage == FaceAuthStage.INITIALIZING || stage == FaceAuthStage.SCANNING
    progressFaceScan.visibility = if (isBusy) View.VISIBLE else View.GONE
    btnFaceAuthAction.isEnabled = !isBusy
    btnFaceAuthAction.alpha = if (isBusy) 0.5f else 1f
    btnFaceAuthAction.text = getString(getActionTextRes(stage))

    val hasProfile = stage != FaceAuthStage.NOT_ENROLLED && stage != FaceAuthStage.INITIALIZING
    btnFaceDelete.isEnabled = hasProfile
    btnFaceDelete.alpha = if (hasProfile) 1f else 0.5f
}

private fun FaceAuthFragment.getStageText(info: FaceAuthStatusInfo): String =
    when (info.stage) {
        FaceAuthStage.INITIALIZING -> getString(R.string.face_status_initializing)
        FaceAuthStage.ENGINE_ERROR -> getFaceEngineErrorText(info.engineStatus)
        FaceAuthStage.NOT_ENROLLED -> getString(R.string.face_auth_stage_not_enrolled)
        FaceAuthStage.CAMERA_PERMISSION_REQUIRED -> getString(R.string.face_status_camera_permission)
        FaceAuthStage.IDLE -> getString(R.string.face_auth_stage_idle)
        FaceAuthStage.SCANNING -> getString(R.string.face_auth_stage_scanning)
        FaceAuthStage.SUCCESS -> getString(R.string.face_auth_stage_success)
        FaceAuthStage.FAILED -> getString(R.string.face_auth_stage_failed)
    }

private fun getStageColorRes(stage: FaceAuthStage): Int =
    when (stage) {
        FaceAuthStage.FAILED, FaceAuthStage.ENGINE_ERROR -> R.color.error_red
        FaceAuthStage.SUCCESS -> R.color.primary_blue
        FaceAuthStage.INITIALIZING, FaceAuthStage.NOT_ENROLLED, FaceAuthStage.CAMERA_PERMISSION_REQUIRED, FaceAuthStage.IDLE,
        FaceAuthStage.SCANNING,
        -> R.color.text_primary
    }

private fun getActionTextRes(stage: FaceAuthStage): Int =
    when (stage) {
        FaceAuthStage.NOT_ENROLLED -> R.string.face_auth_action_enroll
        FaceAuthStage.CAMERA_PERMISSION_REQUIRED -> R.string.face_action_grant_camera
        FaceAuthStage.ENGINE_ERROR -> R.string.face_action_back
        FaceAuthStage.SUCCESS, FaceAuthStage.FAILED -> R.string.face_auth_action_retry
        FaceAuthStage.INITIALIZING, FaceAuthStage.IDLE, FaceAuthStage.SCANNING -> R.string.face_auth_action_start
    }

/** Re-enroll / delete belong to the settings test screen, not to the lock overlay. */
fun FaceAuthFragment.updateAppUnlock(isAppUnlock: Boolean) {
    binding.layoutFaceManage.visibility = if (isAppUnlock) View.GONE else View.VISIBLE
}

fun FaceAuthFragment.handleUiEvent(event: FaceAuthUiEvent) {
    when (event) {
        is FaceAuthUiEvent.Authenticated ->
            // The unlocked app coming to the front is the feedback; the overlay just closes.
            if (event.isAppUnlock) requireActivity().finish()
        FaceAuthUiEvent.ProfileDeleted -> showMessage(message = getString(R.string.face_auth_deleted), type = MessageType.SUCCESS)
    }
}
