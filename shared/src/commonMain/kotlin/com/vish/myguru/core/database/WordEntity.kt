package com.vish.myguru.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "german_words")
data class WordEntity(
    @PrimaryKey val id: String,
    val germanTerm: String,
    val englishTranslation: String,
    val article: String,
    val isMastered: Boolean = false,
    val boxLevel: Int = 1,
    val nextReviewEpoch: Long = 0L // Unix timestamp for the next Leitner review
)
