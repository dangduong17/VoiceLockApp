package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.data.repository.voiceCaptureRepository.VoiceCaptureRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class RecordVoiceSampleUseCase(
    private val voiceCaptureRepository: VoiceCaptureRepository,
) {
    operator fun invoke(maxDurationMs: Long): Flow<Result<FloatArray>> =
        voiceCaptureRepository.recordUtterance(maxDurationMs = maxDurationMs)
}
