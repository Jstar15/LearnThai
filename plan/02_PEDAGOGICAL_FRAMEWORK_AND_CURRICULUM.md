# 02. Pedagogical Framework & Curriculum

## 1. The Second Language Acquisition (SLA) Challenge

English and Western alphabets follow a strict 1-dimensional, left-to-right phonemic structure ($C + V + C$). Learners read and type sequentially:
$$\text{C} \rightarrow \text{A} \rightarrow \text{T}$$

In contrast, **Thai is an Abugida with a 2-Dimensional Spatial Canvas**:
- **Pre-posed vowels** appear to the *left* of the consonant ($V + C$), but are pronounced *after* ($C + V$).
- **Top vowels** sit directly *above* the consonant ($Y$-axis $+1$).
- **Bottom vowels** sit directly *below* the consonant ($Y$-axis $-1$).
- **Tone marks** stack on a second tier *above the top vowel* ($Y$-axis $+2$).
- **Tone values** are not arbitrary: they are a mathematical product of **Consonant Class (Mid/High/Low)** $\times$ **Syllable Type (Live/Dead)** $\times$ **Tone Mark**.

### The Traditional Pedagogy Failure
Most Thai apps dump 44 consonants, 32 vowels, and 5 tone rules on the student at once. This triggers **cognitive overload**, leading to:
1. **Phonological Fossilization**: The learner guesses tones based on English intonation patterns.
2. **Typing Paralysis**: The learner cannot find letters on the 44-key Kedmanee keyboard because they do not know whether to press the vowel or consonant first.

---

## 2. The Progressive Scaffolding Model (5 Core Phases, +2 Optional Extensions)

To guide a student with zero knowledge from Day 1 to typing fluency, our curriculum isolates orthographic and spatial variables across five strict core phases. (Phases 6–7 — invisible/mutating vowels and clusters/silent markers — are optional extensions detailed in doc 07, not yet present in the v1 dataset.)

```
[Phase 1: The Anchors]
  • Only Mid-class consonants (ก, จ, ด, ต, บ, ป, อ)
  • Simple Right-side vowels (า, อ)
  • Direction: Pure left-to-right (1D)
  • Tones: Natural Mid/Low tones, ZERO tone marks
       │
       ▼
[Phase 2: The Left-Shift]
  • Introduces Pre-posed vowels (เ, แ, โ, ใ, ไ)
  • Spatial Shift: Vowel is typed BEFORE consonant, pronounced AFTER
  • Mid-class consonants only
       │
       ▼
[Phase 3: Vertical Stacking]
  • Introduces Y-axis Top vowels (ิ, ี, ึ) and Bottom vowels (ุ, ู)
  • Typing Mechanic: Consonant typed FIRST, then vertical vowel
  • Zero tone marks
       │
       ▼
[Phase 4: Tone Stacking (3-Tier)]
  • Introduces Mai Ek (่) and Mai Tho (้)
  • First Low/High-class consonants appear here (they are required to make tone marks meaningful)
  • Typing Mechanic: Strict Sequence: Consonant -> Vowel -> Tone Mark
  • Teaches vertical layering without syntax crash
       │
       ▼
[Phase 5: Class Complexities & Irregular Finals]
  • Introduces High and Low class consonants (Tone shifts)
  • Introduces Irregular Finals (ร, ล, ณ pronounced as /n/; ถ as /t/; พ as /p/)
  • Full spatial mastery
```

---

## 3. Detailed Phase Breakdown & Typographical Rules

### Phase 1: The Anchors (20 Words)
* **Rule**: Mid-class consonants (ก, จ, ด, ต, บ, ป, อ) + Right-side vowels (า, อ).
* **Typing Rule**: Natural left-to-right typing (identical to English).
* **Sample Words**: กา (kettle), ตา (eye), ปา (throw), ออก (exit), บอก (tell), ดาบ (sword).
* **Pedagogical Objective**: Build confidence. Teach the basic Kedmanee key positions for high-frequency mid-class letters and the right-hand vowel `า` (Key `k`) and `อ` (Key `d`).

