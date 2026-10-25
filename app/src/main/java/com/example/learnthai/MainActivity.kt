package com.example.learnthai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.learnthai.audio.AudioRecorderManager
import com.example.learnthai.audio.SoundFxManager
import com.example.learnthai.audio.TtsManager
import com.example.learnthai.data.repository.LessonRepository
import com.example.learnthai.data.repository.WordRepository
import com.example.learnthai.ui.flashcards.FlashcardsScreen
import com.example.learnthai.ui.keyboardpractice.KeyboardPracticeScreen
import com.example.learnthai.ui.lessons.LessonsScreen
import com.example.learnthai.ui.navigation.Screen
import com.example.learnthai.ui.settings.SettingsScreen
import com.example.learnthai.ui.speechlab.SpeechLabScreen

class MainActivity : ComponentActivity() {

    private lateinit var ttsManager: TtsManager
    private lateinit var soundFxManager: SoundFxManager
    private lateinit var audioRecorderManager: AudioRecorderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ttsManager = TtsManager(this)
        soundFxManager = SoundFxManager(this)
        audioRecorderManager = AudioRecorderManager(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val coroutineScope = rememberCoroutineScope()
                    val wordRepository = remember { WordRepository(this, coroutineScope) }
                    val lessonRepository = remember { LessonRepository(this) }

                    MainAppNavigation(
                        wordRepository = wordRepository,
                        lessonRepository = lessonRepository,
                        ttsManager = ttsManager,
                        soundFxManager = soundFxManager,
                        audioRecorderManager = audioRecorderManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        audioRecorderManager.release()
    }
}

@Composable
fun MainAppNavigation(
    wordRepository: WordRepository,
    lessonRepository: LessonRepository,
    ttsManager: TtsManager,
    soundFxManager: SoundFxManager,
    audioRecorderManager: AudioRecorderManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var targetKeyboardPhase by remember { mutableIntStateOf(1) }

    val navigationItems = listOf(
        Pair(Screen.Lessons, Icons.Default.AutoStories),
        Pair(Screen.Flashcards, Icons.Default.ViewCarousel),
        Pair(Screen.KeyboardPractice, Icons.Default.Keyboard),
        Pair(Screen.SpeechLab, Icons.Default.Mic),
        Pair(Screen.Settings, Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navigationItems.forEach { (screen, icon) ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = screen.title) },
                        label = { Text(screen.title, maxLines = 1) },
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Lessons.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Lessons.route) {
                LessonsScreen(
                    lessonRepository = lessonRepository,
                    ttsManager = ttsManager,
                    onNavigateToKeyboardPhase = { phase ->
                        targetKeyboardPhase = phase
                        navController.navigate(Screen.KeyboardPractice.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Flashcards.route) {
                FlashcardsScreen(
                    wordRepository = wordRepository,
                    ttsManager = ttsManager
                )
            }
            composable(Screen.KeyboardPractice.route) {
                KeyboardPracticeScreen(
                    wordRepository = wordRepository,
                    ttsManager = ttsManager,
                    soundFxManager = soundFxManager,
                    initialPhase = targetKeyboardPhase
                )
            }
            composable(Screen.SpeechLab.route) {
                SpeechLabScreen(
                    wordRepository = wordRepository,
                    ttsManager = ttsManager,
                    audioRecorderManager = audioRecorderManager
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    ttsManager = ttsManager
                )
            }
        }
    }
}
