package com.vish.myguru.features.vacabulary.ui

//import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vish.myguru.features.vacabulary.repository.VocabularyRepository
import com.vish.myguru.features.vacabulary.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


// The sealed interface guarantees that UiState can ONLY be one of these three things.
sealed interface UiState {
    object Loading : UiState
    data class Success(val words: List<Word>) : UiState
    data class Error(val message: String) : UiState
}
class MainViewModel(
    private val repository: VocabularyRepository
) : ViewModel() {
    // 2. Internal Mutable State (Starts as Loading)
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)

    // 3. Public Read-Only State (The UI will listen to this)
    val uiState : StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Fetch the data the moment the ViewModel is created
        fetchVocabulary()
    }

    private fun fetchVocabulary() {
    viewModelScope.launch {
        _uiState.value = UiState.Loading
        val result = repository.fetchWords()

        result.onSuccess { words ->
          //  Log.d("KtorTest", "ViewModel State: SUCCESS! Fetched ${words.size} words.")
            _uiState.value = UiState.Success(words)
        }.onFailure { exception ->
          //  Log.d("KtorTest", "ViewModel State: ERROR! ${exception.message}")
            _uiState.value = UiState.Error(exception.message ?: "Unknown error occurred")
        }
    }



    }
    fun isAlreadyMastered(wordId:String){
        val currentUiState = _uiState.value
        if (currentUiState is UiState.Success) {
            val updatedList = currentUiState.words.filter { it.id != wordId }
        _uiState.value = UiState.Success(updatedList)
            }

    }


}