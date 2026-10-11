package com.vish.myguru.features.vacabulary.ui

import com.vish.myguru.features.vacabulary.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val hund = Word(id = "1", germanTerm = "Hund", englishTranslation = "dog", article = "der", boxLevel = 2)
    private val katze = Word(id = "2", germanTerm = "Katze", englishTranslation = "cat", article = "die", boxLevel = 1)
    private val haus = Word(id = "3", germanTerm = "Haus", englishTranslation = "house", article = "das", boxLevel = 4)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sessionCards(viewModel: MainViewModel): List<Word> =
        assertIs<UiState.ReviewSession>(viewModel.uiState.value).cards

    @Test
    fun startsInLoadingStateBeforeTheSessionIsReady() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeVocabularyRepository(dueWords = listOf(hund)))

        assertEquals(UiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun syncsOnceAndShowsDueCardsOnStart() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = listOf(hund, katze))

        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        assertEquals(1, repository.syncCount)
        assertEquals(listOf(hund, katze), sessionCards(viewModel))
    }

    @Test
    fun showsCompletedWhenNothingIsDue() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeVocabularyRepository(dueWords = emptyList()))
        advanceUntilIdle()

        assertEquals(UiState.CompletedToday, viewModel.uiState.value)
    }

    @Test
    fun stillShowsCachedCardsWhenSyncFails() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = listOf(hund)).apply {
            syncResult = Result.failure(IllegalStateException("offline"))
        }

        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        assertEquals(listOf(hund), sessionCards(viewModel))
    }

    @Test
    fun showsErrorWhenDueCardsCannotBeLoaded() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository().apply {
            dueWordsError = IllegalStateException("database closed")
        }

        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        assertEquals(UiState.Error("database closed"), viewModel.uiState.value)
    }

    @Test
    fun usesFallbackErrorMessageWhenExceptionHasNone() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository().apply {
            dueWordsError = IllegalStateException()
        }

        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        assertEquals(UiState.Error("Failed to load due cards"), viewModel.uiState.value)
    }

    @Test
    fun rememberedCardIsSavedAndRemovedFromTheQueue() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = listOf(hund, katze))
        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        viewModel.onCardReviewed(hund, isCorrect = true)
        advanceUntilIdle()

        assertEquals(listOf(FakeVocabularyRepository.Review("1", 2, true)), repository.reviews)
        assertEquals(listOf(katze), sessionCards(viewModel))
    }

    @Test
    fun forgottenCardIsSavedAndMovedToTheEndInBoxOne() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = listOf(hund, katze, haus))
        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        viewModel.onCardReviewed(hund, isCorrect = false)
        advanceUntilIdle()

        assertEquals(listOf(FakeVocabularyRepository.Review("1", 2, false)), repository.reviews)
        assertEquals(listOf(katze, haus, hund.copy(boxLevel = 1)), sessionCards(viewModel))
    }

    @Test
    fun forgottenCardCanBeRetriedFromBoxOneInTheSameSession() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = listOf(haus))
        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        viewModel.onCardReviewed(haus, isCorrect = false)
        advanceUntilIdle()
        val retry = sessionCards(viewModel).single()
        viewModel.onCardReviewed(retry, isCorrect = true)
        advanceUntilIdle()

        assertEquals(
            listOf(
                FakeVocabularyRepository.Review("3", 4, false),
                FakeVocabularyRepository.Review("3", 1, true)
            ),
            repository.reviews
        )
        assertEquals(UiState.CompletedToday, viewModel.uiState.value)
    }

    @Test
    fun reviewingTheLastCardCompletesTheSession() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeVocabularyRepository(dueWords = listOf(katze)))
        advanceUntilIdle()

        viewModel.onCardReviewed(katze, isCorrect = true)
        advanceUntilIdle()

        assertEquals(UiState.CompletedToday, viewModel.uiState.value)
    }

    @Test
    fun reviewsAreIgnoredOutsideOfASession() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = emptyList())
        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        viewModel.onCardReviewed(hund, isCorrect = true)
        advanceUntilIdle()

        assertTrue(repository.reviews.isEmpty())
        assertEquals(UiState.CompletedToday, viewModel.uiState.value)
    }

    @Test
    fun restartingTheSessionSyncsAgainAndPicksUpNewlyDueCards() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = emptyList())
        val viewModel = MainViewModel(repository)
        advanceUntilIdle()
        assertEquals(UiState.CompletedToday, viewModel.uiState.value)

        repository.dueWords = listOf(haus)
        viewModel.startStudySession()
        advanceUntilIdle()

        assertEquals(2, repository.syncCount)
        assertEquals(listOf(haus), sessionCards(viewModel))
    }
}
