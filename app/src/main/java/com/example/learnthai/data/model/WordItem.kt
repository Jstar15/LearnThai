package com.example.learnthai.data.model

import com.google.gson.annotations.SerializedName

data class WordItem(
    @SerializedName("phase")
    val phase: Int,
    @SerializedName("thai_word")
    val thaiWord: String,
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
    val srsLevel: Int = 0
)
