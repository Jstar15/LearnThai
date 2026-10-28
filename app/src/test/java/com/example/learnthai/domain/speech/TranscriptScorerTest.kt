package com.example.learnthai.domain.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptScorerTest {

    @Test
    fun exactMatchIsFullMarks() {
        assertEquals(100, TranscriptScorer.score("สวัสดีครับ", "สวัสดีครับ"))
    }

    @Test
    fun whitespaceFromRecogniserIsIgnored() {
        assertEquals(
            100,
            TranscriptScorer.score("สวัสดีดองกี้", "สวัสดี ดองกี้ ")
        )
    }

    @Test
    fun punctuationIsIgnored() {
        assertEquals(100, TranscriptScorer.score("เปิดประตู!", "เปิดประตู"))
    }

    @Test
    fun toneAndVowelMarksAreKept() {
        // If marks were stripped these would normalise to the same string.
        val withMarks = TranscriptScorer.normalize("กิน")
        val stripped = TranscriptScorer.normalize("กน")
        assertTrue(withMarks != stripped)
        assertTrue(withMarks.length > stripped.length)
    }

    @Test
    fun oneWrongCharacterScoresHighButNotPerfect() {
        val score = TranscriptScorer.score("ผมอยากกินผัดไทย", "ผมอยากกินผัดไท")
        assertTrue("expected a high partial score, got $score", score in 80..99)
    }

    @Test
    fun completelyDifferentLineScoresLow() {
        val score = TranscriptScorer.score("สวัสดีครับ", "ขอบคุณมาก")
        assertTrue("expected a low score, got $score", score < 40)
    }

    @Test
    fun emptyInputsScoreZero() {
        assertEquals(0, TranscriptScorer.score("", "สวัสดี"))
        assertEquals(0, TranscriptScorer.score("สวัสดี", ""))
        assertEquals(0, TranscriptScorer.score("", ""))
    }

    @Test
    fun scoreNeverExceedsBounds() {
        val score = TranscriptScorer.score("ไป", "ไปไปไปไป")
        assertTrue(score in 0..100)
    }

    @Test
    fun alignmentMarksEveryCharacterWhenIdentical() {
        val alignment = TranscriptScorer.align("สวัสดี", "สวัสดี")
        assertTrue(alignment.targetMatched.all { it })
        assertTrue(alignment.heardMatched.all { it })
    }

    @Test
    fun alignmentMarksSubstitutionOnBothSides() {
        val alignment = TranscriptScorer.align("ผัดไทย", "ผัดไท")
        assertFalse(alignment.targetMatched.last())
        assertTrue(alignment.heardMatched.all { it })
    }

    @Test
    fun alignmentMarksMissingCharactersOnTheTargetSide() {
        val alignment = TranscriptScorer.align("สวัสดีครับ", "สวัสดี")
        val matchedCount = alignment.targetMatched.count { it }
        assertEquals(6, matchedCount)
        assertTrue(alignment.targetMatched.takeLast(3).none { it })
    }

    @Test
    fun missedTokensFindsTheUnmatchedWord() {
        val target = "หอมมากผมหิวแล้ว"
        val tokens = listOf("หอม", "มาก", "ผม", "หิว", "แล้ว")
        val alignment = TranscriptScorer.align(target, "ผมหิวแล้ว")

        val missed = TranscriptScorer.missedTokens(target, tokens, alignment)
        assertEquals(listOf("หอม", "มาก"), missed)
    }

    @Test
    fun missedTokensIsEmptyWhenEverythingMatches() {
        val target = "เปิดประตู"
        val tokens = listOf("เปิด", "ประตู")
        val alignment = TranscriptScorer.align(target, "เปิดประตู")
        assertTrue(TranscriptScorer.missedTokens(target, tokens, alignment).isEmpty())
    }
}
