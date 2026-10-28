# 09. Four More Stories (v1.1 content drop)

> **Status**: **Implemented** — 4 stories, 24 card scenes, 4 covers, 3 new backgrounds, 7 new art rigs; validator clean.
> **Goal**: four more 8-line scenes that reinforce what the keyboard and lessons already teach, using the 300-word deck wherever possible, plus the art rigs they need.

---

## 1. Roster

| # | Story | Teaches | Characters (art rigs) | Background |
|---|---|---|---|---|
| 2 | **Pinocchio Tells a Lie** / ปิโนคีโอโกหก | truth & lies, negation, `ขึ้น` as "getting more so", `ต้อง` (must) | ปิโนคีโอ (boy), เกปเปตโต (oldman) | night street |
| 3 | **Little Red Riding Hood** / หนูน้อยหมวกแดง | directions, body parts + size, `จัง` / `ด้วย`, `ช่วยด้วย` | หนูน้อยหมวกแดง (girl, red hood), หมาป่า (wolf) | forest |
| 4 | **Asking Her Out** / ขอเบอร์หน่อย | invitations, compliments, `ขอ…ได้ไหม`, soft refusal | หนุ่ม (man), ฝน (girl) | night street |
| 5 | **The Cat Who Wants to Type Thai** / แมวอยากพิมพ์ไทย | keyboard & lesson words: พิมพ์, ตัวอักษร, ผิด, ฝึก, ง่าย, เก่ง | แมว (cat), นุ่น (girl) | room + desk keyboard |

Each story declares its own `extra_words`; the validator requires every token to be either in the 300-word deck or declared.

---

## 2. Lines

### Story 2 — Pinocchio Tells a Lie

| # | Speaker | Thai | English | Focus |
|---|---|---|---|---|
| 1 | Geppetto | ปิโนคีโอวันนี้ไปโรงเรียนไหม | Pinocchio, are you going to school today? | `ไหม` question |
| 2 | Pinocchio | ไปครับแต่โรงเรียนปิดครับ | I'm going — but school is closed. | adjective after noun |
| 3 | Geppetto | จริงหรือโรงเรียนปิดวันนี้ | Really? School is closed today? | `จริงหรือ` echo |
| 4 | Pinocchio | จริงครับผมไม่โกหก | It's true, I don't lie. | negation before verb |
| 5 | Geppetto | จมูกของคุณยาวขึ้น | Your nose is getting longer! | `ขึ้น` = more so |
| 6 | Pinocchio | ไม่ใช่ครับจมูกผมไม่ยาว | No! My nose isn't long. | `ไม่ใช่` = no |
| 7 | Geppetto | ปิโนคีโอโกหกไม่ดีต้องพูดจริง | Lying isn't good. You must tell the truth. | `ต้อง` = must |
| 8 | Pinocchio | ขอโทษครับผมจะพูดจริง | Sorry — I'll tell the truth. | `จะ` future |

### Story 3 — Little Red Riding Hood

| # | Speaker | Thai | English | Focus |
|---|---|---|---|---|
| 1 | Wolf | สวัสดีจ้ะหนูน้อยจะไปไหน | Hello, little one. Where are you going? | `จะไปไหน` |
| 2 | Red | ไปบ้านคุณยายค่ะ | To grandma's house. | `ค่ะ` |
| 3 | Wolf | บ้านคุณยายอยู่ที่ไหน | Where is grandma's house? | `อยู่ที่ไหน` |
| 4 | Red | อยู่ใกล้ป่าตรงไปแล้วเลี้ยวขวา | Near the forest — go straight, then turn right. | `แล้ว` sequencing |
| 5 | Wolf | คุณยายหูใหญ่จัง | Grandma has such big ears! | `จัง` = so… |
| 6 | Red | ตาโตจังปากใหญ่ด้วย | What big eyes — and a big mouth too! | `ด้วย` = too |
| 7 | Wolf | เพราะผมจะกินหนูไง | Because I'm going to eat you! | `เพราะ` |
| 8 | Red | ช่วยด้วยมีหมาป่า | Help! There's a wolf! | `ช่วยด้วย` |

