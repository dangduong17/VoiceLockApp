package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.data.repository.speechToTextRepository.SpeechToTextRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class DownloadSpeechLanguageUseCase(
    private val speechToTextRepository: SpeechToTextRepository,
) {
    operator fun invoke(languageTag: String): Flow<Result<Boolean>> = speechToTextRepository.downloadLanguage(languageTag)
}
