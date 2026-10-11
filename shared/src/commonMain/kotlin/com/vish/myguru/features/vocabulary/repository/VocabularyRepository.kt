package com.vish.myguru.features.vocabulary.repository

import com.vish.myguru.core.database.WordDao
import com.vish.myguru.core.database.WordEntity
import com.vish.myguru.currentTimeMillis
import com.vish.myguru.features.vocabulary.model.LeitnerEngine
import com.vish.myguru.features.vocabulary.model.Word
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface VocabularyRepository {
    /** Observes the words that are due for review right now. */
    fun getDueWordsFlow(): Flow<List<Word>>

    /** Pulls the remote word list into the local cache without overwriting existing progress. */
    suspend fun syncWords(): Result<Unit>

    /** Applies the Leitner evaluation for a review and persists the new schedule. */
    suspend fun submitReview(wordId: String, currentBox: Int, isCorrect: Boolean)
}

class DefaultVocabularyRepository(
    private val wordDao: WordDao
) : VocabularyRepository {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
                contentType = ContentType.Any
            )
        }
    }

    // Observe words due right now
    override fun getDueWordsFlow(): Flow<List<Word>> {
        val now = currentTimeMillis()
        return wordDao.observeDueCards(now).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // Sync remote list into local Room cache without overriding existing progress
    override suspend fun syncWords(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val remoteWords = httpClient.get(
                "https://gist.githubusercontent.com/Vishnu39/53b931797358491c32fe88d3122f6a74/raw/vocab.json"
            ).body<List<Word>>()

            val entities = remoteWords.map { word ->
                WordEntity(
                    id = word.id,
                    germanTerm = word.germanTerm,
                    englishTranslation = word.englishTranslation,
                    article = word.article,
                    isMastered = word.isMastered,
                    boxLevel = word.boxLevel,
                    nextReviewEpoch = word.nextReviewEpoch
                )
            }
            wordDao.insertWords(entities)
        }
    }

    // Submit review and persist new Leitner schedule to Room
    override suspend fun submitReview(wordId: String, currentBox: Int, isCorrect: Boolean): Unit = withContext(Dispatchers.IO) {
        val now = currentTimeMillis()
        val evaluation = LeitnerEngine.evaluate(
            currentBox = currentBox,
            isCorrect = isCorrect,
            nowEpoch = now
        )

        wordDao.updateWordProgress(
            wordId = wordId,
            newBox = evaluation.nextBoxLevel,
            nextEpoch = evaluation.nextReviewEpoch,
            isMastered = evaluation.isMastered
        )
    }

    private fun WordEntity.toDomain() = Word(
        id = id,
        germanTerm = germanTerm,
        englishTranslation = englishTranslation,
        article = article,
        isMastered = isMastered,
        boxLevel = boxLevel,
        nextReviewEpoch = nextReviewEpoch
    )
}