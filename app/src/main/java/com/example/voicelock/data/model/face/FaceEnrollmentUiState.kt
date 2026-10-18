package com.example.voicelock.data.model.face

import com.example.voicelock.util.Constant

enum class FaceEnrollmentPhase {
    INITIALIZING,
    ENGINE_ERROR,
    CAMERA_PERMISSION_REQUIRED,
    CAPTURING,
    PROCESSING,
    COMPLETED,
}

enum class FaceEnrollmentError {
    NONE,
    INCONSISTENT_SAMPLES,
    PROCESSING_FAILED,
}

data class FaceEnrollmentUiState(
    val phase: FaceEnrollmentPhase = FaceEnrollmentPhase.INITIALIZING,
    val engineStatus: FaceEngineStatus = FaceEngineStatus.READY,
    val capturedCount: Int = 0,
    val requiredCount: Int = Constant.FACE_ENROLLMENT_SAMPLES,
    val hint: FaceHint = FaceHint.NONE,
    val error: FaceEnrollmentError = FaceEnrollmentError.NONE,
)

/** Everything the progress area renders, so it always changes together. */
data class FaceEnrollmentProgress(
    val phase: FaceEnrollmentPhase,
    val engineStatus: FaceEngineStatus,
    val capturedCount: Int,
    val requiredCount: Int,
)

fun FaceEnrollmentUiState.getProgress(): FaceEnrollmentProgress =
    FaceEnrollmentProgress(phase = phase, engineStatus = engineStatus, capturedCount = capturedCount, requiredCount = requiredCount)

/** The hint and the error share one line under the preview: an error wins over a hint. */
data class FaceEnrollmentMessage(
    val hint: FaceHint,
    val error: FaceEnrollmentError,
)

fun FaceEnrollmentUiState.getMessage(): FaceEnrollmentMessage = FaceEnrollmentMessage(hint = hint, error = error)
