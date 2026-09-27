package com.vish.myguru.features.vacabulary.repository

import com.vish.myguru.core.database.WordDao
import com.vish.myguru.core.database.WordEntity
import com.vish.myguru.features.vacabulary.model.Word
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

val ktorClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }, contentType = ContentType.Any)
    }
}

class VocabularyRepository (private val wordDao: WordDao){

    // 1. Cont stream of unmastered words from local Room DB
    val wordsFlow: Flow<List<Word>> = wordDao.observePendingWords().map{
        entities -> entities.map { it.toDomain() }
    }
    suspend fun fetchWords(): Result<Unit> = withContext(Dispatchers.IO) {
         runCatching {

             val remoteWords= ktorClient.get("https://gist.githubusercontent.com/Vishnu39/53b931797358491c32fe88d3122f6a74/raw/514d36a43cc53e15a80dee10444bd2ba718e946d/vocab.json")
                    .body<List<Word>>()
                val entities = remoteWords.map {
                    word -> WordEntity(
                        id = word.id,
                        germanTerm = word.germanTerm,
                        englishTranslation = word.englishTranslation,
                        article = word.article,
                        isMastered = word.isMastered,
                        nextReviewEpoch = 0L
                    )
                }
             wordDao.insertWords(entities)
        }
    }

    suspend fun fetchA1Vocabulary(): List<Word> = withContext(Dispatchers.IO) {
        delay(2000L)
        listOf(
            Word("1", "Gebäude", "building", "das"),
            Word("2", "Zeitung", "newspaper", "die"),
            Word("3", "Beruf", "profession", "der")
        )
    }

    // 3. Mark word mastered directly in Room
    suspend fun markAsMastered(wordId: String) = withContext(Dispatchers.IO){
        wordDao.markAsMastered(wordId)
    }

    private fun WordEntity.toDomain() = Word(
        id = id,
        germanTerm = germanTerm,
        englishTranslation = englishTranslation,
        article = article,
        isMastered = isMastered
    )



}


