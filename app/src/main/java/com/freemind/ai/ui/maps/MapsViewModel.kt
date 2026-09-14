package com.freemind.ai.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freemind.ai.data.repository.AuthRepository
import com.freemind.ai.data.repository.FavoriteWriteResult
import com.freemind.ai.data.repository.PlacesRepository
import com.freemind.ai.data.repository.SettingsRepository
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.PlaceSearchMode
import com.freemind.ai.util.AppError
import com.freemind.ai.util.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Debounce so a typed word costs one Places call, not one per keystroke. */
private const val AUTOCOMPLETE_DEBOUNCE_MS = 350L
private const val MIN_AUTOCOMPLETE_CHARS = 2

data class MapsUiState(
    val query: String = "",
    val mode: PlaceSearchMode = PlaceSearchMode.PROMPT,
    val results: List<PlaceResult> = emptyList(),
    val selected: PlaceResult? = null,
    val favoritePlaceIds: Set<String> = emptySet(),
    /** Favorited/unfavorited locally but not yet confirmed by the server - see PendingFavoriteQueue. */
    val pendingFavoritePlaceIds: Set<String> = emptySet(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val error: AppError? = null,
)

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapsUiState())
    val uiState: StateFlow<MapsUiState> = _uiState.asStateFlow()

    // One-shot events for a failed favorite write - a Channel (not state) so a
    // Snackbar shown once doesn't reappear on rotation/recomposition.
    private val _favoriteErrorEvents = Channel<AppError>(Channel.BUFFERED)
    val favoriteErrorEvents: Flow<AppError> = _favoriteErrorEvents.receiveAsFlow()

    private var searchJob: Job? = null

    init {
        refreshFavoriteIds()
        observePendingFavorites()
    }

    private fun observePendingFavorites() {
        viewModelScope.launch {
            placesRepository.pendingFavoritePlaceIds.collect { pending ->
                _uiState.value = _uiState.value.copy(pendingFavoritePlaceIds = pending)
            }
        }
    }

    private fun refreshFavoriteIds() {
        val userId = authRepository.userId ?: return
        viewModelScope.launch {
            placesRepository.getFavoritePlaces(userId).onSuccess { favorites ->
                _uiState.value = _uiState.value.copy(favoritePlaceIds = favorites.map { it.placeId }.toSet())
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        // The constrained modes behave like a live list: re-query as the user
        // types. Prompt mode waits for an explicit submit so free-form questions
        // aren't fired off half-written.
        if (_uiState.value.mode != PlaceSearchMode.PROMPT) {
            scheduleAutocomplete(query)
        }
    }

    fun setMode(mode: PlaceSearchMode) {
        if (mode == _uiState.value.mode) return
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            mode = mode,
            results = emptyList(),
            selected = null,
            hasSearched = false,
            error = null,
        )
        if (mode != PlaceSearchMode.PROMPT && _uiState.value.query.isNotBlank()) {
            scheduleAutocomplete(_uiState.value.query)
        }
    }

    private fun scheduleAutocomplete(query: String) {
        searchJob?.cancel()
        if (query.trim().length < MIN_AUTOCOMPLETE_CHARS) {
            _uiState.value = _uiState.value.copy(results = emptyList(), hasSearched = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(AUTOCOMPLETE_DEBOUNCE_MS)
            runSearch(query)
        }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch(query) }
    }

    private suspend fun runSearch(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isBlank()) return

        _uiState.value = _uiState.value.copy(isSearching = true, error = null)

        when (val result = placesRepository.searchPlaces(query, _uiState.value.mode)) {
            is AppResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    results = result.data,
                    selected = result.data.firstOrNull(),
                    isSearching = false,
                    hasSearched = true,
                )
                if (settingsRepository.historyEnabled.first()) {
                    authRepository.userId?.let { userId ->
                        placesRepository.recordSearch(userId, query, _uiState.value.mode, result.data.firstOrNull())
                    }
                }
            }
            is AppResult.Failure -> _uiState.value = _uiState.value.copy(
                isSearching = false,
                hasSearched = true,
                error = result.error,
            )
        }
    }

    fun selectPlace(place: PlaceResult) {
        _uiState.value = _uiState.value.copy(selected = place)
    }

    fun toggleFavorite(place: PlaceResult) {
        val userId = authRepository.userId ?: return
        val isFavorite = place.id in _uiState.value.favoritePlaceIds
        _uiState.value = _uiState.value.copy(
            favoritePlaceIds = if (isFavorite) {
                _uiState.value.favoritePlaceIds - place.id
            } else {
                _uiState.value.favoritePlaceIds + place.id
            },
        )
        viewModelScope.launch {
            val result = if (isFavorite) {
                placesRepository.removeFavoritePlace(userId, place.id)
            } else {
                placesRepository.addFavoritePlace(userId, place)
            }
            when (result) {
                // Success: server confirmed it, optimistic state was already correct.
                // Queued: offline/network failure - PlacesRepository persisted it to
                // retry once connectivity returns (see PendingFavoriteQueue), so the
                // optimistic state stays as-is rather than being reverted.
                FavoriteWriteResult.Success, FavoriteWriteResult.Queued -> Unit
                is FavoriteWriteResult.Failed -> {
                    // A permanent failure (RLS, malformed data) - the optimistic
                    // update was wrong, so revert it and tell the user why.
                    refreshFavoriteIds()
                    _favoriteErrorEvents.send(result.error)
                }
            }
        }
    }

    fun clear() {
        searchJob?.cancel()
        _uiState.value = MapsUiState(
            favoritePlaceIds = _uiState.value.favoritePlaceIds,
            pendingFavoritePlaceIds = _uiState.value.pendingFavoritePlaceIds,
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
