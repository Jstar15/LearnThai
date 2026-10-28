package com.example.learnthai.data.repository

import android.content.Context
import com.example.learnthai.data.model.StoryProgress
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Persists per-story progress (furthest line, completion, replay count) plus the small
 * set of story playback preferences.
 *
 * Mirrors the lightweight JSON-file approach already used by [WordRepository].
 */
class StoryProgressStore(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val progressFile = File(appContext.filesDir, "story_progress.json")
    private val prefs = appContext.getSharedPreferences("story_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _progressFlow = MutableStateFlow<Map<String, StoryProgress>>(emptyMap())
    val progressFlow: StateFlow<Map<String, StoryProgress>> = _progressFlow.asStateFlow()

    init {
        scope.launch {
            _progressFlow.value = readProgress()
        }
    }

    fun progressFor(storyId: String): StoryProgress? = _progressFlow.value[storyId]

    /** Auto-speak each line when the page settles. On by default: stories are audio-first. */
    var autoSpeak: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPEAK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SPEAK, value).apply()

    /** Selected Thai TTS Voice identifier (e.g. "th-th-x-cfn-network"). Null means system/auto best. */
    var selectedVoice: String?
        get() = prefs.getString(KEY_SELECTED_VOICE, null)
        set(value) = prefs.edit().putString(KEY_SELECTED_VOICE, value).apply()

    fun markCardShown(storyId: String, index: Int) {
        if (storyId.isBlank() || index < 0) return
        val current = _progressFlow.value[storyId] ?: StoryProgress()
        if (index <= current.lastCard) return
        update(storyId, current.copy(lastCard = index, updatedAt = System.currentTimeMillis()))
    }

    fun markCompleted(storyId: String) {
        if (storyId.isBlank()) return
        val current = _progressFlow.value[storyId] ?: StoryProgress()
        update(
            storyId,
            current.copy(
                completed = true,
                plays = current.plays + 1,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun reset(storyId: String) {
        val updated = _progressFlow.value.toMutableMap()
        updated.remove(storyId)
        _progressFlow.value = updated
        persist(updated)
    }

    private fun update(storyId: String, progress: StoryProgress) {
        val updated = _progressFlow.value.toMutableMap()
        updated[storyId] = progress
        _progressFlow.value = updated
        persist(updated)
    }

    private fun readProgress(): Map<String, StoryProgress> {
        if (!progressFile.exists()) return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, StoryProgress>>() {}.type
            gson.fromJson<Map<String, StoryProgress>?>(progressFile.readText(), type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun persist(progress: Map<String, StoryProgress>) {
        val snapshot = progress.toMap()
        scope.launch {
            try {
                progressFile.writeText(gson.toJson(snapshot))
            } catch (_: Exception) {
                // Progress is a nicety; never let a failed write break a lesson.
            }
        }
    }

    private companion object {
        const val KEY_AUTO_SPEAK = "auto_speak"
        const val KEY_SELECTED_VOICE = "selected_tts_voice"
    }
}
