# LearnThai: Auditory-to-Motor Thai Kedmanee Keyboard Trainer

> **"Type What You Hear"** — A specialized mobile flight simulator for the Thai language. LearnThai bypasses conventional academic grammar memorization by training direct auditory-to-motor muscle memory on the standard Thai **Kedmanee** keyboard.

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Tests](https://img.shields.io/badge/Tests-Passing%20(100%25)-brightgreen.svg?style=flat)](https://github.com/)

---

## 1. Why LearnThai?

Thai has **44 consonants, 32 vowels, and 4 tone marks** mapped across an un-spaced keyboard layout with vowels wrapping around consonants in 4 spatial directions (left, right, above, and below).

Traditional apps rely on multiple-choice bubbles or drag-and-drop tiles, leaving learners helpless when facing an actual phone keyboard. **LearnThai treats typing as an auditory reflex**:

$$\text{Audio Prompt (TTS)} \longrightarrow \text{Mental Glyph} \longrightarrow \text{Tactile Finger Reflex (Kedmanee)}$$

With deliberate practice of 1 hour daily, a beginner can achieve blind auditory typing fluency on common Thai words within **3 to 4 weeks**.

---

## 2. Core Pillars & Features

### ⌨️ 1. Interactive Kedmanee Keyboard Practice
* **Full Mobile Kedmanee Engine**: Complete 4-row layout with dynamic normal and shift layers. Consonants, vowels, and tone marks are color-coded by phonics category.
* **Guided Mode**: Real-time next-key glowing guidance and error buzzer feedback.
* **Blind Audio Dictation Mode**: Toggle "Blind" to blank out the target word. The learner relies solely on the spoken audio cue (replayable with the speaker button) to type each character. Keys are revealed only upon correct selection.
* **Letter Breakdown Dialog ("i" icon)**: Tap to inspect consonant classes (High, Mid, Low), tone rules, keystroke decomposition, and contextual example sentences with an **inline audio speaker icon** for sentence playback.

### 🎙️ 2. Speech Lab & Dynamic Tone Curve Comparison
* **Elastic DTW Tone Scoring**: Evaluates user pitch against native tone contours using mean-centered **Sakoe-Chiba Dynamic Time Warping** ($w = 4$), Pearson contour correlation, and direction alignment — providing realistic, encouraging tone accuracy (0–100%) rather than brittle Euclidean distance.
* **Visual Pitch Graphs**: Side-by-side pitch curves comparing your vocal inflection against native Thai tone standards (Mid, Low, Falling, High, Rising).
* **Dual Evaluation Modes**: Switch seamlessly between **Tone Mode** (pitch contour analysis) and **Transcribe Mode** (Google Speech Services STT recognition).
* **Randomized Session Decks**: Words are shuffled per phase with a live progress counter (`${current + 1} / 20`), matching the Keyboard Practice experience.

### 🎭 3. Story Mode — Interactive Dialogue Scenes
* **Data-driven scenes**: A story is one JSON file plus art in `assets/stories/`; adding a new story needs **no Kotlin changes**.
* **Flip card per line**: the front shows the speaker's scene art with their Thai line in a speech bubble and native TTS; flip it to get English, romanisation, a tappable word-by-word breakdown, and a "why it's not obvious" tip.
* **Play scene**: reads every line in order, auto-advancing the cards, and ends on a completion card.
* **Say it**: on the back of every card, record yourself reading the line and get a percentage match from Thai speech recognition — with the target and transcript coloured character by character (amber = missed, red = different) and "focus on" chips for the words that didn't land. Words only; tone scoring lives in Speech Lab.
* **Curriculum-linked breakdowns**: tokens that exist in the 300-word deck open the existing tone/class dialog.
* **Ships with twenty scenes** (8 lines each), each teaching one pattern or one conversational function: ordering food, truth & negation, directions & body parts, invitations, keyboard words, comparatives, prices & bargaining, materials & weather, transport & time, symptoms & dosage, family & first meetings, daily routine, hotel requests, why/how questions, complaints, repair phrases, small talk, making plans, market quantities, and asking strangers for directions.
* **Authoring tools**: `tools/generate_story_art.py` (deterministic Pillow scene art) and `tools/validate_stories.py` (token integrity, asset references, vocabulary coverage, new-word budget).

### 🗂️ 4. Flashcards & Leitner Spaced Repetition (review deck)
* Opens from **Lessons → "Review vocabulary (SRS)"** — the Stories tab takes the old flashcard slot, but no SRS progress is lost.
* **3D Flip Cards**: Front shows the Thai script; tap to flip and reveal English meaning, phonetic transliteration, and tone rules.
* **Leitner Algorithm**: Automated review intervals (10 min, 1 day, 3 days, 7 days, 14 days, 30 days) based on rating (`Again`, `Hard`, `Good`, `Easy`).
* **Progress Persistence**: User SRS levels, mistake counts, and review timestamps persist locally across app restarts.

### 📚 5. High-Frequency Thai Curriculum (Phases 1–15)
A curated **300-word dataset** with **0 duplicates** structured into 15 progressive phases (20 words each):

| Phases | Linguistic Focus | Example Words |
|---|---|---|
| **Phase 1** | Linear Orthographic Anchors (Right vowels & mid consonants) | กา, ตา, ปา, อา, จาก, ปาก, ดาบ |
| **Phase 2** | Left-Shift Leading Vowels (ไ-, โ-, เ-, แ-) | ไป, ใจ, ใบ, โต, เอา, เจอ, แปด |
| **Phase 3** | Vertical Top & Bottom Vowels (-ิ, -ี, -ึ, -ื, -ุ, -ู) | ดี, ตี, ปี, ดู, ปู, อีก, จุด, ดึก |
| **Phase 4** | Tone Mark Stacking (Mai Ek `่`, Mai Tho `้`) | ที่, พี่, นี่, นี้, รู้, ขึ้น, ซื้อ, ดื่ม |
| **Phase 5** | Multi-Syllable Essentials & Silent Markers | อาหาร, ผล, การ, ควร, คุณ, รถ, ภาพ |
| **Phases 6–10** | High-Frequency Pronouns, Verbs, Adjectives & Common Phrases | ผม, ฉัน, กิน, นอน, ชอบ, สวย, ร้อน, สวัสดี, ขอบคุณ, กลับบ้าน |
| **Phase 11** | Daily Activities, Directions & Transportation | นั่ง, ยืน, ขับรถ, สถานี, สนามบิน, รถไฟฟ้า, ทางออก, เลี้ยวซ้าย, ตรงไป |
| **Phase 12** | Dining, Ordering & Food Categories | หิวน้ำ, สั่งอาหาร, เช็คบิล, เผ็ด, ข้าวมันไก่, ผัดไทย, ต้มยำกุ้ง, หมู, ไก่ |
| **Phase 13** | Shopping, Bargaining, Colors & Utensils | ราคา, เท่าไหร่, ลดได้ไหม, ถูก, เงินสด, บัตรเครดิต, สีแดง, ถุง, ขวด, จาน |
| **Phase 14** | Family, Relationships & Social Roles | พ่อ, แม่, ลูก, พี่ชาย, น้องสาว, คุณตา, เด็ก, ครู, นักเรียน, ตำรวจ |
| **Phase 15** | Time Increments, Calendar Days & Essential Greetings | ชั่วโมง, นาที, ตอนเช้า, ตอนเที่ยง, ตอนเย็น, วันจันทร์, ไม่เป็นไร, ช่วยด้วย, ลาก่อน |

---

## 3. Architecture & Tech Stack

```
com.example.learnthai/
├── audio/                   # PitchDetector (YIN + DTW tone scoring), AudioRecorderManager
├── data/
│   ├── model/               # WordEntity, WordItem, PitchComparison, ToneLabel, Story*
│   └── repository/          # WordRepository (words + SRS), StoryRepository, StoryProgressStore
├── domain/
│   └── tone/                # ToneRuleEvaluator, ConsonantClass, ToneType
├── tts/                     # TtsManager (Android TextToSpeech th-TH)
├── assets/stories/          # index.json, <story>.json, art/*.webp (content, not code)
├── tools/                   # generate_story_art.py, validate_stories.py
└── ui/
    ├── components/          # WordBreakdownDialog, PitchVisualizer, StoryImage (asset art loader)
    │   └── keyboard/        # KedmaneeLayout, KedmaneeKeyboard (Interactive 4-row layout)
    ├── flashcards/          # FlashcardsScreen (SRS review deck, opened from Lessons)
    ├── keyboardpractice/    # KeyboardPracticeScreen (Guided & Blind modes, seed shuffle)
    ├── speechlab/           # SpeechLabScreen (DTW pitch visualization, STT transcribe)
    ├── stories/             # StoriesScreen, StoryPlayerScreen, flip card / token sheet / tip card
    └── settings/            # SettingsScreen (TTS speed, story playback; opened from the top-right gear)
```

* **Jetpack Compose**: 100% declarative UI with Material 3 theming.
* **Kotlin Coroutines & StateFlow**: Reactive, unidirectional state flow from repository to composables.
* **YIN Algorithm & Dynamic Time Warping (DTW)**: Low-latency DSP pitch tracking and contour correlation.
* **Android Speech Services**: Native `th-TH` Text-To-Speech engine and `RecognizerIntent` speech recognition.
* **Screen Lock**: Strict Portrait-only orientation locked via `MainActivity.kt`.

---

## 4. Development & Build Setup

### Prerequisites
* **Android Studio**: Iguana (2023.2.1) or newer
* **Android SDK**: API 34 (Android 14) minimum SDK 26 (Android 8.0)
* **JDK**: Version 17+

### Running Targeted Unit Tests
```powershell
# Run all unit tests
.\gradlew.bat testDebugUnitTest

# Run specific test suites
.\gradlew.bat testDebugUnitTest --tests com.example.learnthai.ExampleUnitTest
.\gradlew.bat testDebugUnitTest --tests com.example.learnthai.audio.PitchDetectorToneScoreTest
.\gradlew.bat testDebugUnitTest --tests com.example.learnthai.data.model.PhonicsAndKeyboardLogicTest
```

### Assembling & Installing Debug APK
```powershell
# Assemble debug APK
.\gradlew.bat assembleDebug

# Install to connected device (e.g. Google Pixel 7)
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Launch app
adb shell am start -n com.example.learnthai/.MainActivity
```

---

## 5. License
Developed with software craftsmanship principles for language learners worldwide. All rights reserved.
