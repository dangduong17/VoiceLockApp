package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.data.model.voice.VoiceEnrollmentResult
import com.example.voicelock.data.repository.voiceProfileRepository.VoiceProfileRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class EnrollOwnerVoiceUseCase(
    private val voiceProfileRepository: VoiceProfileRepository,
) {
    operator fun invoke(recordings: List<FloatArray>): Flow<Result<VoiceEnrollmentResult>> =
        voiceProfileRepository.enrollOwnerVoice(recordings)
}
