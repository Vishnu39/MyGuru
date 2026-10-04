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


    // Only cards due for review right now
    @Query("""
        SELECT * FROM german_words 
        WHERE isMastered = 0 AND nextReviewEpoch <= :currentEpoch 
        ORDER BY nextReviewEpoch ASC, boxLevel ASC
    """)
    fun observeDueCards(currentEpoch: Long): Flow<List<WordEntity>>

    // NEW: Bulk insert for your GitHub JSON payload
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("UPDATE german_words SET isMastered = 1 WHERE id = :wordId")
    suspend fun markAsMastered(wordId: String)

    @Query("""
        UPDATE german_words 
        SET boxLevel = :newBox, nextReviewEpoch = :nextEpoch, isMastered = :isMastered 
        WHERE id = :wordId
    """)
    suspend fun updateWordProgress(
        wordId: String,
        newBox: Int,
        nextEpoch: Long,
        isMastered: Boolean
    )
}