package com.freemind.ai.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freemind.ai.data.repository.AuthRepository
import com.freemind.ai.data.repository.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val placesRepository: PlacesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun refresh() {
        val userId = authRepository.userId ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, isError = false)
        viewModelScope.launch {
            placesRepository.getSearchHistory(userId)
                .onSuccess { entries -> _uiState.value = _uiState.value.copy(entries = entries, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, isError = true) }
        }
    }

    fun deleteEntry(entryId: String) {
        val userId = authRepository.userId ?: return
        val previous = _uiState.value
        _uiState.value = previous.copy(entries = previous.entries.filterNot { it.id == entryId })
        viewModelScope.launch {
            placesRepository.deleteSearchHistoryEntry(entryId, userId)
                .onFailure { _uiState.value = previous }
        }
    }
}
