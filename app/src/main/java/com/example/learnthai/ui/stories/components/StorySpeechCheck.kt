package com.example.learnthai.ui.stories.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.learnthai.domain.speech.TranscriptAlignment
import com.example.learnthai.domain.speech.TranscriptScorer

private enum class CheckState { IDLE, LISTENING, DONE, ERROR }

/**
 * "Say it" — record the learner reading the card's Thai line, transcribe it with the same
 * `th-TH` recogniser Speech Lab uses, and score the transcript against the target text.
 *
 * After an attempt the target and the transcript are both coloured character by character, so
 * the learner can see exactly which part went wrong (and which words to focus on) rather than
 * just a number. Words only: no pitch/tone analysis — tone work lives in Speech Lab.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StorySpeechCheck(
    targetThai: String,
    tokens: List<String>,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var state by remember(targetThai) { mutableStateOf(CheckState.IDLE) }
    var transcript by remember(targetThai) { mutableStateOf("") }
    var score by remember(targetThai) { mutableIntStateOf(0) }
    var alignment by remember(targetThai) { mutableStateOf<TranscriptAlignment?>(null) }
    var message by remember(targetThai) { mutableStateOf<String?>(null) }
    val recognitionAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val beginListening: () -> Unit = {
        val engine = recognizer
        if (engine == null) {
            state = CheckState.ERROR
            message = "Speech recognition isn't available on this device."
        } else {
            onStopSpeaking()
            transcript = ""
            alignment = null
            message = null
            state = CheckState.LISTENING
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }
            try {
                engine.startListening(intent)
            } catch (e: Exception) {
                state = CheckState.ERROR
                message = "Couldn't start the mic — tap to try again."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginListening()
        } else {
            state = CheckState.ERROR
            message = "Microphone access is needed to check your pronunciation."
        }
    }

    fun requestStart() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) beginListening() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(Unit) {
        if (recognitionAvailable) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        state = CheckState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        if (state == CheckState.LISTENING) {
                            state = CheckState.ERROR
                            message = friendlyError(error)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val heard = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        transcript = heard
                        alignment = TranscriptScorer.align(targetThai, heard)
                        score = TranscriptScorer.score(targetThai, heard)
                        message = null
                        state = CheckState.DONE
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                        if (!partial.isNullOrBlank()) transcript = partial
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } else {
            state = CheckState.ERROR
            message = "Speech recognition isn't available on this device."
        }

        onDispose {
            try {
                recognizer?.destroy()
            } catch (_: Exception) {
                // Nothing useful to do if the engine is already gone.
            }
            recognizer = null
        }
    }

    // A different card means a different sentence: clear the previous attempt.
    LaunchedEffect(targetThai) {
        state = CheckState.IDLE
        transcript = ""
        alignment = null
        score = 0
        message = null
    }

    val mismatchColor = MaterialTheme.colorScheme.error
    val mismatchBackground = mismatchColor.copy(alpha = 0.12f)
    val missedColor = Color(0xFF8A5A00)
    val missedBackground = Color(0xFFFFE0B2).copy(alpha = 0.55f)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SAY IT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "words only · tones not scored",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // The back of the card hides the Thai, so the target line lives inside the check.
            // Once an attempt is scored, characters the recogniser missed turn amber.
            val alignmentNow = alignment
            if (alignmentNow != null && state == CheckState.DONE) {
                HighlightedThai(
                    text = alignmentNow.target,
                    matched = alignmentNow.targetMatched,
                    highlightColor = missedColor,
                    highlightBackground = missedBackground,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            } else {
                Text(
                    text = targetThai,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (state) {
                CheckState.IDLE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    MicButton(enabled = recognitionAvailable, onClick = { requestStart() })
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Say this line out loud",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap the mic when you're ready",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                CheckState.LISTENING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Listening…",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (transcript.isNotBlank()) {
                            Text(
                                text = transcript,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                CheckState.DONE -> Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$score%",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor(score)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = scoreLabel(score),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    if (alignmentNow != null && alignmentNow.heard.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "YOU SAID",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        HighlightedThai(
                            text = alignmentNow.heard,
                            matched = alignmentNow.heardMatched,
                            highlightColor = mismatchColor,
                            highlightBackground = mismatchBackground,
                            fontSize = 18.sp,
                            lineHeight = 26.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "red = different · yellow = missed",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    val missed = alignmentNow
                        ?.let { TranscriptScorer.missedTokens(targetThai, tokens, it) }
                        .orEmpty()

                    if (missed.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "FOCUS ON",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            missed.take(8).forEach { word ->
                                FocusChip(word = word, color = mismatchColor)
                            }
                        }
                    } else if (score >= 95) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Every word matched ✓",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { requestStart() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Try again", fontSize = 12.sp)
                    }
                }

                CheckState.ERROR -> Column {
                    Text(
                        text = message ?: "Couldn't hear that — try again.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { requestStart() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Try again", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Renders Thai text with the unmatched characters highlighted, so the learner can see exactly
 * where the transcript diverged instead of only seeing a percentage.
 */
@Composable
private fun HighlightedThai(
    text: String,
    matched: BooleanArray,
    highlightColor: Color,
    highlightBackground: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit
) {
    val baseColor = MaterialTheme.colorScheme.onSurface
    val annotated = buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val isMatch = matched.getOrElse(index) { true }
            var end = index
            while (end < text.length && matched.getOrElse(end) { true } == isMatch) end++

            val run = text.substring(index, end)
            if (isMatch) {
                append(run)
            } else {
                withStyle(
                    SpanStyle(
                        color = highlightColor,
                        background = highlightBackground,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(run)
                }
            }
            index = end
        }
    }

    Text(
        text = annotated,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = baseColor
    )
}

@Composable
private fun FocusChip(word: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = word,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun MicButton(enabled: Boolean, onClick: () -> Unit) {
    val background = if (enabled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Record your attempt",
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun scoreColor(score: Int): Color = when {
    score >= 85 -> Color(0xFF2E7D32)
    score >= 60 -> Color(0xFFEF6C00)
    else -> MaterialTheme.colorScheme.error
}

private fun scoreLabel(score: Int): String = when {
    score >= 95 -> "almost perfect"
    score >= 85 -> "clear"
    score >= 60 -> "close — check the colours"
    else -> "try again slowly"
}

private fun friendlyError(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't hear speech — tap and speak clearly."
    SpeechRecognizer.ERROR_NO_MATCH -> "Couldn't transcribe that — try a little slower."
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
        "Network issue — check your connection."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recogniser busy — tap again."
    SpeechRecognizer.ERROR_AUDIO -> "Mic busy — tap again."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow microphone access to use this."
    SpeechRecognizer.ERROR_CLIENT -> "Recognition stopped — tap again."
    else -> "Couldn't hear that — try again."
}
