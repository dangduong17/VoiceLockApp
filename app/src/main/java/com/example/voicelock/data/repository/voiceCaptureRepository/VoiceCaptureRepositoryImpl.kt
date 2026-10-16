package com.example.voicelock.data.repository.voiceCaptureRepository

import android.annotation.SuppressLint
import com.example.voicelock.data.local.voice.AudioRecorderHelper
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class VoiceCaptureRepositoryImpl(
    private val audioRecorderHelper: AudioRecorderHelper,
) : VoiceCaptureRepository {

    // RECORD_AUDIO is checked by the screen before it asks for a recording.
    @SuppressLint("MissingPermission")
    override fun recordUtterance(maxDurationMs: Long): Flow<Result<FloatArray>> =
        flow<Result<FloatArray>> { emit(Result.Success(audioRecorderHelper.record(durationMs = maxDurationMs))) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()
}
