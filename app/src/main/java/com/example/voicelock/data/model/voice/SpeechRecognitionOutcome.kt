package com.example.voicelock.data.model.voice

/** Result of one speech-to-text pass over a recorded clip. */
sealed class SpeechRecognitionOutcome {
    /** [detectedLanguage] is set when language detection was requested and the engine reported one. */
    data class Candidates(
        val texts: List<String>,
        val detectedLanguage: String? = null,
    ) : SpeechRecognitionOutcome()

    /** No text; [detectedLanguage] may still be known (e.g. speech in a language with no model). */
    data class NoSpeech(
        val detectedLanguage: String? = null,
    ) : SpeechRecognitionOutcome()

    /** The engine cannot transcribe this (no recognizer, no model for the language, Android < 13). */
    data object Unsupported : SpeechRecognitionOutcome()

    data class Failed(
        val errorCode: Int,
    ) : SpeechRecognitionOutcome()
}

/** Top transcripts, trimmed and without blanks; empty unless [SpeechRecognitionOutcome.Candidates]. */
fun SpeechRecognitionOutcome.getTexts(): List<String> =
    when (this) {
        is SpeechRecognitionOutcome.Candidates -> texts.map { it.trim() }.filter { it.isNotEmpty() }
        is SpeechRecognitionOutcome.NoSpeech -> emptyList()
        SpeechRecognitionOutcome.Unsupported -> emptyList()
        is SpeechRecognitionOutcome.Failed -> emptyList()
    }

fun SpeechRecognitionOutcome.getDetectedLanguage(): String? =
    when (this) {
        is SpeechRecognitionOutcome.Candidates -> detectedLanguage
        is SpeechRecognitionOutcome.NoSpeech -> detectedLanguage
        SpeechRecognitionOutcome.Unsupported -> null
        is SpeechRecognitionOutcome.Failed -> null
    }

/** What the device's speech-to-text can do per language (empty lists when unknown). */
data class SpeechRecognitionSupport(
    /** Offline models already on the device. */
    val installed: List<String> = emptyList(),
    /** Offline models that can be downloaded. */
    val downloadable: List<String> = emptyList(),
    /** Languages the online (network) recognizer handles. */
    val online: List<String> = emptyList(),
)

fun SpeechRecognitionSupport.isInstalled(languageTag: String): Boolean = installed.any { it.equals(languageTag, ignoreCase = true) }
