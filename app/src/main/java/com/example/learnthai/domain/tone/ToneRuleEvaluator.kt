package com.example.learnthai.domain.tone

enum class ConsonantClass {
    MID, HIGH, LOW, UNKNOWN
}

enum class ToneType(val displayName: String, val phoneticSymbol: String) {
    MID("Mid", "—"),
    LOW("Low", "↘"),
    FALLING("Falling", "∧"),
    HIGH("High", "↗"),
    RISING("Rising", "∨")
}

data class SyllableToneBreakdown(
    val initialConsonant: String,
    val consonantClass: ConsonantClass,
    val vowelDescription: String,
    val toneMark: String?,
    val finalConsonant: String?,
    val calculatedTone: ToneType,
    val ruleFormula: String
)

object ToneRuleEvaluator {

    private val midClassConsonants = setOf('ก', 'จ', 'ด', 'ต', 'บ', 'ป', 'อ', 'ฎ', 'ฏ')
    private val highClassConsonants = setOf('ข', 'ฃ', 'ฉ', 'ฐ', 'ถ', 'ผ', 'ฝ', 'ศ', 'ษ', 'ส', 'ห')
    private val lowClassConsonants = setOf(
        'ค', 'ฅ', 'ฆ', 'ง', 'ช', 'ซ', 'ฌ', 'ญ', 'ฑ', 'ฒ', 'ณ', 'ท', 'ธ', 'น',
        'พ', 'ฟ', 'ภ', 'ม', 'ย', 'ร', 'ล', 'ว', 'ฬ', 'ฮ'
    )

    fun getConsonantClass(c: Char): ConsonantClass {
        return when (c) {
            in midClassConsonants -> ConsonantClass.MID
            in highClassConsonants -> ConsonantClass.HIGH
            in lowClassConsonants -> ConsonantClass.LOW
            else -> ConsonantClass.UNKNOWN
        }
    }

    fun analyzeWord(word: String, consonantClassStr: String): SyllableToneBreakdown {
        val firstConsonant = word.firstOrNull { it !in listOf('เ', 'แ', 'โ', 'ใ', 'ไ', '่', '้', '๊', '๋', 'ั', '็', '์') } ?: ' '
        val cClass = when {
            consonantClassStr.contains("Mid", ignoreCase = true) -> ConsonantClass.MID
            consonantClassStr.contains("High", ignoreCase = true) -> ConsonantClass.HIGH
            consonantClassStr.contains("Low", ignoreCase = true) -> ConsonantClass.LOW
            else -> getConsonantClass(firstConsonant)
        }

        val toneMark = when {
            word.contains('่') -> "ไม้เอก (Mai Ek ่)"
            word.contains('้') -> "ไม้โท (Mai Tho ้)"
            word.contains('๊') -> "ไม้ตรี (Mai Tri ๊)"
            word.contains('๋') -> "ไม้จัตวา (Mai Chattawa ๋)"
            else -> null
        }

        val calculatedTone = when {
            word.contains('่') && cClass == ConsonantClass.LOW -> ToneType.FALLING
            word.contains('่') -> ToneType.LOW
            word.contains('้') && cClass == ConsonantClass.LOW -> ToneType.HIGH
            word.contains('้') -> ToneType.FALLING
            cClass == ConsonantClass.HIGH -> ToneType.RISING
            else -> ToneType.MID
        }

        val formula = when {
            toneMark != null -> "${cClass.name.lowercase().replaceFirstChar { it.uppercase() }} Class + $toneMark = ${calculatedTone.displayName} Tone"
            cClass == ConsonantClass.HIGH -> "High Class + Live Syllable (no mark) = Natural Rising Tone"
            cClass == ConsonantClass.MID -> "Mid Class + Live Syllable (no mark) = Natural Mid Tone"
            else -> "Low Class + Live Syllable (no mark) = Natural Mid Tone"
        }

        return SyllableToneBreakdown(
            initialConsonant = firstConsonant.toString(),
            consonantClass = cClass,
            vowelDescription = "Vowel component",
            toneMark = toneMark,
            finalConsonant = if (word.length > 2) word.last().toString() else null,
            calculatedTone = calculatedTone,
            ruleFormula = formula
        )
    }
}