### Story 4 — Asking Her Out

| # | Speaker | Thai | English | Focus |
|---|---|---|---|---|
| 1 | Guy | สวัสดีครับคุณชื่ออะไร | Hi — what's your name? | `ชื่ออะไร` |
| 2 | Girl | ฝนค่ะแล้วคุณล่ะ | Fon. And you? | `แล้วคุณล่ะ` |
| 3 | Guy | ผมชื่อต้นครับคุณสวยมาก | I'm Ton. You're very pretty. | `มาก` at the end |
| 4 | Girl | ขอบคุณค่ะแต่คุณพูดเก่งจัง | Thanks — but you're a smooth talker. | `เก่ง` |
| 5 | Guy | ไปกินข้าวด้วยกันไหมครับ | Want to get dinner together? | `ไป` + verb |
| 6 | Girl | วันนี้ไม่ว่างค่ะพรุ่งนี้ได้ไหม | I'm busy today — how about tomorrow? | `ได้ไหม` |
| 7 | Guy | ได้ครับแล้วผมขอเบอร์ได้ไหม | Sure. Can I have your number? | `ขอ…ได้ไหม` |
| 8 | Girl | ได้ค่ะแต่ต้องใจดีนะ | Okay — but you have to be nice. | `นะ` softener |

### Story 5 — The Cat Who Wants to Type Thai

| # | Speaker | Thai | English | Focus |
|---|---|---|---|---|
| 1 | Cat | นุ่นครับคุณกำลังทำอะไร | Noon, what are you doing? | `กำลัง` = -ing |
| 2 | Noon | ฉันกำลังพิมพ์ภาษาไทย | I'm typing Thai. | ภาษาไทย |
| 3 | Cat | ผมอยากพิมพ์บ้าง | I want to type too. | `บ้าง` |
| 4 | Noon | ได้สิแต่ต้องช้าๆนะ | Sure — but slowly, okay? | `ได้สิ`, `ๆ` |
| 5 | Cat | พิมพ์ตัวอักษรอะไรดี | Which letter should I type? | `อะไรดี` |
| 6 | Noon | พิมพ์กไก่ก่อนง่ายมาก | Type "ก ไก่" first — it's really easy. | `ก่อน` |
| 7 | Cat | ผมพิมพ์ผิดอยากฝึกอีก | I typed it wrong — I want to practise more. | `ผิด` |
| 8 | Noon | ไม่เป็นไรฝึกทุกวันจะเก่ง | No worries — practise daily and you'll be good. | `ทุกวัน` |

---

## 3. Art plan

New rigs in `tools/generate_story_art.py` (all deterministic, same flat style):

| Rig | Used by | Notes |
|---|---|---|
| `person` (base) | all humans | palette + hair style + outfit + optional hat/glasses/mustache/nose length |
| `boy` | Pinocchio | wooden skin, yellow cap, long nose that reads as "lying" |
| `oldman` | Geppetto | white hair + moustache, brown vest |
| `girl` | Red Hood, Fon, Noon | hood variant (red), bob variant (default) |
| `man` | the smooth guy | short hair, blue jacket |
| `wolf` | Red Hood | grey wolf, big snout, ears, tail |
| `cat` | the typing cat | ginger cat, sitting / paw-up poses |

New scenes: night street (existing), **forest** (tree silhouettes + moon + fireflies), **room** (wall, moonlit window, desk + keyboard prop).

Also generalised: covers are now rendered per story from that story's first two characters and written to the story's own `cover` path (previously hard-coded to the Shrek cover).

---

## 4. Execution

1. Story JSONs + index entries → `python tools/validate_stories.py` (token concat, declared extras, asset refs).
2. Art rigs + 24 card scenes + 4 covers → `python tools/generate_story_art.py --all`, eyeball 2–3 frames.
3. `assembleDebug`, install, spot-check the library and one new scene.
4. Update README + this plan with the outcome.
