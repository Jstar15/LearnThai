# 04. Custom Kedmanee Interactive Keyboard & Input Engine

## 1. Why a Custom In-App Keyboard Widget is Critical

For a complete beginner, relying on the system Android IME (like Gboard) creates severe friction:
1. **System Switching Barrier**: Asking a beginner to open Android system settings, install a Thai keyboard package, and toggle language buttons causes high drop-off.
2. **Lack of Dynamic Pedagogical Feedback**: System keyboards cannot illuminate the next target letter, highlight whether a letter requires Shift, or shake/buzz on specific wrong keys before character insertion.
3. **Autocorrect Interference**: System keyboards predict words automatically, bypassing the very muscle memory we are trying to train.

Therefore, the app embeds a **Custom Jetpack Compose Kedmanee Keyboard Widget** optimized for phonics learning.

---

## 2. Standard Kedmanee Layout Specifications

The Kedmanee layout is the universal standard for Thai typing on Android, iOS, and physical keyboards.

### Row-by-Row Character Mapping (Normal Layer vs Shift Layer)

| Row | Normal Keys (Unshifted) | Shifted Keys (Shift Active) |
|---|---|---|
| **Row 1 (Numbers)** | `_` `ๅ` `/` `-` `ภ` `ถ` `ุ` `ึ` `ค` `ต` `จ` `ข` `ช` | `%` `+` `๑` `๒` `๓` `๔` `ู` `฿` `๕` `๖` `๗` `๘` `๙` |
| **Row 2 (Upper)** | `ๆ` `ไ` `ำ` `พ` `ะ` `ั` `ี` `ร` `น` `ย` `บ` `ล` `ฃ` | `๐` `" ` `ฎ` `ฑ` `ธ` `ํ` `๊` `ณ` `ฯ` `ญ` `ฐ` `,` `ฅ` |
| **Row 3 (Home)** | `ฟ` `ห` `ก` `ด` `เ` `้` `่` `า` `ส` `ว` `ง` | `ฤ` `ฆ` `ฏ` `โ` `ฌ` `็` `๋` `ษ` `ศ` `ซ` `.` |
| **Row 4 (Bottom)** | `ผ` `ป` `แ` `อ` `ิ` `ื` `ท` `ม` `ใ` `ฝ` | `( ` `)` `ฉ` `ฮ` `ฺ` `์` `?` `ฒ` `ฬ` `ฦ` |

> **Note (corrected layout):** The Thai numerals (๑–๙, ๐) live on the **Shift** layer; the base layer holds the high-frequency consonants **ภ ถ ค ต จ ข ช** (on the physical number keys), plus ๅ on `1`. ๐ is typed as **Shift + ๆ**. This matters for the Auto-Shift logic: ภ/ถ/ค/ต/จ/ข/ช are **base** keys, not shift keys.

---

## 3. Keyboard State Machine

Every key on the custom keyboard implements a reactive state machine:

```
                  ┌──────────────┐
                  │     IDLE     │
                  └──────┬───────┘
                         │
        [Guided Mode &   │   [User Taps Key]
         Target Matches] │
                         ▼
        ┌───────────────────────────────┐
        │       HIGHLIGHTED_HINT        │
        │  (Soft pulse / primary color) │
        └────────┬──────────────┬───────┘
                 │              │
      [Correct   │              │ [Wrong Key
       Key Tap]  ▼              ▼  Tapped]
┌──────────────────────┐  ┌──────────────────────┐
│    CORRECT_PULSE     │  │      ERROR_BUZZ      │
│ • Green glow         │  │ • Crimson flash      │
│ • Subtle scale (1.1x)│  │ • Horizontal shake   │
│ • Submit char to VM  │  │ • Haptic buzz + SFX  │
└──────────────────────┘  │ • Char NOT inserted  │
                          └──────────────────────┘
```

---

## 4. Auto-Shift Guidance Mechanics

Many crucial Thai letters (such as `ธ`, `ณ`, `ญ`, `ษ`, `ศ`) live on the Kedmanee **Shift Layer**. Complete beginners are often unaware that these letters exist on the same physical key under Shift.

### The 2-Step Scaffolding Engine:
1. **Detection**:
   - The engine checks if the target character is present in `shiftedKeyMap`.
2. **Shift Highlighting**:
   - If `targetChar` is on the Shift layer and Shift is currently **OFF**:
     - The `[⇧ Shift]` key pulses with an amber ring.
     - A small tooltip displays: *"Shift required!"*.
3. **Layer Transition**:
   - As soon as the user taps `[⇧ Shift]`, the keyboard switches to the Shift layer, and the exact target key illuminates with the standard primary hint glow.
   - Once the shifted character is typed, the keyboard automatically returns to normal mode (auto-unshift), matching modern mobile typing UX.

---

## 5. Jetpack Compose Keyboard Implementation Architecture

```kotlin
@Composable
fun KedmaneeKeyboardView(
    currentTargetChar: Char?,
    isGuidedMode: Boolean,
    onKeyValidated: (Char) -> Unit,
    onWrongKeyAttempted: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    var isShifted by remember { mutableStateOf(false) }
    var shakingKey by remember { mutableStateOf<Char?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        val rows = if (isShifted) KedmaneeLayout.shiftedRows else KedmaneeLayout.normalRows

        rows.forEachIndexed { rowIndex, rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // If bottom row, prepend Shift key button
                if (rowIndex == 3) {
                    ShiftKeyButton(
                        isShiftActive = isShifted,
                        isShiftExpected = currentTargetChar?.let { KedmaneeLayout.isShiftedChar(it) } == true && !isShifted,
                        onClick = { isShifted = !isShifted }
                    )
                }

                rowKeys.forEach { char ->
                    val isExpected = isGuidedMode && char == currentTargetChar
                    val isShaking = shakingKey == char

                    KedmaneeKeyButton(
                        char = char,
                        isHighlighted = isExpected,
                        isError = isShaking,
                        onClick = {
                            if (char == currentTargetChar) {
                                onKeyValidated(char)
                                if (isShifted) isShifted = false // Auto-unshift
                            } else {
                                shakingKey = char
                                onWrongKeyAttempted(char)
                            }
                        }
                    )
                }
            }
        }
    }
}
```
