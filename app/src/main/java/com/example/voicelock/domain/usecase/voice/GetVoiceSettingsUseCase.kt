package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.data.model.voice.VoiceSettingsDtoModel
import com.example.voicelock.data.repository.voiceProfileRepository.VoiceProfileRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class GetVoiceSettingsUseCase(
    private val voiceProfileRepository: VoiceProfileRepository,
) {
    operator fun invoke(): Flow<Result<VoiceSettingsDtoModel>> = voiceProfileRepository.getVoiceSettings()
}