### Phase 2: The Left-Shift (20 Words)
* **Rule**: Mid-class consonants + Pre-posed vowels (เ, แ, โ, ใ, ไ).
* **Typing Rule**: The vowel must be pressed FIRST, then the consonant.
* **Sample Words**: ไป (go - `[ไ, ป]`), ใจ (heart - `[ใ, จ]`), โต (grow - `[โ, ต]`), เอา (want - `[เ, อ, า]`).
* **Pedagogical Objective**: Break the Western reflex of typing the consonant first when the vowel sound follows.

### Phase 3: Vertical Stacking (20 Words)
* **Rule**: Top vowels (ิ, ี, ึ) and Bottom vowels (ุ, ู). No tone marks.
* **Typing Rule**: Press base consonant $\rightarrow$ press vertical vowel $\rightarrow$ press final consonant.
* **Sample Words**: ดี (good - `[ด, ี]`), ดู (watch - `[ด, ู]`), ปิด (close - `[ป, ิ, ด]`), ตึก (building - `[ต, ึ, ก]`).
* **Pedagogical Objective**: Master the $Y$-axis key assignments on the upper number row and shift keys (e.g. `ี` on `u`, `ิ` on `b`, `ู` on `^`, `ุ` on `6`).

### Phase 4: Tone Stacking (20 Words)
* **Rule**: Multi-tier vertical stacking with Mai Ek (่) and Mai Tho (้).
* **Typing Rule**: Base Consonant $\rightarrow$ Top/Bottom Vowel $\rightarrow$ Tone Mark.
* **Sample Words**: ที่ (at - `[ท, ี, ่]`), พี่ (older sibling - `[พ, ี, ่]`), นี้ (this - `[น, ี, ้]`), ซื้อ (buy - `[ซ, ื, ้]`), ดื่ม (drink - `[ด, ื, ่, ม]`).
* **Pedagogical Objective**: Prevent the common beginner error of pressing the tone mark before the vowel, which corrupts Thai text rendering.

### Phase 5: Class Complexities & Irregular Finals (20 Words)
* **Rule**: High and Low class consonants and irregular Mae Kon/Kot/Kop endings.
* **Sample Words**:
  - อาหาร (food - final ร pronounced as `/n/`).
  - ผล (result - final ล pronounced as `/n/`).
  - รถ (car - final ถ pronounced as `/t/`).
  - ข้าว (rice - High class ข + Mai Tho = Falling tone).
  - หมอ (doctor - Silent leading ห shifts low-class ม to rising tone).
* **Pedagogical Objective**: Transition from basic phonetic reading to real-world Thai orthographic conventions.

---

## 4. Tone Algebra Breakdown for Learners

In the "i" Breakdown modal, learners see the exact mathematical formula of the word's tone:

$$\text{Tone} = f(\text{Consonant Class}, \text{Vowel Length}, \text{Final Consonant}, \text{Tone Mark})$$

### Example Explanations Shown to User:
1. **Word**: กา (`gaa`)
   - Initial: `ก` (Mid Class)
   - Vowel: `า` (Long Live Vowel)
   - Tone Mark: None
   - **Result**: $\text{Mid} + \text{Live} = \mathbf{Mid\ Tone}$
2. **Word**: ที่ (`thee`)
   - Initial: `ท` (Low Class)
   - Vowel: `ี` (Top Vowel)
   - Tone Mark: `่` (Mai Ek)
   - **Result**: $\text{Low Class} + \text{Mai Ek} = \mathbf{Falling\ Tone}$
3. **Word**: ข้าว (`khaao`)
   - Initial: `ข` (High Class)
   - Tone Mark: `้` (Mai Tho)
   - **Result**: $\text{High Class} + \text{Mai Tho} = \mathbf{Falling\ Tone}$
