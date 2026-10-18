package com.example.voicelock.feature.voice.enrollment

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.data.model.voice.getErrorInfo
import com.example.voicelock.data.model.voice.getProgress
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.databinding.FragmentEnrollmentBinding
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.collectFlowOnView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Voice password enrollment: the user says their passphrase 3 times, in any language; every
 * recording is shown as text and only counts once it is recognized (see [EnrollmentViewModel]).
 */
class EnrollmentFragment : BaseFragment<FragmentEnrollmentBinding, EnrollmentViewModel>(
    FragmentEnrollmentBinding::inflate,
    EnrollmentViewModel::class,
) {
    val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> onMicPermissionResult(isGranted) }

    override fun init(view: View, savedInstanceState: Bundle?) {
        initView()
        settingEvent()
        onSystemBack { backEvent() }
        val mode =
            arguments?.getString(BundleKey.KEY_VOICE_MODE)
                ?.let { runCatching { VoiceVerificationMode.valueOf(it) }.getOrNull() }
                ?: VoiceVerificationMode.UNLOCK
        viewModel.initData(mode)
    }

    override fun subscribeObserver(view: View) {
        viewModel.uiState.map { it.getProgress() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateProgress(it) }
        viewModel.uiState.map { it.getErrorInfo() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateError(it) }
        viewModel.uiState.map { it.sampleTranscripts }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {
            updateSampleTranscripts(it)
        }
    }
}
