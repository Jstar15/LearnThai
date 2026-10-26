package com.example.learnthai

import com.example.learnthai.data.model.WordItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class ExampleUnitTest {
    @Test
    fun testCurriculum15Phases() {
        val file = File("src/main/assets/thai_curriculum_100.json")
        assertTrue("Asset file must exist", file.exists())
        val type = object : TypeToken<List<WordItem>>() {}.type
        val items: List<WordItem> = Gson().fromJson(file.readText(), type)
        assertEquals(300, items.size)

        for (p in 1..15) {
            val phaseWords = items.filter { it.phase == p }
            assertEquals("Phase $p must have 20 words", 20, phaseWords.size)
        }

        val uniqueWords = items.map { it.thaiWord }.toSet()
        assertEquals("All 300 words must be distinct", 300, uniqueWords.size)
    }
}