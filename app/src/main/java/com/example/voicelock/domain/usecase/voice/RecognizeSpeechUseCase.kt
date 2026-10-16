package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.data.model.voice.SpeechRecognitionOutcome
import com.example.voicelock.data.repository.speechToTextRepository.SpeechToTextRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class RecognizeSpeechUseCase(
    private val speechToTextRepository: SpeechToTextRepository,
) {
    operator fun invoke(
        samples: FloatArray,
        languageTag: String,
        biasingStrings: List<String> = emptyList(),
        detectLanguageAmong: List<String> = emptyList(),
    ): Flow<Result<SpeechRecognitionOutcome>> =
        speechToTextRepository.recognize(
            samples = samples,
            languageTag = languageTag,
            biasingStrings = biasingStrings,
            detectLanguageAmong = detectLanguageAmong,
        )
}
