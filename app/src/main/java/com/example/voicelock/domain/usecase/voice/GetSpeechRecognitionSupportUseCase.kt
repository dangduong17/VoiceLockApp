package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.data.model.voice.SpeechRecognitionSupport
import com.example.voicelock.data.repository.speechToTextRepository.SpeechToTextRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class GetSpeechRecognitionSupportUseCase(
    private val speechToTextRepository: SpeechToTextRepository,
) {
    operator fun invoke(): Flow<Result<SpeechRecognitionSupport>> = speechToTextRepository.getSupport()
}
