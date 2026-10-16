package com.example.voicelock.data.repository.speechToTextRepository

import com.example.voicelock.data.local.voice.KeywordAudioRecognizer
import com.example.voicelock.data.model.voice.SpeechRecognitionOutcome
import com.example.voicelock.data.model.voice.SpeechRecognitionSupport
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** The recognizer switches to the main thread itself; these flows only keep callers off it. */
class SpeechToTextRepositoryImpl(
    private val keywordAudioRecognizer: KeywordAudioRecognizer,
) : SpeechToTextRepository {

    override fun isAvailable(): Flow<Result<Boolean>> =
        flow<Result<Boolean>> { emit(Result.Success(keywordAudioRecognizer.isSupportedOnDevice())) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun getSupport(): Flow<Result<SpeechRecognitionSupport>> =
        flow<Result<SpeechRecognitionSupport>> { emit(Result.Success(keywordAudioRecognizer.getRecognitionSupport())) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun recognize(
        samples: FloatArray,
        languageTag: String,
        biasingStrings: List<String>,
        detectLanguageAmong: List<String>,
    ): Flow<Result<SpeechRecognitionOutcome>> =
        flow<Result<SpeechRecognitionOutcome>> {
            emit(
                Result.Success(
                    keywordAudioRecognizer.recognize(
                        samples = samples,
                        languageTag = languageTag,
                        biasingStrings = biasingStrings,
                        detectLanguageAmong = detectLanguageAmong,
                    ),
                ),
            )
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun downloadLanguage(languageTag: String): Flow<Result<Boolean>> =
        flow<Result<Boolean>> { emit(Result.Success(keywordAudioRecognizer.downloadLanguage(languageTag))) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()
}
