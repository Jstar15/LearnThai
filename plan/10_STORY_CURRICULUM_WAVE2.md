# 10. Story Curriculum Coverage: 5 or 10 More?

> **Status**: **Implemented — waves 1 and 2 (stories 6–15).** Final measured coverage: 115/300 deck words (38 %), 19/80 phase 1–4 keyboard anchors, 12→15/15 patterns.
> **Question**: is five more stories enough to make the app "bloom", or does it need ten?

---

## 1. What the current five stories actually cover

Measured with the curriculum deck (`thai_curriculum_100.json`, 300 words):

```
distinct deck words used in stories: 52 / 300   (17%)
phase  1:  2/20    phase  6: 10/20   phase 11:  2/20
phase  2:  2/20    phase  7:  5/20   phase 12:  3/20
phase  3:  3/20    phase  8:  5/20   phase 13:  1/20
phase  4:  1/20    phase  9:  9/20   phase 14:  0/20
phase  5:  7/20    phase 10:  0/20   phase 15:  2/20
```

Seven of the fifteen phases are effectively untouched, and two are at zero. Every story so far teaches *conversation*, not the **keyboard's** content: phase 1–4 words (`กา`, `ตา`, `ปา`, `จาก`, `ปาก`, `ไป`, `ใจ`, `ดี`, `ดู`, `ที่`, `พี่`, `นี่`, `รู้`) are the ones the typing drills are built on, and almost none of them appear in a sentence.

### Pattern coverage (15 core beginner patterns)

| # | Pattern | Covered by |
|---|---|---|
| P1 | question words (`ไหม`, `อะไร`, `ที่ไหน`, `ใคร`, `ทำไม`, `อย่างไร`, `กี่`) | partial — no `ใคร`, `ทำไม`, `อย่างไร`, `กี่` |
| P2 | negation (`ไม่`, `ไม่มี`, `ไม่ใช่`, `ไม่ได้`) | ✅ |
| P3 | tense/aspect (`กำลัง`, `จะ`, `แล้ว`, `เมื่อวาน`) | partial — no past |
| P4 | ability (`...ได้`, `...เป็น`) | partial — only `ได้ไหม` |
| P5 | requests/offers (`ขอ…`, `…ได้ไหม`, `ช่วย…หน่อย`) | ✅ |
| P6 | classifiers/quantity (`สองจาน`, `กี่…`, `ทุก`) | partial — one classifier |
| P7 | comparison (`กว่า`, `ที่สุด`, `เกินไป`) | ✗ (`ขึ้น` only) |
| P8 | location/direction | ✅ |
| P9 | politeness particles | ✅ |
| P10 | connectives (`และ`, `แต่`, `หรือ`, `เพราะ`, `ถ้า`) | partial — no `ถ้า` |
| P11 | time (`กี่โมง`, `ตอนเช้า`, `เมื่อวาน`) | ✗ |
| P12 | possession (`ของ`) | ✅ |
| P13 | benefactive (`ให้`) | ✗ |
| P14 | serial verbs (`ไปกิน`, `มาหา`) | partial |
| P15 | intensifiers (` ๆ`, `จัง`, `มาก`, `นิดหน่อย`) | ✅ |

