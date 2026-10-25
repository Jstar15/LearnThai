# 07. Typing Fluency: Gaps, Edge Cases & the Roadmap to "Type What You Hear"

## 1. The Core Question
> *"Is this everything a person needs to know to type Thai confidently? If they use this app, will they be able to type the words they read and hear?"*

**The Honest Linguistic Reality**:
With the initial 100 words and 5 phases, a learner will master **spatial keyboard mechanics**, **vowel orientations**, and **basic tone stacking**. 
Typing a word they **read** (or a word they **hear from a learned vocabulary**) is fully achievable. Typing *any arbitrary* new word by ear is **not** guaranteed, because Thai spelling is not fully derivable from sound (homophony) — so we scope the benchmark accordingly and teach the *elimination strategies* rather than promising a deterministic sound→spelling rule. The four hurdles below are the orthographic edge cases a fluent typist must still learn:

---

## 2. The 4 Missing Hurdles to "Hear-and-Type" Fluency

```
                       CAN YOU HEAR & TYPE ANY THAI WORD?
                                       │
      ┌────────────────┬───────────────┴───────────────┬────────────────┐
      ▼                ▼                               ▼                ▼
[1. The Homophone  [2. Mutating &               [3. Consonant      [4. Silent Markers
     Ambiguity]        Invisible Vowels]             Clusters]          & Thai Symbols]
 5 letters for /kh/  /o/ has NO key (คน)         Tone sits on       Karan (์) makes
 6 letters for /th/  /a/ becomes (ั) (กัน)       2nd letter (ปลา)   letters silent (สัตว์)
 4 letters for /s/   /er/ becomes (เ-ิ) (เกิด)                      Mai Yamok (ๆ) repeat
```

---

