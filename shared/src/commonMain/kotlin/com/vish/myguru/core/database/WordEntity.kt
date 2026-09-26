package com.vish.myguru.core.database



import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "german_words")
data class WordEntity(
    @PrimaryKey val id: String,
    val germanTerm: String,
    val englishTranslation: String,
    val article: String?,
    val isMastered: Boolean = false,
    val nextReviewEpoch: Long = 0L // Unix timestamp for the next Leitner review
)

