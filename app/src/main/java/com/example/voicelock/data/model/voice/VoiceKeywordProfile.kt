package com.example.voicelock.data.model.voice

/**
 * Stage-1 reference captured at enrollment: the recognizer language the owner chose and what the
 * recognizer transcribed from their own enrollment samples (e.g. "mở khóa", "unlock", "नमस्ते").
 * Unlock attempts are recognized in [languageTag] and fuzzy-matched against [transcripts], so any
 * phrase in any language the device's recognizer supports works without a hard-coded keyword list.
 *
 * Empty [transcripts] means the device could not transcribe that language at enrollment, so
 * Stage 1 is skipped and the speaker + phrase checks decide alone.
 */
data class VoiceKeywordProfile(
    val languageTag: String,
    val transcripts: List<String>,
)
