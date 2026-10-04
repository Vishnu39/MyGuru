package com.vish.myguru.features.vacabulary.repository

import com.vish.myguru.core.database.WordDao
import com.vish.myguru.core.database.WordEntity
import com.vish.myguru.currentTimeMillis
import com.vish.myguru.features.vacabulary.model.LeitnerEngine
import com.vish.myguru.features.vacabulary.model.Word
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

class VocabularyRepository(
    private val wordDao: WordDao
) {
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
    fun getDueWordsFlow(): Flow<List<Word>> {
        val now = currentTimeMillis()
        return wordDao.observeDueCards(now).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // Sync remote list into local Room cache without overriding existing progress
    suspend fun syncWords(): Result<Unit> = withContext(Dispatchers.IO) {
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
    suspend fun submitReview(wordId: String, currentBox: Int, isCorrect: Boolean) = withContext(Dispatchers.IO) {
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