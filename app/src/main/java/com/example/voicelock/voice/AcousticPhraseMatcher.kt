package com.example.voicelock.voice

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure-Kotlin, language-independent phrase matcher (no native library, no model file).
 *
 * Features are MFCCs: log mel sub-band energies (26 bands, 60 Hz – 7.6 kHz) decorrelated with a
 * DCT, with c0 dropped so loudness does not matter, and cepstral mean normalization so the
 * microphone/channel colour cancels out. Two utterances are compared with Dynamic Time Warping
 * constrained to a Sakoe-Chiba band, which tolerates speaking faster or slower but not a
 * different sequence of sounds — i.e. it checks *what* was said, independently of the
 * speech recognizer, while Sherpa-ONNX checks *who* said it.
 *
 * All functions are stateless and thread-safe; call them from Dispatchers.Default.
 */
object AcousticPhraseMatcher {

    private const val SAMPLE_RATE = AudioRecorderHelper.SAMPLE_RATE
    private const val FRAME_SIZE = 400 // 25 ms
    private const val HOP_SIZE = 160 // 10 ms
    private const val FFT_SIZE = 512
    private const val NUM_BINS = FFT_SIZE / 2 + 1
    private const val NUM_MEL_BANDS = 26
    private const val NUM_CEPSTRA = 13 // c0..c12, c0 is dropped -> 12-dim features
    private const val PRE_EMPHASIS = 0.97f
    private const val MIN_FREQ_HZ = 60.0
    private const val MAX_FREQ_HZ = 7_600.0
    private const val LOG_FLOOR = 1e-10f

    // Energy-based voice activity detection used to trim leading/trailing silence.
    private const val VAD_RELATIVE_DB = 35f
    private const val VAD_MIN_PEAK_DB = -45f
    private const val VAD_PADDING_FRAMES = 10
    private const val MIN_SPEECH_FRAMES = 25 // 250 ms of voiced frames

    // DTW constraints.
    private const val BAND_RATIO = 0.2f
    private const val MAX_LENGTH_RATIO = 2.5f

    private const val SERIALIZATION_VERSION = 1

    private val hammingWindow: FloatArray by lazy {
        FloatArray(FRAME_SIZE) { i -> (0.54 - 0.46 * cos(2.0 * PI * i / (FRAME_SIZE - 1))).toFloat() }
    }
    private val melFilterBank: Array<FloatArray> by lazy { buildMelFilterBank() }
    private val dctMatrix: Array<FloatArray> by lazy { buildDctMatrix() }
    private val fft: Fft by lazy { Fft(FFT_SIZE) }

    /**
     * Returns the samples with leading/trailing silence removed (plus 100 ms padding),
     * or null when the clip does not contain enough speech to be usable.
     */
    fun trimSilence(samples: FloatArray): FloatArray? {
        val frames = frameCount(samples.size)
        if (frames == 0) return null
        val energiesDb = FloatArray(frames) { f -> frameRmsDb(samples, f * HOP_SIZE) }
        val peakDb = energiesDb.max()
        if (peakDb < VAD_MIN_PEAK_DB) return null

        val threshold = peakDb - VAD_RELATIVE_DB
        if (energiesDb.count { it >= threshold } < MIN_SPEECH_FRAMES) return null
        val first = energiesDb.indexOfFirst { it >= threshold }
        val last = energiesDb.indexOfLast { it >= threshold }
        val start = max(0, (first - VAD_PADDING_FRAMES) * HOP_SIZE)
        val end = min(samples.size, (last + VAD_PADDING_FRAMES) * HOP_SIZE + FRAME_SIZE)
        return samples.copyOfRange(start, end)
    }

    fun hasSpeech(samples: FloatArray): Boolean = trimSilence(samples) != null

