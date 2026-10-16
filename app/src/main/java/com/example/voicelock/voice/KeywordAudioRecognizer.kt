package com.example.voicelock.voice

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Stage 1 (keyword text) on audio the app has ALREADY recorded: the clip is streamed into
 * Android's SpeechRecognizer through EXTRA_AUDIO_SOURCE (API 33+), so the user speaks once and the
 * very same samples also feed Sherpa-ONNX and the DTW matcher.
 *
 * Returns [Outcome.Unsupported] when the device cannot recognize pre-recorded audio (API < 33, no
 * recognizer, engine refuses the audio source, or it never answers); callers then fall back to the
 * speaker + phrase checks only. Once a device proves unsupported it is not retried this process.
 */
class KeywordAudioRecognizer(
    private val context: Context,
) {
    sealed interface Outcome {
        data class Candidates(
            val texts: List<String>,
        ) : Outcome

        data object NoSpeech : Outcome

        data object Unsupported : Outcome

        data class Failed(
            val errorCode: Int,
        ) : Outcome
    }

    suspend fun recognize(
        samples: FloatArray,
        languageTag: String = Locale.getDefault().toLanguageTag(),
    ): Outcome {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || isKnownUnsupported) return Outcome.Unsupported
        val outcome =
            withTimeoutOrNull(RECOGNITION_TIMEOUT_MS) {
                withContext(Dispatchers.Main) { recognizeOnMain(samples, languageTag) }
            } ?: Outcome.Unsupported.also { Timber.w("SpeechRecognizer did not answer for injected audio") }
        if (outcome == Outcome.Unsupported) isKnownUnsupported = true
        Timber.d("Keyword recognition outcome=%s", outcome)
        return outcome
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun recognizeOnMain(
        samples: FloatArray,
        languageTag: String,
    ): Outcome =
        suspendCancellableCoroutine { continuation ->
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                continuation.resume(Outcome.Unsupported)
                return@suspendCancellableCoroutine
            }
            val recognizer =
                if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
            val (readSide, writeSide) = ParcelFileDescriptor.createPipe()
            val mainHandler = Handler(Looper.getMainLooper())
            var isReleased = false
            var lastPartial: List<String> = emptyList()

            fun release() {
                if (isReleased) return
                isReleased = true
                recognizer.destroy()
                runCatching { readSide.close() }
            }

            fun finish(outcome: Outcome) {
                release()
                if (continuation.isActive) continuation.resume(outcome)
            }

            recognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onResults(results: Bundle?) {
                        val texts =
                            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                                .filter { it.isNotBlank() }
                                .ifEmpty { lastPartial }
                        finish(if (texts.isEmpty()) Outcome.NoSpeech else Outcome.Candidates(texts))
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial =
                            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                                .filter { it.isNotBlank() }
                        if (partial.isNotEmpty()) lastPartial = partial
                    }

                    override fun onError(error: Int) {
                        Timber.w("SpeechRecognizer (injected audio) error=%d partial=%s", error, lastPartial)
                        finish(
                            when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                    if (lastPartial.isNotEmpty()) Outcome.Candidates(lastPartial) else Outcome.NoSpeech
                                in UNSUPPORTED_ERRORS -> Outcome.Unsupported
                                else -> Outcome.Failed(error)
                            },
                        )
                    }

                    override fun onReadyForSpeech(params: Bundle?) = Unit

                    override fun onBeginningOfSpeech() = Unit

                    override fun onRmsChanged(rmsdB: Float) = Unit

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() = Unit

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?,
                    ) = Unit
                },
            )

            continuation.invokeOnCancellation { mainHandler.post { release() } }
            recognizer.startListening(buildIntent(readSide, languageTag))
            streamSamples(samples, writeSide)
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun buildIntent(
        audioSource: ParcelFileDescriptor,
        languageTag: String,
    ): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, audioSource)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, AudioRecorderHelper.SAMPLE_RATE)
        }

    /** Writes the clip as 16-bit little-endian PCM on a background thread, then closes (EOF). */
    private fun streamSamples(
        samples: FloatArray,
        writeSide: ParcelFileDescriptor,
    ) {
        Thread({
            val bytes = ByteBuffer.allocate(samples.size * Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { sample -> bytes.putShort((sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()) }
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { it.write(bytes.array()) }
            } catch (e: IOException) {
                // The recognizer closed its end early (error or cancellation) — nothing left to do.
                Timber.w(e, "Audio stream to SpeechRecognizer closed early")
            }
        }, "keyword-audio-writer").start()
    }

    private companion object {
        const val RECOGNITION_TIMEOUT_MS = 6_000L
        const val MAX_RESULTS = 5

        /** ERROR_CLIENT, LANGUAGE_NOT_SUPPORTED, LANGUAGE_UNAVAILABLE, CANNOT_LISTEN_TO_DOWNLOAD_EVENTS. */
        val UNSUPPORTED_ERRORS = setOf(5, 12, 13, 14)

        @Volatile
        var isKnownUnsupported = false
    }
}
