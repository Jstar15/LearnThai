package com.example.learnthai.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Lessons : Screen("lessons", "Lessons")
    object Stories : Screen("stories", "Stories")
    object Flashcards : Screen("flashcards", "Flashcards")
    object KeyboardPractice : Screen("practice", "Keyboard")
    object SpeechLab : Screen("speech_lab", "Speech Lab")
    object Settings : Screen("settings", "Settings")

    /** Story player destination. `title` is unused; the story supplies its own header. */
    object StoryPlayer : Screen("story/{storyId}", "Story") {
        const val ARG_STORY_ID = "storyId"
        fun routeFor(storyId: String) = "story/$storyId"
    }
}