    /**
     * Extracts a [frames x 12] MFCC matrix. Pass audio already trimmed by [trimSilence]
     * so the DTW alignment is not dominated by silence.
     */
    fun extractFeatures(samples: FloatArray): Array<FloatArray> {
        val frames = frameCount(samples.size)
        require(frames > 0) { "Audio too short: ${samples.size} samples" }

        val emphasized = FloatArray(samples.size)
        emphasized[0] = samples[0]
        for (i in 1 until samples.size) emphasized[i] = samples[i] - PRE_EMPHASIS * samples[i - 1]

        val re = FloatArray(FFT_SIZE)
        val im = FloatArray(FFT_SIZE)
        val power = FloatArray(NUM_BINS)
        val logMel = FloatArray(NUM_MEL_BANDS)
        val window = hammingWindow
        val filters = melFilterBank
        val dct = dctMatrix

        val features =
            Array(frames) { f ->
                re.fill(0f)
                im.fill(0f)
                val offset = f * HOP_SIZE
                for (i in 0 until FRAME_SIZE) re[i] = emphasized[offset + i] * window[i]
                fft.transform(re, im)
                for (k in 0 until NUM_BINS) power[k] = re[k] * re[k] + im[k] * im[k]

                for (m in 0 until NUM_MEL_BANDS) {
                    val filter = filters[m]
                    var energy = 0f
                    for (k in 0 until NUM_BINS) energy += filter[k] * power[k]
                    logMel[m] = ln(max(energy, LOG_FLOOR))
                }

                FloatArray(NUM_CEPSTRA - 1) { c ->
                    val row = dct[c + 1]
                    var sum = 0f
                    for (m in 0 until NUM_MEL_BANDS) sum += row[m] * logMel[m]
                    sum
                }
            }
        applyCepstralMeanNormalization(features)
        return features
    }

    /**
     * DTW similarity in [-1, 1] (1 = identical). Frame cost is cosine distance; diagonal steps are
     * weighted 2 so the accumulated cost normalized by (n + m) is a path-length-independent average.
     * Returns 0 when the utterance lengths differ too much to be the same phrase.
     */
    fun dtwSimilarity(
        query: Array<FloatArray>,
        template: Array<FloatArray>,
    ): Float {
        val n = query.size
        val m = template.size
        if (n == 0 || m == 0) return 0f
        if (max(n, m).toFloat() / min(n, m) > MAX_LENGTH_RATIO) return 0f

        val a = query.map { it.unitNormalized() }
        val b = template.map { it.unitNormalized() }
        val band = maxOf(abs(n - m), (max(n, m) * BAND_RATIO).toInt(), 2)

        var prev = FloatArray(m + 1) { Float.POSITIVE_INFINITY }
        var curr = FloatArray(m + 1)
        prev[0] = 0f
        for (i in 1..n) {
            curr.fill(Float.POSITIVE_INFINITY)
            val center = (i.toLong() * m / n).toInt()
            val jStart = max(1, center - band)
            val jEnd = min(m, center + band)
            for (j in jStart..jEnd) {
                val cost = 1f - dot(a[i - 1], b[j - 1])
                curr[j] = minOf(prev[j] + cost, curr[j - 1] + cost, prev[j - 1] + 2f * cost)
            }
            val swap = prev
            prev = curr
            curr = swap
        }

        val total = prev[m]
        if (total.isInfinite()) return 0f
        return (1f - total / (n + m)).coerceIn(-1f, 1f)
    }

