package com.example.learnthai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnthai.audio.NormalizedPoint
import com.example.learnthai.audio.PitchComparison
import com.example.learnthai.audio.ToneFeedback
import kotlin.math.roundToInt

@Composable
fun PitchVisualizer(comparison: PitchComparison, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "Pitch contour — tone shape",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend(Color(0xFF2E7D32), "Reference")
                Legend(Color(0xFFC62828), "You")
            }

            Spacer(Modifier.height(8.dp))
            PitchCanvas(
                user = comparison.userPoints,
                reference = comparison.refPoints,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )
            Spacer(Modifier.height(8.dp))

            Text(
                "Duration — you ${formatSec(comparison.userDurationMs)} · reference ${formatSec(comparison.refDurationMs)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                comparison.secondary,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                comparison.hint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = when (comparison.feedback) {
                    ToneFeedback.GOOD -> Color(0xFF2E7D32)
                    else -> Color(0xFFE65100)
                }
            )
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2f)
        }
        Spacer(Modifier.size(4.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PitchCanvas(
    user: List<NormalizedPoint>,
    reference: List<NormalizedPoint>,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    val zeroColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val refColor = Color(0xFF2E7D32)
    val userColor = Color(0xFFC62828)

    Canvas(modifier = modifier) {
        if (user.isEmpty() && reference.isEmpty()) return@Canvas

        // Fixed y-range so the visual gap matches the real semitone difference
        // (auto-scaling made "close" and "far" look the same).
        val minSt = -5f
        val maxSt = 5f

        fun y(st: Float) = size.height * (1f - (st - minSt) / (maxSt - minSt))
        fun x(f: Float) = size.width * f.coerceIn(0f, 1f)

        // Grid lines at -2 / 0 / +2 semitones (0 = speaker's own median pitch).
        listOf(-2f, 0f, 2f).forEach { g ->
            val gy = y(g)
            if (gy in 0f..size.height) {
                drawLine(
                    color = if (g == 0f) zeroColor else gridColor,
                    start = Offset(0f, gy),
                    end = Offset(size.width, gy),
                    strokeWidth = if (g == 0f) 2f else 1f
                )
            }
        }

        fun path(points: List<NormalizedPoint>): Path {
            val p = Path()
            if (points.isEmpty()) return p
            p.moveTo(x(points.first().timeFraction), y(points.first().semitone))
            for (i in 1 until points.size) {
                p.lineTo(x(points[i].timeFraction), y(points[i].semitone))
            }
            return p
        }

        if (reference.isNotEmpty()) {
            drawPath(path(reference), refColor, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (user.isNotEmpty()) {
            drawPath(path(user), userColor, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

private fun formatSec(ms: Float): String {
    if (ms <= 0f) return "—"
    val s = ms / 1000f
    val tenths = (s * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}s"
}
