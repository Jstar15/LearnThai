# 12. Story "Say It" — per-card transcription check

> **Status**: **Implemented and verified on device** — 13 unit tests green; idle → listening → score → retry confirmed, including a 100 % capture driven by TTS playback.
> **Goal**: on the **back** of every story card, let the learner record themselves saying the Thai line, transcribe it with the same `th-TH` recogniser Speech Lab uses, and show a **percentage match** with retry. Words only — no tone scoring.
>
> **v2 (this pass)**: the score is now *explainable* — the target and the transcript are coloured character by character, unmatched words are listed as "focus on" chips, and the transcript is large enough to read.

---

## 1. Why this shape

Speech Lab already has the two halves of this: `SpeechRecognizer` with `th-TH` (transcription) and `PitchDetector` (tones). The story check deliberately uses **only transcription**:

- A learner reading a line aloud wants to know *"did the words come out right?"* — that's a text comparison, not a pitch contour.
- Tone feedback already exists in Speech Lab and needs a quiet room + headphones; adding it to every card would make the habit heavy.
- The card is a reading exercise: the Thai sentence is right there, so ASR has a fair target.

The copy will say so explicitly: **"Words only — tones aren't scored here."**

## 2. UX (back of card only)

```
┌──────────────────────────────────────────┐
│ SAY IT                    words only     │
│                                          │
│  🎤  Tap and say the line                │  ← idle
│                                          │
│  ◌ Listening…                            │  ← recording
│                                          │
│  86%                                     │  ← result
│  You said: สวัสดีดองกี้ วันนี้…            │
│  [ Try again ]                           │
└──────────────────────────────────────────┘
```

- Sits below the Listen/Slow buttons, as the **last block** on the back of the card.
- Percentage colour: **≥ 85 green**, **60–84 amber**, **< 60 red**.
- Result also shows what the recogniser heard, so a wrong score is explainable.
- Retry re-arms the mic; the previous score is cleared while listening.
- No auto-start: the learner taps when ready (auto-listening would fight the TTS playback).

## 3. Scoring

`TranscriptScorer` (pure Kotlin, unit-tested):

1. Normalise both strings — drop whitespace and punctuation, keep Thai letters, vowels **and tone marks** (stripping marks would turn กิน into กน).
2. Levenshtein distance on the normalised code points.
3. `score = (1 − distance / max(len)) × 100`, clamped 0–100, exact match short-circuits to 100.

This is deliberately forgiving of spacing (ASR inserts spaces) and harsh on missing syllables, which matches how a listener would judge it.

## 4. States & errors

| State | Trigger | UI |
|---|---|---|
| `IDLE` | card flipped to back | mic button + hint |
| `LISTENING` | mic tapped and permission granted | spinner + "Listening…" (partial transcript updates live) |
| `DONE` | `onResults` | percentage, transcript, retry |
| `ERROR` | recogniser unavailable / `onError` | friendly message + retry |

Error mapping is copied from Speech Lab (`ERROR_NO_MATCH`, `ERROR_SPEECH_TIMEOUT`, `ERROR_NETWORK`, `ERROR_INSUFFICIENT_PERMISSIONS`, …) so both features speak the same language.

## 5. Lifecycle & permissions

- `RECORD_AUDIO` is already in the manifest; the check requests it lazily via `rememberLauncherForActivityResult`, same as Speech Lab.
- One `SpeechRecognizer` per card-back composition, created in `DisposableEffect`, `destroy()`ed on flip-away — so leaving the card silently releases the mic.
- Before listening, TTS is stopped (`onStopSpeaking`) so the app never talks over the learner's attempt.
- If `SpeechRecognizer.isRecognitionAvailable()` is false, the block shows a disabled message instead of a dead button.

## 6. Files

| File | Change |
|---|---|
| `domain/speech/TranscriptScorer.kt` | new — normalisation + similarity |
| `ui/stories/components/StorySpeechCheck.kt` | new — mic UI + recogniser lifecycle |
| `ui/stories/components/StoryFlipCard.kt` | back side renders the check; new `onStopSpeaking` param |
| `ui/stories/StoryPlayerScreen.kt` | passes `{ ttsManager.stop() }` |
| `app/src/test/.../TranscriptScorerTest.kt` | new — exact / near / empty / spacing cases |

## 7. Out of scope

- Tone/pitch scoring on story lines (Speech Lab owns that).
- Storing per-card attempt history (progress store stays about position/completion).
- Pre-baked audio comparison.

## 8. Verification

`TranscriptScorerTest` via `gradlew testDebugUnitTest` → **8 tests, 0 failures**:

| Case | Expectation |
|---|---|
| exact line | 100 |
| recogniser adds spaces | 100 |
| story text has punctuation | 100 |
| tone/vowel marks are preserved | normalised strings differ |
| one wrong character | 80–99 |
| unrelated line | < 40 |
| empty target or heard | 0 |
| repeated input | stays 0–100 |

On-device (Android 15, th-TH offline pack present):
- Back of card shows **SAY IT** with the target Thai line inside the block (the back hides the Thai otherwise).
- Tap mic → `LISTENING` with spinner; system mic indicator appears.
- Result: **60 %** for a partial attempt (`ผมหิวแล้ว` vs `หอมมากผมหิวแล้ว`) with the transcript shown; retry re-arms the mic.
- No auto-start: the block stays `IDLE` until tapped.

**Design note added during implementation:** the target Thai line is rendered inside the check block, because the back of the card only shows English + romanisation. Without it the learner has nothing to read aloud.

---

## 9. v2 — showing *which* characters are wrong

The first version gave a number. A number tells you that you missed, not *what* you missed, so the scorer now also returns an alignment.

### Alignment

`TranscriptScorer.align(target, heard)` runs Levenshtein with a backtracking matrix and returns,
for **both** normalised strings, a `BooleanArray` of matched characters — plus a map from
normalised index back to the raw target index (so story tokens can be mapped onto it).

Characters the recogniser heard but that aren't in the target (insertions) and characters in the
target that were dropped (deletions) both surface as `false`.

### What the learner sees

| Element | Colour | Meaning |
|---|---|---|
| Target line | **amber** on a pale amber background | this character was **missed** |
| "You said" line | **red** on a pale red background | this character was **different** |
| Focus chips | red-tinted chips | the whole **words** to work on (e.g. `หอม`, `มาก`) |

- The transcript is now **18 sp** (was 10 sp) under a small "YOU SAID" label, so it is readable at arm's length.
- A one-line legend — *"red = different · yellow = missed"* — sits under the transcript.
- Missed words come from `missedTokens(target, tokens, alignment)`: a token counts as missed when any
  of its characters was unmatched, which is why the chips say the exact words to practise.
- When everything matches: green "Every word matched ✓" instead of chips.
- Colours are only applied in the `DONE` state; while idle/listening the target stays plain.

### Tests (13 total)

`TranscriptScorerTest` now also covers:
- alignment flags every character on an identical line,
- a substitution (ผัดไทย vs ผัดไท) flags the target side only,
- a truncation (สวัสดีครับ vs สวัสดี) flags the missing tail,
- `missedTokens` returns `["หอม", "มาก"]` for the truncated line and empty when perfect.

### Verification

- `gradlew testDebugUnitTest` → **13 tests, 0 failures**.
- On device: a TTS-fed attempt scored **100 %**, showing the larger transcript, legend and the
  green "Every word matched" line; the earlier partial attempt (60 % with `ผมหิวแล้ว`) is what the
  amber/red path is for. The colour runs are produced by the same `BooleanArray`s the tests assert on.
