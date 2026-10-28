package com.example.learnthai.data.repository

import android.content.Context
import android.util.Log
import com.example.learnthai.data.model.Story
import com.example.learnthai.data.model.StoryIndexFile
import com.example.learnthai.data.model.StorySummary
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the bundled story library from `assets/stories/`.
 *
 * Everything is cached in memory after first read. Failures never crash the app: a missing or
 * malformed story simply does not appear in the library.
 */
class StoryRepository(private val context: Context) {

    private val gson = Gson()

    @Volatile
    private var indexCache: List<StorySummary>? = null

    private val storyCache = HashMap<String, Story>()

    suspend fun loadIndex(): List<StorySummary> = withContext(Dispatchers.IO) {
        indexCache?.let { return@withContext it }

        val loaded = try {
            context.assets.open(INDEX_ASSET).use { input ->
                input.bufferedReader(Charsets.UTF_8).use { reader ->
                    gson.fromJson(reader, StoryIndexFile::class.java)
                }
            }?.stories?.filter { it.id.isNotBlank() && it.file.isNotBlank() }
                ?.sortedWith(compareBy({ it.order }, { it.level }, { it.titleEn }))
                ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load story index", e)
            emptyList()
        }

        indexCache = loaded
        loaded
    }

    suspend fun loadStory(id: String): Story? = withContext(Dispatchers.IO) {
        storyCache[id]?.let { return@withContext it }

        val summary = loadIndex().firstOrNull { it.id == id } ?: return@withContext null

        val story = try {
            context.assets.open("$STORIES_DIR/${summary.file}").use { input ->
                input.bufferedReader(Charsets.UTF_8).use { reader ->
                    gson.fromJson(reader, Story::class.java)
                }
            }?.takeIf { it.cards.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load story $id", e)
            null
        }

        if (story != null) {
            storyCache[id] = story
            validateTokens(story).forEach { Log.w(TAG, "Story ${story.id}: $it") }
        }
        story
    }

    /**
     * A token list that does not concatenate back to the sentence would make the word
     * breakdown lie about the Thai text, so this is surfaced loudly in debug builds.
     */
    private fun validateTokens(story: Story): List<String> {
        val issues = mutableListOf<String>()
        story.cards.forEach { card ->
            val joined = card.tokens.joinToString("") { it.thai }
            if (card.tokens.isNotEmpty() && joined != card.thai) {
                issues.add("card ${card.id}: tokens '${joined}' != thai '${card.thai}'")
            }
            card.tokens.forEach { token ->
                if (token.match != null && token.match.isBlank()) {
                    issues.add("card ${card.id}: blank token match for '${token.thai}'")
                }
            }
        }
        return issues
    }

    private companion object {
        const val TAG = "StoryRepository"
        const val STORIES_DIR = "stories"
        const val INDEX_ASSET = "stories/index.json"
    }
}
