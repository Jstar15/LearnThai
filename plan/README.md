# LearnThai: Phonics, Speech & Kedmanee Typing Android App
## Master Architectural Blueprint & Implementation Plan

> **Target Audience**: Complete English-speaking beginners starting with zero knowledge of the Thai language.
> **Core Value Proposition**: Systematic, tactile training that carries a complete beginner to confident Kedmanee typing through spatial orthography decomposition, guided-to-blind practice, and phonics support — the foundational keyboard skill every Thai learner needs first.
> **Ultimate Goal**: Enable any beginner to **type Thai accurately on the Kedmanee keyboard** — words they *read* and words they *hear* from a growing learned vocabulary. Phonics and tone rules are taught in service of typing fluency, not as a separate spelling test.

---

### Master Plan Documents

1. [01. Architecture & Tech Stack](./01_ARCHITECTURE_AND_TECH_STACK.md)
   - Jetpack Compose, Material 3, Clean Architecture + MVVM.
   - Room Database (SRS Leitner/SM-2, Session Tracking, Mistake Queues).
   - Android TextToSpeech (`th-TH`), SpeechRecognizer, AudioRecord/MediaPlayer.
   - Navigation Compose and State Management.

2. [02. Pedagogical Framework & Curriculum](./02_PEDAGOGICAL_FRAMEWORK_AND_CURRICULUM.md)
   - Cognitive Load Theory applied to Thai Abugida script.
   - The 7-Stage Spatial Orthography Scaffold (Anchors $\rightarrow$ Left-Shift $\rightarrow$ Vertical Stack $\rightarrow$ Tone Stack $\rightarrow$ Irregular Finals $\rightarrow$ Mutating Vowels $\rightarrow$ Clusters & Silent Markers).
   - Tone Algebra: Deconstructing Class (Mid/High/Low) + Syllable Length + Tone Mark.
   - Core Dataset reference (`thai_curriculum_100.json`).

3. [03. UI/UX & User Flows](./03_UI_UX_AND_USER_FLOWS.md)
   - **Flashcards Deck**: Tap-to-flip cards, audio pronunciation, transliteration toggles.
   - **Keyboard Practice (The 3 Modes)**:
     - *Mode 1: Guided Phonics Typing* (visual hints + glowing target keys).
     - *Mode 2: Phonics Translation Typing* (transliteration to Thai).
     - *Mode 3: BLIND AUDIO DICTATION* (Hear audio only $\rightarrow$ type on Kedmanee).
   - **Letter Breakdown Modal ("i" icon)**: Spatial breakdown, consonant class, tone rule breakdown, and mnemonic hooks.
   - **Speech Lab**: Native audio playback, Thai speech recognition (`RecognizerIntent`), side-by-side user voice vs. native audio player.
   - **Settings**: Voice speed slider (0.75x–1.0x), engine picker, Romanization preferences.

4. [04. Custom Kedmanee Interactive Keyboard](./04_KEDMANEE_KEYBOARD_AND_INPUT_ENGINE.md)
   - On-screen Kedmanee keyboard widget (4 rows, normal + shift layers).
   - Key State Machine: `IDLE`, `EXPECTED_HINT`, `CORRECT_PRESSED`, `ERROR_BUZZ`.
   - Assistive scaffolding modes: Guided (next key illuminated) vs. Challenge (blind typing).
   - Auto-Shift guidance mechanics for upper-row letters (`ธ`, `ณ`, `ฯ`, `ษ`).

5. [05. Audio, Speech & Voice Fidelity](./05_AUDIO_AND_SPEECH_RECOGNITION.md)
   - Android TTS `Locale("th", "TH")` setup and voice configuration.
   - Google Speech Services neural voice prioritization.
   - AudioRecord PCM loop to record user speech and play it back for pitch comparison.
   - Handling Android permissions (`RECORD_AUDIO`) gracefully.

6. [06. Implementation Roadmap & Verification](./06_IMPLEMENTATION_ROADMAP_AND_MILESTONES.md)
   - Phased milestones from Gradle dependency configuration to end-to-end testing.
   - Verification gates and testing checklist.

7. [07. Typing Fluency: Gaps, Edge Cases & the Roadmap to "Type What You Hear"](./07_COMPREHENSIVE_MASTERY_GAPS_AND_SOLUTIONS.md)
   - The Homophone Ambiguity (why 5 letters share /kh/ and 6 share /th/).
   - The Invisible & Mutating Vowels (Hidden short /o/, Mai Han-Akat, and Sara เออ $\rightarrow$ เ-ิ).
   - Consonant Clusters, False Clusters (ทร/ศร/จร/สร) & 2nd-letter tone stacking rules.
   - Silent markers (Karan `์`) and conversational repeaters (Mai Yamok `ๆ`).
   - The 3-Mode Practice Ladder (Guided $\rightarrow$ Translation $\rightarrow$ Blind Audio Dictation).
