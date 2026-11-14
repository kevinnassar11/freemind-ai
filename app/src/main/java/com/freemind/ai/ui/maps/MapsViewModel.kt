package com.freemind.ai.ui.maps

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freemind.ai.data.repository.AuthRepository
import com.freemind.ai.data.repository.FavoriteWriteResult
import com.freemind.ai.data.repository.PlacesRepository
import com.freemind.ai.data.location.DeviceLocationProvider
import com.freemind.ai.data.repository.SettingsRepository
import com.freemind.ai.domain.model.DeviceLocation
import com.freemind.ai.domain.model.Itinerary
import com.freemind.ai.domain.model.PlaceDetails
import com.freemind.ai.domain.model.PlaceFilters
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.PlaceSearchMode
import com.freemind.ai.domain.model.TrendingSearch
import com.freemind.ai.util.AppError
import com.freemind.ai.util.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Debounce so a typed word costs one Places call, not one per keystroke. */
private const val AUTOCOMPLETE_DEBOUNCE_MS = 350L
private const val MIN_AUTOCOMPLETE_CHARS = 2

/** A Prompt-mode query counts as a trip-planning request when it reads as travel
 *  intent - e.g. "I'm travelling to Turkey, staying in Istanbul" - even without an
 *  explicit length of stay; ItineraryComposer defaults the day count when one isn't
 *  named. Requiring a duration here rejected exactly the prompts this feature is for. */
private val TRAVEL_INTENT_REGEX = Regex(
    """\b(travel(?:l?ing)?|trip|vacation|holiday|visiting|itinerary|plan(?:ning)?|""" +
        """(?:\d+|an?|one|two|three|four|five|six|seven|eight|nine|ten)[\s-]*(?:days?|nights?|weeks?)|weekend)\b""",
    RegexOption.IGNORE_CASE,
)
private fun isItineraryPrompt(text: String): Boolean = TRAVEL_INTENT_REGEX.containsMatchIn(text)

