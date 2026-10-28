package com.example.learnthai.ui.stories.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnthai.data.model.StoryCard
import com.example.learnthai.data.model.StoryCharacter
import com.example.learnthai.data.model.StoryToken
import com.example.learnthai.ui.components.StoryImage

/**
 * One line of dialogue as a flip card.
 *
 * Front: the speaker's scene with their Thai line in a speech bubble (audio-first).
 * Back: English meaning, romanisation, word-by-word breakdown, and the "why" tip.
 *
 * The card flips on a tap while showing the Thai side. The English side is deliberately
 * tap-to-flip-free so reading/scrolling and tapping word chips cannot flip it by accident;
 * it has an explicit "Show Thai" button instead.
 */
@Composable
fun StoryFlipCard(
    card: StoryCard,
    speaker: StoryCharacter?,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onSpeak: (Float) -> Unit,
    onTokenClick: (StoryToken) -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "StoryCardFlip"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (rotation <= 90f) {
                StoryCardFront(
                    card = card,
                    speaker = speaker,
                    onFlip = onFlip,
                    onSpeak = onSpeak
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                ) {
                    StoryCardBack(
                        card = card,
                        speaker = speaker,
                        onFlip = onFlip,
                        onSpeak = onSpeak,
                        onTokenClick = onTokenClick,
                        onStopSpeaking = onStopSpeaking
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryCardFront(
    card: StoryCard,
    speaker: StoryCharacter?,
    onFlip: () -> Unit,
    onSpeak: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFlip
                )
        ) {
            StoryImage(
                assetPath = card.image,
                contentDescription = "Scene: ${speaker?.nameEn ?: "story"}",
                fallbackLabel = speaker?.nameEn ?: "",
                fallbackInitial = speaker?.nameEn?.take(1) ?: "",
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.38f))
                        )
                    )
            )

            if (speaker != null) {
                SpeakerPill(
                    speaker = speaker,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                IconButton(onClick = { onSpeak(0.85f) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Play line",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { onSpeak(0.6f) }) {
                    Text(text = "🐢", fontSize = 16.sp)
                }
            }
        }

        SpeechBubble(
            text = card.thai,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoryCardBack(
    card: StoryCard,
    speaker: StoryCharacter?,
    onFlip: () -> Unit,
    onSpeak: (Float) -> Unit,
    onTokenClick: (StoryToken) -> Unit,
    onStopSpeaking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = speaker?.nameEn ?: "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "English side",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onFlip) {
                Text(text = "Show Thai", fontSize = 12.sp)
            }
        }

        Text(
            text = card.english,
            fontSize = 21.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        if (card.roman.isNotBlank()) {
            Text(
                text = card.roman,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        Text(
            text = "WORD BY WORD",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            card.tokens.forEach { token ->
                TokenChip(token = token, onClick = { onTokenClick(token) })
            }
        }

        if (card.tip.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            StoryTipCard(tip = card.tip)
        }

        if (card.newWords.isNotEmpty()) {
            Text(
                text = "NEW IN THIS LINE: ${card.newWords.joinToString(", ")}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSpeak(0.85f) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Listen", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { onSpeak(0.6f) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "🐢 Slow", fontSize = 12.sp)
            }
        }

        StorySpeechCheck(
            targetThai = card.thai,
            tokens = card.tokens.map { it.thai },
            onStopSpeaking = onStopSpeaking
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun SpeakerPill(speaker: StoryCharacter, modifier: Modifier = Modifier) {
    val color = storyColor(speaker.color)
    // Keep the label readable even when a story gives a character a very light colour.
    val labelColor = if (color.luminance() > 0.6f) Color(0xFF1B1B1F) else Color.White
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = speaker.nameEn,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor
        )
    }
}

@Composable
private fun SpeechBubble(text: String, modifier: Modifier = Modifier) {
    val bubbleColor = MaterialTheme.colorScheme.surface
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(bubbleColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = text,
                fontSize = 25.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start
            )
        }
        Canvas(
            modifier = Modifier
                .padding(start = 26.dp)
                .size(width = 16.dp, height = 9.dp)
        ) {
            val tail = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path = tail, color = bubbleColor)
        }
    }
}

@Composable
private fun TokenChip(token: StoryToken, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 116.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = token.thai,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        if (token.roman.isNotBlank()) {
            Text(
                text = token.roman,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (token.gloss.isNotBlank()) {
            Text(
                text = token.gloss,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Parses `#RRGGBB` from story JSON; falls back to the app primary if malformed. */
fun storyColor(hex: String, fallback: Color = Color(0xFF6750A4)): Color {
    return try {
        val clean = hex.removePrefix("#")
        when (clean.length) {
            6 -> Color(("FF$clean").toLong(16))
            8 -> Color(clean.toLong(16))
            else -> fallback
        }
    } catch (e: NumberFormatException) {
        fallback
    }
}
