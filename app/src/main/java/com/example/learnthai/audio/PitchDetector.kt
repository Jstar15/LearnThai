package com.example.learnthai.audio

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.sqrt

data class PitchPoint(val timeMs: Float, val f0Hz: Float, val voiced: Boolean)

data class NormalizedPoint(val timeFraction: Float, val semitone: Float)

enum class ToneFeedback { GOOD, DIRECTION_MISMATCH, RANGE_TOO_SMALL, NOT_ENOUGH_VOICE }

enum class ToneLabel(val display: String) {
    MID("mid"),
    LOW("low"),
    FALLING("falling"),
    HIGH("high"),
    RISING("rising")
}

data class PitchComparison(
    val userPoints: List<NormalizedPoint>,
    val refPoints: List<NormalizedPoint>,
    val userDurationMs: Float,
    val refDurationMs: Float,
    val hint: String,
    val secondary: String,
    val feedback: ToneFeedback,
    val userToneLabel: ToneLabel,
    val refToneLabel: ToneLabel,
    val toneScore: Float
)

/**
 * Clean-room YIN fundamental-frequency (pitch) detector plus tone-contour scoring.
 * YIN algorithm: de Cheveigné & Kawahara (2002). No external dependencies.
 */
object PitchDetector {

    fun detect(
        samples: FloatArray,
        sampleRate: Int,
        frameSize: Int = 2048,
        hopSize: Int = 512,
        minF0: Float = 60f,
        maxF0: Float = 450f
    ): List<PitchPoint> {
        val result = ArrayList<PitchPoint>()
        if (samples.size < frameSize || sampleRate <= 0) return result
        val minTau = (sampleRate / maxF0).toInt().coerceAtLeast(2)
        val maxTau = (sampleRate / minF0).toInt().coerceAtMost(frameSize / 2 - 1)
        if (minTau >= maxTau) return result

        // First pass: f0 + frame energy.
        val f0s = ArrayList<Float>()
        val energies = ArrayList<Float>()
        var start = 0
        while (start + frameSize <= samples.size) {
            val frame = samples.copyOfRange(start, start + frameSize)
            f0s.add(yin(frame, sampleRate, minTau, maxTau))
            energies.add(rmsOf(frame))
            start += hopSize
        }
        if (f0s.isEmpty()) return result

        // Energy gate relative to the loudest frame — rejects silence/noise where YIN
        // would otherwise hallucinate a low pitch.
        val maxEnergy = energies.maxOrNull() ?: 0f
        val energyThreshold = maxEnergy * 0.06f
        val voiced = BooleanArray(f0s.size) { i -> f0s[i] > 0f && energies[i] > energyThreshold }

        // Trim to the first..last voiced frame so leading/trailing silence is dropped.
        val first = voiced.indexOfFirst { it }
        val last = voiced.indexOfLast { it }

        for (i in f0s.indices) {
            val isVoiced = first >= 0 && i in first..last && voiced[i]
            val timeMs = i * hopSize * 1000f / sampleRate
            result.add(PitchPoint(timeMs, if (isVoiced) f0s[i] else 0f, isVoiced))
        }
        return result
    }

    private fun rmsOf(frame: FloatArray): Float {
        var sum = 0f
        for (s in frame) sum += s * s
        return sqrt(sum / frame.size)
    }

