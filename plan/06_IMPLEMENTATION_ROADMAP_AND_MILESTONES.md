# 06. Implementation Roadmap & Milestones

## Milestone Overview

```
Milestone 1: Project Foundation & Build System Configuration
     │
     ▼
Milestone 2: Data Models, Room Database & Curriculum Seeding
     │
     ▼
Milestone 3: Audio Services, TTS & Sound Effects Engine
     │
     ▼
Milestone 4: Custom Kedmanee Interactive Keyboard Widget
     │
     ▼
Milestone 5: Core UI Screens (Flashcards, Practice, Speech Lab, Settings)
     │
     ▼
Milestone 6: Verification Gates, Unit Tests & End-to-End QA
```

---

## Detailed Milestone Task Breakdown

### Milestone 1: Project Foundation & Build System Configuration
- [ ] Configure `app/build.gradle.kts` and `gradle/libs.versions.toml`:
  - Add Jetpack Compose BOM & Material 3.
  - Add Jetpack Navigation Compose.
  - Add Room Database (`androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler`).
  - Add DataStore Preferences.
  - Add Gson for JSON asset parsing.
- [ ] Set up theme typography (`Type.kt`) with optimized Thai font line heights to prevent top and bottom tone mark clipping.
- [ ] Set up `MainActivity.kt` with Material 3 `Scaffold` and `NavigationBar`.

### Milestone 2: Data Models, Room Database & Curriculum Seeding
- [ ] Create `WordEntity.kt`, `MistakeLogEntity.kt`, and `WordDao.kt`.
- [ ] Implement `AppDatabase.kt` with Room pre-population callback reading `assets/thai_curriculum_100.json`.
- [ ] Implement `LeitnerSrsEngine.kt`:
  - Ratings: `Again` → relearn now, `Hard` → 1 day, `Good` → 3 days, `Easy` → 7 days; consecutive `Good`/`Easy` promotions step to 14 days → 30 days → Mastered.
- [ ] Write unit tests verifying JSON asset parsing and Room DAO CRUD operations.

### Milestone 3: Audio Services, TTS & Sound Effects Engine
- [ ] Implement `TtsManager.kt`:
  - Initialize `TextToSpeech` with `Locale("th", "TH")`.
  - Provide fallback intent if Thai TTS voice data is missing.
  - Implement adjustable speech rate (0.65x–1.0x).
- [ ] Implement `SoundFxManager.kt`:
  - Set up `SoundPool` for low-latency keystroke taps, success chimes, and wrong-key buzzes.
- [ ] Implement `AudioRecorderManager.kt`:
  - Handle `MediaRecorder` audio capture to local cache for user speech comparison.

### Milestone 4: Custom Kedmanee Interactive Keyboard Widget
- [ ] Build `KedmaneeLayout.kt`:
  - 4-row matrix for normal and shift layers.
  - Character-to-key coordinate lookup map.
- [ ] Implement `KedmaneeKeyButton.kt`:
  - Dynamic visual state: `IDLE`, `HIGHLIGHTED_HINT`, `CORRECT_PULSE`, `ERROR_BUZZ`.
  - Horizontal shake animation on wrong key.
- [ ] Implement Auto-Shift scaffolding logic.
- [ ] Build standalone Compose Preview for interactive testing.

### Milestone 5: Feature Screens & User Flows
- [ ] **FlashcardsScreen**:
  - Horizontal card pager with Phase filtering chips.
  - 3D card flip animation revealing transliteration and tone class badges.
  - SRS rating bar updating Room DB.
- [ ] **KeyboardPracticeScreen (Hero Screen)**:
  - Input box displaying target word, active typing cursor, and completed characters.
  - Live keystroke validation against `word.keystrokeSequence`.
  - Error buzzer on wrong key (without inserting character).
  - Word completion celebration and auto-advance.
  - Letter Breakdown bottom sheet (`WordBreakdownDialog.kt`).
  - End-of-session recap modal with "Review Mistakes" CTA.
- [ ] **SpeechLabScreen**:
  - Microphone hold-to-speak button tied to `SpeechRecognizer` (`th-TH`).
  - Dual playback audio widget (Native Reference vs User Recording).
- [ ] **SettingsScreen**:
  - Voice speed slider, Guided mode switch, Transliteration system picker.
- [ ] **Tone & Contrast Ear-Training** (P1): minimal-pair ABX drills, "which of 5 tones" quizzes, and consonant-contrast (ต/ท, ป/พ, ก/ค) and vowel-length discrimination, gated before Mode 3.
- [ ] **Real Keyboard Mode** (graduation gate): a `BasicTextField` accepting the system Thai IME for periodic mastery checks, plus a Settings helper to enable Thai in Gboard.

### Milestone 6: Verification Gates & Quality Assurance
- [ ] **Unit Tests**:
  - `ToneRuleEvaluatorTest`: Verify tone calculation logic across all 5 phases.
  - `KeystrokeValidatorTest`: Verify left-shift and vertical stacking sequence validation.
  - `LeitnerEngineTest`: Verify interval progressions and mistake demotions.
  - `KedmaneeLayoutTest`: Assert every grapheme in `thai_curriculum_100.json` resolves to a valid (row, key, shift) coordinate, and that the number-row consonants (ภ ถ ค ต จ ข ช) are mapped correctly.
- [ ] **Physical Device / Emulator Verification**:
  - Verify sound playback and low-latency audio response.
  - Verify mic permissions and speech-to-text response on Android 10+.
