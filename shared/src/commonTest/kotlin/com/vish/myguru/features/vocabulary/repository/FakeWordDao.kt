package com.vish.myguru.features.vocabulary.repository

import com.vish.myguru.core.database.WordDao
import com.vish.myguru.core.database.WordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeWordDao(initial: List<WordEntity> = emptyList()) : WordDao {

    private val words = MutableStateFlow(initial.associateBy { it.id })

    val all: List<WordEntity> get() = words.value.values.toList()

    fun get(id: String): WordEntity? = words.value[id]

    override fun observePendingWords(): Flow<List<WordEntity>> =
        words.map { map -> map.values.filter { !it.isMastered }.sortedBy { it.nextReviewEpoch } }

    override fun observeDueCards(currentEpoch: Long): Flow<List<WordEntity>> =
        words.map { map ->
            map.values
                .filter { !it.isMastered && it.nextReviewEpoch <= currentEpoch }
                .sortedWith(compareBy<WordEntity> { it.nextReviewEpoch }.thenBy { it.boxLevel })
        }

    // Mirrors OnConflictStrategy.IGNORE: existing rows keep their progress.
    override suspend fun insertWords(words: List<WordEntity>) {
        this.words.value = this.words.value + words.filter { it.id !in this.words.value }.associateBy { it.id }
    }

    override suspend fun markAsMastered(wordId: String) {
        update(wordId) { it.copy(isMastered = true) }
    }

    override suspend fun updateWordProgress(wordId: String, newBox: Int, nextEpoch: Long, isMastered: Boolean) {
        update(wordId) { it.copy(boxLevel = newBox, nextReviewEpoch = nextEpoch, isMastered = isMastered) }
    }

    private fun update(wordId: String, transform: (WordEntity) -> WordEntity) {
        val current = words.value[wordId] ?: return
        words.value = words.value + (wordId to transform(current))
    }
}
