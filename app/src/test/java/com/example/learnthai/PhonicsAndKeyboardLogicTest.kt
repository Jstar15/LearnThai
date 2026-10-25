package com.example.learnthai.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.learnthai.domain.tone.ConsonantClass
import com.example.learnthai.domain.tone.ToneRuleEvaluator
import com.example.learnthai.domain.tone.ToneType
import com.example.learnthai.ui.components.keyboard.KedmaneeLayout

class PhonicsAndKeyboardLogicTest {

    @Test
    fun testKedmaneeNumberRowConsonantsAreUnshifted() {
        // High frequency consonants on Kedmanee number row must be unshifted (not requiring Shift)
        val numberRowConsonants = listOf('ภ', 'ถ', 'ุ', 'ึ', 'ค', 'ต', 'จ', 'ข', 'ช')
        for (char in numberRowConsonants) {
            val isShifted = KedmaneeLayout.isShiftedChar(char)
            assertEquals("Char '$char' should be on the normal unshifted layer", false, isShifted)
        }
    }

    @Test
    fun testKedmaneeNumeralsAreShifted() {
        // Thai numerals ๑..๙ should be on the shift layer
        val thaiNumerals = listOf('๑', '๒', '๓', '๔', '๕', '๖', '๗', '๘', '๙', '๐')
        for (char in thaiNumerals) {
            val isShifted = KedmaneeLayout.isShiftedChar(char)
            assertEquals("Numeral '$char' should be on the shifted layer", true, isShifted)
        }
    }

    @Test
    fun testToneRuleEvaluatorMidClass() {
        // กา (gaa) = Mid Class + Live Syllable = Mid Tone
        val analysis = ToneRuleEvaluator.analyzeWord("กา", "Mid")
        assertEquals(ConsonantClass.MID, analysis.consonantClass)
        assertEquals(ToneType.MID, analysis.calculatedTone)
    }

    @Test
    fun testToneRuleEvaluatorToneMarkLowClass() {
        // ที่ (thee) = Low Class + Mai Ek = Falling Tone
        val analysis = ToneRuleEvaluator.analyzeWord("ที่", "Low")
        assertEquals(ConsonantClass.LOW, analysis.consonantClass)
        assertEquals(ToneType.FALLING, analysis.calculatedTone)
    }

    @Test
    fun testToneRuleEvaluatorHighClassRising() {
        // เขา (khao) = High Class + Live Syllable = Natural Rising Tone
        val analysis = ToneRuleEvaluator.analyzeWord("เขา", "High")
        assertEquals(ConsonantClass.HIGH, analysis.consonantClass)
        assertEquals(ToneType.RISING, analysis.calculatedTone)
    }
}
