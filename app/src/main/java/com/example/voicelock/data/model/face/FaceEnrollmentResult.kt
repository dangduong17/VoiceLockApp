package com.example.voicelock.data.model.face

sealed class FaceEnrollmentResult {
    data class Success(
        val sampleCount: Int,
    ) : FaceEnrollmentResult()

    /** The captured samples do not look like the same person (e.g. someone else stepped in). */
    data object InconsistentSamples : FaceEnrollmentResult()
}