    private fun yin(frame: FloatArray, fs: Int, minTau: Int, maxTau: Int, threshold: Float = 0.15f): Float {
        val n = frame.size
        val d = FloatArray(maxTau + 1)

        for (tau in 1..maxTau) {
            var sum = 0f
            val limit = n - tau
            var i = 0
            while (i < limit) {
                val delta = frame[i] - frame[i + tau]
                sum += delta * delta
                i++
            }
            d[tau] = sum
        }

        var running = 0f
        for (tau in 1..maxTau) {
            running += d[tau]
            d[tau] = if (running <= 1e-12f) 1f else d[tau] * tau / running
        }

        var tauEst = -1
        for (tau in minTau..maxTau) {
            if (d[tau] < threshold) {
                var t = tau
                while (t + 1 <= maxTau && d[t + 1] < d[t]) t++
                tauEst = t
                break
            }
        }
        if (tauEst == -1) return 0f

        if (tauEst > 1 && tauEst < maxTau) {
            val s0 = d[tauEst - 1]
            val s1 = d[tauEst]
            val s2 = d[tauEst + 1]
            val denom = s0 - 2f * s1 + s2
            if (abs(denom) > 1e-12f) {
                val shift = 0.5f * (s0 - s2) / denom
                if (abs(shift) < 1f) {
                    val better = tauEst + shift
                    if (better > 0f) return fs / better
                }
            }
        }
        return fs / tauEst.toFloat()
    }

    /** Compares a user recording against a reference recording and returns a pitch comparison. */
    fun compare(userSamples: FloatArray, userRate: Int, refSamples: FloatArray, refRate: Int): PitchComparison? {
        if (userSamples.isEmpty() || refSamples.isEmpty()) return null

        val userVoiced = detect(userSamples, userRate).filter { it.voiced }
        val refVoiced = detect(refSamples, refRate).filter { it.voiced }

        // Durations from the voiced span (speech only), so leading/trailing silence is ignored.
        val userDurationMs = if (userVoiced.size >= 2) {
            userVoiced.last().timeMs - userVoiced.first().timeMs
        } else {
            userSamples.size * 1000f / userRate
        }
        val refDurationMs = if (refVoiced.size >= 2) {
            refVoiced.last().timeMs - refVoiced.first().timeMs
        } else {
            refSamples.size * 1000f / refRate
        }

        if (userVoiced.size < 3 || refVoiced.size < 3) {
            return PitchComparison(
                userPoints = emptyList(),
                refPoints = emptyList(),
                userDurationMs = userDurationMs,
                refDurationMs = refDurationMs,
                hint = "Not enough voiced sound detected — try speaking clearly in a quiet room.",
                secondary = "",
                feedback = ToneFeedback.NOT_ENOUGH_VOICE,
                userToneLabel = ToneLabel.MID,
                refToneLabel = ToneLabel.MID,
                toneScore = 0f
            )
        }

        val userMedian = median(userVoiced.map { it.f0Hz })
        val refMedian = median(refVoiced.map { it.f0Hz })
        if (userMedian <= 0f || refMedian <= 0f) return null

        val userPoints = normalize(userVoiced, userMedian)
        val refPoints = normalize(refVoiced, refMedian)

        val userNet = userPoints.last().semitone - userPoints.first().semitone
        val refNet = refPoints.last().semitone - refPoints.first().semitone
        val refDir = direction(refNet)
        val userDir = direction(userNet)

        val feedback: ToneFeedback
        val hint: String
        when {
            refDir != userDir -> {
                feedback = ToneFeedback.DIRECTION_MISMATCH
                hint = "Tone direction differs — the reference $refDir, but you $userDir."
            }
            refDir == "flat" -> {
                feedback = ToneFeedback.GOOD
                hint = "Both stay flat (level tone) — good."
            }
            abs(userNet) < 0.5f * abs(refNet) -> {
                feedback = ToneFeedback.RANGE_TOO_SMALL
                hint = "Right direction, but your pitch moves less — exaggerate the ${if (refNet < 0f) "fall" else "rise"}."
            }
            else -> {
                feedback = ToneFeedback.GOOD
                hint = "Good — your tone contour matches the reference direction and range."
            }
        }

        val offset = 12f * log2(userMedian / refMedian)
        val secondary = "Voice pitch: you sit ${abs(offset).toInt()} semitone(s) " +
            "${if (offset >= 0f) "above" else "below"} the reference (natural voice pitch, not a tone error)."

        val userToneLabel = classifyTone(userPoints)
        val refToneLabel = classifyTone(refPoints)
        val toneScore = toneScore(userPoints, refPoints)

        return PitchComparison(
            userPoints, refPoints, userDurationMs, refDurationMs, hint, secondary, feedback,
            userToneLabel, refToneLabel, toneScore
        )
    }

