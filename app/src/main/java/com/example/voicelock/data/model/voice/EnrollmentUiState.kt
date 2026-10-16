package com.example.voicelock.data.model.voice

import com.example.voicelock.util.Constant

enum class EnrollmentPhase { IDLE, RECORDING, PROCESSING, COMPLETED }

enum class EnrollmentError { NONE, NO_SPEECH, INCONSISTENT_SAMPLES, RECORDING_FAILED, PROCESSING_FAILED, MODEL_MISSING }

data class EnrollmentUiState(
    val phase: EnrollmentPhase = EnrollmentPhase.IDLE,
    val recordedCount: Int = 0,
    val requiredCount: Int = Constant.VOICE_ENROLLMENT_SAMPLES,
    val error: EnrollmentError = EnrollmentError.NONE,
)

fun EnrollmentUiState.canRecord(): Boolean = phase == EnrollmentPhase.IDLE
