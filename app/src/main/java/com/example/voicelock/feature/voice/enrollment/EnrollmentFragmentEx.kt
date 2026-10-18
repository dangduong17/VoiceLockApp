package com.example.voicelock.feature.voice.enrollment

import android.Manifest
import com.example.voicelock.R
import com.example.voicelock.data.model.voice.EnrollmentError
import com.example.voicelock.data.model.voice.EnrollmentErrorInfo
import com.example.voicelock.data.model.voice.EnrollmentPhase
import com.example.voicelock.data.model.voice.EnrollmentProgress
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.setPreventDoubleClickScaleView
import com.example.voicelock.util.showPermissionDeniedMessage
import java.util.Locale
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.example.voicelock.util.isPermissionGranted
import com.example.voicelock.util.setEnabledWithAlpha

fun EnrollmentFragment.backEvent() {
    navigator.navigateUp()
}

fun EnrollmentFragment.settingEvent() {
    binding.btnRecord.setPreventDoubleClickScaleView { recordEvent() }
    binding.btnEnrollAction.setPreventDoubleClickScaleView { actionEvent() }
    binding.btnEnrollRestart.setPreventDoubleClick { viewModel.restartEnrollment() }
}

fun EnrollmentFragment.recordEvent() {
    if (requireContext().isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
        viewModel.recordNextSample()
    } else {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

/** The main button records, cancels a language-pack download, or closes once enrollment is done. */
fun EnrollmentFragment.actionEvent() {
    when (viewModel.uiState.value.phase) {
        EnrollmentPhase.COMPLETED -> navigator.navigateUp()
        EnrollmentPhase.DOWNLOADING_LANGUAGE -> viewModel.cancelLanguageDownload()
        EnrollmentPhase.IDLE -> recordEvent()
        EnrollmentPhase.RECORDING, EnrollmentPhase.RECOGNIZING, EnrollmentPhase.PROCESSING -> {
            // Busy: the button is disabled
        }
    }
}

fun EnrollmentFragment.onMicPermissionResult(isGranted: Boolean) {
    if (isGranted) {
        viewModel.recordNextSample()
    } else {
        showPermissionDeniedMessage(getString(R.string.voice_mic_permission_denied))
    }
}

fun EnrollmentFragment.updateProgress(progress: EnrollmentProgress) = with(binding) {
    val isLockCommand = progress.mode == VoiceVerificationMode.LOCK
    tvEnrollTitle.setText(if (isLockCommand) R.string.voice_lock_enroll_title else R.string.voice_enroll_title)
    tvEnrollSubtitle.setText(if (isLockCommand) R.string.voice_lock_enroll_subtitle else R.string.voice_enroll_subtitle)
    progressSamples.max = progress.requiredCount
    progressSamples.setProgressCompat(progress.recordedCount, true)
    tvSampleCount.text = getString(R.string.voice_enroll_sample_count, progress.recordedCount, progress.requiredCount)
    tvEnrollStatus.text = getStatusText(progress)

    val phase = progress.phase
    val isBusy = phase != EnrollmentPhase.IDLE && phase != EnrollmentPhase.COMPLETED
    // While a language pack downloads the main button becomes "Cancel", so the user is never stuck.
    val isCancellable = phase == EnrollmentPhase.DOWNLOADING_LANGUAGE
    progressRecording.isVisible = isBusy
    btnRecord.setEnabledWithAlpha(phase == EnrollmentPhase.IDLE)
    btnEnrollAction.setEnabledWithAlpha(!isBusy || isCancellable)
    btnEnrollAction.text = getString(getActionTextRes(phase))
    val canRestart = !isBusy && (progress.recordedCount > 0 || phase == EnrollmentPhase.COMPLETED)
    btnEnrollRestart.isInvisible = !canRestart
}

private fun EnrollmentFragment.getStatusText(progress: EnrollmentProgress): String =
    when (progress.phase) {
        EnrollmentPhase.IDLE ->
            getString(
                if (progress.mode == VoiceVerificationMode.LOCK) R.string.voice_lock_enroll_status_idle else R.string.voice_enroll_status_idle,
                progress.recordedCount + 1,
            )
        EnrollmentPhase.RECORDING -> getString(R.string.voice_enroll_status_recording)
        EnrollmentPhase.RECOGNIZING -> getString(R.string.voice_enroll_status_recognizing)
        EnrollmentPhase.DOWNLOADING_LANGUAGE ->
            getString(R.string.voice_enroll_status_downloading_language, getLanguageDisplayName(progress.pendingLanguageTag))
        EnrollmentPhase.PROCESSING -> getString(R.string.voice_enroll_status_processing)
        EnrollmentPhase.COMPLETED ->
            getString(
                if (progress.mode == VoiceVerificationMode.LOCK) R.string.voice_lock_enroll_completed else R.string.voice_enroll_status_completed_phrase,
                progress.recognizedPhrase,
                getLanguageDisplayName(progress.languageTag),
            )
    }

private fun getActionTextRes(phase: EnrollmentPhase): Int =
    when (phase) {
        EnrollmentPhase.COMPLETED -> R.string.voice_enroll_action_done
        EnrollmentPhase.DOWNLOADING_LANGUAGE -> R.string.voice_enroll_action_cancel_download
        EnrollmentPhase.IDLE, EnrollmentPhase.RECORDING, EnrollmentPhase.RECOGNIZING, EnrollmentPhase.PROCESSING ->
            R.string.voice_enroll_action_record
    }

fun EnrollmentFragment.updateError(errorInfo: EnrollmentErrorInfo) {
    val message =
        when (errorInfo.error) {
            EnrollmentError.NONE -> null
            EnrollmentError.NO_SPEECH -> getString(R.string.voice_enroll_error_no_speech)
            EnrollmentError.NOT_RECOGNIZED -> getString(R.string.voice_enroll_error_not_recognized)
            EnrollmentError.PHRASE_MISMATCH -> getString(R.string.voice_enroll_error_phrase_mismatch, errorInfo.rejectedHeardText)
            EnrollmentError.RECOGNITION_UNAVAILABLE -> getString(R.string.voice_enroll_error_recognition_unavailable)
            EnrollmentError.LANGUAGE_DOWNLOAD_FAILED ->
                getString(R.string.voice_enroll_error_language_download, getLanguageDisplayName(errorInfo.pendingLanguageTag))
            EnrollmentError.LANGUAGE_DOWNLOAD_CANCELLED ->
                getString(R.string.voice_enroll_error_language_download_cancelled, getLanguageDisplayName(errorInfo.pendingLanguageTag))
            EnrollmentError.INCONSISTENT_SAMPLES -> getString(R.string.voice_enroll_error_inconsistent)
            EnrollmentError.RECORDING_FAILED -> getString(R.string.voice_enroll_error_recording)
            EnrollmentError.PROCESSING_FAILED -> getString(R.string.voice_enroll_error_processing)
            EnrollmentError.MODEL_MISSING -> getString(R.string.voice_enroll_error_model_missing)
            EnrollmentError.SAME_AS_UNLOCK -> getString(R.string.voice_lock_enroll_error_same_as_unlock)
        }
    binding.tvEnrollError.isVisible = message != null
    binding.tvEnrollError.text = message.orEmpty()
}

/** Speech to text of every accepted recording, one line each. */
fun EnrollmentFragment.updateSampleTranscripts(transcripts: List<String>) {
    binding.tvHeardTexts.isVisible = transcripts.isNotEmpty()
    binding.tvHeardTexts.text =
        transcripts.mapIndexed { index, text ->
            getString(R.string.voice_heard_sample_line, index + 1, getString(R.string.voice_heard_quoted, text))
        }.joinToString(separator = TRANSCRIPT_SEPARATOR)
}

private fun EnrollmentFragment.getLanguageDisplayName(languageTag: String): String {
    val displayLocale = resources.configuration.locales[0]
    return Locale.forLanguageTag(languageTag).getDisplayName(displayLocale).replaceFirstChar { it.titlecase(displayLocale) }
}

private const val TRANSCRIPT_SEPARATOR = "\n"
