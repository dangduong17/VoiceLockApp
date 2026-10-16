package com.example.voicelock.data.model.voice

data class VoiceScores(
    val speakerSimilarity: Float,
    val phraseSimilarity: Float,
    /** The (personal) phrase threshold this attempt was judged against. */
    val phraseThreshold: Float,
)

sealed interface VoiceVerifyResult {
    data class Verified(
        val scores: VoiceScores,
    ) : VoiceVerifyResult

    data class Rejected(
        val scores: VoiceScores,
        val isSpeakerMatch: Boolean,
        val isPhraseMatch: Boolean,
    ) : VoiceVerifyResult

    data object NoSpeechDetected : VoiceVerifyResult

    data object NotEnrolled : VoiceVerifyResult
}

sealed interface VoiceEnrollmentResult {
    /** [consistency] is the lowest cosine similarity of any sample to the centroid. */
    data class Success(
        val consistency: Float,
    ) : VoiceEnrollmentResult

    data class NoSpeechDetected(
        val sampleIndex: Int,
    ) : VoiceEnrollmentResult

    data class InconsistentSamples(
        val consistency: Float,
    ) : VoiceEnrollmentResult
}
