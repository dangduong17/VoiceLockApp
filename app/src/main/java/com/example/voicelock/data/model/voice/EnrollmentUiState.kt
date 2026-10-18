package com.example.voicelock.data.model.voice

import com.example.voicelock.util.Constant

enum class EnrollmentPhase { IDLE, RECORDING, RECOGNIZING, DOWNLOADING_LANGUAGE, PROCESSING, COMPLETED }

enum class EnrollmentError {
    NONE,
    NO_SPEECH,

    /** The recording could not be turned into text in any language: say it again. */
    NOT_RECOGNIZED,

    /** The recording was heard as a different phrase than the earlier ones: say it again. */
    PHRASE_MISMATCH,

    /** No speech-to-text engine on this device can transcribe the recording at all. */
    RECOGNITION_UNAVAILABLE,

    /** The detected language's offline model could not be downloaded (usually no network). */
    LANGUAGE_DOWNLOAD_FAILED,

    /** The user cancelled the language-pack download. */
    LANGUAGE_DOWNLOAD_CANCELLED,
    INCONSISTENT_SAMPLES,
    RECORDING_FAILED,
    PROCESSING_FAILED,
    MODEL_MISSING,

    /** F1: the lock command sounds like the unlock passphrase. */
    SAME_AS_UNLOCK,
}

data class EnrollmentUiState(
    /** UNLOCK: the voice passphrase; LOCK: the F1 lock command. */
    val mode: VoiceVerificationMode = VoiceVerificationMode.UNLOCK,
    val phase: EnrollmentPhase = EnrollmentPhase.IDLE,
    val recordedCount: Int = 0,
    val requiredCount: Int = Constant.VOICE_ENROLLMENT_SAMPLES,
    val error: EnrollmentError = EnrollmentError.NONE,
    /** Language the passphrase was recognized in (Stage 1 recognizes in it from now on). */
    val languageTag: String = "",
    /** The enrolled passphrase as text, shown once enrollment is complete. */
    val recognizedPhrase: String = "",
    /** Speech-to-text of every accepted recording, in the language currently leading. */
    val sampleTranscripts: List<String> = emptyList(),
    /** Text of a rejected recording that did not match the earlier ones (shown with the error). */
    val rejectedHeardText: String = "",
    /** Language detected in the first recording, shown while its model downloads / if that fails. */
    val pendingLanguageTag: String = "",
)

fun EnrollmentUiState.canRecord(): Boolean = phase == EnrollmentPhase.IDLE

/** Everything the progress area renders, so it always changes together. */
data class EnrollmentProgress(
    val mode: VoiceVerificationMode,
    val phase: EnrollmentPhase,
    val recordedCount: Int,
    val requiredCount: Int,
    val recognizedPhrase: String,
    val languageTag: String,
    val pendingLanguageTag: String,
)

fun EnrollmentUiState.getProgress(): EnrollmentProgress =
    EnrollmentProgress(
        mode = mode,
        phase = phase,
        recordedCount = recordedCount,
        requiredCount = requiredCount,
        recognizedPhrase = recognizedPhrase,
        languageTag = languageTag,
        pendingLanguageTag = pendingLanguageTag,
    )

/** The error plus the values its message is formatted with. */
data class EnrollmentErrorInfo(
    val error: EnrollmentError,
    val rejectedHeardText: String,
    val pendingLanguageTag: String,
)

fun EnrollmentUiState.getErrorInfo(): EnrollmentErrorInfo =
    EnrollmentErrorInfo(error = error, rejectedHeardText = rejectedHeardText, pendingLanguageTag = pendingLanguageTag)
