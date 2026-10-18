package com.example.voicelock.feature.face.auth

import android.Manifest
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
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.example.voicelock.util.isPermissionGranted
import com.example.voicelock.util.setEnabledWithAlpha
import com.example.voicelock.util.setTextColorRes

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
    if (requireContext().isPermissionGranted(Manifest.permission.CAMERA)) {
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
    tvFaceAuthStatus.setTextColorRes(getStageColorRes(stage))

    val hintText = if (stage == FaceAuthStage.SCANNING) getFaceHintText(info.hint) else null
    tvFaceAuthHint.isInvisible = hintText == null
    tvFaceAuthHint.text = hintText.orEmpty()

    tvFaceSimilarity.isVisible = info.bestSimilarity != null
    info.bestSimilarity?.let { tvFaceSimilarity.text = getString(R.string.face_auth_similarity, it, Constant.FACE_MATCH_THRESHOLD) }

    val isBusy = stage == FaceAuthStage.INITIALIZING || stage == FaceAuthStage.SCANNING
    progressFaceScan.isVisible = isBusy
    btnFaceAuthAction.setEnabledWithAlpha(!isBusy)
    btnFaceAuthAction.text = getString(getActionTextRes(stage))

    val hasProfile = stage != FaceAuthStage.NOT_ENROLLED && stage != FaceAuthStage.INITIALIZING
    btnFaceDelete.setEnabledWithAlpha(hasProfile)
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
    binding.layoutFaceManage.isVisible = !isAppUnlock
}

fun FaceAuthFragment.handleUiEvent(event: FaceAuthUiEvent) {
    when (event) {
        is FaceAuthUiEvent.Authenticated ->
            // The unlocked app coming to the front is the feedback; the overlay just closes.
            if (event.isAppUnlock) requireActivity().finish()
        FaceAuthUiEvent.ProfileDeleted -> showMessage(message = getString(R.string.face_auth_deleted), type = MessageType.SUCCESS)
    }
}
