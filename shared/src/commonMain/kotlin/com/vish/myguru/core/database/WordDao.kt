package com.vish.myguru.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    // This returns a continuous Flow. If the DB changes, this emits a new list instantly.
    @Query("SELECT * FROM german_words WHERE isMastered = 0 ORDER BY nextReviewEpoch ASC")
    fun observePendingWords(): Flow<List<WordEntity>>

    // NEW: Bulk insert for your GitHub JSON payload
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("UPDATE german_words SET isMastered = 1 WHERE id = :wordId")
    suspend fun markAsMastered(wordId: String)

    @Query("UPDATE german_words SET nextReviewEpoch = :newTime WHERE id = :wordId")
    suspend fun updateReviewTime(wordId: String, newTime: Long)
}