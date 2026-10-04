package com.vish.myguru.features.vacabulary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vish.myguru.features.vacabulary.model.Word
import com.vish.myguru.features.vacabulary.repository.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Loading : UiState
    data class ReviewSession(val cards: List<Word>) : UiState
    data object CompletedToday : UiState
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
     * 2. Takes a snapshot of all cards currently due for review from Room.
     */
    fun startStudySession() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            // Background sync (ignores duplicates already in SQLite)
            repository.syncWords()

            try {
                // Take a one-time snapshot of cards due right now
                val dueCards = repository.getDueWordsFlow().first()

                if (dueCards.isEmpty()) {
                    _uiState.value = UiState.CompletedToday
                } else {
                    _uiState.value = UiState.ReviewSession(cards = dueCards)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load due cards")
            }
        }
    }

    /**
     * Called when a card is swiped.
     * @param word The card that was swiped.
     * @param isCorrect True if swiped right (remembered), False if swiped left (forgotten).
     */
    fun onCardReviewed(word: Word, isCorrect: Boolean) {
        val currentState = _uiState.value
        if (currentState !is UiState.ReviewSession) return

        viewModelScope.launch {
            // 1. Asynchronously persist the Leitner calculation directly to SQLite
            repository.submitReview(
                wordId = word.id,
                currentBox = word.boxLevel,
                isCorrect = isCorrect
            )

            // 2. Manipulate the active session queue in memory
            val updatedQueue = currentState.cards.toMutableList()
            updatedQueue.remove(word)

            if (!isCorrect) {
                // If forgotten: demote to Box 1 and append to end of queue for re-practice
                val recycledCard = word.copy(boxLevel = 1)
                updatedQueue.add(recycledCard)
            }

            // 3. Emit updated session or transition to Completed state
            if (updatedQueue.isEmpty()) {
                _uiState.value = UiState.CompletedToday
            } else {
                _uiState.value = UiState.ReviewSession(cards = updatedQueue)
            }
        }
    }
}