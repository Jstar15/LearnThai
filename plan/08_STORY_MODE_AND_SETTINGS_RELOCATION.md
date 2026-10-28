# 08. Story Mode & Settings Relocation

> **Status**: **Implemented** (v1 shipping) — see §14 for the deltas between this plan and the build.
> **Scope**: (1) move Settings off the bottom bar into a small top-right gear; (2) replace the Flashcards tab with an interactive **Stories** mode; (3) add a data-driven story format, an art pipeline, and a validation toolchain so new stories can be added without touching UI code.

---

## 1. Goals & Non-Goals

### Goals
1. **Settings out of the tab bar.** A single small gear icon in the app's top-right corner becomes the only entry point. The bottom bar drops to four tabs.
2. **Stories replace Flashcards** in the tab bar. Tapping the tab opens a library of short, dialogue-based scenes.
3. **Each line of a story is a flip card**: front = illustration + Thai sentence + native TTS (as if that character is speaking to you); back = English meaning, sentence romanization, word-by-word breakdown, and a "why it works this way" tip.
4. **Reuse the existing vocabulary deck** (300 words, 15 phases) — sentence tokens cross-reference `WordEntity`, and tapping a known token can open the existing `WordBreakdownDialog` (class + tone formula).
5. **Content is data, not code.** A story is one JSON file + a folder of images. No Kotlin changes to add story #2, #3, #N.
6. **Each story teaches one thing.** Structured "useful talk" topics (ordering food, directions, bargaining, taxi, hotel, doctor) plus fairy-tale skins.
7. **First story ships: "Shrek & Donkey — street food"**, 8 lines, beginner-safe.

### Non-Goals (this iteration)
- Voice recording / pronunciation scoring inside stories (Speech Lab already owns that).
- Branching dialogue trees, quizzes, or fill-in-the-blank variants.
- Online content download / CDN. Everything is bundled and offline.
- Deleting the SRS system — the flashcard deck just loses its tab (see Decision **D1**).

---

## 2. Navigation Changes

### 2.1 Before → After

```
BEFORE                                    AFTER
┌──────────────────────────────┐          ┌──────────────────────────────┐
│  (screen content)            │          │ (screen content)      [⚙]  │  ← small gear, top-right
│                              │          │                              │
│                              │          │                              │
├──────────────────────────────┤          ├──────────────────────────────┤
│ Lessons │ Cards │ ⌨ │ 🎙 │ ⚙ │          │ Lessons │ Stories │ ⌨ │ 🎙 │
└──────────────────────────────┘          └──────────────────────────────┘
```

### 2.2 Bottom bar (4 tabs)

| Order | Tab | Route | Icon |
|---|---|---|---|
| 1 | Lessons | `lessons` | `Icons.Default.AutoStories` (unchanged) |
| 2 | **Stories** | `stories` | `Icons.Default.TheaterComedy` (or `MenuBook`) |
| 3 | Keyboard | `practice` | `Icons.Default.Keyboard` |
| 4 | Speech Lab | `speech_lab` | `Icons.Default.Mic` |

`Screen.Settings` keeps its route but is removed from `navigationItems`.

### 2.3 The gear

