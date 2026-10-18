package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.data.local.voice.AcousticPhraseMatcher
import com.example.voicelock.util.Result
import com.example.voicelock.util.resultFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/** Whether a recording contains enough speech to be used (energy-based voice activity detection). */
class DetectSpeechUseCase {
    operator fun invoke(samples: FloatArray): Flow<Result<Boolean>> =
        resultFlow(Dispatchers.Default) { AcousticPhraseMatcher.hasSpeech(samples) }
}
