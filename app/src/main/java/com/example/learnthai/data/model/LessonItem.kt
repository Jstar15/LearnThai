package com.example.learnthai.data.model

import com.google.gson.annotations.SerializedName

data class LessonItem(
    @SerializedName("lesson_number") val lessonNumber: Int,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("target_phase") val targetPhase: Int,
    @SerializedName("summary") val summary: String,
    @SerializedName("characters") val characters: List<LessonCharacter>,
    @SerializedName("key_takeaways") val keyTakeaways: List<String>,
    @SerializedName("example_sentences") val exampleSentences: List<LessonSentence>
)

data class LessonCharacter(
    @SerializedName("char") val char: String,
    @SerializedName("name_thai") val nameThai: String,
    @SerializedName("name_roman") val nameRoman: String,
    @SerializedName("meaning") val meaning: String,
    @SerializedName("class") val charClass: String,
    @SerializedName("kedmanee_key") val kedmaneeKey: String,
    @SerializedName("sound_guide") val soundGuide: String,
    @SerializedName("lookalike_warning") val lookalikeWarning: String
)

data class LessonSentence(
    @SerializedName("thai") val thai: String,
    @SerializedName("roman") val roman: String,
    @SerializedName("english") val english: String,
    @SerializedName("target_word") val targetWord: String
)
