package com.example.voicelock.di

import com.example.voicelock.domain.voice.VoiceAuthEngine
import com.example.voicelock.domain.voice.antispoof.ReplayDetector
import com.example.voicelock.domain.voice.audio.VoiceRecorder
import com.example.voicelock.domain.voice.model.MfccStatsEmbedder
import com.example.voicelock.domain.voice.model.SpeakerEmbedder
import com.example.voicelock.domain.voice.model.TfLiteSpeakerEmbedder
import com.example.voicelock.util.Constant
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import timber.log.Timber

private const val TAG = "CoreModule"

val coreModule = module {
    single { VoiceRecorder() }
    single { ReplayDetector() }
    single<SpeakerEmbedder> {
        val context = androidContext()
        if (TfLiteSpeakerEmbedder.isModelBundled(context = context, assetName = Constant.SPEAKER_MODEL_ASSET)) {
            runCatching {
                TfLiteSpeakerEmbedder(
                    context = context,
                    assetName = Constant.SPEAKER_MODEL_ASSET,
                    defaultThreshold = Constant.TFLITE_THRESHOLD,
                )
            }.onFailure { Timber.tag(TAG).e(it, "Failed to load TFLite speaker model, using MFCC fallback") }
                .getOrNull() ?: MfccStatsEmbedder()
        } else {
            Timber.tag(TAG).w("${Constant.SPEAKER_MODEL_ASSET} not bundled, using MFCC fallback embedder")
            MfccStatsEmbedder()
        }
    }
    single { VoiceAuthEngine(embedder = get(), replayDetector = get()) }
}
