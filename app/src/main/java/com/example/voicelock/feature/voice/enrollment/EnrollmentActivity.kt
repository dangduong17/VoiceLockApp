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

/**
 * Voice password enrollment: the user records the phrase [EnrollmentViewModel] asks for 3 times,
 * then the averaged CAM++ embedding and the phrase template are stored.
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
            if (viewModel.uiState.value.phase == EnrollmentPhase.COMPLETED) {
                setResult(RESULT_OK)
                finish()
            } else {
                onRecordClicked()
            }
        }
        binding.btnEnrollRestart.setPreventDoubleClick { viewModel.restartEnrollment() }
    }

    private fun subscribeObserver() {
        viewModel.uiState
            .map { Triple(it.phase, it.recordedCount, it.requiredCount) }
            .distinctUntilChanged()
            .collectFlowOnView(this) { (phase, recordedCount, requiredCount) ->
                updateProgress(phase, recordedCount, requiredCount)
            }

        viewModel.uiState
            .map { it.error }
            .distinctUntilChanged()
            .collectFlowOnView(this) { error -> updateError(error) }
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

    private fun updateProgress(
        phase: EnrollmentPhase,
        recordedCount: Int,
        requiredCount: Int,
    ) = with(binding) {
        progressSamples.max = requiredCount
        progressSamples.setProgressCompat(recordedCount, true)
        tvSampleCount.text = getString(R.string.voice_enroll_sample_count, recordedCount, requiredCount)

        tvEnrollStatus.text =
            when (phase) {
                EnrollmentPhase.IDLE -> getString(R.string.voice_enroll_status_idle, recordedCount + 1)
                EnrollmentPhase.RECORDING -> getString(R.string.voice_enroll_status_recording)
                EnrollmentPhase.PROCESSING -> getString(R.string.voice_enroll_status_processing)
                EnrollmentPhase.COMPLETED -> getString(R.string.voice_enroll_status_completed)
            }

        val isBusy = phase == EnrollmentPhase.RECORDING || phase == EnrollmentPhase.PROCESSING
        progressRecording.visibility = if (isBusy) View.VISIBLE else View.GONE
        btnRecord.isEnabled = phase == EnrollmentPhase.IDLE
        btnRecord.alpha = if (phase == EnrollmentPhase.IDLE) 1f else 0.5f
        btnEnrollAction.isEnabled = !isBusy
        btnEnrollAction.alpha = if (isBusy) 0.5f else 1f
        btnEnrollAction.text =
            getString(if (phase == EnrollmentPhase.COMPLETED) R.string.voice_enroll_action_done else R.string.voice_enroll_action_record)
        btnEnrollRestart.visibility = if (!isBusy && (recordedCount > 0 || phase == EnrollmentPhase.COMPLETED)) View.VISIBLE else View.INVISIBLE
    }

    private fun updateError(error: EnrollmentError) {
        val messageRes =
            when (error) {
                EnrollmentError.NONE -> null
                EnrollmentError.NO_SPEECH -> R.string.voice_enroll_error_no_speech
                EnrollmentError.INCONSISTENT_SAMPLES -> R.string.voice_enroll_error_inconsistent
                EnrollmentError.RECORDING_FAILED -> R.string.voice_enroll_error_recording
                EnrollmentError.PROCESSING_FAILED -> R.string.voice_enroll_error_processing
                EnrollmentError.MODEL_MISSING -> R.string.voice_enroll_error_model_missing
            }
        binding.tvEnrollError.visibility = if (messageRes == null) View.GONE else View.VISIBLE
        messageRes?.let { binding.tvEnrollError.setText(it) }
    }
}
