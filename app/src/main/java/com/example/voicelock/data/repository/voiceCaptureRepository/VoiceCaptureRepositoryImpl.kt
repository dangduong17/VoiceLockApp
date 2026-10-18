package com.example.voicelock.data.repository.voiceCaptureRepository

import android.annotation.SuppressLint
import com.example.voicelock.data.local.voice.AudioRecorderHelper
import com.example.voicelock.util.Result
import com.example.voicelock.util.resultFlow
import kotlinx.coroutines.flow.Flow

class VoiceCaptureRepositoryImpl(
    private val audioRecorderHelper: AudioRecorderHelper,
) : VoiceCaptureRepository {

    // RECORD_AUDIO is checked by the screen before it asks for a recording.
    @SuppressLint("MissingPermission")
    override fun recordUtterance(maxDurationMs: Long): Flow<Result<FloatArray>> =
        resultFlow { audioRecorderHelper.record(durationMs = maxDurationMs) }
}
