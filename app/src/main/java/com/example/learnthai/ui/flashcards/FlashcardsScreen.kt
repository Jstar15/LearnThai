package com.example.learnthai.ui.flashcards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnthai.audio.TtsManager
import com.example.learnthai.data.model.WordEntity
import com.example.learnthai.data.repository.SrsRating
import com.example.learnthai.data.repository.WordRepository
import com.example.learnthai.ui.components.WordBreakdownDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun FlashcardsScreen(
    wordRepository: WordRepository,
    ttsManager: TtsManager,
    modifier: Modifier = Modifier
) {
    var selectedPhase by remember { mutableIntStateOf(1) }
    val wordsState = wordRepository.getWordsByPhase(selectedPhase).collectAsState(initial = emptyList())
    val words = wordsState.value

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showBreakdownDialog by remember { mutableStateOf<WordEntity?>(null) }

    // Ensure valid index when phase changes
    val currentWord = if (words.isNotEmpty() && currentIndex in words.indices) {
        words[currentIndex]
    } else {
        if (words.isNotEmpty()) currentIndex = 0
        words.firstOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Phase Filter Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items((1..15).toList()) { phase ->
                FilterChip(
                    selected = selectedPhase == phase,
                    onClick = {
                        selectedPhase = phase
                        currentIndex = 0
                        isFlipped = false
                    },
                    label = { Text("Phase $phase") }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Counter
        Text(
            text = if (words.isNotEmpty()) "Word ${currentIndex + 1} of ${words.size}" else "Loading...",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (currentWord != null) {
            // Interactive 3D Flip Card
            val rotation by animateFloatAsState(
                targetValue = if (isFlipped) 180f else 0f,
                animationSpec = tween(durationMillis = 400),
                label = "CardFlipRotation"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isFlipped = !isFlipped
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Top actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { ttsManager.speak(currentWord.thaiWord) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play Audio", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(onClick = { showBreakdownDialog = currentWord }) {
                            Icon(Icons.Default.Info, contentDescription = "Inspect Word", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    // Content based on flip state
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                if (rotation > 90f) rotationY = 180f // Unmirror back content
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (rotation <= 90f) {
                            // Front: Big Thai Script
                            Text(
                                text = currentWord.thaiWord,
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Tap card to reveal translation",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            // Back: English Meaning + Pronunciation + Class
                            Text(
                                text = currentWord.englishTranslation,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentWord.pronunciationGuide,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Text(
                                    text = "${currentWord.consonantClass} Class",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SRS Rating Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SrsRatingButton(
                    text = "Again",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            wordRepository.updateSrsRating(currentWord.thaiWord, SrsRating.AGAIN)
                        }
                        advanceCard(words.size) { currentIndex = it; isFlipped = false }
                    },
                    modifier = Modifier.weight(1f)
                )

                SrsRatingButton(
                    text = "Hard",
                    color = Color(0xFFF57C00),
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            wordRepository.updateSrsRating(currentWord.thaiWord, SrsRating.HARD)
                        }
                        advanceCard(words.size) { currentIndex = it; isFlipped = false }
                    },
                    modifier = Modifier.weight(1f)
                )

                SrsRatingButton(
                    text = "Good",
                    color = Color(0xFF388E3C),
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            wordRepository.updateSrsRating(currentWord.thaiWord, SrsRating.GOOD)
                        }
                        advanceCard(words.size) { currentIndex = it; isFlipped = false }
                    },
                    modifier = Modifier.weight(1f)
                )

                SrsRatingButton(
                    text = "Easy",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            wordRepository.updateSrsRating(currentWord.thaiWord, SrsRating.EASY)
                        }
                        advanceCard(words.size) { currentIndex = it; isFlipped = false }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Breakdown inspection modal
        showBreakdownDialog?.let { word ->
            WordBreakdownDialog(
                word = word,
                onDismiss = { showBreakdownDialog = null },
                onPlayAudio = { rate -> ttsManager.speak(word.thaiWord, rate) },
                onPlaySentence = { sentence -> ttsManager.speak(sentence, 0.85f) }
            )
        }
    }
}

private fun advanceCard(total: Int, onNext: (Int) -> Unit) {
    if (total > 0) {
        onNext((0 until total).random())
    }
}

@Composable
private fun SrsRatingButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
