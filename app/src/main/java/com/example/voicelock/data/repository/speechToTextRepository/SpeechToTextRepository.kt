package com.example.voicelock.data.repository.speechToTextRepository

import com.example.voicelock.data.model.voice.SpeechRecognitionOutcome
import com.example.voicelock.data.model.voice.SpeechRecognitionSupport
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

/** Speech to text on audio the app already recorded (Android's SpeechRecognizer, API 33+). */
interface SpeechToTextRepository {
    /** Whether recorded audio can be transcribed on this device at all (independent of language). */
    fun isAvailable(): Flow<Result<Boolean>>

    fun getSupport(): Flow<Result<SpeechRecognitionSupport>>

    fun recognize(
        samples: FloatArray,
        languageTag: String,
        biasingStrings: List<String>,
        detectLanguageAmong: List<String>,
    ): Flow<Result<SpeechRecognitionOutcome>>

    /** Downloads the offline model of [languageTag]; emits true once it is installed. */
    fun downloadLanguage(languageTag: String): Flow<Result<Boolean>>
}
