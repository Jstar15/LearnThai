package com.example.learnthai.data.model

import com.google.gson.annotations.SerializedName

data class WordEntity(
    @SerializedName("thai_word")
    val thaiWord: String,
    @SerializedName("phase")
    val phase: Int,
    @SerializedName("english_translation")
    val englishTranslation: String,
    @SerializedName("consonant_class")
    val consonantClass: String,
    @SerializedName("keystroke_sequence")
    val keystrokeSequence: List<String>,
    @SerializedName("pronunciation_guide")
    val pronunciationGuide: String,
    @SerializedName("contextual_example")
    val contextualExample: String,
    @SerializedName("srs_level")
    var srsLevel: Int = 0,
    var nextReviewTimestamp: Long = 0L,
    var timesReviewed: Int = 0,
    var mistakesCount: Int = 0,
    var lastReviewedTimestamp: Long = 0L
) {
    val keystrokeSequenceJson: String
        get() = com.google.gson.Gson().toJson(keystrokeSequence)
}

data class MistakeLog(
    val thaiWord: String,
    val expectedChar: String,
    val typedChar: String,
    val timestamp: Long = System.currentTimeMillis()
)
