package com.example.voicelock.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/**
 * Device probe (not a regular test): synthesizes known phrases with the device TTS and checks what
 * the speech-to-text stack can do with injected audio — offline, online, and online with language
 * detection. Results go to logcat with tag "VLProbe".
 */
@RunWith(AndroidJUnit4::class)
class LanguageDetectionProbe {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun probe() =
        runBlocking {
            val recognizer = KeywordAudioRecognizer(context)
            val support = recognizer.getRecognitionSupport()
            log("installed=${support.installed}")
            log("downloadable(${support.downloadable.size})=${support.downloadable}")
            log("online(${support.online.size})=${support.online.take(80)}")

            val detectAmong = (support.installed + support.downloadable).distinct()
            for ((languageTag, text) in listOf("ja-JP" to "こんにちは", "en-US" to "unlock my phone", "vi-VN" to "mở khóa")) {
                val samples = synthesize(languageTag, text)
                if (samples == null) {
                    log("[$languageTag] TTS unavailable, skipped")
                    continue
                }
                log("[$languageTag] '$text' ${samples.size} samples")
                log("[$languageTag] offline vi-VN -> ${recognizer.recognize(samples, "vi-VN")}")
                log("[$languageTag] online $languageTag -> ${recognizer.recognize(samples, languageTag, useOnline = true)}")
                log(
                    "[$languageTag] online detect -> " +
                        recognizer.recognize(samples, "vi-VN", detectLanguageAmong = detectAmong, useOnline = true),
                )
                log(
                    "[$languageTag] offline detect -> " +
                        recognizer.recognize(samples, "vi-VN", detectLanguageAmong = detectAmong),
                )
            }
        }

    /** Downloads the Japanese offline model through the app's code path, then transcribes offline. */
    @Test
    fun downloadProbe() =
        runBlocking {
            val recognizer = KeywordAudioRecognizer(context)
            log("before download installed=${recognizer.getInstalledLanguages()}")
            val isInstalled = recognizer.downloadLanguage("ja-JP")
            log("downloadLanguage(ja-JP) -> $isInstalled, installed=${recognizer.getInstalledLanguages()}")
            val samples = synthesize("ja-JP", "こんにちは") ?: return@runBlocking log("TTS unavailable")
            log("offline ja-JP after download -> ${recognizer.recognize(samples, "ja-JP")}")
        }

    /** Device TTS -> 16 kHz mono float samples, or null when the voice is not available. */
    private suspend fun synthesize(
        languageTag: String,
        text: String,
    ): FloatArray? {
        val initialized = CompletableDeferred<Boolean>()
        lateinit var tts: TextToSpeech
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            tts = TextToSpeech(context) { status -> initialized.complete(status == TextToSpeech.SUCCESS) }
        }
        try {
            if (withTimeoutOrNull(10_000) { initialized.await() } != true) return null
            val availability = tts.setLanguage(Locale.forLanguageTag(languageTag))
            if (availability < TextToSpeech.LANG_AVAILABLE) return null

            val file = File(context.cacheDir, "probe_$languageTag.wav")
            val done = CompletableDeferred<Boolean>()
            tts.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        done.complete(true)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        done.complete(false)
                    }
                },
            )
            tts.synthesizeToFile(text, Bundle(), file, "probe")
            if (withTimeoutOrNull(20_000) { done.await() } != true) return null
            return readWavAs16kMono(file.readBytes())
        } finally {
            tts.shutdown()
        }
    }

    private fun readWavAs16kMono(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val channels = buffer.getShort(22).toInt()
        val sampleRate = buffer.getInt(24)
        var offset = 12
        var dataOffset = 44
        var dataSize = bytes.size - 44
        while (offset + 8 <= bytes.size) {
            val id = String(bytes, offset, 4, Charsets.US_ASCII)
            val size = buffer.getInt(offset + 4)
            if (id == "data") {
                dataOffset = offset + 8
                dataSize = minOf(size, bytes.size - dataOffset)
                break
            }
            offset += 8 + size
        }
        val frames = dataSize / (2 * channels)
        val mono = FloatArray(frames) { i -> buffer.getShort(dataOffset + i * 2 * channels) / 32768f }
        val outLength = (frames.toLong() * 16_000 / sampleRate).toInt()
        return FloatArray(outLength) { i ->
            val position = i.toDouble() * sampleRate / 16_000
            val index = position.toInt().coerceAtMost(frames - 2)
            val fraction = (position - index).toFloat()
            mono[index] * (1 - fraction) + mono[index + 1] * fraction
        }
    }

    private fun log(message: String) {
        Log.i("VLProbe", message)
    }
}
