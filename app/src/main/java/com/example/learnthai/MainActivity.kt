package com.example.learnthai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.learnthai.audio.AudioRecorderManager
import com.example.learnthai.audio.SoundFxManager
import com.example.learnthai.audio.TtsManager
import com.example.learnthai.data.repository.LessonRepository
import com.example.learnthai.data.repository.StoryProgressStore
import com.example.learnthai.data.repository.StoryRepository
import com.example.learnthai.data.repository.WordRepository
import com.example.learnthai.ui.flashcards.FlashcardsScreen
import com.example.learnthai.ui.keyboardpractice.KeyboardPracticeScreen
import com.example.learnthai.ui.lessons.LessonsScreen
import com.example.learnthai.ui.navigation.Screen
import com.example.learnthai.ui.settings.SettingsScreen
import com.example.learnthai.ui.speechlab.SpeechLabScreen
import com.example.learnthai.ui.stories.StoriesScreen
import com.example.learnthai.ui.stories.StoryPlayerScreen
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {

    private lateinit var ttsManager: TtsManager
    private lateinit var soundFxManager: SoundFxManager
    private lateinit var audioRecorderManager: AudioRecorderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The app uses a light surface and a transparent status bar: without this the
        // system's white status icons are invisible against the background.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

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
                    val storyRepository = remember { StoryRepository(this) }
                    val storyProgressStore = remember {
                        StoryProgressStore(this).also { store ->
                            ttsManager.applyPreferredVoice(store.selectedVoice)
                        }
                    }

                    MainAppNavigation(
                        wordRepository = wordRepository,
                        lessonRepository = lessonRepository,
                        storyRepository = storyRepository,
                        storyProgressStore = storyProgressStore,
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
    storyRepository: StoryRepository,
    storyProgressStore: StoryProgressStore,
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
        Pair(Screen.Stories, Icons.Default.TheaterComedy),
        Pair(Screen.KeyboardPractice, Icons.Default.Keyboard),
        Pair(Screen.SpeechLab, Icons.Default.Mic)
    )

    fun navigateToTab(screen: Screen) {
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

    Scaffold(
        topBar = {
            // Settings is not a tab any more: a single small gear at the app's top-right corner.
            if (currentRoute != Screen.Settings.route) {
                SettingsGearBar(
                    onSettings = {
                        navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                navigationItems.forEach { (screen, icon) ->
                    val onStoryPlayer = currentRoute?.startsWith("story/") == true
                    val isSelected = currentRoute == screen.route ||
                        (screen == Screen.Stories && onStoryPlayer)

                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = screen.title) },
                        label = { Text(screen.title, maxLines = 1) },
                        selected = isSelected,
                        onClick = { navigateToTab(screen) }
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
                    },
                    onOpenFlashcards = {
                        navController.navigate(Screen.Flashcards.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Stories.route) {
                StoriesScreen(
                    storyRepository = storyRepository,
                    progressStore = storyProgressStore,
                    onOpenStory = { storyId ->
                        navController.navigate(Screen.StoryPlayer.routeFor(storyId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Screen.StoryPlayer.route,
                arguments = listOf(
                    navArgument(Screen.StoryPlayer.ARG_STORY_ID) { type = NavType.StringType }
                )
            ) { entry ->
                StoryPlayerScreen(
                    storyId = entry.arguments?.getString(Screen.StoryPlayer.ARG_STORY_ID).orEmpty(),
                    storyRepository = storyRepository,
                    progressStore = storyProgressStore,
                    wordRepository = wordRepository,
                    ttsManager = ttsManager,
                    onBack = { navController.popBackStack() }
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
                    ttsManager = ttsManager,
                    progressStore = storyProgressStore,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun SettingsGearBar(onSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(44.dp)
            .padding(end = 10.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
