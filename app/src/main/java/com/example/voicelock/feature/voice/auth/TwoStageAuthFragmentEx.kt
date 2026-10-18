package com.example.voicelock.feature.voice.auth

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import androidx.core.content.ContextCompat
import com.example.voicelock.R
import com.example.voicelock.data.model.voice.AuthFailure
import com.example.voicelock.data.model.voice.AuthStage
import com.example.voicelock.data.model.voice.AuthStageInfo
import com.example.voicelock.data.model.voice.TwoStageAuthUiEvent
import com.example.voicelock.data.model.voice.VoiceScores
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.model.voice.canStart
import com.example.voicelock.data.model.voice.isBusy
import com.example.voicelock.util.setPreventDoubleClickScaleView
import com.example.voicelock.util.showPermissionDeniedMessage

fun TwoStageAuthFragment.initView() {
    // Basic setup if any
}

fun TwoStageAuthFragment.backEvent() {
    navigator.navigateUp()
}

fun TwoStageAuthFragment.settingEvent() {
    binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
        if (isChecked) modeEvent(checkedId)
    }
    binding.btnAuthAction.setPreventDoubleClickScaleView { actionEvent() }
}

private fun TwoStageAuthFragment.modeEvent(checkedId: Int) {
    viewModel.setMode(if (checkedId == R.id.btnModeLock) VoiceVerificationMode.LOCK else VoiceVerificationMode.UNLOCK)
}

/** Without a voice profile the button opens enrollment; otherwise it starts (or retries) a check. */
fun TwoStageAuthFragment.actionEvent() {
    if (viewModel.uiState.value.stage == AuthStage.NOT_ENROLLED) {
        navigator.navigateTo(R.id.action_twoStageAuthFragment_to_enrollmentFragment)
    } else {
        startEvent()
    }
}