data class MapsUiState(
    val query: String = "",
    val mode: PlaceSearchMode = PlaceSearchMode.PROMPT,
    val results: List<PlaceResult> = emptyList(),
    val selected: PlaceResult? = null,
    val favoritePlaceIds: Set<String> = emptySet(),
    /** Favorited/unfavorited locally but not yet confirmed by the server - see PendingFavoriteQueue. */
    val pendingFavoritePlaceIds: Set<String> = emptySet(),
    val trendingSearches: List<TrendingSearch> = emptyList(),
    /** Device position, once permission is granted and a fix is available. Drives the
     *  blue dot, the distance on each result, and the bias sent to Places. */
    val deviceLocation: DeviceLocation? = null,
    val hasLocationPermission: Boolean = false,
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    /** True when `results` came from the offline cache rather than a live call. */
    val isShowingCachedResults: Boolean = false,
    /** Every result of the last search before [filters] were applied, so toggling a
     *  filter re-slices these instead of costing another Places call. */
    val unfilteredResults: List<PlaceResult> = emptyList(),
    val filters: PlaceFilters = PlaceFilters(),
    /** A generated day-by-day trip plan, set instead of `results` when the Prompt
     *  query reads as trip planning rather than a place lookup. */
    val itinerary: Itinerary? = null,
    /** Which day of [itinerary] the map is zoomed to; null shows the whole trip. */
    val selectedDay: Int? = null,
    /** Set once the current plan has been saved on the device (or was opened from there). */
    val savedItineraryId: String? = null,
    /** The place whose details sheet is open, if any. */
    val detailsPlace: PlaceResult? = null,
    val details: PlaceDetails? = null,
    val detailsPhoto: Bitmap? = null,
    val isLoadingDetails: Boolean = false,
    val error: AppError? = null,
)

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val deviceLocationProvider: DeviceLocationProvider,
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
        loadTrendingSearches()
        refreshDeviceLocation()
    }

    /**
     * Called on start and again after the permission prompt is answered. Silently
     * does nothing without permission - location is optional, and searching still
     * works without it.
     */
    fun refreshDeviceLocation() {
        val granted = deviceLocationProvider.hasPermission()
        _uiState.value = _uiState.value.copy(hasLocationPermission = granted)
        if (!granted) {
            _uiState.value = _uiState.value.copy(deviceLocation = null)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deviceLocation = deviceLocationProvider.current())
        }
    }

    private fun observePendingFavorites() {
        viewModelScope.launch {
            placesRepository.pendingFavoritePlaceIds.collect { pending ->
                _uiState.value = _uiState.value.copy(pendingFavoritePlaceIds = pending)
            }
        }
    }

    private fun loadTrendingSearches() {
        viewModelScope.launch {
            placesRepository.getTrendingSearches().onSuccess { trending ->
                _uiState.value = _uiState.value.copy(trendingSearches = trending)
            }
        }
    }

    fun onTrendingSearchSelected(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        search()
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
            unfilteredResults = emptyList(),
            selected = null,
            hasSearched = false,
            itinerary = null,
            selectedDay = null,
            savedItineraryId = null,
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

        if (_uiState.value.mode == PlaceSearchMode.PROMPT && isItineraryPrompt(query)) {
            runItineraryGeneration(query)
            return
        }

        _uiState.value = _uiState.value.copy(
            isSearching = true,
            error = null,
            itinerary = null,
            selectedDay = null,
            savedItineraryId = null,
        )

        // Re-read position for this search rather than reusing the start-up fix: the
        // user may have granted permission, or simply moved, since the screen opened.
        // Awaited directly here (not via refreshDeviceLocation(), which updates state
        // from inside its own separately-launched coroutine) so `bias` below is never
        // a search behind - reading _uiState.value right after firing that launch
        // would race it and see the previous fix.
        val hasPermission = deviceLocationProvider.hasPermission()
        val bias = if (hasPermission) deviceLocationProvider.current() else null
        _uiState.value = _uiState.value.copy(hasLocationPermission = hasPermission, deviceLocation = bias)

        // Results keep the order Places returned them in: that ranking is relevance
        // and prominence, which is what a query like "best coffee" is asking for.
        // Location is used to bias *which* places come back, and to label how far
        // each one is - not to re-sort them into nearest-first.
        when (val result = placesRepository.searchPlaces(query, _uiState.value.mode, bias)) {
            is AppResult.Success -> {
                val visible = _uiState.value.filters.apply(result.data.results)
                _uiState.value = _uiState.value.copy(
                    unfilteredResults = result.data.results,
                    results = visible,
                    selected = visible.firstOrNull(),
                    isSearching = false,
                    hasSearched = true,
                    isShowingCachedResults = result.data.isFromCache,
                )
                // A cache-served result isn't a new search - recording it would
                // double-count the same query in History and Trending Searches.
                // A search that found nothing isn't worth recording either: it can't
                // be re-opened from History and it would skew Trending Searches.
                if (!result.data.isFromCache &&
                    result.data.results.isNotEmpty() &&
                    settingsRepository.historyEnabled.first()
                ) {
                    authRepository.userId?.let { userId ->
                        placesRepository.recordSearch(userId, query, _uiState.value.mode, result.data.results.firstOrNull())
                    }
                }
            }
            is AppResult.Failure -> _uiState.value = _uiState.value.copy(
                isSearching = false,
                hasSearched = true,
                isShowingCachedResults = false,
                error = result.error,
            )
        }
    }

    private suspend fun runItineraryGeneration(prompt: String) {
        _uiState.value = _uiState.value.copy(
            isSearching = true,
            error = null,
            itinerary = null,
            selectedDay = null,
            savedItineraryId = null,
            results = emptyList(),
            unfilteredResults = emptyList(),
            selected = null,
        )
        when (val result = placesRepository.generateItinerary(prompt)) {
            is AppResult.Success -> {
                val plan = result.data
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    hasSearched = true,
                    itinerary = plan,
                    // Marked by the same Marker loop that draws normal search results;
                    // `selected` reuses the existing camera-recenter effect so the map
                    // opens on the destination without any separate code path.
                    results = plan.places,
                    unfilteredResults = plan.places,
                    selected = plan.destination ?: plan.places.firstOrNull(),
                )
                // Plans show up in History like any other search, so they can be re-opened.
                if (settingsRepository.historyEnabled.first()) {
                    authRepository.userId?.let { userId ->
                        placesRepository.recordSearch(userId, prompt, PlaceSearchMode.PROMPT, plan.destination)
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

    // ---- Filters ---------------------------------------------------------------

    fun toggleOpenNow() = updateFilters { it.copy(openNow = !it.openNow) }

    /** Toggles a minimum star rating; picking the active one clears it. */
    fun toggleMinRating(rating: Double) =
        updateFilters { it.copy(minRating = if (it.minRating == rating) null else rating) }

    /** Toggles an upper price bound (1 = $ ... 4 = $$$$); picking the active one clears it. */
    fun toggleMaxPrice(level: Int) =
        updateFilters { it.copy(maxPriceLevel = if (it.maxPriceLevel == level) null else level) }

    fun clearFilters() = updateFilters { PlaceFilters() }

    private fun updateFilters(change: (PlaceFilters) -> PlaceFilters) {
        val state = _uiState.value
        val filters = change(state.filters)
        // Trip plans are not filterable - their places are the plan.
        if (state.itinerary != null) {
            _uiState.value = state.copy(filters = filters)
            return
        }
        val visible = filters.apply(state.unfilteredResults)
        _uiState.value = state.copy(
            filters = filters,
            results = visible,
            selected = state.selected?.takeIf { sel -> visible.any { it.id == sel.id } } ?: visible.firstOrNull(),
        )
    }

    // ---- Trip plans ------------------------------------------------------------

    /** Zooms the map to one day of the plan; null returns to the whole trip. */
    fun selectDay(day: Int?) {
        _uiState.value = _uiState.value.copy(selectedDay = day)
    }

    /** Saves the open plan on this device, or removes it if it was already saved. */
    fun toggleSavePlan() {
        val plan = _uiState.value.itinerary ?: return
        val userId = authRepository.userId ?: return
        val savedId = _uiState.value.savedItineraryId
        viewModelScope.launch {
            if (savedId == null) {
                val id = placesRepository.saveItinerary(userId, plan)
                _uiState.value = _uiState.value.copy(savedItineraryId = id)
            } else {
                placesRepository.deleteSavedItinerary(userId, savedId)
                _uiState.value = _uiState.value.copy(savedItineraryId = null)
            }
        }
    }

    /** Opens a plan kept from earlier - works offline, no search is run. */
    fun openSavedPlan(id: String) {
        val userId = authRepository.userId ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val plan = placesRepository.getSavedItinerary(userId, id)
            if (plan == null) {
                _uiState.value = _uiState.value.copy(
                    hasSearched = true,
                    error = AppError.Unknown("That saved plan could not be opened."),
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                query = "",
                mode = PlaceSearchMode.PROMPT,
                itinerary = plan,
                selectedDay = null,
                savedItineraryId = id,
                results = plan.places,
                unfilteredResults = plan.places,
                selected = plan.destination ?: plan.places.firstOrNull(),
                hasSearched = true,
                isSearching = false,
                isShowingCachedResults = false,
                error = null,
            )
        }
    }

    // ---- Place details ---------------------------------------------------------

    fun openDetails(place: PlaceResult) {
        _uiState.value = _uiState.value.copy(
            detailsPlace = place,
            details = null,
            detailsPhoto = null,
            isLoadingDetails = true,
        )
        viewModelScope.launch {
            val details = (placesRepository.getPlaceDetails(place.id) as? AppResult.Success)?.data
            if (_uiState.value.detailsPlace?.id == place.id) {
                _uiState.value = _uiState.value.copy(details = details, isLoadingDetails = false)
            }
        }
        place.photoReference?.let { ref ->
            viewModelScope.launch {
                val photo = placesRepository.getPlacePhoto(ref)
                if (_uiState.value.detailsPlace?.id == place.id) {
                    _uiState.value = _uiState.value.copy(detailsPhoto = photo)
                }
            }
        }
    }

    fun closeDetails() {
        _uiState.value = _uiState.value.copy(
            detailsPlace = null,
            details = null,
            detailsPhoto = null,
            isLoadingDetails = false,
        )
    }

    fun selectPlace(place: PlaceResult) {
        _uiState.value = _uiState.value.copy(selected = place)
    }

    /**
     * Arriving at a specific place from outside a search - a tapped favorite, or a
     * push-notification deep link. Puts it in `results` too (not just `selected`), so
     * it actually gets a marker and a one-row results sheet, rather than leaving
     * unrelated leftover search results on screen under an unmarked recenter.
     */
    fun focusPlace(place: PlaceResult) {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            query = "",
            mode = PlaceSearchMode.PROMPT,
            results = listOf(place),
            unfilteredResults = listOf(place),
            selected = place,
            hasSearched = true,
            isSearching = false,
            isShowingCachedResults = false,
            itinerary = null,
            selectedDay = null,
            savedItineraryId = null,
            error = null,
        )
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
        // Clearing the text clears the query and its results - not the mode. The
        // mode is a separate choice the user made explicitly, and silently dropping
        // it back to Prompt here ran their next search in a mode they hadn't picked.
        _uiState.value = MapsUiState(
            mode = _uiState.value.mode,
            filters = _uiState.value.filters,
            favoritePlaceIds = _uiState.value.favoritePlaceIds,
            pendingFavoritePlaceIds = _uiState.value.pendingFavoritePlaceIds,
            trendingSearches = _uiState.value.trendingSearches,
            deviceLocation = _uiState.value.deviceLocation,
            hasLocationPermission = _uiState.value.hasLocationPermission,
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
