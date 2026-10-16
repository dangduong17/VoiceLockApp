package com.example.voicelock.feature.voice.enrollment

import android.annotation.SuppressLint
import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.voice.EnrollmentError
import com.example.voicelock.data.model.voice.EnrollmentPhase
import com.example.voicelock.data.model.voice.EnrollmentUiState
import com.example.voicelock.data.model.voice.VoiceEnrollmentResult
import com.example.voicelock.data.model.voice.canRecord
import com.example.voicelock.domain.usecase.voice.EnrollOwnerVoiceUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceUnlockEnabledUseCase
import com.example.voicelock.domain.usecase.voice.WarmUpVoiceModelUseCase
import com.example.voicelock.util.onError
import com.example.voicelock.util.onSuccess
import com.example.voicelock.voice.AcousticPhraseMatcher
import com.example.voicelock.voice.AudioRecorderHelper
import com.example.voicelock.voice.VoiceModelMissingException
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Collects [EnrollmentUiState.requiredCount] consecutive recordings of the voice password, then
 * hands them to [EnrollOwnerVoiceUseCase]. A recording without speech is discarded and can simply
 * be repeated; a rejected enrollment (inconsistent samples) restarts from the first sample.
 *
 * Recordings are kept here rather than in the UiState: they are large and must not be diffed.
 */
class EnrollmentViewModel(
    private val audioRecorderHelper: AudioRecorderHelper,
    private val enrollOwnerVoiceUseCase: EnrollOwnerVoiceUseCase,
    private val warmUpVoiceModelUseCase: WarmUpVoiceModelUseCase,
    private val setVoiceUnlockEnabledUseCase: SetVoiceUnlockEnabledUseCase,
) : BaseViewModel<EnrollmentUiState, Nothing>(EnrollmentUiState()) {

    private val recordings = mutableListOf<FloatArray>()

    init {
        // Load the CAM++ model while the user reads the instructions; failures surface at enrollment.
        launchIO { warmUpVoiceModelUseCase().first() }
    }

    /** The caller must hold RECORD_AUDIO (EnrollmentActivity checks it before calling). */
    @SuppressLint("MissingPermission")
    fun recordNextSample() {
        if (!getCurrentState().canRecord()) return
        setState { copy(phase = EnrollmentPhase.RECORDING, error = EnrollmentError.NONE) }

        launchIO(onError = { setState { copy(phase = EnrollmentPhase.IDLE, error = EnrollmentError.RECORDING_FAILED) } }) {
            val samples = audioRecorderHelper.record()
            if (!AcousticPhraseMatcher.hasSpeech(samples)) {
                setState { copy(phase = EnrollmentPhase.IDLE, error = EnrollmentError.NO_SPEECH) }
                return@launchIO
            }

            recordings += samples
            val count = recordings.size
            setState { copy(phase = EnrollmentPhase.IDLE, recordedCount = count) }
            if (count >= getCurrentState().requiredCount) enroll()
        }
    }

    fun restartEnrollment() {
        val phase = getCurrentState().phase
        if (phase == EnrollmentPhase.IDLE || phase == EnrollmentPhase.COMPLETED) resetSamples(EnrollmentError.NONE)
    }

    private suspend fun enroll() {
        setState { copy(phase = EnrollmentPhase.PROCESSING) }
        enrollOwnerVoiceUseCase(recordings.toList()).first()
            .onSuccess { result ->
                when (result) {
                    is VoiceEnrollmentResult.Success -> {
                        // Enrolling is how the user opts in, so voice unlock becomes active right away.
                        setVoiceUnlockEnabledUseCase(true).first()
                        setState { copy(phase = EnrollmentPhase.COMPLETED) }
                    }
                    is VoiceEnrollmentResult.NoSpeechDetected -> resetSamples(EnrollmentError.NO_SPEECH)
                    is VoiceEnrollmentResult.InconsistentSamples -> resetSamples(EnrollmentError.INCONSISTENT_SAMPLES)
                }
            }.onError { throwable ->
                Timber.e(throwable, "Voice enrollment failed")
                resetSamples(
                    if (throwable is VoiceModelMissingException) EnrollmentError.MODEL_MISSING else EnrollmentError.PROCESSING_FAILED,
                )
            }
    }

    private fun resetSamples(error: EnrollmentError) {
        recordings.clear()
        setState { copy(phase = EnrollmentPhase.IDLE, recordedCount = 0, error = error) }
    }
}
