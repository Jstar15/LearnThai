package com.example.learnthai.domain.speech

/** Result of aligning the target line against what the recogniser heard. */
data class TranscriptAlignment(
    /** Normalised target (no spaces/punctuation) — what the UI renders. */
    val target: String,
    /** Normalised transcript — what the UI renders as "you said". */
    val heard: String,
    /** For each character of [target]: true when the recogniser matched it. */
    val targetMatched: BooleanArray,
    /** For each character of [heard]: true when it corresponds to the target. */
    val heardMatched: BooleanArray,
    /** Normalised index -> index in the raw target string (used to map story tokens). */
    val targetSourceIndex: IntArray
)

/**
 * Compares a speech-recognition transcript with the Thai line the learner was meant to say.
 *
 * Deliberately text-only: no pitch or tone analysis. Tone work belongs to Speech Lab; a story
 * card asks the simpler question "did the words come out right?".
 */
object TranscriptScorer {

    /** Punctuation that the recogniser (or the story text) may add and that never changes meaning. */
    private const val PUNCTUATION = ".,!?…;:\"'()[]{}<>-–—“”‘’"

    /**
     * Keeps letters, digits, Thai vowels and tone marks; drops spaces and punctuation.
     *
     * Note: we must NOT use `isLetterOrDigit`, because Thai vowel and tone marks are combining
     * marks (Mn), not letters — stripping them would turn กิน into กน.
     */
    fun normalize(text: String): String =
        text.filterNot { it.isWhitespace() || PUNCTUATION.indexOf(it) >= 0 }

    /** 0–100 similarity between the target line and what was heard. */
    fun score(target: String, heard: String): Int {
        val a = normalize(target)
        val b = normalize(heard)
        if (a.isEmpty() || b.isEmpty()) return 0
        if (a == b) return 100

        val distance = levenshtein(a, b)
        val ratio = 1.0 - distance.toDouble() / maxOf(a.length, b.length)
        return (ratio * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Character-level alignment used to paint the diff in the UI.
     *
     * Returns, for both strings, which characters were matched. Unmatched characters on the
     * target side are what the learner missed; unmatched characters on the heard side are what
     * the recogniser heard instead.
     */
    fun align(targetRaw: String, heardRaw: String): TranscriptAlignment {
        val target = normalizeWithMap(targetRaw)
        val heard = normalizeWithMap(heardRaw)

        val rows = target.text.length
        val cols = heard.text.length
        val targetMatched = BooleanArray(rows)
        val heardMatched = BooleanArray(cols)

        if (rows == 0 || cols == 0) {
            return TranscriptAlignment(
                target = target.text,
                heard = heard.text,
                targetMatched = targetMatched,
                heardMatched = heardMatched,
                targetSourceIndex = target.sourceIndex
            )
        }

        // Backtracking matrix: 0 = match, 1 = substitute, 2 = delete (target char missing),
        // 3 = insert (extra char heard).
        val directions = Array(rows + 1) { ByteArray(cols + 1) }
        var previous = IntArray(cols + 1) { it }
        var current = IntArray(cols + 1)
        for (i in 1..rows) {
            directions[i][0] = 2
        }
        for (j in 1..cols) {
            directions[0][j] = 3
        }

        for (i in 1..rows) {
            current[0] = i
            for (j in 1..cols) {
                val same = target.text[i - 1] == heard.text[j - 1]
                val substitution = previous[j - 1] + if (same) 0 else 1
                val deletion = previous[j] + 1
                val insertion = current[j - 1] + 1

                var best = substitution
                var dir: Byte = if (same) 0 else 1
                if (deletion < best) {
                    best = deletion
                    dir = 2
                }
                if (insertion < best) {
                    best = insertion
                    dir = 3
                }
                current[j] = best
                directions[i][j] = dir
            }
            val swap = previous
            previous = current
            current = swap
        }

        var i = rows
        var j = cols
        while (i > 0 || j > 0) {
            when (directions[i][j].toInt()) {
                0 -> {
                    targetMatched[i - 1] = true
                    heardMatched[j - 1] = true
                    i--
                    j--
                }
                1 -> {
                    i--
                    j--
                }
                2 -> i--
                else -> j--
            }
        }

        return TranscriptAlignment(
            target = target.text,
            heard = heard.text,
            targetMatched = targetMatched,
            heardMatched = heardMatched,
            targetSourceIndex = target.sourceIndex
        )
    }

    /**
     * Story tokens the learner missed, so the UI can say "focus on: หอม, มาก".
     *
     * [tokens] are the card's Thai tokens (they concatenate to [targetRaw]); a token counts as
     * missed when any of its characters was not matched by the recogniser.
     */
    fun missedTokens(
        targetRaw: String,
        tokens: List<String>,
        alignment: TranscriptAlignment
    ): List<String> {
        if (tokens.isEmpty() || alignment.target.isEmpty()) return emptyList()

        val missed = mutableListOf<String>()
        var rawOffset = 0

        tokens.forEach { token ->
            val start = rawOffset
            val end = start + token.length
            rawOffset = end

            var anyMissed = false
            for (normalizedIndex in alignment.target.indices) {
                val source = alignment.targetSourceIndex[normalizedIndex]
                if (source in start until end && !alignment.targetMatched[normalizedIndex]) {
                    anyMissed = true
                    break
                }
            }
            if (anyMissed && token.isNotBlank()) missed.add(token)
        }
        return missed
    }

    private data class NormalizedText(val text: String, val sourceIndex: IntArray)

    private fun normalizeWithMap(raw: String): NormalizedText {
        val builder = StringBuilder(raw.length)
        val indices = ArrayList<Int>(raw.length)
        raw.forEachIndexed { index, char ->
            if (!char.isWhitespace() && PUNCTUATION.indexOf(char) < 0) {
                builder.append(char)
                indices.add(index)
            }
        }
        return NormalizedText(builder.toString(), indices.toIntArray())
    }

    private fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    substitution
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