    /** Classifies a normalized contour into an approximate Thai tone by net change + range. */
    fun classifyTone(points: List<NormalizedPoint>): ToneLabel {
        if (points.size < 2) return ToneLabel.MID
        val net = points.last().semitone - points.first().semitone
        return when {
            net < -2.0f -> ToneLabel.FALLING
            net > 2.0f -> ToneLabel.RISING
            net < -0.8f -> ToneLabel.LOW
            net > 0.8f -> ToneLabel.HIGH
            else -> ToneLabel.MID
        }
    }

    /** Scores contour closeness (0..1) via RMSE of semitone curves on a common grid. */
    fun toneScore(user: List<NormalizedPoint>, ref: List<NormalizedPoint>): Float {
        if (user.isEmpty() || ref.isEmpty()) return 0f
        val n = 32
        val u = resample(user, n)
        val r = resample(ref, n)
        var sumSq = 0f
        for (i in 0 until n) {
            val d = u[i] - r[i]
            sumSq += d * d
        }
        val rmse = sqrt(sumSq / n)
        return (1f - rmse / 2f).coerceIn(0f, 1f)
    }

    /** Scores how well a recognized string matches the target (0..1) via Levenshtein similarity. */
    fun wordScore(recognized: String?, target: String): Float {
        val a = (recognized ?: "").trim()
        val b = target.trim()
        if (a.isEmpty()) return 0f
        if (a == b) return 1f
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 0f
        return (1f - levenshtein(a, b).toFloat() / maxLen).coerceIn(0f, 1f)
    }

    /** Parses a known tone from a pronunciation guide string (e.g. "gaa (Mid tone)"). */
    fun parseKnownTone(guide: String): ToneLabel? = when {
        guide.contains("Falling tone") -> ToneLabel.FALLING
        guide.contains("Rising tone") -> ToneLabel.RISING
        guide.contains("High tone") -> ToneLabel.HIGH
        guide.contains("Low tone") -> ToneLabel.LOW
        guide.contains("Mid tone") -> ToneLabel.MID
        else -> null
    }

    private fun resample(points: List<NormalizedPoint>, n: Int): FloatArray {
        val out = FloatArray(n)
        if (points.isEmpty()) return out
        for (i in 0 until n) {
            val t = i.toFloat() / (n - 1)
            var j = 0
            while (j < points.size - 1 && points[j + 1].timeFraction < t) j++
            val p0 = points[j]
            val p1 = points.getOrElse(j + 1) { p0 }
            val span = p1.timeFraction - p0.timeFraction
            val frac = if (span <= 0f) 0f else ((t - p0.timeFraction) / span).coerceIn(0f, 1f)
            out[i] = p0.semitone + (p1.semitone - p0.semitone) * frac
        }
        return out
    }

    private fun levenshtein(a: String, b: String): Int {
        val n = a.length
        val m = b.length
        if (n == 0) return m
        if (m == 0) return n
        var prev = IntArray(m + 1) { it }
        var curr = IntArray(m + 1)
        for (i in 1..n) {
            curr[0] = i
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[m]
    }

    private fun normalize(voiced: List<PitchPoint>, medianHz: Float): List<NormalizedPoint> {
        if (voiced.isEmpty()) return emptyList()
        val t0 = voiced.first().timeMs
        val t1 = voiced.last().timeMs
        val span = (t1 - t0).coerceAtLeast(1f)
        return voiced.map { NormalizedPoint((it.timeMs - t0) / span, 12f * log2(it.f0Hz / medianHz)) }
    }

    private fun direction(netSemitones: Float): String = when {
        netSemitones > 1.2f -> "rises"
        netSemitones < -1.2f -> "falls"
        else -> "flat"
    }

    private fun median(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2f
    }
}
