package com.example.voicelock.data.repository.voiceCaptureRepository

import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

interface VoiceCaptureRepository {
    /**
     * Records one utterance as 16 kHz mono float PCM. [maxDurationMs] is an upper bound: capture
     * stops early once the speaker has finished. The caller must hold RECORD_AUDIO.
     */
    fun recordUtterance(maxDurationMs: Long): Flow<Result<FloatArray>>
}