**Answer: five more is not enough — but "cover the deck" is the wrong target, and ten is enough for the patterns.** Five stories buy roughly two patterns; they leave `ใคร`/`ทำไม`/`อย่างไร`, comparison, past tense, time, and benefactives untouched — exactly the things that make a learner feel fluent. Ten more, selected against a matrix, closes 12 of the 15 patterns (and Wave 2's five close the rest).

The deck number needs an honest recalibration, because it is not what I expected:

| Stories total | Deck words used (measured) | Patterns covered |
|---|---|---|
| 5 | 52 / 300 (17 %) | 9 / 15 |
| 10 | **82 / 300 (27 %)** | **12 / 15** |
| 15 | **115 / 300 (38 %)** | **15 / 15** |
| 20 (final) | **133 / 300 (44 %)** | 15 / 15 + 5 function sets |

Each 8-line scene adds only **~6 new deck words**, not 20–25, because dialogue is dominated by function words (`ไม่`, `ครับ`, `จะ`, `แล้ว`, `ที่`) and by names. That is normal and desirable for fluency — repetition is the point.

More importantly, the 300-word deck is a **typing corpus**, not a conversation corpus: it contains `ปอด` (lung), `ดุก` (fierce), `อึด` (enduring), `บอด` (blind) because those words drill specific Kedmanee keys. Forcing them into a story would make the dialogue bizarre. So "80 % deck coverage" was a bad goal. The goals that matter are:

1. **Pattern coverage** — 15/15 by story 15;
2. **High-frequency utility** — the ~120 words a beginner actually needs (pronouns, question words, `ได้`, `ต้อง`, `เพราะ`, numbers, food, transport, body, family);
3. **Keyboard anchors** — each phase 1–4 word (`กา`, `ตา`, `ปาก`, `จาก`, `ไป`, `ใจ`, `ที่`, `พี่`, `รู้`) appearing in at least one natural line, so the typing drills and the stories share vocabulary.

With those three targets, **15 stories is where the pattern set is complete**, and the final five (16–20) shift from grammar to *functions* — repair, small talk, planning, quantities, wayfinding. The full level assessment is in [11. What Level Does This Actually Take You To?](./11_WHAT_LEVEL_DOES_THIS_TAKE_YOU_TO.md).

---

## 2. Wave 1 — stories 6–10 (implemented)

| # | Story | Teaches | New pattern | Rigs |
|---|---|---|---|---|
| 6 | **Goldilocks & the Bears** | size, comparison, possession, fear | P7 `กว่า` / `ที่สุด` / `เกินไป`, `ใคร` | girl (blonde), bear ×2 |
| 7 | **Cinderella at Chatuchak** | prices, numbers, colours, bargaining | P6 numbers + classifiers, `เท่าไหร่`/`ลดได้ไหม` | girl, vendor |
| 8 | **The Three Little Pigs** | materials, weather, strength | P7 `ดีกว่า`, `แข็งแรง`, `เพราะ` | pig ×3, wolf |
| 9 | **Taxi to the Airport** | fares, distance, duration, hurry | P11 `กี่โมง`, P13 `ช่วย…หน่อย` | man, driver |
| 10 | **At the Doctor** | symptoms, medicine, instructions | P4 `ต้อง`, dosage pattern `วันละ…ครั้ง` | girl, doctor |

### Story 6 — Goldilocks & the Bears

| # | Speaker | Thai | English |
|---|---|---|---|
| 1 | Goldilocks | บ้านหลังนี้ใหญ่จัง | This house is so big! |
| 2 | Papa Bear | ใครมาที่บ้านเรา | Who came to our house? |
| 3 | Goldilocks | ข้าวต้มนี้ร้อนเกินไป | This porridge is too hot. |
| 4 | Baby Bear | ของผมหวานกว่าและอร่อยกว่า | Mine is sweeter and tastier. |
| 5 | Goldilocks | เตียงนี้แข็งที่สุด | This bed is the hardest. |
| 6 | Papa Bear | เธอนอนบนเตียงเรา | She slept on our bed. |
| 7 | Papa Bear | หนูน้อยอย่ากลัว | Little one, don't be afraid. |
| 8 | Goldilocks | ขอโทษค่ะฉันจะกลับบ้าน | Sorry — I'll go home. |

### Story 7 — Cinderella at Chatuchak

| # | Speaker | Thai | English |
|---|---|---|---|
| 1 | Cinderella | สวัสดีค่ะเสื้อตัวนี้เท่าไหร่ | Hello — how much is this shirt? |
| 2 | Vendor | สามร้อยบาทค่ะ | Three hundred baht. |
| 3 | Cinderella | แพงจังลดได้ไหมคะ | That's pricey — can you discount? |
| 4 | Vendor | ได้ค่ะสองร้อยห้าสิบบาท | Sure, two hundred and fifty. |
| 5 | Cinderella | มีสีน้ำเงินไหมคะ | Do you have it in blue? |
| 6 | Vendor | มีค่ะไซซ์อะไรดี | Yes — what size? |
| 7 | Cinderella | ไซซ์กลางค่ะฉันจะซื้อ | Medium — I'll take it. |
| 8 | Vendor | ขอบคุณค่ะจ่ายเงินตรงนี้ | Thanks, pay here. |

### Story 8 — The Three Little Pigs

| # | Speaker | Thai | English |
|---|---|---|---|
| 1 | Pig 1 | ผมจะทำบ้านจากฟาง | I'll build a house from straw. |
| 2 | Pig 2 | บ้านไม้ดีกว่า | A wooden house is better. |
| 3 | Wolf | เปิดประตู! | Open the door! |
| 4 | Pig 1 | ไม่เปิด! | Not opening! |
| 5 | Wolf | ผมจะเป่าบ้านคุณ | I'll blow your house down. |
| 6 | Pig 2 | ลมแรงมากหนาวด้วย | The wind is strong — and cold. |
| 7 | Pig 3 | บ้านอิฐไม่พัง | The brick house won't fall. |
| 8 | Wolf | บ้านนี้แข็งแรงจัง | This house is so strong! |

### Story 9 — Taxi to the Airport

| # | Speaker | Thai | English |
|---|---|---|---|
| 1 | Passenger | ไปสนามบินเท่าไหร่ครับ | How much to the airport? |
| 2 | Driver | สามร้อยบาทครับ | Three hundred baht. |
| 3 | Passenger | ไกลไหมครับ | Is it far? |
| 4 | Driver | ไม่ไกลแต่รถติดมาก | Not far, but the traffic is bad. |
| 5 | Passenger | ถึงกี่โมงครับ | What time will we arrive? |
| 6 | Driver | ประมาณครึ่งชั่วโมงครับ | About half an hour. |
| 7 | Passenger | ช่วยเร็วๆหน่อยครับผมรีบ | Please go faster — I'm in a hurry. |
| 8 | Driver | ถึงแล้วครับ | We're here. |

### Story 10 — At the Doctor

| # | Speaker | Thai | English |
|---|---|---|---|
| 1 | Doctor | เป็นอะไรครับ | What's the matter? |
| 2 | Patient | ฉันปวดหัวและไม่สบาย | I have a headache and feel unwell. |
| 3 | Doctor | เจ็บคอไหม | Does your throat hurt? |
| 4 | Patient | เจ็บนิดหน่อยค่ะ | A little bit. |
| 5 | Doctor | ต้องกินยาและดื่มน้ำมากๆ | You must take medicine and drink lots of water. |
| 6 | Patient | วันละกี่ครั้งคะ | How many times a day? |
| 7 | Doctor | วันละสามครั้งหลังอาหาร | Three times a day, after meals. |
| 8 | Patient | ขอบคุณค่ะคุณหมอ | Thank you, doctor. |

---

## 3. Wave 2 — stories 11–15 (implemented)

| # | Story | Teaches | New pattern closed |
|---|---|---|---|
| 11 | **Meeting the Family** | `นี่คือ`, `อายุเท่าไหร่`, `ทำงานที่ไหน`, `พูด…ได้ไหม` | possession/identification, age |
| 12 | **A Day in Bangkok** | `เมื่อวาน`, `ตื่น`, `กี่โมง`, `แปดโมง`, `เลิกงาน` | past time + clock time |
| 13 | **Hotel Check-in** | `มีห้องว่างไหม`, `คืนละ`, `ราคา`, `ชั้น`, `ขอ…ได้ไหม` | requests, per-unit prices |
| 14 | **First Thai Lesson** | `ทำไม`, `อย่างไร`, `อ่าน`, `ฟัง`, `เข้าใจ` | why/how questions |
| 15 | **Something's Wrong** | `ถ้า`, `ให้` (benefactive), `เกินไป`, `ครั้งหน้า` | conditionals, benefactives |

### Story 11 — Meeting the Family

| # | Thai | English |
|---|---|---|
| 1 | แม่ค่ะนี่คือต้น | Mom, this is Ton. |
| 2 | สวัสดีจ้ะต้นอายุเท่าไหร่ | Hello Ton — how old are you? |
| 3 | ยี่สิบห้าครับ | Twenty-five. |
| 4 | ทำงานที่ไหน | Where do you work? |
| 5 | ทำงานที่กรุงเทพครับ | I work in Bangkok. |
| 6 | คุณพูดภาษาไทยได้ไหม | Can you speak Thai? |
| 7 | พูดได้นิดหน่อยครับ | I can speak a little. |
| 8 | แม่ชอบเขาค่ะ | Mom likes him. |

### Story 12 — A Day in Bangkok

| # | Thai | English |
|---|---|---|
| 1 | เมื่อวานคุณทำอะไร | What did you do yesterday? |
| 2 | ตื่นตอนเช้าแล้วไปทำงาน | Woke up in the morning, then went to work. |
| 3 | เริ่มงานกี่โมงครับ | What time do you start? |
| 4 | เริ่มแปดโมงค่ะ | I start at eight. |
| 5 | แล้วเลิกงานกี่โมง | And what time do you finish? |
| 6 | หกโมงเย็นค่ะกลับบ้านแล้วนอน | Six in the evening — home, then sleep. |
| 7 | คุณเหนื่อยไหม | Are you tired? |
| 8 | เหนื่อยแต่มีความสุข | Tired — but happy. |

### Story 13 — Hotel Check-in

| # | Thai | English |
|---|---|---|
| 1 | สวัสดีครับมีห้องว่างไหม | Hello — do you have a room free? |
| 2 | มีค่ะห้องเดี่ยวหรือห้องคู่ | Yes — single or double? |
| 3 | ห้องคู่ครับราคาเท่าไหร่ | A double — how much? |
| 4 | คืนละพันสองค่ะรวมอาหารเช้า | 1,200 a night, breakfast included. |
| 5 | มีน้ำร้อนไหม | Is there hot water? |
| 6 | มีค่ะอยู่ชั้นสาม | Yes, on the third floor. |
| 7 | ขอห้องชั้นล่างได้ไหม | Could I have a room downstairs? |
| 8 | ได้ค่ะนี่คือกุญแจ | Of course — here's your key. |

### Story 14 — First Thai Lesson

| # | Thai | English |
|---|---|---|
| 1 | วันนี้ครูจะสอนภาษาไทย | Today I'm going to teach Thai. |
| 2 | ภาษาไทยยากไหมครับ | Is Thai difficult? |
| 3 | ยากนิดหน่อยแต่สนุกมาก | A little hard — but a lot of fun. |
| 4 | ทำไมคนไทยพูดเร็ว | Why do Thai people speak fast? |
| 5 | เพราะเราพูดทุกวัน | Because we speak it every day. |
| 6 | ผมอ่านภาษาไทยอย่างไร | How do I read Thai? |
| 7 | นักเรียนอ่านทีละคำแล้วฟังเสียง | Read one word at a time, then listen. |
| 8 | ผมเข้าใจแล้วขอบคุณครับ | I understand now — thank you. |

### Story 15 — Something's Wrong

| # | Thai | English |
|---|---|---|
| 1 | ขอโทษค่ะอาหารช้ามาก | Excuse me — the food is very slow. |
| 2 | ขอโทษครับเดี๋ยวมาตอนนี้ | Sorry — it's coming right now. |
| 3 | นี่ไม่ใช่ผัดไทยที่ฉันสั่ง | This isn't the Pad Thai I ordered. |
| 4 | ขอโทษครับผมเอาให้ใหม่ | Sorry — I'll bring you a new one. |
| 5 | แล้วก็เผ็ดเกินไปด้วย | And it's too spicy, too. |
| 6 | ถ้าไม่เผ็ดผมเปลี่ยนให้ | If you don't want it spicy, I'll swap it. |
| 7 | ขอบคุณค่ะแต่ต้องเร็วๆนะ | Thanks — but please make it quick. |
| 8 | ได้ครับครั้งหน้าบอกผมได้ | Of course — next time just tell me. |

**Remaining gap after 15:** phase 10 is still thin (3/20) and only 19 of 80 phase 1–4 keyboard anchors appear in scenes. The next content work should not be more stories but a **"keyboard seed" pass**: rewrite two or three lines per story to use `ใจ`, `รู้`, `พี่`, `กา`, `ดาบ`, `ปอด`-style phase 1–4 words where they naturally fit (e.g. `รู้ไหม` for `รู้`, `พี่ชาย` for `พี่`).

---

## 4. Measuring "bloom" instead of guessing

Extend `tools/validate_stories.py` with a `--coverage` report:

* distinct curriculum words used / 300, broken down per phase;
* which of the 15 patterns have at least one example (pattern tags on cards, e.g. `"patterns": ["comparison"]`);
* a "keyboard anchors" count: how many phase 1–4 words appear in stories.

Then "are we done?" is a number, not a feeling. Targets for v1.0: **15/15 patterns**, every phase represented by at least two words, and every phase 1–4 keyboard anchor used in a natural line. Deck coverage is reported for interest but **not** targeted — stories are scenes, not a dictionary.

---

## 5. Art additions for Wave 1

| Rig / prop | Used by | Notes |
|---|---|---|
| `bear` (new) | Goldilocks | round ears, muzzle; papa / mama / baby via scale + colour |
| `pig` (new) | Three Little Pigs | pink, snout, ears; three colours |
| `doctor` (new) | At the Doctor | person rig in a white coat |
| `car` prop | Taxi | simple street car, drawn behind the characters |
| `house` prop | Pigs | straw / wood / brick variants by colour |
| `barbell`? no — `desk` only when a scene asks for it | Cat story | desk is now opt-in via `prop: "keyboard"` |

Backgrounds reuse the existing four: `market`, `street`, `forest`, `room`.