    fun serialize(features: Array<FloatArray>): ByteArray {
        val dims = features.firstOrNull()?.size ?: 0
        val buffer =
            ByteBuffer.allocate(3 * Int.SIZE_BYTES + features.size * dims * Float.SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(SERIALIZATION_VERSION).putInt(features.size).putInt(dims)
        features.forEach { frame -> frame.forEach(buffer::putFloat) }
        return buffer.array()
    }

    fun deserialize(bytes: ByteArray): Array<FloatArray> {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(buffer.getInt() == SERIALIZATION_VERSION) { "Unsupported phrase template version" }
        val frames = buffer.getInt()
        val dims = buffer.getInt()
        require(frames >= 0 && dims >= 0 && buffer.remaining() == frames * dims * Float.SIZE_BYTES) {
            "Corrupted phrase template"
        }
        return Array(frames) { FloatArray(dims) { buffer.getFloat() } }
    }

    private fun frameCount(sampleCount: Int): Int =
        if (sampleCount < FRAME_SIZE) 0 else 1 + (sampleCount - FRAME_SIZE) / HOP_SIZE

    private fun frameRmsDb(
        samples: FloatArray,
        offset: Int,
    ): Float {
        var sum = 0.0
        for (i in offset until offset + FRAME_SIZE) sum += samples[i] * samples[i]
        val rms = sqrt(sum / FRAME_SIZE)
        return (20.0 * log10(max(rms, 1e-9))).toFloat()
    }

    private fun applyCepstralMeanNormalization(features: Array<FloatArray>) {
        val dims = features[0].size
        val mean = FloatArray(dims)
        features.forEach { frame -> for (d in 0 until dims) mean[d] += frame[d] }
        for (d in 0 until dims) mean[d] /= features.size
        features.forEach { frame -> for (d in 0 until dims) frame[d] -= mean[d] }
    }

    private fun FloatArray.unitNormalized(): FloatArray {
        val norm = sqrt(dot(this, this))
        return if (norm < 1e-6f) FloatArray(size) else FloatArray(size) { this[it] / norm }
    }

    private fun dot(
        a: FloatArray,
        b: FloatArray,
    ): Float {
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }

    private fun hzToMel(hz: Double): Double = 2595.0 * log10(1.0 + hz / 700.0)

    private fun melToHz(mel: Double): Double = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)

    private fun buildMelFilterBank(): Array<FloatArray> {
        val minMel = hzToMel(MIN_FREQ_HZ)
        val maxMel = hzToMel(MAX_FREQ_HZ)
        val edgesHz =
            DoubleArray(NUM_MEL_BANDS + 2) { i -> melToHz(minMel + i * (maxMel - minMel) / (NUM_MEL_BANDS + 1)) }
        val binHz = SAMPLE_RATE.toDouble() / FFT_SIZE

        return Array(NUM_MEL_BANDS) { m ->
            val left = edgesHz[m]
            val center = edgesHz[m + 1]
            val right = edgesHz[m + 2]
            FloatArray(NUM_BINS) { k ->
                val hz = k * binHz
                when {
                    hz <= left || hz >= right -> 0f
                    hz <= center -> ((hz - left) / (center - left)).toFloat()
                    else -> ((right - hz) / (right - center)).toFloat()
                }
            }
        }
    }

    private fun buildDctMatrix(): Array<FloatArray> {
        val scale = sqrt(2.0 / NUM_MEL_BANDS)
        return Array(NUM_CEPSTRA) { c ->
            FloatArray(NUM_MEL_BANDS) { m -> (scale * cos(PI * c * (m + 0.5) / NUM_MEL_BANDS)).toFloat() }
        }
    }

    /** Iterative in-place radix-2 complex FFT with precomputed twiddles. Size must be a power of 2. */
    private class Fft(
        private val size: Int,
    ) {
        private val bitReversed = IntArray(size)
        private val cosTable = FloatArray(size / 2) { i -> cos(2.0 * PI * i / size).toFloat() }
        private val sinTable = FloatArray(size / 2) { i -> sin(2.0 * PI * i / size).toFloat() }

        init {
            require(size > 0 && size and (size - 1) == 0) { "FFT size must be a power of 2" }
            val bits = Integer.numberOfTrailingZeros(size)
            for (i in 0 until size) bitReversed[i] = Integer.reverse(i) ushr (32 - bits)
        }

        fun transform(
            re: FloatArray,
            im: FloatArray,
        ) {
            for (i in 0 until size) {
                val j = bitReversed[i]
                if (j > i) {
                    var tmp = re[i]
                    re[i] = re[j]
                    re[j] = tmp
                    tmp = im[i]
                    im[i] = im[j]
                    im[j] = tmp
                }
            }
            var length = 2
            while (length <= size) {
                val half = length / 2
                val step = size / length
                for (start in 0 until size step length) {
                    for (k in 0 until half) {
                        val wr = cosTable[k * step]
                        val wi = -sinTable[k * step]
                        val a = start + k
                        val b = a + half
                        val tr = re[b] * wr - im[b] * wi
                        val ti = re[b] * wi + im[b] * wr
                        re[b] = re[a] - tr
                        im[b] = im[a] - ti
                        re[a] += tr
                        im[a] += ti
                    }
                }
                length *= 2
            }
        }
    }
}
