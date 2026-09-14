package com.freemind.ai.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freemind.ai.data.repository.AuthRepository
import com.freemind.ai.data.repository.FavoriteWriteResult
import com.freemind.ai.data.repository.PlacesRepository
import com.freemind.ai.domain.model.FavoritePlace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<FavoritePlace> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val placesRepository: PlacesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    fun refresh() {
        val userId = authRepository.userId ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, isError = false)
        viewModelScope.launch {
            placesRepository.getFavoritePlaces(userId)
                .onSuccess { favorites -> _uiState.value = _uiState.value.copy(favorites = favorites, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, isError = true) }
        }
    }

    fun removeFavorite(place: FavoritePlace) {
        val userId = authRepository.userId ?: return
        val previous = _uiState.value
        _uiState.value = previous.copy(favorites = previous.favorites.filterNot { it.id == place.id })
        viewModelScope.launch {
            when (placesRepository.removeFavoritePlace(userId, place.placeId)) {
                // Success: confirmed. Queued: offline, will retry later - keep the
                // optimistic removal either way.
                FavoriteWriteResult.Success, FavoriteWriteResult.Queued -> Unit
                // Permanent failure: the removal didn't actually happen, put it back.
                is FavoriteWriteResult.Failed -> _uiState.value = previous
            }
        }
    }
}
