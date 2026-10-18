package com.example.voicelock.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun vietnameseDiacriticsAndCase_areIgnored() {
        assertEquals("ha noi", "Hà Nội".normalizeForComparison())
        assertEquals("ha noi", "HA NOI".normalizeForComparison())
        assertEquals("ha noi", "hà nội".normalizeForComparison())
    }

    @Test
    fun dStroke_becomesD() {
        assertEquals("da nang", "Đà Nẵng".normalizeForComparison())
        assertEquals("dau", "đậu".normalizeForComparison())
    }

    @Test
    fun whitespace_isCollapsedAndTrimmed() {
        assertEquals("ho chi minh", "  Hồ   Chí\tMinh  ".normalizeForComparison())
    }

    @Test
    fun differentWords_stayDifferent() {
        assertNotEquals("Hà Nội".normalizeForComparison(), "Hải Phòng".normalizeForComparison())
    }

    @Test
    fun blank_becomesEmpty() {
        assertEquals("", "   ".normalizeForComparison())
    }
}
