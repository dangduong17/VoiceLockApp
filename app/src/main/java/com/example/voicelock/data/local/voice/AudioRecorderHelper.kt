package com.example.voicelock.data.local.voice

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.log10

/**
 * Captures a fixed-length clip of 16 kHz mono PCM audio as normalized floats in [-1, 1],
 * the input format expected by both Sherpa-ONNX and [AcousticPhraseMatcher].
 *
 * Prefers ENCODING_PCM_FLOAT and falls back to 16-bit PCM (converted in place) on devices
 * whose HAL refuses float capture. Recording is cancellable: cancelling the calling coroutine
 * stops capture within one read chunk and always releases the AudioRecord.
 *
 * [durationMs] is an upper bound: capture ends early once the speaker has talked and then been
 * silent for [END_SILENCE_MS], so a short passphrase does not wait for the full window.
 */
class AudioRecorderHelper {

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun record(durationMs: Long = DEFAULT_DURATION_MS): FloatArray =
        withContext(Dispatchers.IO) {
            val totalSamples = (SAMPLE_RATE * durationMs / 1000L).toInt()
            val output = FloatArray(totalSamples)
            val endpointer = Endpointer()
            val session = openSession()
            try {
                session.audioRecord.startRecording()
                check(session.audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    "Microphone is busy or unavailable"
                }
                var offset = 0
                while (offset < totalSamples) {
                    ensureActive()
                    val toRead = minOf(session.chunkSize, totalSamples - offset)
                    val read = session.read(output, offset, toRead)
                    check(read >= 0) { "AudioRecord read failed with code $read" }
                    val isUtteranceOver = endpointer.accept(output, offset, read)
                    offset += read
                    if (isUtteranceOver) break
                }
                if (offset < totalSamples) output.copyOf(offset) else output
            } finally {
                runCatching { session.audioRecord.stop() }
                session.audioRecord.release()
            }
        }

    @SuppressLint("MissingPermission")
    private fun openSession(): RecordingSession =
        createAudioRecord(AudioFormat.ENCODING_PCM_FLOAT)?.let(::FloatSession)
            ?: createAudioRecord(AudioFormat.ENCODING_PCM_16BIT)?.let(::ShortSession)
            ?: error("Unable to initialize AudioRecord at $SAMPLE_RATE Hz")

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(encoding: Int): AudioRecord? {
        val minBufferBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, encoding)
        if (minBufferBytes <= 0) return null
        val bytesPerSample = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
        val bufferBytes = maxOf(minBufferBytes, CHUNK_SAMPLES * bytesPerSample * 4)
        val record =
            runCatching {
                AudioRecord(
                    // VOICE_RECOGNITION skips AGC/noise suppression on most devices, which keeps the
                    // voice timbre closer to what the speaker-embedding model was trained on.
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    encoding,
                    bufferBytes,
                )
            }.getOrNull() ?: return null
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Timber.tag(TAG).w("AudioRecord init failed for encoding $encoding")
            record.release()
            return null
        }
        return record
    }

    private sealed class RecordingSession(
        val audioRecord: AudioRecord,
    ) {
        val chunkSize: Int = CHUNK_SAMPLES

        abstract fun read(
            target: FloatArray,
            offset: Int,
            length: Int,
        ): Int
    }

    private class FloatSession(
        audioRecord: AudioRecord,
    ) : RecordingSession(audioRecord) {
        override fun read(
            target: FloatArray,
            offset: Int,
            length: Int,
        ): Int = audioRecord.read(target, offset, length, AudioRecord.READ_BLOCKING)
    }

    private class ShortSession(
        audioRecord: AudioRecord,
    ) : RecordingSession(audioRecord) {
        private val buffer = ShortArray(CHUNK_SAMPLES)

        override fun read(
            target: FloatArray,
            offset: Int,
            length: Int,
        ): Int {
            val read = audioRecord.read(buffer, 0, length, AudioRecord.READ_BLOCKING)
            for (i in 0 until read) target[offset + i] = buffer[i] / 32768f
            return read
        }
    }

    /**
     * Energy endpointer over 100 ms chunks: speech is a chunk clearly above the quietest chunk seen
     * so far (the noise floor); the utterance is over after [END_SILENCE_MS] of non-speech that
     * follows at least [MIN_SPEECH_MS] of speech.
     */
    private class Endpointer {
        private var noiseFloorDb = Float.MAX_VALUE
        private var speechMs = 0
        private var silenceAfterSpeechMs = 0

        fun accept(
            buffer: FloatArray,
            offset: Int,
            length: Int,
        ): Boolean {
            if (length <= 0) return false
            var sum = 0.0
            for (i in offset until offset + length) sum += buffer[i] * buffer[i]
            val db = (10.0 * log10(maxOf(sum / length, 1e-12))).toFloat()
            noiseFloorDb = minOf(noiseFloorDb, db)
            val chunkMs = length * 1000 / SAMPLE_RATE

            val isSpeech = db > SPEECH_MIN_DB && db > noiseFloorDb + SPEECH_ABOVE_NOISE_DB
            if (isSpeech) {
                speechMs += chunkMs
                silenceAfterSpeechMs = 0
            } else if (speechMs > 0) {
                silenceAfterSpeechMs += chunkMs
            }
            return speechMs >= MIN_SPEECH_MS && silenceAfterSpeechMs >= END_SILENCE_MS
        }
    }

    companion object {
        private const val TAG = "AudioRecorderHelper"

        const val SAMPLE_RATE = 16_000
        const val DEFAULT_DURATION_MS = 2_500L
        private const val CHUNK_SAMPLES = 1_600 // 100 ms

        private const val SPEECH_MIN_DB = -50f
        private const val SPEECH_ABOVE_NOISE_DB = 12f
        private const val MIN_SPEECH_MS = 300
        private const val END_SILENCE_MS = 600
    }
}
