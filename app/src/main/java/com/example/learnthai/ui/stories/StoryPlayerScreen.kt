package com.example.learnthai.ui.stories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.learnthai.audio.TtsManager
import com.example.learnthai.data.model.Story
import com.example.learnthai.data.model.StoryCard
import com.example.learnthai.data.model.StoryToken
import com.example.learnthai.data.model.WordEntity
import com.example.learnthai.data.repository.StoryProgressStore
import com.example.learnthai.data.repository.StoryRepository
import com.example.learnthai.data.repository.WordRepository
import com.example.learnthai.ui.components.WordBreakdownDialog
import com.example.learnthai.ui.stories.components.StoryFlipCard
import com.example.learnthai.ui.stories.components.StoryTokenSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plays one story: a pager of dialogue flip cards, native TTS per line, per-line progress,
 * and a "play scene" mode that reads the whole scene in order.
 */
@Composable
fun StoryPlayerScreen(
    storyId: String,
    storyRepository: StoryRepository,
    progressStore: StoryProgressStore,
    wordRepository: WordRepository,
    ttsManager: TtsManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var story by remember(storyId) { mutableStateOf<Story?>(null) }
    var loading by remember(storyId) { mutableStateOf(true) }

    LaunchedEffect(storyId) {
        story = storyRepository.loadStory(storyId)
        loading = false
    }

    val loaded = story
    when {
        loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        loaded == null || loaded.cards.isEmpty() -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "That story could not be opened.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back to stories") }
            }
        }

        else -> StoryPlayerContent(
            story = loaded,
            progressStore = progressStore,
            wordRepository = wordRepository,
            ttsManager = ttsManager,
            onBack = onBack,
            modifier = modifier
        )
    }
}

@Composable
private fun StoryPlayerContent(
    story: Story,
    progressStore: StoryProgressStore,
    wordRepository: WordRepository,
    ttsManager: TtsManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cards = story.cards
    val scope = rememberCoroutineScope()

    val startPage = remember(story.id) {
        (progressStore.progressFor(story.id)?.lastCard ?: 0).coerceIn(0, cards.lastIndex)
    }
    val pagerState = rememberPagerState(initialPage = startPage) { cards.size }

    var isPlaying by remember { mutableStateOf(false) }
    var showCompletion by remember { mutableStateOf(false) }
    var selectedToken by remember { mutableStateOf<StoryToken?>(null) }
    var breakdownWord by remember { mutableStateOf<WordEntity?>(null) }
    val flippedCards = remember { mutableStateMapOf<String, Boolean>() }

    BackHandler(onBack = onBack)

    // Auto-speak the line whenever the page settles (unless the scene is being played).
    LaunchedEffect(pagerState.settledPage, story.id) {
        val card = cards.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        if (progressStore.autoSpeak && !isPlaying) {
            ttsManager.speak(card.thai, 0.85f)
        }
    }

    // Remember how far the learner got.
    LaunchedEffect(pagerState.settledPage, story.id) {
        progressStore.markCardShown(story.id, pagerState.settledPage)
    }

    LaunchedEffect(showCompletion) {
        if (showCompletion) progressStore.markCompleted(story.id)
    }

    // "Play scene": speak each line, advance, stop on the last one.
    LaunchedEffect(isPlaying, story.id) {
        if (!isPlaying) return@LaunchedEffect
        while (isPlaying) {
            val page = pagerState.currentPage
            val card = cards.getOrNull(page)
            if (card == null) {
                isPlaying = false
                break
            }
            ttsManager.speak(card.thai, 0.85f)
            delay(estimatedLineMillis(card))

            if (page >= cards.lastIndex) {
                isPlaying = false
                showCompletion = true
                break
            }
            pagerState.animateScrollToPage(page + 1)
            delay(320)
        }
    }

    DisposableEffect(Unit) {
        onDispose { ttsManager.stop() }
    }

    val currentCard = cards.getOrNull(pagerState.currentPage)
    val currentSpeaker = story.character(currentCard?.speaker)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to stories"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = story.titleEn,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = buildString {
                        append(story.teachesLabel)
                        if (currentSpeaker != null) append(" · ${currentSpeaker.nameEn}")
                        append(" · line ${pagerState.currentPage + 1} of ${cards.size}")
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            PageDots(
                total = cards.size,
                current = pagerState.currentPage
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            pageSpacing = 12.dp
        ) { page ->
            val card = cards[page]
            val speaker = story.character(card.speaker)
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Height follows the screen: taller than the old fixed 0.86 aspect, but capped so
                // the scene art never becomes a narrow strip on very tall phones.
                val cardHeight = minOf(maxHeight * 0.95f, maxWidth * 1.32f)
                StoryFlipCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight),
                    card = card,
                    speaker = speaker,
                    isFlipped = flippedCards[card.id] == true,
                    onFlip = { flippedCards[card.id] = !(flippedCards[card.id] ?: false) },
                    onSpeak = { rate -> ttsManager.speak(card.thai, rate) },
                    onTokenClick = { token -> selectedToken = token },
                    onStopSpeaking = { ttsManager.stop() }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage - 1).coerceAtLeast(0)
                        )
                    }
                },
                enabled = pagerState.currentPage > 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous line",
                    modifier = Modifier.size(16.dp)
                )
            }

            Button(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "Stop" else "Play scene",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (pagerState.currentPage < cards.lastIndex) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next line",
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Button(
                    onClick = { showCompletion = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Finish", fontSize = 12.sp)
                }
            }
        }
    }

    selectedToken?.let { token ->
        StoryTokenSheet(
            token = token,
            hasCurriculumEntry = token.match != null,
            onOpenToneDetails = { match ->
                selectedToken = null
                scope.launch {
                    breakdownWord = wordRepository.getWord(match)
                }
            },
            onDismiss = { selectedToken = null }
        )
    }

    breakdownWord?.let { word ->
        WordBreakdownDialog(
            word = word,
            onDismiss = { breakdownWord = null },
            onPlayAudio = { rate -> ttsManager.speak(word.thaiWord, rate) },
            onPlaySentence = { sentence -> ttsManager.speak(sentence, 0.85f) }
        )
    }

    if (showCompletion) {
        StoryCompletionDialog(
            story = story,
            onReadAgain = {
                showCompletion = false
                scope.launch { pagerState.scrollToPage(0) }
            },
            onBack = onBack
        )
    }
}

@Composable
private fun PageDots(total: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == current) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

@Composable
private fun StoryCompletionDialog(
    story: Story,
    onReadAgain: () -> Unit,
    onBack: () -> Unit
) {
    Dialog(onDismissRequest = onBack) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "จบแล้ว!", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Scene complete",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "You practiced: ${story.teachesLabel.lowercase()}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                if (story.aim.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = story.aim,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = onReadAgain, modifier = Modifier.fillMaxWidth()) {
                    Text("Read again")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to stories")
                }
            }
        }
    }
}

/** Rough speech length so play-scene does not advance before the TTS has finished. */
private fun estimatedLineMillis(card: StoryCard): Long {
    val base = card.thai.length * 110L
    return (base + 900L).coerceIn(1800L, 6500L)
}
