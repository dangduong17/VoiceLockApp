package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.data.local.voice.AcousticPhraseMatcher
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Whether a recording contains enough speech to be used (energy-based voice activity detection). */
class DetectSpeechUseCase {
    operator fun invoke(samples: FloatArray): Flow<Result<Boolean>> =
        flow<Result<Boolean>> { emit(Result.Success(AcousticPhraseMatcher.hasSpeech(samples))) }
            .flowOn(Dispatchers.Default)
            .catchAsResult()
}
