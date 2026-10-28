package com.example.learnthai.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for the interactive Story mode.
 *
 * A story is fully data-driven: `assets/stories/index.json` lists the library and each
 * story lives in its own JSON file. Art lives in `assets/stories/art/` so adding a story
 * never requires a Kotlin change (no R.drawable registry).
 */

data class StoryIndexFile(
    @SerializedName("schema_version") val schemaVersion: Int = 1,
    @SerializedName("stories") val stories: List<StorySummary> = emptyList()
)

data class StorySummary(
    @SerializedName("id") val id: String = "",
    @SerializedName("file") val file: String = "",
    @SerializedName("title_en") val titleEn: String = "",
    @SerializedName("title_th") val titleTh: String = "",
    @SerializedName("teaches_label") val teachesLabel: String = "",
    @SerializedName("teaches_key") val teachesKey: String = "",
    @SerializedName("level") val level: Int = 1,
    @SerializedName("line_count") val lineCount: Int = 0,
    @SerializedName("cover") val cover: String = "",
    @SerializedName("order") val order: Int = 0,
    @SerializedName("tags") val tags: List<String> = emptyList()
)

data class Story(
    @SerializedName("schema_version") val schemaVersion: Int = 1,
    @SerializedName("id") val id: String = "",
    @SerializedName("title_en") val titleEn: String = "",
    @SerializedName("title_th") val titleTh: String = "",
    @SerializedName("teaches_label") val teachesLabel: String = "",
    @SerializedName("teaches_detail") val teachesDetail: String = "",
    @SerializedName("level") val level: Int = 1,
    @SerializedName("phases") val phases: List<Int> = emptyList(),
    @SerializedName("aim") val aim: String = "",
    @SerializedName("characters") val characters: List<StoryCharacter> = emptyList(),
    @SerializedName("extra_words") val extraWords: List<StoryExtraWord> = emptyList(),
    @SerializedName("cards") val cards: List<StoryCard> = emptyList()
) {
    fun character(id: String?): StoryCharacter? =
        if (id.isNullOrBlank()) null else characters.firstOrNull { it.id == id }
}

data class StoryCharacter(
    @SerializedName("id") val id: String = "",
    @SerializedName("name_en") val nameEn: String = "",
    @SerializedName("name_th") val nameTh: String = "",
    @SerializedName("color") val color: String = "#6750A4",
    @SerializedName("art_rig") val artRig: String = ""
)

data class StoryExtraWord(
    @SerializedName("thai") val thai: String = "",
    @SerializedName("roman") val roman: String = "",
    @SerializedName("gloss") val gloss: String = ""
)

data class StoryCard(
    @SerializedName("id") val id: String = "",
    @SerializedName("speaker") val speaker: String = "",
    @SerializedName("listener") val listener: String? = null,
    @SerializedName("image") val image: String = "",
    @SerializedName("thai") val thai: String = "",
    @SerializedName("roman") val roman: String = "",
    @SerializedName("english") val english: String = "",
    @SerializedName("tokens") val tokens: List<StoryToken> = emptyList(),
    @SerializedName("tip") val tip: String = "",
    @SerializedName("new_words") val newWords: List<String> = emptyList(),
    @SerializedName("register") val register: String = "polite",
    @SerializedName("audio") val audio: String? = null,
    @SerializedName("focus") val focus: String = ""
)

data class StoryToken(
    @SerializedName("thai") val thai: String = "",
    @SerializedName("roman") val roman: String = "",
    @SerializedName("gloss") val gloss: String = "",
    @SerializedName("note") val note: String = "",
    /** Exact curriculum word to look up for tone/class details, when one exists. */
    @SerializedName("match") val match: String? = null
)

data class StoryProgress(
    @SerializedName("last_card") val lastCard: Int = 0,
    @SerializedName("completed") val completed: Boolean = false,
    @SerializedName("plays") val plays: Int = 0,
    @SerializedName("updated_at") val updatedAt: Long = 0L
)
