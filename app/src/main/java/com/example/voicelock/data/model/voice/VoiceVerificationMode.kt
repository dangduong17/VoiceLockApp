package com.example.voicelock.data.model.voice

/**
 * Acceptance thresholds for the two Stage-2 checks.
 *
 * [speakerSimilarity] is the cosine similarity between the CAM++ embedding of the attempt and the
 * enrolled centroid; [phraseSimilarity] is the DTW similarity returned by AcousticPhraseMatcher.
 * The defaults are conservative starting points — calibrate them on real devices by logging the
 * scores of genuine and impostor attempts (see TwoStageAuthActivity's score readout).
 */
data class VoiceThresholds(
    val speakerSimilarity: Float,
    val phraseSimilarity: Float,
)

/**
 * UNLOCK grants access, so it uses stricter thresholds (fewer false accepts).
 * LOCK only re-locks/confirms the owner, so it is more lenient (fewer annoying false rejects).
 */
enum class VoiceVerificationMode(
    val defaultThresholds: VoiceThresholds,
) {
    // CAM++ cosine scores for a ~1 s passphrase are much lower than for long speech (the owner
    // scored 0.42 against a 0.60 threshold on a real device), so the speaker bar is set for short
    // phrases; the keyword check and the DTW phrase match carry part of the decision.
    LOCK(VoiceThresholds(speakerSimilarity = 0.35f, phraseSimilarity = 0.40f)),
    UNLOCK(VoiceThresholds(speakerSimilarity = 0.40f, phraseSimilarity = 0.45f)),
}
