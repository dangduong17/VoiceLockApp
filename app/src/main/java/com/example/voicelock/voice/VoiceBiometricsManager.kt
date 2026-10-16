package com.example.voicelock.voice

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.example.voicelock.data.model.voice.VoiceEnrollmentResult
import com.example.voicelock.data.model.voice.VoiceScores
import com.example.voicelock.data.model.voice.VoiceThresholds
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.model.voice.VoiceVerifyResult
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Owner voice biometrics on top of Sherpa-ONNX speaker verification (CAM++ model).
 *
 * - Enrollment: every sample is trimmed, embedded and L2-normalized; the normalized mean
 *   (centroid) becomes the voiceprint. The first sample's MFCC matrix is kept as the Stage-2
 *   phrase template. Both are stored in EncryptedSharedPreferences (AES-256-GCM, Keystore key).
 * - Verification: the speaker embedding and the DTW phrase match run concurrently on
 *   Dispatchers.Default; both must pass the thresholds of the requested mode.
 *
 * The native extractor is created lazily (model load takes ~0.5–1 s) and is NOT thread-safe, so
 * every call is serialized through [extractorMutex]. Register this class as a Koin `single`.
 */
class VoiceBiometricsManager(
    private val context: Context,
    private val securePrefs: SharedPreferences,
    private val modelAssetPath: String = DEFAULT_MODEL_ASSET_PATH,
    private val numThreads: Int = DEFAULT_NUM_THREADS,
) {
    private val extractorMutex = Mutex()
    private var extractor: SpeakerEmbeddingExtractor? = null

    /** Loads the model ahead of time so the first verification is not delayed. */
    suspend fun warmUp() {
        withContext(Dispatchers.Default) { withExtractor { } }
    }

    fun isModelAvailable(): Boolean =
        runCatching { context.assets.list("")?.contains(modelAssetPath) == true }.getOrDefault(false)

    fun isEnrolled(): Boolean =
        securePrefs.getString(KEY_MODEL, null) == modelAssetPath &&
            securePrefs.contains(KEY_CENTROID) &&
            securePrefs.contains(KEY_PHRASE_TEMPLATE)

    fun getEnrolledSampleCount(): Int = if (isEnrolled()) securePrefs.getInt(KEY_SAMPLE_COUNT, 0) else 0

    suspend fun enrollOwnerVoice(recordings: List<FloatArray>): VoiceEnrollmentResult =
        withContext(Dispatchers.Default) {
            require(recordings.isNotEmpty()) { "At least one recording is required" }

            val trimmed =
                recordings.mapIndexed { index, samples ->
                    AcousticPhraseMatcher.trimSilence(samples)
                        ?: return@withContext VoiceEnrollmentResult.NoSpeechDetected(sampleIndex = index)
                }

            val embeddings = trimmed.map { computeEmbedding(it) }
            val centroid = averageEmbedding(embeddings)
            val consistency = embeddings.minOf { cosineSimilarity(it, centroid) }
            Timber.d("Voice enrollment consistency=%.3f", consistency)
            if (consistency < MIN_ENROLLMENT_CONSISTENCY) {
                return@withContext VoiceEnrollmentResult.InconsistentSamples(consistency)
            }

            val phraseTemplate = AcousticPhraseMatcher.extractFeatures(trimmed.first())
            withContext(Dispatchers.IO) {
                securePrefs.edit(commit = true) {
                    putString(KEY_MODEL, modelAssetPath)
                    putString(KEY_CENTROID, encodeFloats(centroid))
                    putString(KEY_SAMPLE_EMBEDDINGS, encodeFloats(embeddings.flatMap { it.asList() }.toFloatArray()))
                    putString(KEY_PHRASE_TEMPLATE, encodeBytes(AcousticPhraseMatcher.serialize(phraseTemplate)))
                    putInt(KEY_SAMPLE_COUNT, recordings.size)
                }
            }
            VoiceEnrollmentResult.Success(consistency)
        }

    suspend fun verify(
        samples: FloatArray,
        mode: VoiceVerificationMode,
        thresholds: VoiceThresholds = mode.defaultThresholds,
    ): VoiceVerifyResult {
        val profile = withContext(Dispatchers.IO) { loadProfile() } ?: return VoiceVerifyResult.NotEnrolled

        return withContext(Dispatchers.Default) {
            val speech = AcousticPhraseMatcher.trimSilence(samples) ?: return@withContext VoiceVerifyResult.NoSpeechDetected

            val scores =
                coroutineScope {
                    val speakerScore = async { speakerSimilarity(computeEmbedding(speech), profile) }
                    val phraseScore =
                        async {
                            AcousticPhraseMatcher.dtwSimilarity(
                                AcousticPhraseMatcher.extractFeatures(speech),
                                profile.phraseTemplate,
                            )
                        }
                    VoiceScores(speakerSimilarity = speakerScore.await(), phraseSimilarity = phraseScore.await())
                }
            Timber.d("Voice verify mode=%s speaker=%.3f phrase=%.3f", mode, scores.speakerSimilarity, scores.phraseSimilarity)

            val isSpeakerMatch = scores.speakerSimilarity >= thresholds.speakerSimilarity
            val isPhraseMatch = scores.phraseSimilarity >= thresholds.phraseSimilarity
            if (isSpeakerMatch && isPhraseMatch) {
                VoiceVerifyResult.Verified(scores)
            } else {
                VoiceVerifyResult.Rejected(scores, isSpeakerMatch = isSpeakerMatch, isPhraseMatch = isPhraseMatch)
            }
        }
    }

    fun clearEnrollment() {
        securePrefs.edit {
            remove(KEY_MODEL)
            remove(KEY_CENTROID)
            remove(KEY_SAMPLE_EMBEDDINGS)
            remove(KEY_PHRASE_TEMPLATE)
            remove(KEY_SAMPLE_COUNT)
        }
    }

    suspend fun release() {
        extractorMutex.withLock {
            extractor?.release()
            extractor = null
        }
    }

    private suspend fun computeEmbedding(samples: FloatArray): FloatArray =
        withExtractor { extractor ->
            val stream = extractor.createStream()
            try {
                stream.acceptWaveform(samples, AudioRecorderHelper.SAMPLE_RATE)
                stream.inputFinished()
                check(extractor.isReady(stream)) { "Not enough audio to compute a speaker embedding" }
                extractor.compute(stream).l2Normalized()
            } finally {
                stream.release()
            }
        }

    private suspend fun <T> withExtractor(block: (SpeakerEmbeddingExtractor) -> T): T =
        extractorMutex.withLock {
            if (extractor == null && !isModelAvailable()) throw VoiceModelMissingException(modelAssetPath)
            val instance =
                extractor ?: SpeakerEmbeddingExtractor(
                    context.assets,
                    SpeakerEmbeddingExtractorConfig(model = modelAssetPath, numThreads = numThreads),
                ).also { extractor = it }
            block(instance)
        }

    private fun loadProfile(): VoiceProfile? {
        if (!isEnrolled()) return null
        return runCatching {
            val centroid = decodeFloats(securePrefs.getString(KEY_CENTROID, null)!!)
            // Profiles enrolled before per-sample embeddings were stored only have the centroid.
            val sampleEmbeddings =
                securePrefs.getString(KEY_SAMPLE_EMBEDDINGS, null)
                    ?.let(::decodeFloats)
                    ?.takeIf { it.isNotEmpty() && it.size % centroid.size == 0 }
                    ?.asList()
                    ?.chunked(centroid.size) { it.toFloatArray() }
                    .orEmpty()
            VoiceProfile(
                centroid = centroid,
                sampleEmbeddings = sampleEmbeddings,
                phraseTemplate = AcousticPhraseMatcher.deserialize(decodeBytes(securePrefs.getString(KEY_PHRASE_TEMPLATE, null)!!)),
            )
        }.onFailure { Timber.e(it, "Stored voice profile is corrupted") }
            .getOrNull()
    }

    /**
     * Short passphrases (~1 s) give noisy CAM++ embeddings, so the attempt is scored against the
     * centroid and each enrolled sample, keeping the best match.
     */
    private fun speakerSimilarity(
        embedding: FloatArray,
        profile: VoiceProfile,
    ): Float {
        val centroidScore = cosineSimilarity(embedding, profile.centroid)
        val bestSampleScore = profile.sampleEmbeddings.maxOfOrNull { cosineSimilarity(embedding, it) } ?: centroidScore
        return maxOf(centroidScore, bestSampleScore)
    }

    private fun averageEmbedding(embeddings: List<FloatArray>): FloatArray {
        val dim = embeddings.first().size
        val sum = FloatArray(dim)
        embeddings.forEach { embedding ->
            require(embedding.size == dim) { "Embedding dimension mismatch" }
            for (i in 0 until dim) sum[i] += embedding[i]
        }
        return sum.l2Normalized()
    }

    private fun cosineSimilarity(
        a: FloatArray,
        b: FloatArray,
    ): Float {
        require(a.size == b.size) { "Embedding dimension mismatch: ${a.size} vs ${b.size}" }
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator < 1e-9f) 0f else dot / denominator
    }

    private fun FloatArray.l2Normalized(): FloatArray {
        var sumSquares = 0f
        for (value in this) sumSquares += value * value
        val norm = sqrt(sumSquares)
        return if (norm < 1e-9f) copyOf() else FloatArray(size) { this[it] / norm }
    }

    private fun encodeFloats(values: FloatArray): String {
        val buffer = ByteBuffer.allocate(values.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach(buffer::putFloat)
        return encodeBytes(buffer.array())
    }

    private fun decodeFloats(encoded: String): FloatArray {
        val buffer = ByteBuffer.wrap(decodeBytes(encoded)).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(buffer.remaining() / Float.SIZE_BYTES) { buffer.getFloat() }
    }

    private fun encodeBytes(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decodeBytes(encoded: String): ByteArray = Base64.decode(encoded, Base64.NO_WRAP)

    private class VoiceProfile(
        val centroid: FloatArray,
        val sampleEmbeddings: List<FloatArray>,
        val phraseTemplate: Array<FloatArray>,
    )

    companion object {
        /** File name inside app/src/main/assets (Sherpa-ONNX reads it through the AssetManager). */
        const val DEFAULT_MODEL_ASSET_PATH = "3dspeaker_speech_campplus_sv_zh_en_16k-common_advanced.onnx"
        private const val DEFAULT_NUM_THREADS = 2

        /** Reject an enrollment whose samples disagree (noise, different speakers, clipped audio). */
        private const val MIN_ENROLLMENT_CONSISTENCY = 0.70f

        private const val KEY_MODEL = "voice_profile_model"
        private const val KEY_CENTROID = "voice_profile_centroid"
        private const val KEY_SAMPLE_EMBEDDINGS = "voice_profile_sample_embeddings"
        private const val KEY_PHRASE_TEMPLATE = "voice_profile_phrase_template"
        private const val KEY_SAMPLE_COUNT = "voice_profile_sample_count"
    }
}

/** The CAM++ .onnx file is not in app/src/main/assets. */
class VoiceModelMissingException(
    modelAssetPath: String,
) : IllegalStateException("Speaker model not found in assets: $modelAssetPath")
