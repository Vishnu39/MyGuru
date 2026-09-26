package com.vish.myguru.features.vacabulary.repository

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

class VocabularyRepository {
    suspend fun fetchWords(): Result<List<Word>> = withContext(Dispatchers.IO) {
         runCatching {

                ktorClient.get("https://gist.githubusercontent.com/Vishnu39/53b931797358491c32fe88d3122f6a74/raw/514d36a43cc53e15a80dee10444bd2ba718e946d/vocab.json")
                    .body<List<Word>>()

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
}


