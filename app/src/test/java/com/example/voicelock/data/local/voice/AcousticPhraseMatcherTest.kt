package com.example.voicelock.data.local.voice

import com.example.voicelock.domain.usecase.voice.MatchVoiceKeywordUseCase
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class AcousticPhraseMatcherTest {

    private val sampleRate = AudioRecorderHelper.SAMPLE_RATE

    /** Two "syllables" with different pitch, surrounded by silence. */
    private fun syntheticPhrase(
        firstHz: Double,
        secondHz: Double,
        stretch: Double = 1.0,
    ): FloatArray {
        val silence = FloatArray(sampleRate / 4)
        val syllable = { hz: Double ->
            val length = (sampleRate * 0.4 * stretch).toInt()
            FloatArray(length) { i ->
                val t = i.toDouble() / sampleRate
                (0.3 * sin(2 * PI * hz * t) + 0.15 * sin(2 * PI * 2.7 * hz * t)).toFloat()
            }
        }
        return silence + syllable(firstHz) + syllable(secondHz) + silence
    }

    @Test
    fun trimSilence_returnsNullForSilenceAndNoise() {
        assertNull(AcousticPhraseMatcher.trimSilence(FloatArray(sampleRate * 2)))
        val random = Random(1)
        assertNull(AcousticPhraseMatcher.trimSilence(FloatArray(sampleRate) { (random.nextFloat() - 0.5f) * 1e-4f }))
    }

    @Test
    fun trimSilence_removesLeadingAndTrailingSilence() {
        val phrase = syntheticPhrase(200.0, 400.0)
        val trimmed = AcousticPhraseMatcher.trimSilence(phrase)!!
        assertTrue(trimmed.size < phrase.size)
        assertTrue(trimmed.size >= sampleRate * 0.8)
    }

    @Test
    fun dtw_samePhraseScoresHigherThanDifferentPhrase() {
        val template = AcousticPhraseMatcher.extractFeatures(AcousticPhraseMatcher.trimSilence(syntheticPhrase(200.0, 400.0))!!)
        val sameSlower = AcousticPhraseMatcher.extractFeatures(AcousticPhraseMatcher.trimSilence(syntheticPhrase(200.0, 400.0, 1.3))!!)
        val different = AcousticPhraseMatcher.extractFeatures(AcousticPhraseMatcher.trimSilence(syntheticPhrase(400.0, 200.0))!!)

        assertEquals(1f, AcousticPhraseMatcher.dtwSimilarity(template, template), 1e-4f)
        val same = AcousticPhraseMatcher.dtwSimilarity(sameSlower, template)
        val other = AcousticPhraseMatcher.dtwSimilarity(different, template)
        assertTrue("same=$same other=$other", same > other)
    }

    @Test
    fun serialization_roundTrips() {
        val features = AcousticPhraseMatcher.extractFeatures(syntheticPhrase(250.0, 500.0))
        val restored = AcousticPhraseMatcher.deserialize(AcousticPhraseMatcher.serialize(features))
        assertEquals(features.size, restored.size)
        features.indices.forEach { assertArrayEquals(features[it], restored[it], 0f) }
    }

    @Test
    fun keyword_matchesIgnoringCaseAndDiacritics() =
        runBlocking {
            val useCase = MatchVoiceKeywordUseCase()
            val keywords = listOf("unlock", "mở khóa")
            assertEquals(Result.Success(true), useCase(listOf("Mở khoá"), keywords).first())
            assertEquals(Result.Success(true), useCase(listOf("xin chào", "MO KHOA đi"), keywords).first())
            assertEquals(Result.Success(true), useCase(listOf("Unlock!"), keywords).first())
            assertEquals(Result.Success(false), useCase(listOf("unlocked", "mở cửa"), keywords).first())
        }

    @Test
    fun keyword_toleratesSmallRecognizerSlips() =
        runBlocking {
            val useCase = MatchVoiceKeywordUseCase()
            assertEquals(Result.Success(true), useCase(listOf("mở hóa"), listOf("mở khóa")).first())
            assertEquals(Result.Success(false), useCase(listOf("khóa"), listOf("mở khóa")).first())
            assertEquals(Result.Success(false), useCase(listOf("kon", "call"), listOf("mở khóa")).first())
        }

    @Test
    fun keyword_keepsMarksOfNonLatinScripts() =
        runBlocking {
            val useCase = MatchVoiceKeywordUseCase()
            // Devanagari vowel signs are meaningful: नमस्ते must not collapse into नमसत.
            assertEquals(Result.Success(true), useCase(listOf("नमस्ते"), listOf("नमस्ते")).first())
            assertEquals(Result.Success(false), useCase(listOf("नमक"), listOf("नमस्ते")).first())
            // Scripts written without spaces still match inside a longer hypothesis.
            assertEquals(Result.Success(true), useCase(listOf("ロック解除"), listOf("ロック解除")).first())
        }
}
