# 01. Architecture & Tech Stack

## 1. Technical Stack Overview

| Layer | Technology | Rationale |
|---|---|---|
| **Language** | Kotlin 2.0+ | Modern Android standard, type safety, Coroutines & Flow native. |
| **UI Framework** | Jetpack Compose + Material 3 | Declarative UI, essential for dynamic custom keyboard layout animations, key highlights, and smooth state updates. |
| **Architecture Pattern** | MVVM + Clean Architecture | Unidirectional Data Flow (UDF), high cohesion, testable domain rules (tone calculations, SRS logic). |
| **Persistence** | Android Room Database (SQLite) | Local-first; stores vocabulary, SRS stage, mistake counts, and session stats with zero cloud dependency. |
| **Preferences** | Jetpack DataStore | Audio speed, transliteration preferences, theme, and assistive keyboard hint modes. |
| **Text-to-Speech** | Android `TextToSpeech` (`th-TH`) | High-speed native TTS with custom voice enumeration and pitch/rate adjustments. |
| **Speech-to-Text** | Android `SpeechRecognizer` | Built-in offline/online speech transcription with `RecognizerIntent.EXTRA_LANGUAGE = "th-TH"`. |
| **Voice Recording** | `MediaRecorder` & `MediaPlayer` | Lightweight local recording buffer allowing learners to listen back to their own pronunciation attempts. |
| **Sound Effects** | `SoundPool` | Low-latency instant audio feedback for keystroke taps, success chimes, and error buzzers. |

---

## 2. Architectural Blueprint

```
+-----------------------------------------------------------------------------------+
|                                 PRESENTATION LAYER                                |
|                                                                                   |
|  [MainActivity] (Compose Navigation Host)                                         |
|      ├── FlashcardsScreen          (FlashcardsViewModel)                         |
|      ├── KeyboardPracticeScreen    (KeyboardPracticeViewModel)                   |
|      ├── SpeechLabScreen           (SpeechLabViewModel)                          |
|      └── SettingsScreen            (SettingsViewModel)                           |
|                                                                                   |
|  [Reusable Compose UI Components]                                                 |
|      ├── KedmaneeKeyboardView      (Custom Interactive On-Screen Keyboard)        |
|      ├── WordBreakdownModal        ("i" Info Sheet with Spatial Tone Grammar)    |
|      ├── VoiceComparisonPlayer     (Side-by-side Native vs User audio player)    |
|      └── SrsBadge                  (Visual mastery level indicator)              |
+-----------------------------------------------------------------------------------+
                                       ▲ │
                                State  │ │ User Actions / Events
                                Flows  │ ▼
+-----------------------------------------------------------------------------------+
|                                   DOMAIN LAYER                                    |
|                                                                                   |
|  [Use Cases & Business Logic]                                                     |
|      ├── EvaluateKeystrokeUseCase   (Checks expected char vs Kedmanee input)      |
|      ├── CalculateToneRuleUseCase   (Decomposes Thai syllable into tone formula)  |
|      ├── UpdateWordSrsUseCase       (Applies modified Leitner spaced repetition)  |
|      ├── GetNextStudyBatchUseCase   (Fetches queue prioritizing due/mistake words)|
|      └── CompareSpeechResultUseCase (Calculates phonetic/string similarity)       |
+-----------------------------------------------------------------------------------+
                                       ▲ │
                                       │ ▼
+-----------------------------------------------------------------------------------+
|                                   DATA LAYER                                      |
|                                                                                   |
|  [Repositories]                                                                   |
|      ├── WordRepositoryImpl        (Reads bundled JSON asset, caches in Room)     |
|      ├── SrsRepositoryImpl         (Tracks interval, ease factor, review dates)   |
|      └── UserVoiceRepositoryImpl   (Manages temporary WAV/AAC user voice caches)  |
|                                                                                   |
|  [Data Sources]                                                                   |
|      ├── Room Database (WordEntity, ReviewHistoryEntity, MistakeLogEntity)        |
|      ├── AssetCurriculumReader (Parses `thai_curriculum_100.json`)                |
|      ├── AudioEngine (TtsManager, SoundPoolFxPlayer, AudioRecorderManager)        |
|      └── PreferencesDataStore (User settings)                                     |
+-----------------------------------------------------------------------------------+
```

---

## 3. Package Structure

```kotlin
com.example.learnthai/
├── audio/
│   ├── AudioRecorderManager.kt       // Records user's voice to cache file
│   ├── SoundFxManager.kt              // Low-latency SoundPool (buzz, tap, correct)
│   └── TtsManager.kt                  // TextToSpeech wrapper with voice discovery
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── MistakeDao.kt
│   │   │   └── WordDao.kt
│   │   └── entity/
│   │       ├── MistakeEntity.kt
│   │       └── WordEntity.kt
│   ├── model/
│   │   ├── KedmaneeKey.kt             // Keyboard layout data model
│   │   ├── ToneBreakdown.kt           // Syllable decomposition data model
│   │   └── WordItem.kt
│   └── repository/
│       ├── SrsRepository.kt
│       └── WordRepository.kt
├── domain/
│   ├── srs/
│   │   ├── LeitnerEngine.kt           // SRS interval calculation
│   │   └── SrsLevel.kt
│   └── tone/
│       ├── ConsonantClass.kt          // Mid, High, Low
│       ├── ToneRuleEvaluator.kt       // Explains how tone was formed
│       └── ToneType.kt                // Low, Mid, Falling, High, Rising
├── ui/
│   ├── components/
│   │   ├── AudioPlayButton.kt
│   │   ├── KedmaneeKeyboardView.kt    // Custom 4-row interactive keyboard
│   │   ├── WordBreakdownDialog.kt     // Phonics "i" inspector
│   │   └── SrsProgressIndicator.kt
│   ├── flashcards/
│   │   ├── FlashcardCard.kt
│   │   ├── FlashcardsScreen.kt
│   │   └── FlashcardsViewModel.kt
│   ├── keyboardpractice/
│   │   ├── KeyboardPracticeScreen.kt
│   │   ├── KeyboardPracticeViewModel.kt
│   │   └── SessionSummaryDialog.kt
│   ├── speechlab/
│   │   ├── SpeechLabScreen.kt
│   │   ├── SpeechLabViewModel.kt
│   │   └── AudioComparisonWidget.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   ├── navigation/
│   │   ├── AppNavigation.kt
│   │   └── ScreenRoute.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── MainActivity.kt
```

---

## 4. Gradle Dependency Strategy

To support Jetpack Compose, Room, and Modern Media APIs, `app/build.gradle.kts` requires:
- `androidx.compose.ui:ui` & `androidx.compose.material3:material3`
- `androidx.navigation:navigation-compose:2.8.+`
- `androidx.room:room-runtime:2.6.+` & `androidx.room:room-ktx:2.6.+`
- `androidx.datastore:datastore-preferences:1.1.+`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.+`
- `com.google.code.gson:gson:2.11.+` (for JSON asset parsing)