fun TwoStageAuthFragment.startEvent() {
    val isGranted =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    if (isGranted) {
        viewModel.startAuthentication()
    } else {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

fun TwoStageAuthFragment.onMicPermissionResult(isGranted: Boolean) {
    if (isGranted) {
        viewModel.startAuthentication()
    } else {
        showPermissionDeniedMessage(getString(R.string.voice_mic_permission_denied))
    }
}

/** Coming back to a locked app (overlay brought to front) listens again right away. */
fun TwoStageAuthFragment.onScreenStarted() {
    val state = viewModel.uiState.value
    if (state.isAppUnlock && hasAutoStarted && state.canStart()) startEvent()
}

fun TwoStageAuthFragment.updateStage(stageInfo: AuthStageInfo) = with(binding) {
    val stage = stageInfo.stage
    tvAuthStage.text = getStageText(stageInfo)
    tvAuthStage.setTextColor(ContextCompat.getColor(requireContext(), getStageColorRes(stage)))

    val isBusy = stage.isBusy()
    progressAuth.visibility = if (isBusy) View.VISIBLE else View.GONE
    btnAuthAction.isEnabled = !isBusy
    btnAuthAction.alpha = if (isBusy) 0.5f else 1f
    btnAuthAction.text = getString(getActionTextRes(stage))
    btnModeLock.isEnabled = !isBusy
    btnModeUnlock.isEnabled = !isBusy

    // On the lock overlay the user opened a locked app on purpose, so listening starts by itself.
    if (viewModel.uiState.value.isAppUnlock && stage == AuthStage.IDLE && !hasAutoStarted) {
        hasAutoStarted = true
        startEvent()
    }
}

private fun TwoStageAuthFragment.getStageText(stageInfo: AuthStageInfo): String =
    when (stageInfo.stage) {
        AuthStage.CHECKING_ENROLLMENT -> getString(R.string.voice_auth_stage_checking)
        AuthStage.NOT_ENROLLED -> getString(R.string.voice_auth_stage_not_enrolled)
        AuthStage.IDLE -> getString(R.string.voice_auth_stage_idle)
        AuthStage.CAPTURING_VOICE -> getString(R.string.voice_auth_stage_capturing)
        AuthStage.VERIFYING -> getString(R.string.voice_auth_stage_verifying)
        AuthStage.SUCCESS -> getString(R.string.voice_auth_stage_success)
        AuthStage.FAILED -> getString(getFailureMessageRes(stageInfo.failure))
    }

private fun getStageColorRes(stage: AuthStage): Int =
    when (stage) {
        AuthStage.FAILED -> R.color.error_red
        AuthStage.SUCCESS -> R.color.primary_blue
        AuthStage.CHECKING_ENROLLMENT, AuthStage.NOT_ENROLLED, AuthStage.IDLE, AuthStage.CAPTURING_VOICE, AuthStage.VERIFYING ->
            R.color.text_primary
    }

private fun getActionTextRes(stage: AuthStage): Int =
    when (stage) {
        AuthStage.NOT_ENROLLED -> R.string.voice_auth_action_enroll
        AuthStage.SUCCESS, AuthStage.FAILED -> R.string.voice_auth_action_retry
        AuthStage.CHECKING_ENROLLMENT, AuthStage.IDLE, AuthStage.CAPTURING_VOICE, AuthStage.VERIFYING -> R.string.voice_auth_action_start
    }

private fun getFailureMessageRes(failure: AuthFailure): Int =
    when (failure) {
        AuthFailure.KEYWORD_MISMATCH -> R.string.voice_auth_error_keyword
        AuthFailure.RECOGNIZER_ERROR -> R.string.voice_auth_error_recognizer
        AuthFailure.NO_SPEECH -> R.string.voice_auth_error_no_speech
        AuthFailure.SPEAKER_MISMATCH -> R.string.voice_auth_error_speaker
        AuthFailure.PHRASE_MISMATCH -> R.string.voice_auth_error_phrase
        AuthFailure.SPEAKER_AND_PHRASE_MISMATCH -> R.string.voice_auth_error_both
        AuthFailure.MODEL_MISSING -> R.string.voice_auth_error_model_missing
        AuthFailure.NONE, AuthFailure.UNEXPECTED_ERROR -> R.string.voice_auth_error_unexpected
    }

fun TwoStageAuthFragment.updateHeardText(heardText: String?) {
    binding.tvRecognizedText.visibility = if (heardText == null) View.GONE else View.VISIBLE
    binding.tvRecognizedText.text =
        if (heardText.isNullOrEmpty()) getString(R.string.voice_auth_recognized_nothing) else getString(R.string.voice_auth_recognized, heardText)
}

fun TwoStageAuthFragment.updateScores(scores: VoiceScores?) {
    binding.tvScores.visibility = if (scores == null) View.GONE else View.VISIBLE
    scores?.let {
        binding.tvScores.text = getString(R.string.voice_auth_scores, it.speakerSimilarity, it.phraseSimilarity, it.phraseThreshold)
    }
}

fun TwoStageAuthFragment.updateMode(mode: VoiceVerificationMode) {
    binding.toggleMode.check(if (mode == VoiceVerificationMode.LOCK) R.id.btnModeLock else R.id.btnModeUnlock)
}

/** The Lock/Unlock threshold toggle is only for the test screen; the lock overlay always unlocks. */
fun TwoStageAuthFragment.updateAppUnlock(isAppUnlock: Boolean) {
    binding.toggleMode.visibility = if (isAppUnlock) View.GONE else View.VISIBLE
    binding.tvVolumeKeyHint.visibility = if (isAppUnlock) View.VISIBLE else View.GONE
}

/** Volume up / down on the lock overlay (F0): same as tapping start / retry; ignored while busy. */
fun TwoStageAuthFragment.volumeKeyEvent() {
    val state = viewModel.uiState.value
    if (state.isAppUnlock && state.canStart()) startEvent()
}

fun TwoStageAuthFragment.handleUiEvent(event: TwoStageAuthUiEvent) {
    when (event) {
        is TwoStageAuthUiEvent.Authenticated ->
            // The unlocked app coming to the front is the feedback; the overlay just closes.
            if (event.isAppUnlock) requireActivity().finish()
    }
}
