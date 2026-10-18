package com.example.voicelock.util

import java.text.Normalizer
import java.util.Locale

private val COMBINING_MARKS = Regex("\\p{Mn}+")
private val WHITESPACE = Regex("\\s+")

/**
 * Canonical form of a free-text answer, so "Hà Nội", "ha noi" and " HA  NOI " are the same answer:
 * Vietnamese/Latin diacritics removed (đ -> d), lower-cased, whitespace collapsed and trimmed.
 */
fun String.normalizeForComparison(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .replace('đ', 'd')
        .replace('Đ', 'D')
        .lowercase(Locale.ROOT)
        .replace(WHITESPACE, " ")
        .trim()