### Hurdle 1: The Homophone Ambiguity (Multiple Letters per Sound)
In English, if you hear /b/, it is almost always `b`.
In Thai, sounds are shared across different consonant classes:
- Sound **/th/** can be typed using **6 different letters**: `ท`, `ธ`, `ถ`, `ฐ`, `ฑ`, `ฒ`.
- Sound **/kh/** can be typed using **5 letters**: `ค`, `ข`, `ฆ`, `ฃ`, `ฅ`.
- Sound **/s/** can be typed using **4 letters**: `ส`, `ศ`, `ษ`, `ซ`.
- Sound **/ph/** can be typed using **4 letters**: `พ`, `ผ`, `ภ`, `พ`.

**The Solution Needed in App**:
1. **Tone Reverse-Engineering**:
   - If the learner hears a **Rising tone** on a live syllable without a tone mark (e.g. `khao` / rising), it **MUST** be High-class `ข` (`เขา`), not Low-class `ค`!
   - We must teach the learner to **listen to the tone to eliminate 50% of candidate consonants**.
2. **Contextual Word Disambiguation**:
   - Words cannot be dictated in a vacuum. The app must provide micro-context (e.g., *"khao (to eat) -> ข้าว"* vs *"khao (mountain/he) -> เขา"* vs *"khao (white) -> ขาว"*).

---

### Hurdle 2: Mutating & Invisible Vowels (สระลดรูป & สระเปลี่ยนรูป)
English speakers expect that every vowel sound they hear corresponds to a vowel key on the keyboard. In Thai, vowels mutate or vanish completely:

1. **The Invisible Short /o/ (สระโอะลดรูป)**:
   - When a syllable has an initial and final consonant with a short /o/ sound, **NO vowel is typed at all**!
   - You hear: *"khon"* $\rightarrow$ You type: `ค` + `น` (`คน`).
   - You hear: *"phom"* $\rightarrow$ You type: `ผ` + `ม` (`ผม`).
   - You hear: *"nok"* $\rightarrow$ You type: `น` + `ก` (`นก`).
   - *If an app does not teach this, the learner will frantically search the keyboard for an 'o' key!*
2. **The Mutating Short /a/ (สระอะเปลี่ยนรูป)**:
   - Open syllable: `ก` + `ะ` = `กะ`.
   - Closed syllable with final: The `ะ` jumps to the top as Mai Han-Akat `ั`!
   - You hear: *"kan"* $\rightarrow$ You type: `ก` + `ั` + `น` (`กัน`).
   - You hear: *"wan"* $\rightarrow$ You type: `ว` + `ั` + `น` (`วัน`).
3. **The Mutating /er/ (สระเออเปลี่ยนรูป)**:
   - You hear: *"koet"* $\rightarrow$ The vowel `เ-อ` mutates into `เ-ิ`: You type `เ` + `ก` + `ิ` + `ด` (`เกิด`).
   - You hear: *"doen"* $\rightarrow$ You type `เ` + `ด` + `ิ` + `น` (`เดิน`).

---

### Hurdle 3: Consonant Clusters & Stacked Tone Placement
Many common words begin with twin consonants (คำควบกล้ำ e.g., `ปร`, `ปล`, `ตร`, `กล`, `ขว`, `คร`):
- `ปลา` (*plaa* - fish): `ป` + `ล` + `า`.
- `ใคร` (*khrai* - who): `ใ` + `ค` + `ร`.

**The Critical Typing Gotcha**:
When a word has a consonant cluster AND a top vowel or tone mark, **the vowel and tone mark MUST be typed over the SECOND consonant, NOT the first!**
- Example: `เปลี่ยน` (*plian* - change):
  - Correct typing order: `เ` $\rightarrow$ `ป` $\rightarrow$ `ล` $\rightarrow$ `ี` $\rightarrow$ `่` $\rightarrow$ `ย` $\rightarrow$ `น`.
  - If a beginner types `ี` over `ป`, the text breaks!

**False clusters (ต้องระวัง):** some letter pairs look like clusters but produce a single, unrelated sound, breaking the "type what you hear" instinct:
   - `ทร` → /s/ in ทราย, ทราม, แทรก (but /tʰ/ in ทรง, ทราบ — context-dependent).
   - `ศร` → /s/ (สร้าง), `จร` → /j/ (จริง), `สร` → /s/ (สรุป).
These must be memorized as whole spellings, not sound-mapped.

---

### Hurdle 4: Silent Markers & Thai Punctuation
Real Thai typing requires specific keyboard symbols that have no sound of their own:
1. **Thanthakhat / Karan (`์`)**:
   - Cancels the sound of a letter (e.g. `สัตว์` - animal, `โทรศัพท์` - telephone, `วันเสาร์` - Saturday).
   - In Kedmanee: Located on Shift + `c` (key `g`).
2. **Mai Yamok (`ๆ`)**:
   - The repetition mark used in almost every conversational sentence (e.g. `มากๆ` - very much, `ช้าๆ` - slowly, `เร็วๆ` - quickly).
   - In Kedmanee: Key `q` (top left).
3. **Paiyannoi (`ฯ`)**:
   - Abbreviation marker (e.g. `กรุงเทพฯ` - Bangkok).

---

## 3. What We Need to Add to the Plan

To guarantee that any student can truly **hear a word and type it on the keyboard**, we expand the curriculum with **Three New Learning Dimensions**:

### Dimension A: The 3-Mode Practice Ladder
Instead of only one typing mode, we establish a 3-tier progressive training pipeline:

```
[Mode 1: Guided Phonics Typing] (Current Plan)
  • See Thai word + English meaning.
  • Kedmanee keys illuminate with glowing hints.
  • Goal: Finger-to-key physical orientation.
       │
       ▼
[Mode 2: Phonics Translation Typing]
  • See English + Paiboon Romanization only (e.g., "dta-laad").
  • Keys do NOT glow.
  • Goal: Memory recall of letter-to-key mapping.
       │
       ▼
[Mode 3: BLIND AUDIO DICTATION (The Hero "Type What You Hear" Mode)]
  • NO THAI TEXT ON SCREEN — audio + English gloss only (the gloss disambiguates homophones like /kʰâːw/).
  • Audio speaks: "ดื่ม" (deum) — screen shows only "to drink".
  • User must:
      1. Identify the initial sound /d/ -> Mid class 'ด'.
      2. Identify the long vowel /eu/ -> Top vowel 'ื'.
      3. Identify tone: Low tone -> Requires Mai Ek '่'.
      4. Identify final sound /m/ -> 'ม'.
      5. Type sequence: [ด] -> [ื] -> [่] -> [ม].
  • Tapping [Hint] reveals only the tone formula or consonant class pill.
```

---

### Dimension B: The Advanced Orthography Module (Phase 6 & Phase 7 — optional extensions)
The v1 dataset (`thai_curriculum_100.json`) implements Phases 1–5. Phases 6–7 below are **optional extensions** (adding 40 more words) and are not yet present in the dataset:

- **Phase 6: The Invisible & Mutating Vowels (20 words)**:
  - Invisible `โอะ`: คน (person), ผม (I/hair), นก (bird), รถ (car), ลม (wind), หมด (out of).
  - Mutating `อะ` $\rightarrow$ `ั`: วัน (day), กัน (each other), ฟัน (teeth), วัด (temple), ขับ (drive).
  - Mutating `เออ` $\rightarrow$ `เ-ิ`: เดิน (walk), เกิด (born), เปิด (open), เกิน (exceed).
- **Phase 7: Clusters, Silent Markers & Repeaters (20 words)**:
  - True clusters: ปลา (fish), ใคร (who), ใกล้ (near), เปลี่ยน (change), เพราะ (because).
  - False clusters: ทราย (sand, ทร→/s/), จริง (true, จร→/j/), สร้าง (build, สร→/s/).
  - Karan silent mark: สัตว์ (animal), เสาร์ (Saturday), สัปดาห์ (week), เบอร์ (number).
  - Mai Yamok: มากๆ (very), บ่อยๆ (often), จริงๆ (really).

---

### Dimension C: The Kedmanee "Finger Drill" & Keyboard Explorer
Before or alongside typing full words:
- An interactive **Kedmanee Key Explorer**:
  - Tap any key on the keyboard to hear its consonant name (e.g. tap `ก` $\rightarrow$ hear *"Gor Gai"*).
  - Displays whether it is Mid, High, or Low class with color coding (Blue, Green, Orange).
  - Shows Shift-layer equivalents clearly.
