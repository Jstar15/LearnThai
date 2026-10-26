package com.example.learnthai.ui.keyboardpractice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnthai.audio.SoundFxManager
import com.example.learnthai.audio.TtsManager
import com.example.learnthai.data.model.WordEntity
import com.example.learnthai.data.repository.WordRepository
import com.example.learnthai.ui.components.WordBreakdownDialog
import com.example.learnthai.ui.components.keyboard.KedmaneeKeyboardView
import com.example.learnthai.ui.components.keyboard.KedmaneeLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KeyboardPracticeScreen(
    wordRepository: WordRepository,
    ttsManager: TtsManager,
    soundFxManager: SoundFxManager,
    initialPhase: Int = 1,
    modifier: Modifier = Modifier
) {
    var selectedPhase by remember(initialPhase) { mutableIntStateOf(initialPhase) }
    var isGuidedMode by remember { mutableStateOf(true) }
    var isBlindMode by remember { mutableStateOf(false) }

    val rawWordsState = wordRepository.getWordsByPhase(selectedPhase).collectAsState(initial = emptyList())
    val rawWords = rawWordsState.value

    // Active session words (shuffled per session)
    var sessionWords by remember { mutableStateOf<List<WordEntity>>(emptyList()) }
    var sessionSeed by remember { mutableIntStateOf(0) }

    // Session state
    var sessionPhase by remember { mutableIntStateOf(selectedPhase) }
    var lastSessionSeed by remember { mutableIntStateOf(sessionSeed) }
    var isMistakeActive by remember { mutableStateOf(false) }

    LaunchedEffect(isMistakeActive) {
        if (isMistakeActive) {
            delay(400)
            isMistakeActive = false
        }
    }

    var currentWordIndex by remember { mutableIntStateOf(0) }
    var isSessionCompleted by remember { mutableStateOf(false) }
    val typedKeystrokes = remember { mutableStateListOf<String>() }
    var showBreakdownDialog by remember { mutableStateOf<WordEntity?>(null) }
    var isWordComplete by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Track words that encountered mistakes during this session
    val wordsWithMistakes = remember { mutableStateMapOf<String, WordEntity>() }
    var currentWordHadMistake by remember { mutableStateOf(false) }

    // Initialize or re-shuffle session words only on initial load, phase switch, or Play Again click
    LaunchedEffect(rawWords, selectedPhase, sessionSeed) {
        if (rawWords.isNotEmpty()) {
            val phaseChanged = selectedPhase != sessionPhase
            val seedChanged = sessionSeed != lastSessionSeed
            val needsInit = sessionWords.isEmpty()

            if (needsInit || phaseChanged || seedChanged) {
                sessionPhase = selectedPhase
                lastSessionSeed = sessionSeed
                sessionWords = rawWords.shuffled()
                currentWordIndex = 0
                isSessionCompleted = false
                typedKeystrokes.clear()
                wordsWithMistakes.clear()
                currentWordHadMistake = false
                errorMessage = null
            }
        }
    }

    val currentWord = if (sessionWords.isNotEmpty() && currentWordIndex in sessionWords.indices) {
        sessionWords[currentWordIndex]
    } else null

    // Parse expected sequence for the word
    val expectedSequence: List<String> = remember(currentWord) {
        if (currentWord != null) {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(currentWord.keystrokeSequenceJson, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    val currentTargetChar: Char? = if (typedKeystrokes.size < expectedSequence.size) {
        expectedSequence[typedKeystrokes.size].firstOrNull()
    } else null

    // Speak audio when new word loads
    LaunchedEffect(currentWord?.thaiWord) {
        currentWord?.let {
            typedKeystrokes.clear()
            isWordComplete = false
            currentWordHadMistake = false
            errorMessage = null
            ttsManager.speak(it.thaiWord)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Controls: Phase Carousel & Guided switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items((1..10).toList()) { phase ->
                    FilterChip(
                        selected = selectedPhase == phase,
                        onClick = {
                            if (selectedPhase != phase) {
                                selectedPhase = phase
                                sessionSeed++
                            }
                        },
                        label = { Text("P$phase", fontSize = 12.sp) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Guided",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGuidedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Switch(
                        checked = isGuidedMode,
                        onCheckedChange = { isGuidedMode = it },
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Blind",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBlindMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Switch(
                        checked = isBlindMode,
                        onCheckedChange = { isBlindMode = it },
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress bar
        val progressFraction = if (sessionWords.isNotEmpty()) {
            if (isSessionCompleted) 1f else currentWordIndex.toFloat() / sessionWords.size
        } else 0f

        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isSessionCompleted) {
            // Session Completion Screen
            val totalWords = sessionWords.size
            val mistakesCount = wordsWithMistakes.size
            val perfectScore = totalWords - mistakesCount

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Session Complete",
                        tint = Color(0xFF388E3C),
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Phase $selectedPhase Complete!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Score: $perfectScore / $totalWords correct on first try",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (mistakesCount == 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Review Mistakes Section
                    if (mistakesCount > 0) {
                        Text(
                            text = "Words to Review ($mistakesCount):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(wordsWithMistakes.values.toList()) { mistakeWord ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { ttsManager.speak(mistakeWord.thaiWord) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Listen",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            Column {
                                                Text(
                                                    text = mistakeWord.thaiWord,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${mistakeWord.pronunciationGuide} • ${mistakeWord.englishTranslation}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { showBreakdownDialog = mistakeWord },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = "Details",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎉 Flawless! 0 keystroke mistakes.\nYou have mastered this session!",
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Play Again & Next Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                sessionSeed++ // reshuffles session words and restarts
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Play Again", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play Again", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        if (selectedPhase < 10) {
                            OutlinedButton(
                                onClick = {
                                    selectedPhase++
                                    sessionSeed++
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Phase ${selectedPhase + 1} ➔", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else if (currentWord != null) {
            // Hero Practice Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header: Counter, translation, audio, info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { ttsManager.speak(currentWord.thaiWord) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = MaterialTheme.colorScheme.primary)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentWord.englishTranslation,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${currentWordIndex + 1} / ${sessionWords.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = { showBreakdownDialog = currentWord }) {
                            Icon(Icons.Default.Info, contentDescription = "Breakdown", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input display box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isWordComplete) 2.dp else 1.dp,
                                color = if (isWordComplete) Color(0xFF388E3C) else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        expectedSequence.forEachIndexed { index, charStr ->
                            val isTyped = index < typedKeystrokes.size
                            val isCurrentTarget = index == typedKeystrokes.size

                            val rawChar = if (isTyped) {
                                typedKeystrokes[index]
                            } else if (!isBlindMode) {
                                charStr
                            } else {
                                ""
                            }
                            val isCombining = rawChar.isNotEmpty() && KedmaneeLayout.isCombiningMark(rawChar)
                            val displayText = if (rawChar.isNotEmpty()) KedmaneeLayout.getDisplayString(rawChar) else ""

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isTyped -> Color(0xFFE8F5E9)
                                            isCurrentTarget && isMistakeActive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                            isCurrentTarget -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isCurrentTarget) 1.5.dp else 0.dp,
                                        color = when {
                                            isCurrentTarget && isMistakeActive -> MaterialTheme.colorScheme.error
                                            isCurrentTarget -> MaterialTheme.colorScheme.primary
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayText,
                                    fontSize = if (isCombining) 14.sp else 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isTyped -> Color(0xFF2E7D32)
                                        isCurrentTarget && isMistakeActive -> MaterialTheme.colorScheme.error
                                        isCurrentTarget -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                            }
                        }

                        if (isWordComplete) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Complete",
                                tint = Color(0xFF388E3C),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Romanji pronunciation guide shown below the Thai when Guided mode is ON (and hidden in Blind mode until word is complete)
                    AnimatedVisibility(visible = isGuidedMode && (!isBlindMode || isWordComplete)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = currentWord.pronunciationGuide,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Gentle hint / error banner
                    AnimatedVisibility(visible = errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // The Interactive Kedmanee Keyboard
            KedmaneeKeyboardView(
                currentTargetChar = currentTargetChar,
                isGuidedMode = isGuidedMode,
                onKeyValidated = { char ->
                    errorMessage = null
                    soundFxManager.playTapFeedback()
                    typedKeystrokes.add(char.toString())

                    if (typedKeystrokes.size == expectedSequence.size) {
                        isWordComplete = true
                        soundFxManager.playCorrectFeedback()

                        // Record if this word had mistakes
                        if (currentWordHadMistake) {
                            wordsWithMistakes[currentWord.thaiWord] = currentWord
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            delay(600)
                            if (currentWordIndex < sessionWords.size - 1) {
                                currentWordIndex++
                            } else {
                                isSessionCompleted = true
                            }
                        }
                    }
                },
                onWrongKeyAttempted = { wrongChar ->
                    currentWordHadMistake = true
                    wordsWithMistakes[currentWord.thaiWord] = currentWord
                    soundFxManager.playErrorBuzz()
                    isMistakeActive = true
                    val targetDisplay = currentTargetChar?.let { KedmaneeLayout.getDisplayString(it) } ?: ""
                    errorMessage = if (isBlindMode) "Try again!" else "Oops! Look for '${targetDisplay}'"
                    CoroutineScope(Dispatchers.IO).launch {
                        wordRepository.recordKeystrokeMistake(
                            thaiWord = currentWord.thaiWord,
                            expected = currentTargetChar?.toString() ?: "",
                            typed = wrongChar.toString()
                        )
                    }
                }
            )
        }

        // Inspection breakdown sheet
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
