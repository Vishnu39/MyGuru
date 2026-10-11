package com.vish.myguru.features.vacabulary.ui

import com.vish.myguru.features.vacabulary.model.Word
import com.vish.myguru.features.vacabulary.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeVocabularyRepository(
    var dueWords: List<Word> = emptyList()
) : VocabularyRepository {

    data class Review(val wordId: String, val currentBox: Int, val isCorrect: Boolean)

    var syncResult: Result<Unit> = Result.success(Unit)
    var dueWordsError: Throwable? = null

    var syncCount = 0
        private set
    val reviews = mutableListOf<Review>()

    override fun getDueWordsFlow(): Flow<List<Word>> = flow {
        dueWordsError?.let { throw it }
        emit(dueWords)
    }

    override suspend fun syncWords(): Result<Unit> {
        syncCount++
        return syncResult
    }

    override suspend fun submitReview(wordId: String, currentBox: Int, isCorrect: Boolean) {
        reviews += Review(wordId, currentBox, isCorrect)
    }
}
