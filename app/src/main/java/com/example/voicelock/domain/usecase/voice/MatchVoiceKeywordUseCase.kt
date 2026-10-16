package com.example.voicelock.domain.usecase.voice

import com.example.voicelock.util.Constant
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.Normalizer
import java.util.Locale

/**
 * Stage 1: checks whether any SpeechRecognizer hypothesis contains one of the unlock keywords.
 * Text is lower-cased and stripped of diacritics ("Mở khoá", "mở khóa", "MO KHOA" all match
 * "mo khoa"), and the keyword must appear as whole words.
 */
class MatchVoiceKeywordUseCase {
    operator fun invoke(
        candidates: List<String>,
        keywords: List<String> = Constant.VOICE_UNLOCK_KEYWORDS,
    ): Flow<Result<Boolean>> =
        flow<Result<Boolean>> {
            val normalizedKeywords = keywords.map { " ${normalize(it)} " }
            val isMatch =
                candidates.any { candidate ->
                    val text = " ${normalize(candidate)} "
                    normalizedKeywords.any { keyword -> text.contains(keyword) }
                }
            emit(Result.Success(isMatch))
        }.flowOn(Dispatchers.Default)
            .catchAsResult()

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(DIACRITICS_REGEX, "")
            .replace('đ', 'd')
            .replace(NON_WORD_REGEX, " ")
            .trim()
            .replace(WHITESPACE_REGEX, " ")

    private companion object {
        val DIACRITICS_REGEX = Regex("\\p{Mn}+")
        val NON_WORD_REGEX = Regex("[^\\p{L}\\p{N}]+")
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
