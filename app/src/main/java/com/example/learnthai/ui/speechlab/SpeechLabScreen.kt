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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.learnthai.audio.AudioRecorderManager
import com.example.learnthai.audio.TtsManager
import com.example.learnthai.data.model.WordEntity
import com.example.learnthai.data.repository.WordRepository
import com.example.learnthai.ui.components.WordBreakdownDialog

@Composable
fun SpeechLabScreen(
    wordRepository: WordRepository,
    ttsManager: TtsManager,
    audioRecorderManager: AudioRecorderManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPhase by remember { mutableIntStateOf(1) }
    val wordsState = wordRepository.getWordsByPhase(selectedPhase).collectAsState(initial = emptyList())
    val words = wordsState.value

    var currentIndex by remember { mutableIntStateOf(0) }
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var showBreakdownDialog by remember { mutableStateOf<WordEntity?>(null) }

    val currentWord = if (words.isNotEmpty() && currentIndex in words.indices) {
        words[currentIndex]
    } else {
        if (words.isNotEmpty()) currentIndex = 0
        words.firstOrNull()
    }

    // SpeechRecognizer setup
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening(context, speechRecognizer, audioRecorderManager) { text, listening ->
                recognizedText = text
                isListening = listening
            }
        }
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
                        isListening = false
                        audioRecorderManager.stopRecording()
                    }
                    override fun onError(error: Int) {
                        isListening = false
                        audioRecorderManager.stopRecording()
                        recognizedText = "Recognition error ($error)"
                    }
                    override fun onResults(results: Bundle?) {
                        isListening = false
                        audioRecorderManager.stopRecording()
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        recognizedText = matches?.firstOrNull() ?: ""
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
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
                        recognizedText = null
                    },
                    label = { Text("Phase $phase") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentWord != null) {
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

            Spacer(modifier = Modifier.height(24.dp))

            // Big Record Button
            val micButtonColor by animateColorAsState(
                targetValue = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                label = "MicBtnColor"
            )

            Box(
                modifier = Modifier
                    .size(90.dp)
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
                        } else {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                                audioRecorderManager.stopRecording()
                                isListening = false
                            } else {
                                recognizedText = null
                                startListening(context, speechRecognizer, audioRecorderManager) { text, listening ->
                                    recognizedText = text
                                    isListening = listening
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Speak",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isListening) "Listening in Thai... Speak now!" else "Tap microphone to speak",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Recognition Result & Dual Audio Player
            if (recognizedText != null) {
                val isMatch = recognizedText?.trim() == currentWord.thaiWord

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
                            text = if (isMatch) "Awesome! Match: $recognizedText" else "Heard: $recognizedText",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMatch) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Self-Assessment: Compare your voice with native audio",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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
                                Text("Native Audio", fontSize = 12.sp)
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

            Spacer(modifier = Modifier.weight(1f))

            // Next Word CTA
            Button(
                onClick = {
                    recognizedText = null
                    if (currentIndex < words.size - 1) currentIndex++ else currentIndex = 0
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next Word")
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

private fun startListening(
    context: Context,
    speechRecognizer: SpeechRecognizer?,
    audioRecorderManager: AudioRecorderManager,
    onStatusChange: (String?, Boolean) -> Unit
) {
    if (speechRecognizer == null) return

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("th-TH"))
    }

    try {
        audioRecorderManager.startRecording()
        speechRecognizer.startListening(intent)
        onStatusChange(null, true)
    } catch (e: Exception) {
        e.printStackTrace()
        onStatusChange("Failed to start speech recognition", false)
    }
}
