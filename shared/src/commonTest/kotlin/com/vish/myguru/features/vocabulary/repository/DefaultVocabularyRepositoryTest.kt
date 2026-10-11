package com.vish.myguru.features.vocabulary.repository

import com.vish.myguru.core.database.WordEntity
import com.vish.myguru.core.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultVocabularyRepositoryTest {

    private val vocabJson = """
        [
          {"id": "1", "germanTerm": "Hund", "englishTranslation": "dog", "article": "der", "unknownField": true},
          {"id": "2", "germanTerm": "Katze", "englishTranslation": "cat", "article": "die"}
        ]
    """.trimIndent()

    private fun mockEngine(status: HttpStatusCode = HttpStatusCode.OK, body: String = vocabJson) = MockEngine {
        if (status == HttpStatusCode.OK) {
            // The gist serves raw JSON as text/plain.
            respond(body, status, headersOf(HttpHeaders.ContentType, "text/plain; charset=utf-8"))
        } else {
            respondError(status)
        }
    }

    private fun repository(dao: FakeWordDao, engine: MockEngine = mockEngine()) =
        DefaultVocabularyRepository(wordDao = dao, httpClient = createHttpClient(engine))

    @Test
    fun syncDownloadsTheWordListAndStoresIt() = runTest {
        val dao = FakeWordDao()
        val engine = mockEngine()

        val result = repository(dao, engine).syncWords()

        assertTrue(result.isSuccess)
        assertEquals(DefaultVocabularyRepository.VOCABULARY_URL, engine.requestHistory.single().url.toString())
        assertEquals(setOf("Hund", "Katze"), dao.all.map { it.germanTerm }.toSet())
        assertTrue(dao.all.all { it.boxLevel == 1 && !it.isMastered })
    }

    @Test
    fun syncKeepsProgressOfWordsAlreadyStored() = runTest {
        val learned = WordEntity("1", "Hund", "dog", "der", boxLevel = 4, nextReviewEpoch = 123L)
        val dao = FakeWordDao(listOf(learned))

        repository(dao).syncWords()

        assertEquals(learned, dao.get("1"))
        assertEquals(2, dao.all.size)
    }

    @Test
    fun syncReportsFailureOnServerError() = runTest {
        val dao = FakeWordDao()

        val result = repository(dao, mockEngine(HttpStatusCode.InternalServerError)).syncWords()

        assertTrue(result.isFailure)
        assertTrue(dao.all.isEmpty())
    }

    @Test
    fun syncReportsFailureOnMalformedJson() = runTest {
        val dao = FakeWordDao()

        val result = repository(dao, mockEngine(body = "not json")).syncWords()

        assertTrue(result.isFailure)
        assertTrue(dao.all.isEmpty())
    }

    @Test
    fun dueWordsAreMappedToTheDomainModel() = runTest {
        val dao = FakeWordDao(
            listOf(
                WordEntity("1", "Hund", "dog", "der", boxLevel = 2, nextReviewEpoch = 0L),
                WordEntity("2", "Katze", "cat", "die", isMastered = true),
                WordEntity("3", "Haus", "house", "das", nextReviewEpoch = Long.MAX_VALUE)
            )
        )

        val due = repository(dao).getDueWordsFlow().first()

        assertEquals(listOf("1"), due.map { it.id })
        assertEquals(2, due.single().boxLevel)
        assertEquals("der", due.single().article)
    }

    @Test
    fun correctReviewPromotesTheWordAndSchedulesItLater() = runTest {
        val dao = FakeWordDao(listOf(WordEntity("1", "Hund", "dog", "der", boxLevel = 2)))

        repository(dao).submitReview(wordId = "1", currentBox = 2, isCorrect = true)

        val saved = dao.get("1")!!
        assertEquals(3, saved.boxLevel)
        assertFalse(saved.isMastered)
        assertTrue(saved.nextReviewEpoch > 0L)
    }

    @Test
    fun correctReviewInTheLastBoxMastersTheWord() = runTest {
        val dao = FakeWordDao(listOf(WordEntity("1", "Hund", "dog", "der", boxLevel = 5)))

        repository(dao).submitReview(wordId = "1", currentBox = 5, isCorrect = true)

        assertTrue(dao.get("1")!!.isMastered)
    }

    @Test
    fun wrongReviewSendsTheWordBackToBoxOne() = runTest {
        val dao = FakeWordDao(listOf(WordEntity("1", "Hund", "dog", "der", boxLevel = 4)))

        repository(dao).submitReview(wordId = "1", currentBox = 4, isCorrect = false)

        assertEquals(1, dao.get("1")!!.boxLevel)
    }
}
