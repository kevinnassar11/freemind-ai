package com.freemind.ai.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freemind.ai.data.repository.AuthRepository
import com.freemind.ai.data.repository.FavoriteWriteResult
import com.freemind.ai.data.repository.PlacesRepository
import com.freemind.ai.data.repository.SavedItinerarySummary
import com.freemind.ai.domain.model.FavoriteList
import com.freemind.ai.domain.model.FavoritePlace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<FavoritePlace> = emptyList(),
    /** The user's named groups of favorites, oldest first. */
    val lists: List<FavoriteList> = emptyList(),
    /** The list being viewed; null shows every favorite. */
    val selectedListId: String? = null,
    /** Trip plans saved on this device (from Prompt mode), newest first. */
    val plans: List<SavedItinerarySummary> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
) {
    /** The favorites in the list being viewed. */
    val visibleFavorites: List<FavoritePlace>
        get() = if (selectedListId == null) favorites else favorites.filter { it.listId == selectedListId }
}

/** A list action that didn't go through - shown once as a snackbar. */
enum class ListError { CREATE_FAILED, RENAME_FAILED, DELETE_FAILED, MOVE_FAILED }

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val placesRepository: PlacesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _listErrors = Channel<ListError>(Channel.BUFFERED)
    val listErrors: Flow<ListError> = _listErrors.receiveAsFlow()

    init {
        val userId = authRepository.userId
        if (userId != null) {
            viewModelScope.launch {
                placesRepository.observeSavedItineraries(userId).collect { plans ->
                    _uiState.value = _uiState.value.copy(plans = plans)
                }
            }
        }
    }

    fun deletePlan(plan: SavedItinerarySummary) {
        val userId = authRepository.userId ?: return
        viewModelScope.launch { placesRepository.deleteSavedItinerary(userId, plan.id) }
    }

    fun refresh() {
        val userId = authRepository.userId ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, isError = false)
        viewModelScope.launch {
            // Plans saved on another device appear here too; the local list updates via the
            // Flow above once the sync writes them. Never blocks or fails the favorites load.
            launch { placesRepository.syncSavedItineraries(userId) }
            placesRepository.getFavoriteLists(userId).onSuccess { lists ->
                val selected = _uiState.value.selectedListId?.takeIf { id -> lists.any { it.id == id } }
                _uiState.value = _uiState.value.copy(lists = lists, selectedListId = selected)
            }
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

    // ---- Lists ---------------------------------------------------------------------

    fun selectList(listId: String?) {
        _uiState.value = _uiState.value.copy(selectedListId = listId)
    }

    /** Creates a list and, if [thenFile] is given, files that favorite into it. */
    fun createList(name: String, thenFile: FavoritePlace? = null) {
        val userId = authRepository.userId ?: return
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            placesRepository.createFavoriteList(userId, clean)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(lists = _uiState.value.lists + list)
                    if (thenFile != null) moveToList(thenFile, list.id)
                }
                .onFailure { _listErrors.send(ListError.CREATE_FAILED) }
        }
    }

    fun renameList(list: FavoriteList, name: String) {
        val userId = authRepository.userId ?: return
        val clean = name.trim()
        if (clean.isEmpty() || clean == list.name) return
        val previous = _uiState.value
        _uiState.value = previous.copy(lists = previous.lists.map { if (it.id == list.id) it.copy(name = clean) else it })
        viewModelScope.launch {
            placesRepository.renameFavoriteList(userId, list.id, clean).onFailure {
                _uiState.value = previous
                _listErrors.send(ListError.RENAME_FAILED)
            }
        }
    }

    /** Deletes the list only; the places in it go back to "no list". */
    fun deleteList(list: FavoriteList) {
        val userId = authRepository.userId ?: return
        val previous = _uiState.value
        _uiState.value = previous.copy(
            lists = previous.lists.filterNot { it.id == list.id },
            favorites = previous.favorites.map { if (it.listId == list.id) it.copy(listId = null) else it },
            selectedListId = previous.selectedListId?.takeIf { it != list.id },
        )
        viewModelScope.launch {
            placesRepository.deleteFavoriteList(userId, list.id).onFailure {
                _uiState.value = previous
                _listErrors.send(ListError.DELETE_FAILED)
            }
        }
    }

    /** Files a favorite under [listId], or takes it out of its list when null. */
    fun moveToList(place: FavoritePlace, listId: String?) {
        val userId = authRepository.userId ?: return
        val previous = _uiState.value
        _uiState.value = previous.copy(
            favorites = previous.favorites.map { if (it.id == place.id) it.copy(listId = listId) else it },
        )
        viewModelScope.launch {
            placesRepository.setFavoriteList(userId, place.placeId, listId).onFailure {
                _uiState.value = previous
                _listErrors.send(ListError.MOVE_FAILED)
            }
        }
    }
}
