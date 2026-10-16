package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.model.voice.VoiceVerifyResult
import com.example.voicelock.data.repository.voiceProfileRepository.VoiceProfileRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class VerifyOwnerVoiceUseCase(
    private val voiceProfileRepository: VoiceProfileRepository,
) {
    operator fun invoke(
        samples: FloatArray,
        mode: VoiceVerificationMode,
        isKeywordChecked: Boolean,
    ): Flow<Result<VoiceVerifyResult>> = voiceProfileRepository.verifyOwnerVoice(samples, mode, isKeywordChecked)
}
