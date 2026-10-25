package com.example.learnthai.data.repository

import android.content.Context
import com.example.learnthai.data.model.MistakeLog
import com.example.learnthai.data.model.WordEntity
import com.example.learnthai.data.model.WordItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader

class WordRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _wordsFlow = MutableStateFlow<List<WordEntity>>(emptyList())
    val allWordsFlow: Flow<List<WordEntity>> = _wordsFlow.asStateFlow()

    private val userStorageFile = File(context.filesDir, "user_word_progress.json")
    private val mistakeLogs = mutableListOf<MistakeLog>()

    init {
        scope.launch(Dispatchers.IO) {
            loadWords()
        }
    }

    fun getAllWords(): Flow<List<WordEntity>> = allWordsFlow

    fun getWordsByPhase(phase: Int): Flow<List<WordEntity>> {
        return _wordsFlow.map { list -> list.filter { it.phase == phase } }
    }

    fun getMistakeWords(): Flow<List<WordEntity>> {
        return _wordsFlow.map { list -> list.filter { it.mistakesCount > 0 }.sortedByDescending { it.mistakesCount } }
    }

    suspend fun getWord(thaiWord: String): WordEntity? = withContext(Dispatchers.IO) {
        _wordsFlow.value.find { it.thaiWord == thaiWord }
    }

    suspend fun recordKeystrokeMistake(thaiWord: String, expected: String, typed: String) = withContext(Dispatchers.IO) {
        val updated = _wordsFlow.value.map { word ->
            if (word.thaiWord == thaiWord) {
                word.copy(mistakesCount = word.mistakesCount + 1)
            } else {
                word
            }
        }
        _wordsFlow.value = updated
        mistakeLogs.add(MistakeLog(thaiWord, expected, typed))
        saveProgress(updated)
    }

    suspend fun updateSrsRating(thaiWord: String, rating: SrsRating) = withContext(Dispatchers.IO) {
        val current = _wordsFlow.value.find { it.thaiWord == thaiWord } ?: return@withContext
        val currentLevel = current.srsLevel

        val (newLevel, nextIntervalMillis) = when (rating) {
            SrsRating.AGAIN -> Pair(0, 10 * 60 * 1000L) // 10 minutes
            SrsRating.HARD -> Pair(maxOf(0, currentLevel), 24 * 60 * 60 * 1000L) // 1 day
            SrsRating.GOOD -> {
                val nextLvl = minOf(5, currentLevel + 1)
                val days = when (nextLvl) {
                    1 -> 1L
                    2 -> 3L
                    3 -> 7L
                    4 -> 14L
                    else -> 30L
                }
                Pair(nextLvl, days * 24 * 60 * 60 * 1000L)
            }
            SrsRating.EASY -> {
                val nextLvl = minOf(5, currentLevel + 2)
                val days = when (nextLvl) {
                    1 -> 2L
                    2 -> 5L
                    3 -> 10L
                    4 -> 20L
                    else -> 45L
                }
                Pair(nextLvl, days * 24 * 60 * 60 * 1000L)
            }
        }

        val now = System.currentTimeMillis()
        val updated = _wordsFlow.value.map { word ->
            if (word.thaiWord == thaiWord) {
                word.copy(
                    srsLevel = newLevel,
                    nextReviewTimestamp = now + nextIntervalMillis,
                    timesReviewed = word.timesReviewed + 1,
                    lastReviewedTimestamp = now
                )
            } else {
                word
            }
        }
        _wordsFlow.value = updated
        saveProgress(updated)
    }

    private suspend fun loadWords() = withContext(Dispatchers.IO) {
        // 1. Load initial base words from bundled assets
        val baseWords: List<WordEntity> = try {
            val inputStream = context.assets.open("thai_curriculum_100.json")
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<List<WordItem>>() {}.type
            val items: List<WordItem> = Gson().fromJson(reader, type)
            reader.close()
            items.map {
                WordEntity(
                    thaiWord = it.thaiWord,
                    phase = it.phase,
                    englishTranslation = it.englishTranslation,
                    consonantClass = it.consonantClass,
                    keystrokeSequence = it.keystrokeSequence,
                    pronunciationGuide = it.pronunciationGuide,
                    contextualExample = it.contextualExample
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }

        // 2. Overlay any saved user SRS & mistake progress
        val finalWords = if (userStorageFile.exists()) {
            try {
                val savedType = object : TypeToken<Map<String, UserProgressRecord>>() {}.type
                val savedProgress: Map<String, UserProgressRecord> = Gson().fromJson(userStorageFile.readText(), savedType) ?: emptyMap()
                baseWords.map { word ->
                    val progress = savedProgress[word.thaiWord]
                    if (progress != null) {
                        word.copy(
                            srsLevel = progress.srsLevel,
                            nextReviewTimestamp = progress.nextReviewTimestamp,
                            timesReviewed = progress.timesReviewed,
                            mistakesCount = progress.mistakesCount,
                            lastReviewedTimestamp = progress.lastReviewedTimestamp
                        )
                    } else {
                        word
                    }
                }
            } catch (e: Exception) {
                baseWords
            }
        } else {
            baseWords
        }

        _wordsFlow.value = finalWords
    }

    private fun saveProgress(words: List<WordEntity>) {
        try {
            val progressMap = words.associate { word ->
                word.thaiWord to UserProgressRecord(
                    srsLevel = word.srsLevel,
                    nextReviewTimestamp = word.nextReviewTimestamp,
                    timesReviewed = word.timesReviewed,
                    mistakesCount = word.mistakesCount,
                    lastReviewedTimestamp = word.lastReviewedTimestamp
                )
            }
            userStorageFile.writeText(Gson().toJson(progressMap))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class UserProgressRecord(
        val srsLevel: Int,
        val nextReviewTimestamp: Long,
        val timesReviewed: Int,
        val mistakesCount: Int,
        val lastReviewedTimestamp: Long
    )
}

enum class SrsRating {
    AGAIN, HARD, GOOD, EASY
}
