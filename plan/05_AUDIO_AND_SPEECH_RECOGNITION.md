# 05. Audio, Speech & Voice Fidelity

## 1. High-Fidelity Thai Text-to-Speech (TTS) Strategy

### The "Robotic Audio" Problem
Standard default TTS engines on older Android versions often produce robotic, monotone Thai voices because Thai is a tonal language where syllable pitch contours dictate meaning. If pitch contours are flattened, comprehension drops drastically.

### Solution Architecture
1. **Engine Discovery**:
   - Query available TTS engines via `TextToSpeech.getEngines()`.
   - Prioritize `com.google.android.tts` (Google Speech Services), which contains high-fidelity Neural Network voices for Thai (`th-th-x-cfn-network` or `th-th-x-cfo-local`).
2. **Missing Voice Detection & One-Tap Installer**:
   - When initializing TTS with `Locale("th", "TH")`, check the return code:
     - `TextToSpeech.LANG_MISSING_DATA`: Launch `Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)` so the user can download Google's high-definition Thai voice pack in one tap.
3. **Phonics Rate & Pitch Tuning**:
   - Default speech rate: `0.85x` (slowed down slightly to make vowel length differences between short and long vowels discernible to English ears).
   - Slow-motion mode ("Turtle Mode"): `0.70x` for letter-by-letter sound breakdown.

---

## 2. Speech Recognition Pipeline (`th-TH`)

```
   [User presses "Hold to Speak"]
                │
                ├─────────────────────────────────────────┐
                ▼                                         ▼
   [AudioRecorderManager]                       [SpeechRecognizer]
 • Records microphone input                   • Configured with:
 • Saves to `cache/attempt.aac`                 `EXTRA_LANGUAGE = "th-TH"`
 • Prepares audio for playback                • Returns a transcribed Thai string (text only)
                │                                         │
                ▼                                         ▼
        [Audio Cache Ready]                      [Transcribed Thai String]
                │                                         │
                └───────────────────┬─────────────────────┘
                                    │
                                    ▼
                     [SpeechEvaluationEngine]
                     • Compares Target vs Transcribed
                     • Calculates Phonemic Distance
                     • Activates Dual Playback Player
```

### SpeechRecognizer limitations (design decision)

`SpeechRecognizer` returns a **transcribed string only — it does not report tone**, and its Thai model often normalizes tone errors or autocorrects to the more common word (e.g. ปา vs ป้า). Therefore the Speech Lab does **not** auto-grade tone. It uses recognition as a loose "did you say a real/close Thai word" gate, and relies on **self-assessment** (native vs. user playback) for tone/pronunciation. Optional future work: F0 pitch extraction on raw `AudioRecord` PCM to *estimate* tone contours.

### Android Permissions & Setup
- Permission required in `AndroidManifest.xml`:
  ```xml
  <uses-permission android:name="android.permission.RECORD_AUDIO" />
  ```
- Handled with Accompanist / Jetpack Compose `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`.

---

## 3. Side-by-Side Audio Comparison Player

When learning tonal languages, self-correction is impossible unless the learner can **hear what they actually sound like** in comparison with native pronunciation.

```
+-------------------------------------------------------------+
|  Result: Close!                                             |
|  Target:    ปา  [Mid Tone]                                  |
|  You Said:  ป้า  [Falling Tone]                             |
+-------------------------------------------------------------+
|                                                             |
|   [🔊 Native Reference]          [🎙️ Your Recording]        |
|     (Flat mid pitch)               (High-to-low drop)       |
|                                                             |
|  Tip: Keep your pitch completely steady like singing 'do'.  |
+-------------------------------------------------------------+
```

### Playback Implementation
- **Reference Button**: Triggers `TtsManager.speak(word.thaiWord, rate = 0.85f)`.
- **User Voice Button**: Triggers lightweight `MediaPlayer` pointing to `context.cacheDir/attempt.aac`.
- Prevents audio collisions by stopping any active stream before starting the other.
