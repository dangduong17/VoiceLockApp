package com.example.voicelock.data.local.voice

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.ModelDownloadListener
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import com.example.voicelock.data.model.voice.SpeechRecognitionOutcome
import com.example.voicelock.data.model.voice.SpeechRecognitionSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
 * Returns [SpeechRecognitionOutcome.Unsupported] when the device cannot recognize pre-recorded audio (API < 33, no
 * recognizer, engine refuses the audio source, or it never answers); callers then fall back to the
 * speaker + phrase checks only.
 *
 * The recognizer is built for a live microphone, so the clip is trimmed, padded with silence
 * (lead-in + trailing silence lets the endpointer close the utterance) and streamed in chunks;
 * [biasingStrings] (the owner's enrolled phrase) steer on-device engines toward it.
 *
 * With internet the online engine goes first (much more accurate, notably for Vietnamese); the
 * on-device engine is the fallback, so recognition still works offline.
 */
class KeywordAudioRecognizer(
    private val context: Context,
) {
    /** Pre-recorded audio can be recognized at all (API 33+); per-language support is separate. */
    fun isSupportedOnDevice(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * [useOnline] null (default) = online first when the device has internet, falling back to the
     * on-device engine when the online pass gives no text; true / false = that engine only.
     * Language detection requests stay on-device: the online engine ignores them (verified on device).
     */
    suspend fun recognize(
        samples: FloatArray,
        languageTag: String = Locale.getDefault().toLanguageTag(),
        biasingStrings: List<String> = emptyList(),
        detectLanguageAmong: List<String> = emptyList(),
        useOnline: Boolean? = null,
    ): SpeechRecognitionOutcome {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return SpeechRecognitionOutcome.Unsupported
        val audio = withContext(Dispatchers.Default) { prepareAudio(samples) }
        val isOnlineFirst = useOnline == null && detectLanguageAmong.isEmpty() && hasInternet()
        if (isOnlineFirst) {
            val online =
                recognizeOnce(
                    audio = audio,
                    languageTag = languageTag,
                    biasingStrings = biasingStrings,
                    detectLanguageAmong = detectLanguageAmong,
                    useOnline = true,
                    timeoutMs = ONLINE_RECOGNITION_TIMEOUT_MS,
                )
            if (online is SpeechRecognitionOutcome.Candidates) return online
        }
        return recognizeOnce(
            audio = audio,
            languageTag = languageTag,
            biasingStrings = biasingStrings,
            detectLanguageAmong = detectLanguageAmong,
            useOnline = useOnline ?: false,
            timeoutMs = RECOGNITION_TIMEOUT_MS,
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun recognizeOnce(
        audio: FloatArray,
        languageTag: String,
        biasingStrings: List<String>,
        detectLanguageAmong: List<String>,
        useOnline: Boolean,
        timeoutMs: Long,
    ): SpeechRecognitionOutcome {
        val outcome =
            withTimeoutOrNull(timeoutMs) {
                withContext(Dispatchers.Main) { recognizeOnMain(audio, languageTag, biasingStrings, detectLanguageAmong, useOnline) }
            } ?: SpeechRecognitionOutcome.Unsupported.also { Timber.tag(TAG).w("SpeechRecognizer (online=%s) did not answer for injected audio", useOnline) }
        Timber.tag(TAG).d("Keyword recognition language=%s online=%s outcome=%s", languageTag, useOnline, outcome)
        return outcome
    }

    private fun hasInternet(): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Languages the on-device recognizer has installed (API 33+), e.g. ["vi-VN", "en-US"]. Empty when
     * unknown (older Android, no on-device engine, or the query fails).
     */
    suspend fun getInstalledLanguages(): List<String> = getRecognitionSupport().installed

    suspend fun getRecognitionSupport(): SpeechRecognitionSupport {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return SpeechRecognitionSupport()
        val info =
            withTimeoutOrNull(SUPPORT_QUERY_TIMEOUT_MS) {
                withContext(Dispatchers.Main) { querySupport() }
            } ?: SpeechRecognitionSupport()
        Timber.tag(TAG).d("Recognizer support installed=%s downloadable=%d online=%d", info.installed, info.downloadable.size, info.online.size)
        return info
    }

    /**
     * Asks the system to download the offline model for [languageTag] and waits until it is installed
     * (needs network; the system may ask the user first). Returns false as soon as the system reports
     * a failure or refusal (Android 14+), or after [DOWNLOAD_TIMEOUT_MS]. Cancellable: the caller's
     * "Cancel" button simply cancels the coroutine.
     */
    suspend fun downloadLanguage(languageTag: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (isInstalled(languageTag)) return true
        val isInstalled =
            withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val result = withContext(Dispatchers.Main) { requestDownload(languageTag) }
                    Timber.tag(TAG).d("Model download for %s reported %s", languageTag, result)
                    // SCHEDULED = the system will fetch it (e.g. waiting for a network); keep checking.
                    if (result == DownloadResult.FAILED) false else waitUntilInstalled(languageTag)
                } else {
                    withContext(Dispatchers.Main) { triggerDownloadFireAndForget(languageTag) }
                    waitUntilInstalled(languageTag)
                }
            } ?: false
        Timber.tag(TAG).d("Download of %s finished installed=%s", languageTag, isInstalled)
        return isInstalled
    }

    private enum class DownloadResult { SUCCEEDED, SCHEDULED, FAILED }

    private suspend fun isInstalled(languageTag: String): Boolean = getInstalledLanguages().any { it.equals(languageTag, ignoreCase = true) }

    private suspend fun waitUntilInstalled(languageTag: String): Boolean {
        while (!isInstalled(languageTag)) delay(DOWNLOAD_POLL_MS)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun requestDownload(languageTag: String): DownloadResult =
        suspendCancellableCoroutine { continuation ->
            val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            val mainHandler = Handler(Looper.getMainLooper())
            fun finish(result: DownloadResult) {
                mainHandler.post { recognizer.destroy() }
                if (continuation.isActive) continuation.resume(result)
            }
            continuation.invokeOnCancellation { mainHandler.post { recognizer.destroy() } }
            runCatching {
                recognizer.triggerModelDownload(
                    downloadIntent(languageTag),
                    context.mainExecutor,
                    object : ModelDownloadListener {
                        override fun onProgress(completedPercent: Int) = Unit

                        override fun onSuccess() = finish(DownloadResult.SUCCEEDED)

                        override fun onScheduled() = finish(DownloadResult.SCHEDULED)

                        override fun onError(error: Int) {
                            Timber.tag(TAG).w("Model download for %s failed error=%d", languageTag, error)
                            finish(DownloadResult.FAILED)
                        }
                    },
                )
            }.onFailure {
                Timber.tag(TAG).w(it, "triggerModelDownload failed for %s", languageTag)
                finish(DownloadResult.FAILED)
            }
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun triggerDownloadFireAndForget(languageTag: String) {
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        runCatching { recognizer.triggerModelDownload(downloadIntent(languageTag)) }
            .onFailure { Timber.tag(TAG).w(it, "triggerModelDownload failed for %s", languageTag) }
        Handler(Looper.getMainLooper()).postDelayed({ recognizer.destroy() }, DESTROY_AFTER_TRIGGER_MS)
    }

    private fun downloadIntent(languageTag: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun querySupport(): SpeechRecognitionSupport =
        suspendCancellableCoroutine { continuation ->
            if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                continuation.resume(SpeechRecognitionSupport())
                return@suspendCancellableCoroutine
            }
            val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            val mainHandler = Handler(Looper.getMainLooper())
            fun finish(info: SpeechRecognitionSupport) {
                mainHandler.post { recognizer.destroy() }
                if (continuation.isActive) continuation.resume(info)
            }
            continuation.invokeOnCancellation { mainHandler.post { recognizer.destroy() } }
            recognizer.checkRecognitionSupport(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                context.mainExecutor,
                object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: RecognitionSupport) =
                        finish(
                            SpeechRecognitionSupport(
                                installed = recognitionSupport.installedOnDeviceLanguages,
                                downloadable = recognitionSupport.supportedOnDeviceLanguages,
                                online = recognitionSupport.onlineLanguages,
                            ),
                        )

                    override fun onError(error: Int) {
                        Timber.tag(TAG).w("checkRecognitionSupport error=%d", error)
                        finish(SpeechRecognitionSupport())
                    }
                },
            )
        }

    /** Speech only, plus 200 ms lead-in and 700 ms trailing silence. */
    private fun prepareAudio(samples: FloatArray): FloatArray {
        val speech = AcousticPhraseMatcher.trimSilence(samples) ?: samples
        val lead = AudioRecorderHelper.SAMPLE_RATE * LEAD_SILENCE_MS / 1000
        val trail = AudioRecorderHelper.SAMPLE_RATE * TRAIL_SILENCE_MS / 1000
        return FloatArray(lead) + speech + FloatArray(trail)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun recognizeOnMain(
        samples: FloatArray,
        languageTag: String,
        biasingStrings: List<String>,
        detectLanguageAmong: List<String>,
        useOnline: Boolean,
    ): SpeechRecognitionOutcome =
        suspendCancellableCoroutine { continuation ->
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                continuation.resume(SpeechRecognitionOutcome.Unsupported)
                return@suspendCancellableCoroutine
            }
            val recognizer =
                if (!useOnline && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
            val (readSide, writeSide) = ParcelFileDescriptor.createPipe()
            val mainHandler = Handler(Looper.getMainLooper())
            var isReleased = false
            var lastPartial: List<String> = emptyList()
            var detectedLanguage: String? = null

            fun release() {
                if (isReleased) return
                isReleased = true
                recognizer.destroy()
                runCatching { readSide.close() }
            }

            fun finish(outcome: SpeechRecognitionOutcome) {
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
                        finish(if (texts.isEmpty()) SpeechRecognitionOutcome.NoSpeech(detectedLanguage) else SpeechRecognitionOutcome.Candidates(texts, detectedLanguage))
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial =
                            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                                .filter { it.isNotBlank() }
                        if (partial.isNotEmpty()) lastPartial = partial
                    }

                    override fun onError(error: Int) {
                        Timber.tag(TAG).w("SpeechRecognizer (injected audio) error=%d partial=%s", error, lastPartial)
                        finish(
                            when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                    if (lastPartial.isNotEmpty()) SpeechRecognitionOutcome.Candidates(lastPartial, detectedLanguage) else SpeechRecognitionOutcome.NoSpeech(detectedLanguage)
                                in UNSUPPORTED_ERRORS -> SpeechRecognitionOutcome.Unsupported
                                else -> SpeechRecognitionOutcome.Failed(error)
                            },
                        )
                    }

                    // API 34+: only called when detection was requested in the intent.
                    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    override fun onLanguageDetection(results: Bundle) {
                        // Engines report lower-case tags ("ja-jp"); normalize to BCP-47 ("ja-JP").
                        results.getString(SpeechRecognizer.DETECTED_LANGUAGE)?.let { detectedLanguage = Locale.forLanguageTag(it).toLanguageTag() }
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
            recognizer.startListening(buildIntent(readSide, languageTag, biasingStrings, detectLanguageAmong, useOnline))
            streamSamples(samples, writeSide)
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun buildIntent(
        audioSource: ParcelFileDescriptor,
        languageTag: String,
        biasingStrings: List<String>,
        detectLanguageAmong: List<String>,
        useOnline: Boolean,
    ): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, !useOnline)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, audioSource)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, AudioRecorderHelper.SAMPLE_RATE)
            if (biasingStrings.isNotEmpty()) putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, ArrayList(biasingStrings))
            if (detectLanguageAmong.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Let the engine identify the language and switch to it, so one pass also transcribes
                // in the right language.
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                putStringArrayListExtra(RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES, ArrayList(detectLanguageAmong))
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, RecognizerIntent.LANGUAGE_SWITCH_HIGH_PRECISION)
                putStringArrayListExtra(RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES, ArrayList(detectLanguageAmong))
            }
        }

    /**
     * Streams the clip as 16-bit little-endian PCM in 100 ms chunks, then closes (EOF). Chunks go out
     * ~5x faster than real time: pacing at 1x added ~3.5 s per attempt without changing the text.
     */
    private fun streamSamples(
        samples: FloatArray,
        writeSide: ParcelFileDescriptor,
    ) {
        Thread({
            val bytes = ByteBuffer.allocate(samples.size * Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { sample -> bytes.putShort((sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()) }
            val data = bytes.array()
            val chunkBytes = AudioRecorderHelper.SAMPLE_RATE * Short.SIZE_BYTES * CHUNK_MS / 1000
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { output ->
                    var offset = 0
                    while (offset < data.size) {
                        val length = minOf(chunkBytes, data.size - offset)
                        output.write(data, offset, length)
                        output.flush()
                        offset += length
                        Thread.sleep(CHUNK_PAUSE_MS)
                    }
                }
            } catch (e: InterruptedException) {
                Timber.tag(TAG).w(e, "Audio stream to SpeechRecognizer interrupted")
            } catch (e: IOException) {
                // The recognizer closed its end early (error or cancellation) — nothing left to do.
                Timber.tag(TAG).w(e, "Audio stream to SpeechRecognizer closed early")
            }
        }, "keyword-audio-writer").start()
    }

    private companion object {
        private const val TAG = "KeywordAudioRecognizer"

        const val RECOGNITION_TIMEOUT_MS = 6_000L

        // Shorter, so a slow network costs little before the on-device pass takes over.
        const val ONLINE_RECOGNITION_TIMEOUT_MS = 4_000L
        const val SUPPORT_QUERY_TIMEOUT_MS = 3_000L
        const val DOWNLOAD_TIMEOUT_MS = 60_000L
        const val DOWNLOAD_POLL_MS = 2_000L
        const val DESTROY_AFTER_TRIGGER_MS = 5_000L
        const val LEAD_SILENCE_MS = 200
        const val TRAIL_SILENCE_MS = 700
        const val CHUNK_MS = 100
        const val CHUNK_PAUSE_MS = 20L
        const val MAX_RESULTS = 5

        /** ERROR_CLIENT, LANGUAGE_NOT_SUPPORTED, LANGUAGE_UNAVAILABLE, CANNOT_LISTEN_TO_DOWNLOAD_EVENTS. */
        val UNSUPPORTED_ERRORS = setOf(5, 12, 13, 14)
    }
}
