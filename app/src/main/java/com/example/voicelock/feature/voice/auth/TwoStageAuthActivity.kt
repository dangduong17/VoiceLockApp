package com.example.voicelock.feature.voice.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.voicelock.R
import com.example.voicelock.data.model.voice.AuthFailure
import com.example.voicelock.data.model.voice.AuthStage
import com.example.voicelock.data.model.voice.TwoStageAuthUiEvent
import com.example.voicelock.data.model.voice.VoiceScores
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.model.voice.canStart
import com.example.voicelock.databinding.ActivityTwoStageAuthBinding
import com.example.voicelock.feature.voice.enrollment.EnrollmentActivity
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.collectFlowOnView
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.setPreventDoubleClickScaleView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Two-stage voice check on a single utterance: one AudioRecord capture, then SpeechRecognizer keyword
 * + Sherpa-ONNX speaker + MFCC/DTW phrase on that same audio (see TwoStageAuthViewModel).
 *
 * Opened from Home/Settings it is a test screen (Lock/Unlock thresholds selectable). Opened by
 * LockOverlayActivity with KEY_LOCK_TARGET_PACKAGE it unlocks that app: it starts listening
 * immediately, marks the app unlocked and finishes with RESULT_OK; leaving it any other way
 * (back, "use PIN or pattern", not enrolled) returns RESULT_CANCELED.
 */
class TwoStageAuthActivity : AppCompatActivity() {

