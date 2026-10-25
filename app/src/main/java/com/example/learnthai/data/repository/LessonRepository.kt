package com.example.learnthai.data.repository

import android.content.Context
import com.example.learnthai.data.model.LessonItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStreamReader

class LessonRepository(private val context: Context) {

    private var cachedLessons: List<LessonItem>? = null

    fun getLessons(): Flow<List<LessonItem>> = flow {
        if (cachedLessons != null) {
            emit(cachedLessons!!)
            return@flow
        }

        try {
            context.assets.open("thai_curriculum_10_lessons.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<LessonItem>>() {}.type
                    val lessons: List<LessonItem> = Gson().fromJson(reader, type)
                    cachedLessons = lessons
                    emit(lessons)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun getLessonByNumber(number: Int): LessonItem? {
        return cachedLessons?.find { it.lessonNumber == number }
    }
}
