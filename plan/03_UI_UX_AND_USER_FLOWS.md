# 03. UI/UX & User Flows

## 1. Global Navigation Architecture

The application uses Material 3 `NavigationBar` with four primary top-level destinations:

```
[Main Navigation Bar]
  ├── 📇 Flashcards        (Route: "flashcards")
  ├── ⌨️ Keyboard Practice  (Route: "practice")  <-- HERO SCREEN
  ├── 🎙️ Speech Lab         (Route: "speech_lab")
  └── ⚙️ Settings           (Route: "settings")
```

---

## 2. Screen 1: Flashcards (Vocabulary Discovery)

### User Flow & Layout
1. **Top Bar**:
   - Phase Selector Chip Carousel (`All`, `Phase 1: Anchors`, `Phase 2: Left-Shift`, `Phase 3: Stacking`, `Phase 4: Tones`, `Phase 5: Finals`).
   - Mastery progress counter (e.g. `14/20 Mastered`).
2. **Interactive Flashcard**:
   - **Front State**:
     - Large, clean Thai font (`44sp`, `Noto Sans Thai`).
     - Large speaker button with immediate audio playback.
     - Subtitle prompt: *"Tap card to reveal meaning & phonetics"*.
   - **Back State (Flipped with 3D Rotation Animation)**:
     - English translation in prominent bold typography.
     - Phonetic Romanization with tone markers (e.g. `bpai [Mid]`).
     - Consonant Class badge (e.g. `[Mid-Class]` in blue, `[High-Class]` in green, `[Low-Class]` in orange).
     - Contextual sentence with audio button.
3. **SRS Rating Bar (Bottom)**:
   - Four quick-tap buttons: `Again` (Red, relearn now ~10 min), `Hard` (Orange, 1 day), `Good` (Green, 3 days), `Easy` (Blue, 7 days). Consecutive `Good`/`Easy` ratings promote the word through 14-day and 30-day intervals until `Mastered`.
   - Tapping an SRS rating updates Room DB and slides the next card in smoothly.

---

## 3. Screen 2: Keyboard Practice (Hero Screen)

### Layout Wireframe
```
+-------------------------------------------------------------+
| [Phase 1: The Anchors]     [Progress: 7/20]     [Score: 90] |
+-------------------------------------------------------------+
|                                                             |
|   Target Prompt:  "To go"                                   |
|   Phonetic Hint:  bpai (Mid tone)                           |
|                                                             |
|   +-----------------------------------------------------+   |
|   |   [🔊]   ไ  | _                                [ ℹ️ ]|   |
|   +-----------------------------------------------------+   |
|   (Green: Correct)  (Pulsing: Active Cursor) (Ghost letters)|
|                                                             |
+-------------------------------------------------------------+
|                                                             |
|             CUSTOM INTERACTIVE KEDMANEE KEYBOARD             |
|                                                             |
|  [ _ ][ ๅ ][ / ][ - ][ ภ ][ ถ ][ ุ ][ ึ ][ ค ][ ต ][ จ ][ ข ][ ช ]... |
|  [ ๆ ][ ไ ][ ำ ][ พ ][ ะ ][ ั ][ ี ][ ร ][ น ][ ย ][ บ ][ ล ]  |
|  [ ฟ ][ ห ][ ก ][ ด ][ เ ][ ้ ][ ่ ][ า ][ ส ][ ว ][ ง ]     |
|  [Shift] [ ผ ][ ป ][ แ ][ อ ][ ิ ][ ื ][ ท ][ ม ][ ใ ][ฝ]... |
|                                                             |
+-------------------------------------------------------------+
```

### Keystroke Interaction & Error-Correction Flow
1. **Target Word Loading**:
   - The word's required `keystroke_sequence` is loaded (e.g. for `ไป`: `["ไ", "ป"]`).
   - The target prompt displays the English translation and plays the native Thai pronunciation automatically.
2. **Dynamic Key Hinting (Guided Mode)**:
   - The key for the *next required character* has a subtle glowing outline, drawing the beginner's eye without cognitive strain.
3. **Validating Keystrokes**:
   - **CORRECT KEY PRESSED**:
     - Light tactile haptic tick.
     - Pleasant subtle wooden tap sound (`SoundPool`).
     - Character fills the input box with a soft spring-bounce scale animation.
     - Target cursor advances to the next character.
   - **WRONG KEY PRESSED**:
     - Pronounced error buzz sound effect (`raw/wrong_buzz.wav`).
     - Distinct haptic vibration pattern (two quick pulses).
     - The pressed wrong key flashes crimson red and performs a quick horizontal shake animation ($ \pm 4\text{dp} $).
     - **Crucial Rule**: The wrong letter is **NOT** inserted into the text field. The learner is never left with broken text or required to hunt for the backspace key.
     - A gentle contextual banner appears at the top: *"Remember: Pre-posed vowel ไ is typed before consonant ป!"*
