package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.util.Result
import com.example.voicelock.util.resultFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

/**
 * Stage 1: does any SpeechRecognizer hypothesis say one of the expected phrases?
 *
 * Expected phrases are normally what the recognizer transcribed from the owner's own enrollment
 * samples, so the language is whatever the owner enrolled in. Matching is tolerant on purpose:
 * - case-insensitive; accents are dropped for Latin script only ("Mở khoá" == "mo khoa"), while
 *   marks that carry meaning in other scripts (Devanagari matras, Thai vowels, ...) are kept;
 * - the phrase may appear as whole words inside a longer hypothesis ("ừ mở khóa đi");
 * - small recognizer slips are accepted: character similarity >= [MIN_SIMILARITY] with spaces
 *   ignored, which also covers scripts written without spaces (Chinese, Japanese, Thai).
 */
class MatchVoiceKeywordUseCase {
    operator fun invoke(
        candidates: List<String>,
        expectedPhrases: List<String>,
    ): Flow<Result<Boolean>> =
        resultFlow(Dispatchers.Default) {
            val expected = expectedPhrases.map(::normalize).filter { it.isNotEmpty() }
            val isMatch =
                candidates.map(::normalize).filter { it.isNotEmpty() }.any { candidate ->
                    expected.any { phrase -> isPhraseMatch(candidate, phrase) }
                }
            isMatch
        }

    private fun isPhraseMatch(
        candidate: String,
        phrase: String,
    ): Boolean {
        if (" $candidate ".contains(" $phrase ")) return true
        return similarity(candidate.withoutSpaces(), phrase.withoutSpaces()) >= MIN_SIMILARITY
    }

    private fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        val builder = StringBuilder(decomposed.length)
        var previousBaseIsLatin = false
        decomposed.forEach { char ->
            if (Character.getType(char) == Character.NON_SPACING_MARK.toInt()) {
                if (!previousBaseIsLatin) builder.append(char)
            } else {
                previousBaseIsLatin = Character.UnicodeScript.of(char.code) == Character.UnicodeScript.LATIN
                builder.append(if (char == 'đ') 'd' else char)
            }
        }
        return Normalizer.normalize(builder, Normalizer.Form.NFC)
            .replace(NON_WORD_REGEX, " ")
            .trim()
            .replace(WHITESPACE_REGEX, " ")
    }

    private fun String.withoutSpaces(): String = replace(" ", "")

    /** 1 - Levenshtein distance / longer length, over Unicode code points. */
    private fun similarity(
        a: String,
        b: String,
    ): Float {
        val x = a.codePoints().toArray()
        val y = b.codePoints().toArray()
        if (x.isEmpty() || y.isEmpty()) return 0f
        var previous = IntArray(y.size + 1) { it }
        var current = IntArray(y.size + 1)
        for (i in 1..x.size) {
            current[0] = i
            for (j in 1..y.size) {
                val substitution = previous[j - 1] + if (x[i - 1] == y[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, substitution)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return 1f - previous[y.size].toFloat() / max(x.size, y.size)
    }

    private companion object {
        const val MIN_SIMILARITY = 0.8f

        // Letters, digits and combining marks (Mn/Mc) survive; everything else becomes a space.
        val NON_WORD_REGEX = Regex("[^\\p{L}\\p{N}\\p{M}]+")
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
