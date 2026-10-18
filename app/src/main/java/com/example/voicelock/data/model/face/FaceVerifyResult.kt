package com.example.voicelock.data.model.face

sealed class FaceVerifyResult {
    data class Verified(
        val similarity: Float,
    ) : FaceVerifyResult()

    data class Rejected(
        val similarity: Float,
    ) : FaceVerifyResult()

    data object NotEnrolled : FaceVerifyResult()
}