4. **Word Completion**:
   - Uplifting completion chime.
   - Word box highlights in vibrant green with a checkmark badge.
   - Score updates (+10 XP, + streak multiplier).
   - Auto-advances to the next word after 500ms.
5. **Session Recap & Mistake Review**:
   - At the end of a 10-word or 20-word session, a modal displays:
     - Accuracy rate (e.g., `85%`).
     - Perfect words vs. words with retry attempts.
     - **"Review Mistakes" CTA Button**: Immediately launches a focused mini-session containing *only* the words where the user made an error.

---

## 4. Letter Breakdown Modal ("i" Inspector)

Tapping the `[ℹ️]` button on any word opens a bottom sheet with an orthographic breakdown:

```
+-------------------------------------------------------------+
|  Word Breakdown: ไป (bpai) - "To go"                        |
+-------------------------------------------------------------+
|                                                             |
|  1. Spatial Layout: [Pre-posed Vowel] + [Base Consonant]   |
|     • 'ไ' (Sara Ai Mai Malai) sits to the left.             |
|     • 'ป' (Pla - Mid Class) sits to the right.              |
|                                                             |
|  2. Typing Sequence:                                        |
|     Step 1: Press 'ไ' (Kedmanee row 2, key 'w')             |
|     Step 2: Press 'ป' (Kedmanee row 4, key 'x')             |
|                                                             |
|  3. Tone Rule:                                              |
|     • Initial Consonant: ป (Mid Class)                      |
|     • Vowel: ไ (Inherent Live Vowel sound)                  |
|     • Formula: Mid Class + Live Syllable = MID TONE         |
|                                                             |
|  4. Mnemonic Memory Hook:                                   |
|     "Before you GO (ไป), your feet (ไ) must step first!"    |
|                                                             |
|  [🔊 Play Normal Speed]     [🐢 Play Slow Speed (0.75x)]    |
+-------------------------------------------------------------+
```

---

## 5. Screen 3: Speech Lab (Pronunciation & Self-Listening)

### User Flow
1. **Card Presentation**: Target Thai word shown in large font with speaker icon.
2. **Audio Prompt**: User listens to native pronunciation.
3. **Record Action**: User holds the large floating microphone button and pronounces the word in Thai.
4. **Speech Recognition**:
   - Android `SpeechRecognizer` (`th-TH`) transcribes the speech.
   - Simultaneously, `AudioRecorderManager` saves the raw microphone audio to a local cache file (`cache/user_speech.aac`).
5. **Dual Comparison Widget**:
   - If the word is mispronounced or transcribed differently:
     - **Button A: "Native Speaker"** (Plays reference TTS at crystal clear speed).
     - **Button B: "Your Attempt"** (Plays back the user's recorded voice).
   - Hearing their own voice side-by-side with the native model allows students to detect tone inflections (e.g., distinguishing a flat mid tone from a falling or rising tone).
6. **Result Badge**:
   - Exact match: Green checkmark with congratulatory chime.
   - Close/near match: Friendly prompt showing what was heard and inviting self-comparison (e.g., *"Heard: ป้า — tap native vs. your voice and try keeping your pitch flat."*). Note: the recognizer returns text only (no tone), so tone feedback is self-assessment via the dual playback widget, not automatic grading.

---

## 6. Screen 4: Settings & Sound Configuration

1. **Audio Preferences**:
   - **TTS Voice Selection**: Lists available `th-TH` engines (e.g. Google Speech Services, Samsung TTS) with sample preview.
   - **Speech Rate Slider**: Adjustable from `0.65x` (phonics slow-motion) to `1.0x` (conversational speed).
2. **Typing Scaffolding Mode**:
   - *Guided Mode (Beginner)*: Next key is highlighted on the on-screen keyboard.
   - *Challenge Mode (Intermediate)*: No key highlights; pure memory recall.
3. **Transliteration System**:
   - *Paiboon Phonics* (default; recommended for English speakers, includes explicit tone accents `à, ā, á, â, ǎ`).
   - *Thai Only* (Immersive mode).
   - *(RTGS removed: it is tone-blind and collapses ต/ท and ป/พ, which undermines this app's core distinctions.)*
4. **Tactile & Sound Feedback**:
   - Sound FX Volume slider.
   - Haptic Feedback intensity toggle.
