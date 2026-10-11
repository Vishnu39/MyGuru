package com.vish.myguru.features.vocabulary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vish.myguru.features.vocabulary.model.Word
import com.vish.myguru.features.vocabulary.repository.VocabularyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Loading : UiState

    /**
     * @property cards the review queue; the first card is the one on screen.
     * @property syncFailed true when the latest sync failed; [cards] then come from the local cache.
     * @property reviewedCount number of reviews done in this session. The UI keys the card on it,
     *   so every review gets a fresh swipe state, even when a forgotten card comes straight back.
     */
    data class ReviewSession(
        val cards: List<Word>,
        val syncFailed: Boolean = false,
        val reviewedCount: Int = 0
    ) : UiState

    /**
     * @property syncFailed true when the latest sync failed, so new words may be missing.
     */
    data class CompletedToday(val syncFailed: Boolean = false) : UiState

    data class Error(val message: String) : UiState
}

class MainViewModel(
    private val repository: VocabularyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        startStudySession()
    }

    /**
     * Initializes today's review session:
     * 1. Triggers network sync to grab any new words from GitHub Gist into Room.
     *    A failed sync is not fatal: cached cards are still shown and the state is flagged.
     * 2. Takes a snapshot of all cards currently due for review from Room.
     */
    private var sessionJob: Job? = null

    fun startStudySession() {
        // Restarting (e.g. "Check for Updates" tapped twice) replaces any session still loading.
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            _uiState.value = UiState.Loading

            // Sync new words (ignores duplicates already in SQLite); keep going offline
            val syncFailed = repository.syncWords().isFailure

            try {
                // Take a one-time snapshot of cards due right now
                val dueCards = repository.getDueWordsFlow().first()

                if (dueCards.isEmpty()) {
                    _uiState.value = UiState.CompletedToday(syncFailed = syncFailed)
                } else {
                    _uiState.value = UiState.ReviewSession(cards = dueCards, syncFailed = syncFailed)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load due cards")
            }
        }
    }

    /**
     * Called when a card is swiped.
     *
     * The queue is updated synchronously, before the review is persisted, so a repeated callback
     * for the same swipe no longer sees the old queue. Only the card on top of the queue can be
     * reviewed; anything else is a stale event and is ignored.
     *
     * @param word The card that was swiped.
     * @param isCorrect True if swiped right (remembered), False if swiped left (forgotten).
     */
    fun onCardReviewed(word: Word, isCorrect: Boolean) {
        val currentState = _uiState.value as? UiState.ReviewSession ?: return
        if (currentState.cards.firstOrNull() != word) return

        // 1. Update the session queue in memory
        val updatedQueue = currentState.cards.drop(1).toMutableList()
        if (!isCorrect) {
            // If forgotten: demote to Box 1 and append to end of queue for re-practice
            updatedQueue.add(word.copy(boxLevel = 1))
        }

        // 2. Emit updated session or transition to Completed state
        _uiState.value = if (updatedQueue.isEmpty()) {
            UiState.CompletedToday(syncFailed = currentState.syncFailed)
        } else {
            currentState.copy(cards = updatedQueue, reviewedCount = currentState.reviewedCount + 1)
        }

        // 3. Persist the Leitner calculation to SQLite
        viewModelScope.launch {
            repository.submitReview(
                wordId = word.id,
                currentBox = word.boxLevel,
                isCorrect = isCorrect
            )
        }
    }
}