- Implemented as a **custom 44 dp top bar** in the shared `Scaffold(topBar = { ... })`, not a `TopAppBar` (avoids a second title next to every screen's own in-content header).
- Contents: a `Row` that is empty except for a 32 dp `IconButton` (`Icons.Default.Settings`, 18–20 dp icon, `MaterialTheme.colorScheme.onSurfaceVariant`, subtle `surfaceVariant` circle behind it) at `Alignment.CenterEnd` with 12 dp end padding.
- Insets: `Modifier.windowInsetsPadding(WindowInsets.statusBars)` — **required** because `targetSdk = 35` forces edge-to-edge on Android 15 even though `enableEdgeToEdge()` isn't called.
- Content description `"Settings"`; keep a 48 dp touch target via `minimumInteractiveComponentSize` so the visible icon stays small but stays hittable.
- On the Settings screen itself the gear is hidden; the screen renders its own header row with a back arrow (`Icons.AutoMirrored.Filled.ArrowBack`), title "Settings", and the existing cards below. System back also returns to the previous tab.
- Gear navigates with `navController.navigate(Screen.Settings.route)` (a push, not a tab switch).

### 2.4 Tab-selection edge case

When the story player (`story/{storyId}`) is open, the **Stories** tab must stay highlighted:

```kotlin
val isSelected = currentRoute == screen.route ||
    (screen == Screen.Stories && currentRoute?.startsWith("story") == true)
```

### 2.5 Decision D1 — what happens to Flashcards?

| Option | Effect | Recommendation |
|---|---|---|
| **A. Keep it reachable** | Stories takes the tab; a compact "Review vocabulary (SRS)" pill at the top of the Lessons screen opens `FlashcardsScreen` as a pushed route. SRS progress stays alive and the 300-word deck still gets practiced. | ✅ **Recommended** (~15 lines, no data loss) |
| B. Remove entirely | Delete `FlashcardsScreen.kt`, the `flashcards` route, SRS UI. Smallest surface, but the Leitner intervals and mistake stats have no consumer. | Only if you never use SRS |
| C. Keep both tabs | 6 tabs → cramped labels on 360 dp screens. | ✗ |

---

## 3. Screen 1: Stories Library (`stories`)

```
┌────────────────────────────────────────────┐
│  Story Time                          [⚙]   │
│  Short scenes. Real Thai. One lesson each. │
├────────────────────────────────────────────┤
│ ┌────────────────────────────────────────┐ │
│ │ [ cover art: ogre + donkey at a stall ]│ │
│ │  ── scrim gradient ──                  │ │
│ │  🍜 Ordering street food               │ │
│ │  Shrek & Donkey at the Night Market    │ │
│ │  Beginner · 8 lines · ▓▓▓▓░░░░ 5/8     │ │
│ └────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────┐ │
│ │ [ cover art: red hood at a crossroads ]│ │
│ │  🧭 Asking for directions              │ │
│ │  Little Red Riding Hood Gets Lost      │ │
│ │  Beginner+ · 8 lines · new             │ │
│ └────────────────────────────────────────┘ │
└────────────────────────────────────────────┘
```

- `LazyColumn` of story cards, sorted by `order` then `level`.
- Card: 16:9 cover `Image` (asset) + bottom scrim gradient + title (Thai subtitle optional) + two chips: **teaching tag** (`teaches_label`) and **level**; footer shows `n lines` and either a progress bar (`lastCard + 1 / size`) or a `NEW` badge. Completed stories show a check.
- Tap → `story/{id}`.
- Empty/error state: if `index.json` is missing or malformed, show a friendly "No stories installed" card instead of crashing (validated by tooling, but never trust assets at runtime).
- Continue-where-you-left-off: if `lastCard > 0` and not completed, add a thin "Continue at line N" strip on the card.

---

## 4. Screen 2: Story Player (`story/{storyId}`)

### 4.1 Layout

```
┌────────────────────────────────────────────┐
│ [←]  Shrek & Donkey        ● ● ● ○ ○ ○ ○ ○ │   ← dots = lines
├────────────────────────────────────────────┤
│ ┌────────────────────────────────────────┐ │
│ │  ┌ Shrek ────────────────┐             │ │
│ │  │                        │            │ │
│ │  │      [ SCENE ART ]     │            │ │
│ │  │   speaker lit, others  │            │ │
│ │  │   dimmed behind        │            │ │
│ │  │                        │            │ │
│ │  └────────────────────────┘            │ │
│ │  ╭──────────────────────────────────╮  │ │
│ │  │ สวัสดีดองกี้ วันนี้เราจะกินอะไรกัน │  │ │  ← speech bubble, Thai 26–30 sp
│ │  ╰──────────────────────────────────╯  │ │
│ │              [🔊]   Tap to flip  ⟳     │ │
│ └────────────────────────────────────────┘ │
│                                            │
│  [ ◀ Prev ]   [ ▶ Play scene ]   [ Next ▶ ]│
└────────────────────────────────────────────┘
```

**Front (listening side).** Scene image fills the card's upper area; the speaker's name in a coloured pill (per-character colour from the story JSON); the Thai line in a speech bubble with a tail pointing toward the speaker; a speaker button; "Tap card to flip". Audio auto-plays when the page settles (toggleable, see §4.5).

**Back (study side).** Vertically scrollable:
1. English translation (bold, ~20 sp).
2. Romanization of the whole sentence (14 sp, muted).
3. **Word breakdown**: `FlowRow` of token chips — `Thai` (16 sp bold) over `roman` (10 sp) over `gloss` (10 sp, muted). Tapping a chip opens the `TokenSheet` (gloss, grammar note, and — if the token matches the curriculum — a "Tone & class details" button that reuses `WordBreakdownDialog`).
4. **Tip card** ("Why it's not obvious") — tertiary container, lightbulb icon, 1–3 sentences.
5. `new words` chips (≤ 3 per card) and a `+ Add to review` action that bumps the SRS level of matching curriculum words (reuses `WordRepository.updateSrsRating(word, GOOD)` — cheap and consistent).
6. Replay buttons: normal (0.85x) and slow (0.65x).

### 4.2 Flip mechanics

- Reuse the proven approach from `FlashcardsScreen`: `animateFloatAsState(0f..180f)`, `graphicsLayer { rotationY; cameraDistance = 12f * density }`, render front when `rotation <= 90f`, un-mirror back with `rotationY = 180f`.
- 400 ms `tween`, eased.
- **Per-card state**: `flippedIds: SnapshotStateMap<String, Boolean>` (or a `rememberSaveable` set keyed by card id), so swiping to another line never inherits a flipped state and swiping back restores it.
- Tap anywhere on the card flips; the speaker button and token chips consume the tap.
- Respect `LocalAccessibilityManager` reduce-motion: if animations are disabled, flip instantly (no rotation) and rely on a crossfade.

### 4.3 Paging & gestures

- `HorizontalPager` (Foundation, stable in Compose 1.7 / BOM 2024.10.01) — swipe left/right moves between lines; drag does **not** flip.
- `rememberPagerState { cards.size }`; dots + `line i of n` label; `Prev`/`Next` buttons mirror pager moves.
- Images: prefetch the next/previous page's bitmap (`LaunchedEffect(pagerState.currentPage)` pre-decodes ±1) so swiping is instant.
- Vertical scroll lives inside the card only on the back side; the pager owns horizontal drag, so no conflict.

### 4.4 Play-scene (auto-advance)

`Play scene` runs a coroutine: speak line → wait `max(1.8 s, estimated speech duration + 0.8 s)` → advance page → repeat until the last line, then show the **completion card**:

```
┌──────────────────────────────────────┐
│  จบแล้ว!  Scene complete             │
│  You practiced: ordering street food │
│  New words: 8 · Lines: 8             │
│  [ Read again ]   [ Back to stories ]│
└──────────────────────────────────────┘
```

Speech-duration estimate: `thai.length * 90 ms / rate`, clamped 1.2–6 s. (TTS exposes no reliable duration for `QUEUE_FLUSH`; `UtteranceProgressListener.onDone` could be wired later — noted as a refinement.)

Stop conditions: page change by the user, back navigation, or the screen leaving composition (`DisposableEffect` → `ttsManager.stop()`).

### 4.5 Audio

- Uses the existing `TtsManager.speak(text, rate)` — `th-TH`, 0.85 default, 0.65 for slow.
- Auto-speak on page settle is **on by default** (the story is audio-first), with a small toggle in Settings → "Story playback: auto-speak new lines".
- Per-card optional pre-baked clip: schema field `audio` (asset path). If present, play via `MediaPlayer` instead of TTS — lets you drop in native recordings later with zero code change. Authored as `null` for now.

### 4.6 Accessibility

- The whole card is one semantics node: `stateDescription = "Thai side" | "English side"`, `customActions = [Speak, Flip card]`.
- Front: `contentDescription = "Shrek says: <Thai>"`; back: English first, then chips (chips are individually readable).
- Min touch target 48 dp on all icon buttons; Thai text uses `lineHeight` ≥ 1.35× for stacked tone marks; no fixed `maxLines` on the bubble (Thai can break mid-word — acceptable, ICU dictionary breaking handles most cases).

---

## 5. Data Model

### 5.1 File layout

```
app/src/main/assets/stories/
├── index.json                          ← library manifest (order, ids, list metadata)
├── shrek_donkey_streetfood.json        ← one file per story
└── art/
    ├── shrek_streetfood_cover.webp
    ├── shrek_streetfood_01.webp … 08.webp
```

Images live in **assets**, not `res/drawable`, deliberately: `BitmapFactory` + `AssetManager` means adding a story needs **zero Kotlin changes** (no `R.drawable.*` registry), and one shared loader composes with an `LruCache`.

### 5.2 `index.json`

```json
{
  "schema_version": 1,
  "stories": [
    {
      "id": "shrek_donkey_streetfood",
      "file": "shrek_donkey_streetfood.json",
      "title_en": "Shrek & Donkey at the Night Market",
      "title_th": "ชเร็คกับดองกี้ที่ตลาดกลางคืน",
      "teaches_label": "Ordering street food",
      "teaches_key": "food_order",
      "level": 2,
      "cover": "art/shrek_streetfood_cover.webp",
      "order": 1,
      "tags": ["dialogue", "polite"]
    }
  ]
}
```

### 5.3 Story file

```json
{
  "schema_version": 1,
  "id": "shrek_donkey_streetfood",
  "title_en": "Shrek & Donkey at the Night Market",
  "title_th": "ตลาดกลางคืน",
  "teaches_label": "Ordering street food",
  "teaches_detail": "Order politely, say what you want, and understand the vendor's reply.",
  "level": 2,
  "phases": [6, 7, 12, 13],
  "aim": "Order two plates of Pad Thai, 'not spicy', and understand 'just a moment'.",
  "characters": [
    { "id": "shrek",  "name_en": "Shrek",  "name_th": "ชเร็ค",  "color": "#4E7A2E", "art_rig": "ogre" },
    { "id": "donkey", "name_en": "Donkey", "name_th": "ดองกี้", "color": "#8A8F98", "art_rig": "donkey" },
    { "id": "auntie", "name_en": "Auntie", "name_th": "ป้า",    "color": "#C2622E", "art_rig": "vendor" }
  ],
  "extra_words": [
    { "thai": "เลี้ยง", "roman": "líang", "gloss": "to treat someone (to a meal)" },
    { "thai": "ใจดี",  "roman": "jai-dii", "gloss": "kind / good-hearted" }
  ],
  "cards": [ /* see §5.6 */ ]
}
```

### 5.4 Card schema

| Field | Type | Required | Purpose |
|---|---|---|---|
| `id` | string | ✅ | Stable flip/progress key |
| `speaker` | character id | ✅ | Who is talking (pill + bubble alignment + lit art) |
| `listener` | character id | — | Who they're talking to (dimmed in art; future reactions) |
| `image` | asset path | ✅ | Scene art; missing → Compose placeholder |
| `thai` | string | ✅ | Natural Thai, **no spaces** |
| `roman` | string | ✅ | Whole-sentence transliteration |
| `english` | string | ✅ | Natural English, not word-for-word |
| `tokens` | array | ✅ | Curated segmentation (see §5.5) |
| `tip` | string | — | "Why it's not obvious" explanation |
| `new_words` | string[] | — | Tokens the learner hasn't seen (validator caps at 3) |
| `register` | `"casual"｜"polite"` | — | Badge + future filter |
| `audio` | asset path | — | Optional pre-baked clip; TTS when null |
| `focus` | string | — | e.g. `"want + verb"`, `"classifier"`, `"polite particle"` |

### 5.5 Tokens

```json
{
  "thai": "ขอ",
  "roman": "khǎaw",
  "gloss": "may I have / please",
  "note": "Softer than สั่ง; literally 'request'.",
  "match": "ขอ"
}
```

- `concat(tokens.thai) == card.thai` becomes a **hard invariant** checked by the authoring tool and (cheaply) at load time in debug builds. Any drift means the breakdown lies about the sentence.
- `match` (optional) is the exact string to look up in `WordRepository` for the tone/class dialog. Omit for function words not in the deck.
- `.` `,` `!` `?` attach inside the preceding token by convention (`ครับ!`) so chips never render punctuation alone.
- Tokens display the way the ear hears them, not the dictionary spelling: e.g. `จริงๆ` stays one token even though it is a repeated word.

### 5.6 Example card (line 6)

```json
{
  "id": "sfs_06",
  "speaker": "shrek",
  "listener": "auntie",
  "image": "art/shrek_streetfood_06.webp",
  "thai": "ขอผัดไทยสองจานไม่เผ็ดครับ",
  "roman": "khǎaw phàt-thai sǎawng jaan mâi phèt khráp",
  "english": "Two plates of Pad Thai, not spicy, please.",
  "focus": "classifier",
  "register": "polite",
  "new_words": ["ขอ"],
  "tip": "Thai doesn't need 'of'. Put the number straight onto the noun with its classifier — จาน (plate). 'ไม่' goes before the adjective it flips: ไม่เผ็ด = not spicy.",
  "tokens": [
    { "thai": "ขอ",     "roman": "khǎaw",    "gloss": "may I have", "note": "ขอ + noun = polite request" },
    { "thai": "ผัดไทย", "roman": "phàt-thai", "gloss": "Pad Thai",   "match": "ผัดไทย" },
    { "thai": "สอง",    "roman": "sǎawng",    "gloss": "two" },
    { "thai": "จาน",    "roman": "jaan",      "gloss": "plate (classifier)", "match": "จาน" },
    { "thai": "ไม่",    "roman": "mâi",       "gloss": "not" },
    { "thai": "เผ็ด",   "roman": "phèt",      "gloss": "spicy", "match": "เผ็ด" },
    { "thai": "ครับ",   "roman": "khráp",     "gloss": "polite particle (male)" }
  ],
  "audio": null
}
```

---

## 6. Kotlin Layer

```
data/model/StoryModels.kt
    StoryIndexFile, StorySummary
    Story, StoryCharacter, StoryCard, StoryToken
    StoryProgress (lastCard, completed, plays, updatedAt)

data/repository/StoryRepository.kt
    loadIndex(): StorySummary[]        (cached, Dispatchers.IO)
    loadStory(id): Story               (cached by id)
    validate(story): List<String>      (debug-only token concat + ref checks)

data/repository/StoryProgressStore.kt
    story_progress.json in filesDir    (same pattern as WordRepository.UserProgressRecord)
    progressFlow: Flow<Map<String, StoryProgress>>
    markCardShown(storyId, index), markCompleted(storyId), reset(storyId)

ui/stories/StoriesScreen.kt
ui/stories/StoryPlayerScreen.kt
ui/stories/components/StoryFlipCard.kt
ui/stories/components/StoryTokenSheet.kt
ui/stories/components/StoryTipCard.kt
ui/components/StoryImage.kt            (asset loader + LruCache + placeholder)
tools/generate_story_art.py            (Pillow)
tools/validate_stories.py              (Python authoring checks)
```

Notes:
- `StoryRepository` takes `Context` and returns nullable/`Result`-ish values — the library must never crash on a bad asset.
- Progress writes are debounced (500 ms) to avoid churn on every swipe.
- No new Gradle dependencies. **No Coil.** Asset bitmaps are decoded with `BitmapFactory` + `inSampleSize` and cached in an `LruCache<String, ImageBitmap>` (≈16 MB), exposed as `@Composable StoryImage(name, contentDescription, modifier)`.
- Load target width capped at 1080 px (`inSampleSize` from card width × density) so 1024-wide art costs ~2 MB per decoded page, and only ±1 page is kept.

---

## 7. Art Pipeline

### 7.1 Why programmatic art

There is no image-generation service wired into this project, and the repo already has a working Python/Pillow asset pipeline (`generate_icons.py`). So: a deterministic **flat storybook illustration generator** gives consistent, license-clean art for every card, regenerable in seconds, with no external calls.

Style contract: flat vector-ish, 3–4 colours per scene + one warm accent, rounded shapes, no gradients on characters, soft 8 % drop shadows, **no baked-in text** (all Thai/English is a Compose overlay → correct fonts, tones, and translations stay editable).

### 7.2 `tools/generate_story_art.py`

- Palette + rigs: `draw_ogre(facing, pose)`, `draw_donkey(facing, pose)`, `draw_vendor(...)`, plus prop helpers (`stall`, `wok`, `plate`, `steam`, `moon`, `signboard`).
- Scene recipes per card: character left/right, which one is "lit" (speaker), prop, mood (`happy`, `hungry`, `generous`, `surprised`), time of day (night market = indigo sky + warm lanterns).
- Reads the story JSON, so **art is generated from the same file that drives the app** — `python tools/generate_story_art.py --story shrek_donkey_streetfood`.
- Output: `assets/stories/art/*.webp` at **1024×683 (3:2)**, quality 82, target 60–110 KB each; cover at 1024×576 (16:9).
- Deterministic (fixed seed, no randomness) so re-running never churns the diff.

### 7.3 Card-by-card art brief — Story 1

| # | Speaker | Scene |
|---|---|---|
| cover | — | Wide night market, lanterns, ogre + donkey walking in, title space at bottom |
| 01 | Shrek | Ogre waving at donkey between two food stalls |
| 02 | Donkey | Donkey pointing at a pad thai plate, other hand patting an empty pocket, sad eyebrow |
| 03 | Shrek | Ogre hand on chest, generous grin, coin sparkle |
| 04 | Donkey | Donkey with heart eyes, hands clasped |
| 05 | Auntie | Vendor behind a cart with a wok, friendly wai |
| 06 | Shrek | Ogre holding up two fingers, pointing at the menu board |
| 07 | Auntie | Vendor nodding, wok in hand, steam |
| 08 | Donkey | Donkey leaning in, sniffing steam lines, drool, plate steaming |

### 7.4 Fallback & swap path

- If an image is missing or fails to decode, `StoryImage` renders a deterministic gradient (seeded by card id) + the speaker's initial in a circle + the speaker's name. A story is always readable, even unillustrated.
- To replace generated art with commissioned/AI art: drop files with the same names into `assets/stories/art/`. No code change. (Optionally keep `art/_generated/` for Pillow output and `art/_final/` for swaps, selected by a manifest flag.)

### 7.5 IP note

"Shrek" and "Donkey" are DreamWorks characters. For a personal build this is fine; if this app is ever published, rename to original look-alikes (e.g. **"Yai the Ogre" / "Donkey"**) and keep the generated art deliberately generic (green ogre + grey donkey, no film-specific costumes, no logos). The data format makes the rename a text edit in one JSON file, not a code change.

---

## 8. Content Plan

### 8.1 Story 1 — "Shrek & Donkey at the Night Market" (8 lines)

Teaches: **ordering street food + polite particles + `อยาก` + verb + negation**. Male particles `ครับ` (Shrek, Donkey) vs female `ค่ะ/คะ` (Auntie) appear naturally.

| # | Speaker | Thai | English | Tip |
|---|---|---|---|---|
| 1 | Shrek | สวัสดีดองกี้ วันนี้เราจะกินอะไรกัน | Hi Donkey! What are we eating today? | `กัน` at the end = "…together", makes it chatty |
| 2 | Donkey | ผมอยากกินผัดไทย แต่ผมไม่มีเงิน | I want Pad Thai, but I have no money. | `อยาก` + verb, no "to"; `ไม่มี` = not have |
| 3 | Shrek | ไม่เป็นไร ผมเลี้ยงเอง | No worries — my treat. | `เอง` = "myself"; `เลี้ยง` = to treat someone |
| 4 | Donkey | ขอบคุณมาก คุณใจดีจริงๆ | Thank you so much, you're really kind. | `ใจดี` = heart + good; `ๆ` repeats to intensify |
| 5 | Auntie | ยินดีต้อนรับค่ะ จะสั่งอะไรดีคะ | Welcome! What would you like to order? | `ค่ะ` vs `คะ` — statement vs question particle |
| 6 | Shrek | ขอผัดไทยสองจานไม่เผ็ดครับ | Two plates of Pad Thai, not spicy, please. | Number + classifier; `ไม่` before the adjective |
| 7 | Auntie | ได้ค่ะ รอสักครู่ | Sure — just a moment. | `ได้` = "can/OK"; `สักครู่` = a moment |
| 8 | Donkey | หอมมาก ผมหิวแล้ว | It smells so good, I'm hungry now. | `แล้ว` = a change of state, "now/already" |

Word coverage: ~16 of the sentence tokens already exist in the 300-word deck (`สวัสดี`, `วันนี้`, `กิน`, `ผม`, `อยาก`, `ผัดไทย`, `แต่`, `มี`, `ไม่เป็นไร`, `ขอบคุณ`, `คุณ`, `สั่ง`, `ดี`, `จาน`, `เผ็ด`, `หิว`). The validator reports the exact number and lists `extra_words` (≈14 function words: `เรา`, `จะ`, `อะไร`, `กัน`, `ไม่มี`, `เงิน`, `เลี้ยง`, `เอง`, `ใจดี`, `จริงๆ`, `ขอ`, `ได้`, `รอ`, `สักครู่`, `หอม`, `แล้ว`) — each gets a chip on the back side so nothing is silently unknown.

### 8.2 Backlog — one teaching focus per story

| # | Story | Teaches | Phases | Lines |
|---|---|---|---|---|
| 1 | Shrek & Donkey: night market | Ordering food, `อยาก` + verb, classifiers, ครับ/ค่ะ | 6,7,12,13 | 8 |
| 2 | Little Red Riding Hood gets lost | Directions: `ตรงไป`, `เลี้ยวซ้าย`, `ข้ามถนน`, `ใกล้/ไกล` | 11 | 8 |
| 3 | Cinderella at Chatuchak | Bargaining: `เท่าไหร่`, `ลดได้ไหม`, `แพง`, `ถูก` | 13 | 8 |
| 4 | Goldilocks & the three bears | Size & degree: `ใหญ่`, `เล็ก`, `ร้อน`, `อร่อย`, comparatives | 8,14 | 7 |
| 5 | The three little pigs | Materials & weather: `ฟาง`, `ไม้`, `อิฐ`, `ลม`, `หนาว` | 8,10 | 8 |
| 6 | The tortoise & the hare | Speed & time: `เร็ว`, `ช้า`, `ก่อน`, `หลัง`, `ชั่วโมง` | 8,15 | 7 |
| 7 | Taxi to the airport | Transport & fares: `ไป…เท่าไหร่`, `สนามบิน`, `ตรง`, `จอด` | 11,13 | 8 |
| 8 | Hotel check-in | Requests & availability: `มีห้องไหม`, `คืน`, `ราคา`, `รวม` | 13 | 8 |
| 9 | At the doctor | Symptoms: `เจ็บ`, `ปวด`, `หวัด`, `แพ้`, body parts | 8 + body | 8 |
| 10 | Meeting the family | Kinship + politeness: `พี่`, `น้อง`, `คุณตา`, `님` | 14 | 8 |

Sequencing rule: a story may only assume words from phases **≤ the phase its `level` maps to**, plus its own `extra_words` (≤ 8). The validator enforces the new-word budget so cognitive load stays sane.

---

## 9. Authoring Toolchain — `tools/validate_stories.py`

Run: `python tools/validate_stories.py` (CI-friendly, exit code 1 on error).

Checks:
1. **Schema**: required fields present, types correct, ids unique across the library.
2. **Token integrity** (hard error): `"".join(tokens.thai) == card.thai` for every card.
3. **References**: every `speaker`/`listener` exists; every `image`/`cover`/`audio` file exists; `index.json` entries point at real story files with matching ids.
4. **Coverage report** (advisory): per story, % of tokens found in `thai_curriculum_100.json`; any unmatched word must be declared in `extra_words` (otherwise error).
5. **New-word budget** (error if > 8 per story, warn if > 3 per card).
6. **Length sanity**: `thai` ≤ 60 chars, `english` ≤ 90 chars (fits the bubble at 360 dp without scrolling).
7. **Phase check**: warn when a card uses a curriculum word from a phase above `max(phases)` (catches "teaching out of order").
8. `--fix-tokens` flag: re-segments a sentence with longest-match and prints a candidate token list for authoring (does not overwrite; prints a diff).

**Add a story in 4 steps:** write JSON → run validator → `python tools/generate_story_art.py --story <id>` (or drop in art) → add one entry to `index.json`. Rebuild, done.

---

## 10. File-by-File Change List

### New
- `app/src/main/assets/stories/index.json`
- `app/src/main/assets/stories/shrek_donkey_streetfood.json`
- `app/src/main/assets/stories/art/*.webp` (cover + 8)
- `app/src/main/java/com/example/learnthai/data/model/StoryModels.kt`
- `app/src/main/java/com/example/learnthai/data/repository/StoryRepository.kt`
- `app/src/main/java/com/example/learnthai/data/repository/StoryProgressStore.kt`
- `app/src/main/java/com/example/learnthai/ui/stories/StoriesScreen.kt`
- `app/src/main/java/com/example/learnthai/ui/stories/StoryPlayerScreen.kt`
- `app/src/main/java/com/example/learnthai/ui/stories/components/StoryFlipCard.kt`
- `app/src/main/java/com/example/learnthai/ui/stories/components/StoryTokenSheet.kt`
- `app/src/main/java/com/example/learnthai/ui/stories/components/StoryTipCard.kt`
- `app/src/main/java/com/example/learnthai/ui/components/StoryImage.kt`
- `tools/generate_story_art.py`, `tools/validate_stories.py`

### Modified
- `ui/navigation/Screen.kt` — add `Stories`, `StoryPlayer` route + `routeFor(id)`; keep `Settings`.
- `MainActivity.kt` — 4-tab list (`Stories` replaces `Flashcards`); `Scaffold(topBar = { SettingsGearBar(...) })`; `story/{storyId}` destination; pass `storyRepository` + `storyProgressStore`; highlight Stories on the player route; hide gear on Settings.
- `ui/settings/SettingsScreen.kt` — header row with back arrow; add "Story playback → auto-speak new lines" switch (small `PreferenceStore` or a prefs blob in `StoryProgressStore`).
- `ui/lessons/LessonsScreen.kt` — *(Decision D1-A)* compact "Review vocabulary (SRS)" pill → `flashcards` route.
- `plan/README.md` — index entry 8.
- `plan/03_UI_UX_AND_USER_FLOWS.md` — update nav diagram + replace the Flashcards section with Stories (keep the flashcard spec under a "legacy/review deck" heading under D1-A).
- `README.md` — feature list + architecture tree (sync at the end, not per milestone).

### Deleted (only under D1-B)
- `ui/flashcards/FlashcardsScreen.kt`

---

## 11. Build Order & Verification Gates

| Milestone | Work | Gate |
|---|---|---|
| **M0 — Art spike** (S) | `generate_story_art.py` with ogre/donkey rigs; generate cover + card 01; decode on device | Image looks intentional at phone size; < 120 KB; decode < 30 ms |
| **M1 — Shell** (S) | `Screen.kt` changes, 4-tab bar, gear top bar, Settings header w/ back, Stories placeholder screen | Gear reachable on every tab; no double titles; back from Settings works; Stories tab highlighted on the player route |
| **M2 — Data** (M) | Models, `StoryRepository`, `StoryProgressStore`, validator, Story 1 JSON | Validator exits 0; app lists Story 1 with cover, progress, teaching chip |
| **M3 — Player** (L) | Pager, flip card, bubble, tokens, tip, TTS autoplay, play-scene, completion, progress persistence | Flip/swipe state correct across 8 lines; TTS silent-engine case degrades gracefully; kill + reopen resumes at the right line |
| **M4 — Art + polish** (M) | Remaining 7 images, empty/error states, TalkBack labels, small-screen pass (360 × 640) | No clipping at 360 dp; all 9 images < 120 KB (~1 MB total); fallback verified by deleting one asset |
| **M5 — Prove extensibility** (S) | Story 2 ("Red Riding Hood" — directions) authored with **no Kotlin changes** | Adding a story = JSON + art + one index line; validator green |
| **M6 — Docs** (S) | Update README, plan/03, plan/README | Docs match shipped behaviour |

Verification commands:
```powershell
python tools/validate_stories.py
python tools/generate_story_art.py --all
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 12. Risks & Open Decisions

| # | Risk / decision | Mitigation / default |
|---|---|---|
| R1 | **Thai doesn't use spaces** — sentence layout can look like a wall of text | Bubble at 26–30 sp with generous `lineHeight`; the back side always provides the token breakdown; ICU line-breaking handles most wraps |
| R2 | **TTS latency/quality varies by device** | `TtsManager` already falls back; auto-speak only after `isReady`; "Play scene" waits on state, not fixed timers; slow (0.65x) button always available |
| R3 | **Very long English translations** overflow the back side | Back side scrolls; validator caps English at 90 chars |
| R4 | **Image memory** with a pager | Cap decode at 1080 px, ±1 page prefetch, 16 MB `LruCache`, WebP q82 |
| R5 | **Token drift** between `thai` and `tokens` | Hard validator check + debug assertion at load |
| R6 | **IP** (Shrek/Donkey) | Personal build OK; look-alike rename path documented (§7.5); art is original |
| R7 | **D1** — flashcards fate | Default **A**: keep reachable from Lessons; B removes SRS UI entirely |
| R8 | Story art quality vs. time | Programmatic flat art now; swap-in path for commissioned art with no code change |

---

## 13. Acceptance Checklist

- [x] Bottom bar shows exactly **Lessons · Stories · Keyboard · Speech Lab**.
- [x] No Settings tab; a small gear at the app's top-right opens Settings from every tab; Settings has a back arrow and no gear.
- [x] Stories tab opens a library; Story 1 renders a cover, teaching chip, level, line count, and progress.
- [x] Opening Story 1 auto-speaks line 1; the card shows the scene, the speaker's name, and the Thai sentence in a bubble.
- [x] Tapping the card flips (400 ms, 3D) to English + romanization + token chips + tip; flipping back restores Thai.
- [x] Swiping changes lines without flipping; flip state is remembered per line.
- [x] Tapping a token with a `match` opens the existing tone/class breakdown dialog.
- [x] "Play scene" reads all 8 lines in order, advancing cards, ending on a completion card.
- [x] Progress persists across app restarts; "Continue at line N" appears in the library.
- [x] Deleting one art file degrades to a placeholder, not a crash.
- [x] `python tools/validate_stories.py` exits 0; adding Story 2 requires **no Kotlin edits**.

---

## 14. Implementation Notes (deltas from this plan)

Shipped and verified on a physical device (Pixel-class, 1080×2400, Android 15).

| Planned | Shipped | Why |
|---|---|---|
| Gear in a 44 dp custom top bar | Same, plus `WindowCompat.isAppearanceLightStatusBars = true` | `targetSdk 35` forces edge-to-edge; white status icons were invisible on the light background |
| Story card fills the pager (`fillMaxSize`) | Card is `fillMaxWidth().aspectRatio(0.86f)`, centered | A full-height card cropped 50 % of the 3:2 art width, cutting both characters out of frame |
| Flashcards: Decision **D1-A** | Implemented: "Review vocabulary (SRS)" chip on the Lessons header opens the existing `FlashcardsScreen` | No SRS progress is orphaned |
| Validator: error above 8 `extra_words` | Warning above 24; the hard rule is "every token is in the curriculum or declared" | Particles (`ครับ`, `ค่ะ`, `คะ`, `ไม่`, `แล้ว`…) are unavoidable in real dialogue — 18 extras in story 1 |
| Index metadata | Added `line_count` | The library needs the denominator for its progress bar without parsing every story file |
| Cover + card art 16:9 / 3:2 | 3:2 for every scene; covers reuse the cover scene | One crop-safe composition for both surfaces |
| Per-card pre-baked audio | Schema field `audio` present, `null` everywhere | Runtime `th-TH` TTS is used; the field is the hook for native recordings later |
| Per-card "Add to review" | Shipped; only bumps words whose `match` exists and `srsLevel == 0` | Avoids pushing already-studied words back to the start of the Leitner ladder |

Verification performed:
- `python tools/validate_stories.py` → 0 errors, 0 warnings (57 % of tokens already in the 300-word deck).
- `python tools/generate_story_art.py --all` → cover + 8 scenes, 165 KB total.
- `.\gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
- On device: library → player → flip → token sheet → tone/class dialog → play-scene auto-advance → completion card → progress ("Continue at line 3") → Settings via gear → SRS deck via Lessons chip.

