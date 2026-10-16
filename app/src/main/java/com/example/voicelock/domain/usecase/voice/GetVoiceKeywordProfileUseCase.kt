package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.data.model.voice.VoiceKeywordProfile
import com.example.voicelock.data.repository.voiceProfileRepository.VoiceProfileRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class GetVoiceKeywordProfileUseCase(
    private val voiceProfileRepository: VoiceProfileRepository,
) {
    operator fun invoke(): Flow<Result<VoiceKeywordProfile?>> = voiceProfileRepository.getKeywordProfile()
}
