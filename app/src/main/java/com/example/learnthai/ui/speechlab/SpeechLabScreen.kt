package com.example.learnthai.ui.speechlab

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.learnthai.audio.AudioRecorderManager
import com.example.learnthai.audio.PitchComparison
import com.example.learnthai.audio.PitchDetector
import com.example.learnthai.audio.ToneLabel
import com.example.learnthai.audio.TtsManager
import com.example.learnthai.audio.WavFile
import com.example.learnthai.data.model.WordEntity
import com.example.learnthai.data.repository.WordRepository
import com.example.learnthai.ui.components.PitchVisualizer
import com.example.learnthai.ui.components.WordBreakdownDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Stage { IDLE, TRANSCRIBING, RECORDING_PITCH }

@Composable
fun SpeechLabScreen(
    wordRepository: WordRepository,
    ttsManager: TtsManager,
    audioRecorderManager: AudioRecorderManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPhase by remember { mutableIntStateOf(1) }
    val wordsState = wordRepository.getWordsByPhase(selectedPhase).collectAsState(initial = emptyList())
    val words = wordsState.value

    var currentIndex by remember { mutableIntStateOf(0) }
    var stage by remember { mutableStateOf(Stage.IDLE) }
    var transcriptCaptured by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var recognitionNote by remember { mutableStateOf<String?>(null) }
    var showBreakdownDialog by remember { mutableStateOf<WordEntity?>(null) }

    var pitchComparison by remember { mutableStateOf<PitchComparison?>(null) }
    var isAnalyzingPitch by remember { mutableStateOf(false) }
    var isPreparingPitch by remember { mutableStateOf(false) }
    var pitchError by remember { mutableStateOf<String?>(null) }
    var cancelled by remember { mutableStateOf(false) }

    val currentWord = if (words.isNotEmpty() && currentIndex in words.indices) {
        words[currentIndex]
    } else {
        words.firstOrNull()
    }
    val currentWordRef = rememberUpdatedState(currentWord)
    val stageRef = rememberUpdatedState(stage)

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun resetWord() {
        if (stage == Stage.RECORDING_PITCH) audioRecorderManager.stopRecording()
        if (stage == Stage.TRANSCRIBING) speechRecognizer?.cancel()
        recognizedText = null
        recognitionNote = null
        pitchComparison = null
        pitchError = null
        transcriptCaptured = false
        isAnalyzingPitch = false
        isPreparingPitch = false
        cancelled = false
        stage = Stage.IDLE
    }

    fun finishTranscription() {
        stage = Stage.IDLE
        transcriptCaptured = true
    }

    fun startTranscription() {
        cancelled = false
        recognizedText = null
        recognitionNote = null
        pitchComparison = null
        pitchError = null
        transcriptCaptured = false
        stage = Stage.TRANSCRIBING
        startListening(context, speechRecognizer) { text ->
            recognizedText = text
        }
        // Safety net: if the recognizer never auto-finishes, stop after 6s.
        scope.launch {
            delay(6000)
            if (stageRef.value == Stage.TRANSCRIBING) {
                speechRecognizer?.stopListening()
                finishTranscription()
            }
        }
    }

    fun cancelTranscription() {
        cancelled = true
        speechRecognizer?.cancel()
        stage = Stage.IDLE
        recognizedText = null
        recognitionNote = "Cancelled — tap to try again."
    }

    fun analyzePitchAfterRecording(refPcm: WavFile.Pcm) {
        audioRecorderManager.stopRecording()
        stage = Stage.IDLE
        isPreparingPitch = false
        if (!audioRecorderManager.hasRecording()) {
            pitchError = "No audio captured — tap Retry and speak clearly."
            return
        }
        isAnalyzingPitch = true
        pitchError = null
        scope.launch {
            val comparison = computePitchComparison(refPcm, audioRecorderManager)
            pitchComparison = comparison
            isAnalyzingPitch = false
        }
    }

    fun startPitchRecording() {
        pitchComparison = null
        pitchError = null
        stage = Stage.RECORDING_PITCH
        isPreparingPitch = true
        scope.launch {
            val word = currentWordRef.value
            if (word == null) {
                stage = Stage.IDLE
                isPreparingPitch = false
                return@launch
            }
            val refPcm = synthesizeReference(word, ttsManager)
            if (refPcm == null || stageRef.value != Stage.RECORDING_PITCH) {
                stage = Stage.IDLE
                isPreparingPitch = false
                if (refPcm == null) pitchError = "Couldn't build a pitch reference (TTS synthesis unavailable on this device)."
                return@launch
            }

            val refDurationMs = refPcm.samples.size * 1000f / refPcm.sampleRate
            audioRecorderManager.startRecording()
            isPreparingPitch = false

            // Record for the reference duration plus a small reaction buffer, then auto-stop.
            val recordMs = (refDurationMs + 1200f).toLong().coerceIn(2500L, 6000L)
            delay(recordMs)
            if (stageRef.value == Stage.RECORDING_PITCH) {
                analyzePitchAfterRecording(refPcm)
            }
        }
    }

    fun cancelPitchRecording() {
        audioRecorderManager.stopRecording()
        stage = Stage.IDLE
        isPreparingPitch = false
        pitchError = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startTranscription()
    }

    DisposableEffect(Unit) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        if (!cancelled) finishTranscription()
                    }
                    override fun onError(error: Int) {
                        if (cancelled) return
                        recognizedText = null
                        recognitionNote = friendlyError(error)
                        finishTranscription()
                    }
                    override fun onResults(results: Bundle?) {
                        if (cancelled) return
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        recognizedText = matches?.firstOrNull() ?: ""
                        recognitionNote = null
                        finishTranscription()
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        if (cancelled) return
                        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            recognizedText = partial
                            recognitionNote = null
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
        onDispose {
            speechRecognizer?.destroy()
            audioRecorderManager.release()
        }
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
            items(listOf(1, 2, 3, 4, 5)) { phase ->
                FilterChip(
                    selected = selectedPhase == phase,
                    onClick = {
                        selectedPhase = phase
                        currentIndex = 0
                        resetWord()
                    },
                    label = { Text("Phase $phase") }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (currentWord != null) {
            // Scrollable middle content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Target Word Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { ttsManager.speak(currentWord.thaiWord) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play Native", tint = MaterialTheme.colorScheme.primary)
                            }

                            Text(
                                text = currentWord.englishTranslation,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(onClick = { showBreakdownDialog = currentWord }) {
                                Icon(Icons.Default.Info, contentDescription = "Breakdown", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentWord.thaiWord,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = currentWord.pronunciationGuide,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Big Record Button
                val micButtonColor by animateColorAsState(
                    targetValue = if (stage == Stage.IDLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    label = "MicBtnColor"
                )

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(micButtonColor)
                        .border(4.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            val hasMicPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasMicPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@IconButton
                            }

                            when (stage) {
                                Stage.TRANSCRIBING -> cancelTranscription()
                                Stage.RECORDING_PITCH -> cancelPitchRecording()
                                Stage.IDLE -> if (transcriptCaptured) startPitchRecording() else startTranscription()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (stage == Stage.IDLE) Icons.Default.Mic else Icons.Default.Stop,
                            contentDescription = if (stage == Stage.IDLE) "Speak" else "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (stage) {
                        Stage.TRANSCRIBING -> "Say the word, then pause — it finishes automatically"
                        Stage.RECORDING_PITCH -> if (isPreparingPitch) "Preparing…" else "Say the word — recording (stops automatically)"
                        Stage.IDLE -> if (transcriptCaptured) "Now record your tone — tap and say the word" else "Tap microphone to speak"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recognition Result & Dual Audio Player
                if (recognizedText != null || recognitionNote != null) {
                    val isMatch = recognizedText?.trim() == currentWord.thaiWord
                    val title = if (recognizedText != null) {
                        if (isMatch) "Match: $recognizedText" else "Heard: $recognizedText"
                    } else {
                        recognitionNote ?: ""
                    }
                    val titleColor = if (recognizedText != null && isMatch) Color(0xFF2E7D32) else Color(0xFFE65100)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMatch) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = titleColor
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { ttsManager.speak(currentWord.thaiWord, 0.85f) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Native", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { audioRecorderManager.playRecording() },
                                    modifier = Modifier.weight(1f),
                                    enabled = audioRecorderManager.hasRecording()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Your Voice", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Pitch visualization + score
                if (isPreparingPitch || isAnalyzingPitch) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (isPreparingPitch) "Preparing…" else "Analyzing pitch…",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (pitchComparison != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SpeechScoreCard(
                        comparison = pitchComparison!!,
                        recognizedText = recognizedText,
                        targetWord = currentWord.thaiWord,
                        knownTone = PitchDetector.parseKnownTone(currentWord.pronunciationGuide)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PitchVisualizer(comparison = pitchComparison!!)
                } else if (pitchError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = pitchError!!,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom action bar (always visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { resetWord() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Retry")
                }

                Button(
                    onClick = {
                        resetWord()
                        if (currentIndex < words.size - 1) currentIndex++ else currentIndex = 0
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next Word")
                    Spacer(modifier = Modifier.size(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        showBreakdownDialog?.let { word ->
            WordBreakdownDialog(
                word = word,
                onDismiss = { showBreakdownDialog = null },
                onPlayAudio = { rate -> ttsManager.speak(word.thaiWord, rate) }
            )
        }
    }
}

@Composable
private fun SpeechScoreCard(
    comparison: PitchComparison,
    recognizedText: String?,
    targetWord: String,
    knownTone: ToneLabel?
) {
    val wordScore = if (recognizedText != null) PitchDetector.wordScore(recognizedText, targetWord) else 0f
    val toneScore = comparison.toneScore
    val total = 0.6f * wordScore + 0.4f * toneScore
    val totalPct = (total * 100).toInt()
    val refLabel = knownTone ?: comparison.refToneLabel
    val userLabel = comparison.userToneLabel

    val scoreColor = when {
        total >= 0.8f -> Color(0xFF2E7D32)
        total >= 0.5f -> Color(0xFFE65100)
        else -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Score $totalPct%",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Word ${(wordScore * 100).toInt()}% · Tone ${(toneScore * 100).toInt()}%",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (recognizedText != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Heard: $recognizedText",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Target: ${refLabel.display} · You: ${userLabel.display}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (refLabel == userLabel) Color(0xFF2E7D32) else Color(0xFFE65100)
            )
            if (recognizedText == null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Word not recognized — score based on tone only.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun startListening(
    context: Context,
    speechRecognizer: SpeechRecognizer?,
    onResult: (String?) -> Unit
) {
    if (speechRecognizer == null) return

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("th-TH"))
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
    }

    try {
        speechRecognizer.startListening(intent)
        onResult(null)
    } catch (e: Exception) {
        e.printStackTrace()
        onResult(null)
    }
}

private fun friendlyError(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't hear speech — speak clearly and try again."
    SpeechRecognizer.ERROR_NO_MATCH -> "Couldn't transcribe that — try speaking a little longer and clearer."
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue — check your connection."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy — tap again."
    SpeechRecognizer.ERROR_AUDIO -> "Mic busy — tap again."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow microphone access."
    SpeechRecognizer.ERROR_CLIENT -> "Recognition stopped — tap again."
    else -> "Recognition unavailable (code $error)."
}

private suspend fun synthesizeReference(word: WordEntity, ttsManager: TtsManager): WavFile.Pcm? =
    withContext(Dispatchers.Default) {
        val file = ttsManager.synthesizeToFile(word.thaiWord, 0.85f)
        file?.let { WavFile.readPcm16(it) }
    }

private suspend fun computePitchComparison(
    refPcm: WavFile.Pcm,
    audioRecorderManager: AudioRecorderManager
): PitchComparison? = withContext(Dispatchers.Default) {
    val userSamples = audioRecorderManager.getPcmSamples() ?: return@withContext null
    val userRate = AudioRecorderManager.SAMPLE_RATE
    PitchDetector.compare(userSamples, userRate, refPcm.samples, refPcm.sampleRate)
}
