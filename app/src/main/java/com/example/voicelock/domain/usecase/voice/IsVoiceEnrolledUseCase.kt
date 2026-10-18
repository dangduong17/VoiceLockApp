package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.repository.voiceProfileRepository.VoiceProfileRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class IsVoiceEnrolledUseCase(
    private val voiceProfileRepository: VoiceProfileRepository,
) {
    operator fun invoke(mode: VoiceVerificationMode = VoiceVerificationMode.UNLOCK): Flow<Result<Boolean>> =
        voiceProfileRepository.isVoiceEnrolled(mode)
}
