package com.vish.myguru.features.vocabulary.ui

import com.vish.myguru.features.vocabulary.model.Word
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
        assertEquals(UiState.ReviewSession(listOf(hund, katze), syncFailed = false), viewModel.uiState.value)
    }

    @Test
    fun showsCompletedWhenNothingIsDue() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeVocabularyRepository(dueWords = emptyList()))
        advanceUntilIdle()

        assertEquals(UiState.CompletedToday(), viewModel.uiState.value)
    }

    @Test
    fun syncFailureIsFlaggedButCachedCardsAreStillShown() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = listOf(hund)).apply {
            syncResult = Result.failure(IllegalStateException("offline"))
        }

        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        assertEquals(UiState.ReviewSession(listOf(hund), syncFailed = true), viewModel.uiState.value)
    }

    @Test
    fun syncFailureIsFlaggedWhenNothingIsDue() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = emptyList()).apply {
            syncResult = Result.failure(IllegalStateException("offline"))
        }

        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        assertEquals(UiState.CompletedToday(syncFailed = true), viewModel.uiState.value)
    }

    @Test
    fun syncFailureFlagIsKeptForTheRestOfTheSession() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = listOf(hund, katze)).apply {
            syncResult = Result.failure(IllegalStateException("offline"))
        }
        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        viewModel.onCardReviewed(hund, isCorrect = true)
        advanceUntilIdle()
        assertEquals(
            UiState.ReviewSession(listOf(katze), syncFailed = true, reviewedCount = 1),
            viewModel.uiState.value
        )

        viewModel.onCardReviewed(katze, isCorrect = true)
        advanceUntilIdle()
        assertEquals(UiState.CompletedToday(syncFailed = true), viewModel.uiState.value)
    }

    @Test
    fun successfulRetryClearsTheSyncFailure() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = listOf(hund)).apply {
            syncResult = Result.failure(IllegalStateException("offline"))
        }
        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        repository.syncResult = Result.success(Unit)
        viewModel.startStudySession()
        advanceUntilIdle()

        assertEquals(UiState.ReviewSession(listOf(hund), syncFailed = false), viewModel.uiState.value)
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
        assertEquals(UiState.CompletedToday(), viewModel.uiState.value)
    }

    @Test
    fun reviewingTheLastCardCompletesTheSession() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeVocabularyRepository(dueWords = listOf(katze)))
        advanceUntilIdle()

        viewModel.onCardReviewed(katze, isCorrect = true)
        advanceUntilIdle()

        assertEquals(UiState.CompletedToday(), viewModel.uiState.value)
    }

    @Test
    fun reviewsAreIgnoredOutsideOfASession() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = emptyList())
        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        viewModel.onCardReviewed(hund, isCorrect = true)
        advanceUntilIdle()

        assertTrue(repository.reviews.isEmpty())
        assertEquals(UiState.CompletedToday(), viewModel.uiState.value)
    }

    @Test
    fun restartingTheSessionSyncsAgainAndPicksUpNewlyDueCards() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = emptyList())
        val viewModel = MainViewModel(repository)
        advanceUntilIdle()
        assertEquals(UiState.CompletedToday(), viewModel.uiState.value)

        repository.dueWords = listOf(haus)
        viewModel.startStudySession()
        advanceUntilIdle()

        assertEquals(2, repository.syncCount)
        assertEquals(listOf(haus), sessionCards(viewModel))
    }

    @Test
    fun queueIsUpdatedBeforeTheReviewIsSaved() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = listOf(hund, katze))
        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        viewModel.onCardReviewed(hund, isCorrect = true)

        assertEquals(listOf(katze), sessionCards(viewModel))
        advanceUntilIdle()
        assertEquals(1, repository.reviews.size)
    }

    @Test
    fun duplicateSwipeOfTheSameCardIsOnlyCountedOnce() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = listOf(hund, katze))
        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        viewModel.onCardReviewed(hund, isCorrect = true)
        viewModel.onCardReviewed(hund, isCorrect = true)
        advanceUntilIdle()

        assertEquals(listOf(FakeVocabularyRepository.Review("1", 2, true)), repository.reviews)
        assertEquals(listOf(katze), sessionCards(viewModel))
    }

    @Test
    fun reviewOfACardThatIsNotOnTopIsIgnored() = runTest(dispatcher) {
        val repository = FakeVocabularyRepository(dueWords = listOf(hund, katze))
        val viewModel = MainViewModel(repository)
        advanceUntilIdle()

        viewModel.onCardReviewed(katze, isCorrect = false)
        advanceUntilIdle()

        assertTrue(repository.reviews.isEmpty())
        assertEquals(listOf(hund, katze), sessionCards(viewModel))
    }

    @Test
    fun everyReviewAdvancesTheReviewedCount() = runTest(dispatcher) {
        // A single forgotten box-1 card comes back identical, so the count is what tells the UI
        // to show a fresh card.
        val viewModel = MainViewModel(FakeVocabularyRepository(dueWords = listOf(katze)))
        advanceUntilIdle()

        viewModel.onCardReviewed(katze, isCorrect = false)
        advanceUntilIdle()

        val state = assertIs<UiState.ReviewSession>(viewModel.uiState.value)
        assertEquals(listOf(katze), state.cards)
        assertEquals(1, state.reviewedCount)
    }
}
