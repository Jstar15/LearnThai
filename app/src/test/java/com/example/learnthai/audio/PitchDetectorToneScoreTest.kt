package com.example.learnthai.audio

import org.junit.Assert.assertTrue
import org.junit.Test

class PitchDetectorToneScoreTest {

    @Test
    fun testExactFallingToneContourScoresHigh() {
        // Falling tone contour: starts at +3.0 semitones and drops to -3.0 semitones
        val ref = (0..10).map { i: Int ->
            val frac = i / 10f
            NormalizedPoint(frac, 3.0f - 6.0f * frac)
        }
        val user = (0..10).map { i: Int ->
            val frac = i / 10f
            NormalizedPoint(frac, 3.0f - 6.0f * frac)
        }

        val score = PitchDetector.toneScore(user, ref)
        assertTrue("Exact match should score >= 0.95, got $score", score >= 0.95f)
    }

    @Test
    fun testTimeShiftedContourDoesNotCollapseToZero() {
        // Reference falling tone: falls steadily from 0 to 1
        val ref = (0..20).map { i: Int ->
            val frac = i / 20f
            NormalizedPoint(frac, 3.0f - 6.0f * frac)
        }
        // User starts fall slightly later (delayed onset by ~15% time)
        val user = (0..20).map { i: Int ->
            val frac = i / 20f
            val semitone = if (frac < 0.2f) 3.0f else 3.0f - 6.0f * ((frac - 0.2f) / 0.8f)
            NormalizedPoint(frac, semitone)
        }

        val score = PitchDetector.toneScore(user, ref)
        assertTrue("Time-shifted contour should score comfortably high (>= 0.75), got $score", score >= 0.75f)
    }

    @Test
    fun testOppositeDirectionScoresLow() {
        // Reference is falling: +3 to -3
        val ref = (0..10).map { i: Int ->
            val frac = i / 10f
            NormalizedPoint(frac, 3.0f - 6.0f * frac)
        }
        // User is rising: -3 to +3
        val user = (0..10).map { i: Int ->
            val frac = i / 10f
            NormalizedPoint(frac, -3.0f + 6.0f * frac)
        }

        val score = PitchDetector.toneScore(user, ref)
        assertTrue("Opposite tone direction should score low (< 0.45), got $score", score < 0.45f)
    }

    @Test
    fun testLevelMidToneScoresHigh() {
        // Both mid tones: flat around 0 semitones with minor natural jitter
        val ref = (0..10).map { i: Int ->
            NormalizedPoint(i / 10f, 0.1f)
        }
        val user = (0..10).map { i: Int ->
            NormalizedPoint(i / 10f, -0.1f)
        }

        val score = PitchDetector.toneScore(user, ref)
        assertTrue("Level tone match should score >= 0.85, got $score", score >= 0.85f)
    }
}
