package com.example.learnthai.ui.components.keyboard

object KedmaneeLayout {

    // 4 Rows for normal unshifted Kedmanee layout
    // Row 1 (Numbers row): high-frequency consonants ภ ถ ค ต จ ข ช live on base keys 4,5,7,8,9,0,-
    val normalRows: List<List<Char>> = listOf(
        listOf('_', 'ๅ', '/', '-', 'ภ', 'ถ', 'ุ', 'ึ', 'ค', 'ต', 'จ', 'ข', 'ช'),
        listOf('ๆ', 'ไ', 'ำ', 'พ', 'ะ', 'ั', 'ี', 'ร', 'น', 'ย', 'บ', 'ล', 'ฃ'),
        listOf('ฟ', 'ห', 'ก', 'ด', 'เ', '้', '่', 'า', 'ส', 'ว', 'ง'),
        listOf('ผ', 'ป', 'แ', 'อ', 'ิ', 'ื', 'ท', 'ม', 'ใ', 'ฝ')
    )

    // 4 Rows for shifted Kedmanee layout (Thai numerals ๑-๙, ๐ and shift consonants)
    val shiftedRows: List<List<Char>> = listOf(
        listOf('%', '+', '๑', '๒', '๓', '๔', 'ู', '฿', '๕', '๖', '๗', '๘', '๙'),
        listOf('๐', '"', 'ฎ', 'ฑ', 'ธ', 'ํ', '๊', 'ณ', 'ฯ', 'ญ', 'ฐ', ',', 'ฅ'),
        listOf('ฤ', 'ฆ', 'ฏ', 'โ', 'ฌ', '็', '๋', 'ษ', 'ศ', 'ซ', '.'),
        listOf('(', ')', 'ฉ', 'ฮ', 'ฺ', '์', '?', 'ฒ', 'ฬ', 'ฦ')
    )

    // Corresponding standard QWERTY keys for unshifted layer
    private val normalQwertyRows: List<List<String>> = listOf(
        listOf("`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "="),
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "[", "]", "\\"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", ";", "'"),
        listOf("z", "x", "c", "v", "b", "n", "m", ",", ".", "/")
    )

    // Corresponding standard QWERTY keys for shifted layer
    private val shiftedQwertyRows: List<List<String>> = listOf(
        listOf("~", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "+"),
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "{", "}", "|"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", ":", "\""),
        listOf("Z", "X", "C", "V", "B", "N", "M", "<", ">", "?")
    )

    private val charToQwertyMap: Map<Char, String> by lazy {
        val map = mutableMapOf<Char, String>()
        normalRows.forEachIndexed { rIdx, row ->
            row.forEachIndexed { cIdx, char ->
                if (cIdx < normalQwertyRows[rIdx].size) {
                    map[char] = normalQwertyRows[rIdx][cIdx]
                }
            }
        }
        shiftedRows.forEachIndexed { rIdx, row ->
            row.forEachIndexed { cIdx, char ->
                if (cIdx < shiftedQwertyRows[rIdx].size) {
                    map[char] = shiftedQwertyRows[rIdx][cIdx]
                }
            }
        }
        map
    }

    // Map each character to whether it requires the Shift key
    private val shiftedCharSet: Set<Char> by lazy {
        shiftedRows.flatten().toSet()
    }

    fun isShiftedChar(char: Char): Boolean {
        return shiftedCharSet.contains(char)
    }

    fun getQwertyKey(char: Char): String? {
        val raw = charToQwertyMap[char] ?: return null
        // Only return alphabetic Roman characters (a-z, A-Z), omit numerals and punctuation symbols
        return if (raw.length == 1 && raw.first().isLetter()) raw.lowercase() else null
    }

    // Returns true if the character is a combining Thai vowel, tone mark, or diacritic mark
    fun isCombiningMark(char: Char): Boolean {
        val code = char.code
        // U+0E31: Mai Han-Akat
        // U+0E34..U+0E3A: Sara I, Ii, Ue, Uee, U, Uu, Phinthu
        // U+0E47..U+0E4E: Maitaikhu, Mai Ek, Mai Tho, Mai Tri, Mai Chattawa, Thanthakhat (Karan), Nikhahit, Yamakkan
        return code == 0x0E31 || (code in 0x0E34..0x0E3A) || (code in 0x0E47..0x0E4E)
    }

    fun isCombiningMark(str: String): Boolean {
        return str.length == 1 && isCombiningMark(str.first())
    }

    // Returns a display-safe string for standalone rendering on keyboard keys
    fun getDisplayString(char: Char): String {
        return if (isCombiningMark(char)) {
            "\u25CC$char" // Prepend dotted circle placeholder so font engine renders the top/bottom mark
        } else {
            char.toString()
        }
    }

    // Overload for String inputs (e.g. from JSON lesson models)
    fun getDisplayString(str: String): String {
        return if (str.length == 1) {
            getDisplayString(str.first())
        } else {
            str
        }
    }

    // Quick lookup to find row index (0..3) for any character
    fun findCharPosition(char: Char): Pair<Int, Boolean>? {
        normalRows.forEachIndexed { rowIndex, row ->
            if (row.contains(char)) return Pair(rowIndex, false)
        }
        shiftedRows.forEachIndexed { rowIndex, row ->
            if (row.contains(char)) return Pair(rowIndex, true)
        }
        return null
    }
}
