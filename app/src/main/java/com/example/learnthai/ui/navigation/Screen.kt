package com.example.learnthai.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Lessons : Screen("lessons", "Lessons")
    object Flashcards : Screen("flashcards", "Flashcards")
    object KeyboardPractice : Screen("practice", "Keyboard")
    object SpeechLab : Screen("speech_lab", "Speech Lab")
    object Settings : Screen("settings", "Settings")
}
