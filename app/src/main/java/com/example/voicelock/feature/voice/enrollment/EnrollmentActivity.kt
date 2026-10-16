package com.example.voicelock.feature.voice.enrollment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.voicelock.R
import com.example.voicelock.data.model.voice.EnrollmentError
import com.example.voicelock.data.model.voice.EnrollmentPhase
import com.example.voicelock.databinding.ActivityEnrollmentBinding
import com.example.voicelock.util.collectFlowOnView
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.setPreventDoubleClickScaleView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

/**
 * Voice password enrollment: the user says their passphrase 3 times, in any language; the app works
 * out the language itself (see [EnrollmentViewModel]) and stores the CAM++ voiceprint, the phrase
 * template and what the recognizer heard.
 *
 * Plain Activity shell (same status as LockOverlayActivity) so it can be started from anywhere,
 * e.g. `startActivity(Intent(context, EnrollmentActivity::class.java))`. Finishes with RESULT_OK
 * once enrollment has been saved.
 */
class EnrollmentActivity : AppCompatActivity() {

    private val viewModel: EnrollmentViewModel by viewModel()
    private lateinit var binding: ActivityEnrollmentBinding

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.recordNextSample()
            } else {
                Toast.makeText(this, R.string.voice_mic_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnrollmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settingEvent()
        subscribeObserver()
    }

    private fun settingEvent() {
        binding.btnRecord.setPreventDoubleClickScaleView { onRecordClicked() }
        binding.btnEnrollAction.setPreventDoubleClickScaleView {
            when (viewModel.uiState.value.phase) {
                EnrollmentPhase.COMPLETED -> {
                    setResult(RESULT_OK)
                    finish()
                }
                EnrollmentPhase.DOWNLOADING_LANGUAGE -> viewModel.cancelLanguageDownload()
                else -> onRecordClicked()
            }
        }
        binding.btnEnrollRestart.setPreventDoubleClick { viewModel.restartEnrollment() }
    }

    private fun subscribeObserver() {
        viewModel.uiState
            .map { ProgressUi(it.phase, it.recordedCount, it.requiredCount, it.recognizedPhrase, it.languageTag, it.pendingLanguageTag) }
            .distinctUntilChanged()
            .collectFlowOnView(this) { progress -> updateProgress(progress) }

        viewModel.uiState
            .map { Triple(it.error, it.rejectedHeardText, it.pendingLanguageTag) }
            .distinctUntilChanged()
            .collectFlowOnView(this) { (error, rejectedHeardText, pendingLanguageTag) -> updateError(error, rejectedHeardText, pendingLanguageTag) }

        viewModel.uiState
            .map { it.sampleTranscripts }
            .distinctUntilChanged()
            .collectFlowOnView(this) { transcripts -> updateHeardTexts(transcripts) }
    }

    private fun onRecordClicked() {
        val isGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (isGranted) {
            viewModel.recordNextSample()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun updateProgress(progress: ProgressUi) = with(binding) {
        val (phase, recordedCount, requiredCount, recognizedPhrase, languageTag, pendingLanguageTag) = progress
        progressSamples.max = requiredCount
        progressSamples.setProgressCompat(recordedCount, true)
        tvSampleCount.text = getString(R.string.voice_enroll_sample_count, recordedCount, requiredCount)

        tvEnrollStatus.text =
            when (phase) {
                EnrollmentPhase.IDLE -> getString(R.string.voice_enroll_status_idle, recordedCount + 1)
                EnrollmentPhase.RECORDING -> getString(R.string.voice_enroll_status_recording)
                EnrollmentPhase.RECOGNIZING -> getString(R.string.voice_enroll_status_recognizing)
                EnrollmentPhase.DOWNLOADING_LANGUAGE ->
                    getString(R.string.voice_enroll_status_downloading_language, languageDisplayName(pendingLanguageTag))
                EnrollmentPhase.PROCESSING -> getString(R.string.voice_enroll_status_processing)
                EnrollmentPhase.COMPLETED ->
                    getString(R.string.voice_enroll_status_completed_phrase, recognizedPhrase, languageDisplayName(languageTag))
            }

        val isBusy = phase != EnrollmentPhase.IDLE && phase != EnrollmentPhase.COMPLETED
        progressRecording.visibility = if (isBusy) View.VISIBLE else View.GONE
        btnRecord.isEnabled = phase == EnrollmentPhase.IDLE
        btnRecord.alpha = if (phase == EnrollmentPhase.IDLE) 1f else 0.5f
        // While a language pack downloads the main button becomes "Cancel", so the user is never stuck.
        val isCancellable = phase == EnrollmentPhase.DOWNLOADING_LANGUAGE
        btnEnrollAction.isEnabled = !isBusy || isCancellable
        btnEnrollAction.alpha = if (isBusy && !isCancellable) 0.5f else 1f
        btnEnrollAction.text =
            getString(
                when (phase) {
                    EnrollmentPhase.COMPLETED -> R.string.voice_enroll_action_done
                    EnrollmentPhase.DOWNLOADING_LANGUAGE -> R.string.voice_enroll_action_cancel_download
                    else -> R.string.voice_enroll_action_record
                },
            )
        btnEnrollRestart.visibility = if (!isBusy && (recordedCount > 0 || phase == EnrollmentPhase.COMPLETED)) View.VISIBLE else View.INVISIBLE
    }

    private fun updateError(
        error: EnrollmentError,
        rejectedHeardText: String,
        pendingLanguageTag: String,
    ) {
        val messageRes =
            when (error) {
                EnrollmentError.NONE -> null
                EnrollmentError.NO_SPEECH -> R.string.voice_enroll_error_no_speech
                EnrollmentError.NOT_RECOGNIZED -> R.string.voice_enroll_error_not_recognized
                EnrollmentError.PHRASE_MISMATCH -> R.string.voice_enroll_error_phrase_mismatch
                EnrollmentError.RECOGNITION_UNAVAILABLE -> R.string.voice_enroll_error_recognition_unavailable
                EnrollmentError.LANGUAGE_DOWNLOAD_FAILED -> R.string.voice_enroll_error_language_download
                EnrollmentError.LANGUAGE_DOWNLOAD_CANCELLED -> R.string.voice_enroll_error_language_download_cancelled
                EnrollmentError.INCONSISTENT_SAMPLES -> R.string.voice_enroll_error_inconsistent
                EnrollmentError.RECORDING_FAILED -> R.string.voice_enroll_error_recording
                EnrollmentError.PROCESSING_FAILED -> R.string.voice_enroll_error_processing
                EnrollmentError.MODEL_MISSING -> R.string.voice_enroll_error_model_missing
            }
        binding.tvEnrollError.visibility = if (messageRes == null) View.GONE else View.VISIBLE
        messageRes?.let { res ->
            binding.tvEnrollError.text =
                when (error) {
                    EnrollmentError.PHRASE_MISMATCH -> getString(res, rejectedHeardText)
                    EnrollmentError.LANGUAGE_DOWNLOAD_FAILED, EnrollmentError.LANGUAGE_DOWNLOAD_CANCELLED ->
                        getString(res, languageDisplayName(pendingLanguageTag))
                    else -> getString(res)
                }
        }
    }

    /** Speech to text of every accepted recording, one line each. */
    private fun updateHeardTexts(transcripts: List<String>) {
        binding.tvHeardTexts.visibility = if (transcripts.isEmpty()) View.GONE else View.VISIBLE
        binding.tvHeardTexts.text =
            transcripts.mapIndexed { index, text ->
                getString(R.string.voice_heard_sample_line, index + 1, getString(R.string.voice_heard_quoted, text))
            }.joinToString("\n")
    }

    private fun languageDisplayName(languageTag: String): String {
        val displayLocale = resources.configuration.locales[0]
        return Locale.forLanguageTag(languageTag).getDisplayName(displayLocale).replaceFirstChar { it.titlecase(displayLocale) }
    }

    private data class ProgressUi(
        val phase: EnrollmentPhase,
        val recordedCount: Int,
        val requiredCount: Int,
        val recognizedPhrase: String,
        val languageTag: String,
        val pendingLanguageTag: String,
    )

}
