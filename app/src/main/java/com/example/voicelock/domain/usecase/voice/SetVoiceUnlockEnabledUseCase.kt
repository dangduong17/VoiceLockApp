package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.data.repository.voiceProfileRepository.VoiceProfileRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class SetVoiceUnlockEnabledUseCase(
    private val voiceProfileRepository: VoiceProfileRepository,
) {
    operator fun invoke(isEnabled: Boolean): Flow<Result<Unit>> = voiceProfileRepository.setVoiceUnlockEnabled(isEnabled)
}