    private val viewModel: TwoStageAuthViewModel by viewModel()
    private lateinit var binding: ActivityTwoStageAuthBinding
    private var isAppUnlock = false
    private var hasAutoStarted = false

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.startAuthentication()
            } else {
                Toast.makeText(this, R.string.voice_mic_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

    private val enrollmentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { viewModel.checkEnrollment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTwoStageAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val targetPackageName = intent.getStringExtra(BundleKey.KEY_LOCK_TARGET_PACKAGE)
        isAppUnlock = !targetPackageName.isNullOrBlank()
        if (isAppUnlock) applyLockScreenWindowFlags()

        settingEvent()
        subscribeObserver()
        viewModel.initData(targetPackageName)
    }

    override fun onStart() {
        super.onStart()
        // Coming back to a locked app (overlay task brought to front) listens again right away.
        if (isAppUnlock && hasAutoStarted && viewModel.uiState.value.canStart()) onStartClicked()
    }

    override fun onStop() {
        super.onStop()
        viewModel.cancelAuthentication()
    }

    private fun settingEvent() {
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.setMode(if (checkedId == R.id.btnModeLock) VoiceVerificationMode.LOCK else VoiceVerificationMode.UNLOCK)
        }
        binding.btnAuthAction.setPreventDoubleClickScaleView {
            if (viewModel.uiState.value.stage == AuthStage.NOT_ENROLLED) {
                enrollmentLauncher.launch(Intent(this, EnrollmentActivity::class.java))
            } else {
                onStartClicked()
            }
        }
        // Lock overlay only: RESULT_CANCELED tells LockOverlayActivity to fall back to PIN/pattern.
        binding.btnUseOtherMethod.setPreventDoubleClick { finish() }
        binding.toggleMode.visibility = if (isAppUnlock) View.GONE else View.VISIBLE
        binding.btnUseOtherMethod.visibility = if (isAppUnlock) View.VISIBLE else View.GONE
    }

    private fun subscribeObserver() {
        viewModel.uiState
            .map { it.stage to it.failure }
            .distinctUntilChanged()
            .collectFlowOnView(this) { (stage, failure) ->
                updateStage(stage, failure)
                handleAppUnlockStage(stage)
            }

        viewModel.uiState
            .map { it.recognizedText }
            .distinctUntilChanged()
            .collectFlowOnView(this) { text -> updateRecognizedText(text) }

        viewModel.uiState
            .map { it.scores }
            .distinctUntilChanged()
            .collectFlowOnView(this) { scores -> updateScores(scores) }

        viewModel.uiState
            .map { it.mode }
            .distinctUntilChanged()
            .collectFlowOnView(this) { mode ->
                binding.toggleMode.check(if (mode == VoiceVerificationMode.LOCK) R.id.btnModeLock else R.id.btnModeUnlock)
            }

        viewModel.uiEvent.collectFlowOnView(this) { event -> handleEvent(event) }
    }

    private fun handleEvent(event: TwoStageAuthUiEvent) {
        when (event) {
            TwoStageAuthUiEvent.Authenticated -> {
                setResult(RESULT_OK)
                if (isAppUnlock) finish()
            }
        }
    }

    /**
     * On the lock overlay the user opened a locked app on purpose, so listening starts right away;
     * without a voice profile the overlay falls back to PIN/pattern.
     */
    private fun handleAppUnlockStage(stage: AuthStage) {
        if (!isAppUnlock) return
        when (stage) {
            AuthStage.NOT_ENROLLED -> finish()
            AuthStage.IDLE ->
                if (!hasAutoStarted) {
                    hasAutoStarted = true
                    onStartClicked()
                }
            else -> Unit
        }
    }

    private fun applyLockScreenWindowFlags() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        )
    }

    private fun onStartClicked() {
        val isGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (isGranted) {
            viewModel.startAuthentication()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun updateStage(
        stage: AuthStage,
        failure: AuthFailure,
    ) = with(binding) {
        tvAuthStage.text =
            when (stage) {
                AuthStage.CHECKING_ENROLLMENT -> getString(R.string.voice_auth_stage_checking)
                AuthStage.NOT_ENROLLED -> getString(R.string.voice_auth_stage_not_enrolled)
                AuthStage.IDLE -> getString(R.string.voice_auth_stage_idle)
                AuthStage.CAPTURING_VOICE -> getString(R.string.voice_auth_stage_capturing)
                AuthStage.VERIFYING -> getString(R.string.voice_auth_stage_verifying)
                AuthStage.SUCCESS -> getString(R.string.voice_auth_stage_success)
                AuthStage.FAILED -> getString(failureMessage(failure))
            }
        tvAuthStage.setTextColor(
            ContextCompat.getColor(
                this@TwoStageAuthActivity,
                when (stage) {
                    AuthStage.FAILED -> R.color.error_red
                    AuthStage.SUCCESS -> R.color.primary_blue
                    else -> R.color.text_primary
                },
            ),
        )

        val isBusy =
            stage == AuthStage.CHECKING_ENROLLMENT ||
                stage == AuthStage.CAPTURING_VOICE ||
                stage == AuthStage.VERIFYING
        progressAuth.visibility = if (isBusy) View.VISIBLE else View.GONE
        btnAuthAction.isEnabled = !isBusy
        btnAuthAction.alpha = if (isBusy) 0.5f else 1f
        btnAuthAction.text =
            getString(
                when (stage) {
                    AuthStage.NOT_ENROLLED -> R.string.voice_auth_action_enroll
                    AuthStage.SUCCESS, AuthStage.FAILED -> R.string.voice_auth_action_retry
                    else -> R.string.voice_auth_action_start
                },
            )
        btnModeLock.isEnabled = !isBusy
        btnModeUnlock.isEnabled = !isBusy
    }

    private fun updateRecognizedText(text: String) {
        binding.tvRecognizedText.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
        binding.tvRecognizedText.text = getString(R.string.voice_auth_recognized, text)
    }

    private fun updateScores(scores: VoiceScores?) {
        binding.tvScores.visibility = if (scores == null) View.GONE else View.VISIBLE
        scores?.let {
            binding.tvScores.text = getString(R.string.voice_auth_scores, it.speakerSimilarity, it.phraseSimilarity)
        }
    }

    private fun failureMessage(failure: AuthFailure): Int =
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
}
