package com.freemind.ai.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freemind.ai.data.export.ExportService
import com.freemind.ai.data.repository.AuthRepository
import com.freemind.ai.data.repository.PlacesRepository
import com.freemind.ai.data.repository.SettingsRepository
import com.freemind.ai.data.repository.SupabaseRepository
import com.freemind.ai.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val historyEnabled: Boolean = true,
    val tripRemindersEnabled: Boolean = true,
    val userEmail: String? = null,
    val userDisplayName: String? = null,
    val isClearingHistory: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val historyCleared: Boolean = false,
)

/** A CSV export ready to hand to a share-sheet Intent. */
data class ExportRequest(val uri: Uri, val fileName: String)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val supabaseRepository: SupabaseRepository,
    private val placesRepository: PlacesRepository,
    private val exportService: ExportService,
) : ViewModel() {

    private val busyState = MutableStateFlow(SettingsUiState())

    private val _exportEvents = Channel<ExportRequest>(Channel.BUFFERED)
    val exportEvents: Flow<ExportRequest> = _exportEvents.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeMode,
        settingsRepository.historyEnabled,
        settingsRepository.tripRemindersEnabled,
        authRepository.currentUser,
        busyState,
    ) { themeMode, historyEnabled, remindersEnabled, profile, busy ->
        busy.copy(
            themeMode = themeMode,
            historyEnabled = historyEnabled,
            tripRemindersEnabled = remindersEnabled,
            userEmail = profile?.email,
            userDisplayName = profile?.displayName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHistoryEnabled(enabled) }
    }

    /** Turns trip reminders on or off, and schedules (or cancels) them for every saved plan. */
    fun setTripRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTripRemindersEnabled(enabled)
            authRepository.userId?.let { placesRepository.refreshTripReminders(it) }
        }
    }

    fun exportFavorites() {
        val userId = authRepository.userId ?: return
        viewModelScope.launch {
            placesRepository.getFavoritePlaces(userId).onSuccess { favorites ->
                val uri = exportService.exportFavorites(favorites)
                _exportEvents.send(ExportRequest(uri, "freemind_favorites.csv"))
            }
        }
    }

    fun exportSearchHistory() {
        val userId = authRepository.userId ?: return
        viewModelScope.launch {
            placesRepository.getSearchHistory(userId).onSuccess { entries ->
                val uri = exportService.exportSearchHistory(entries)
                _exportEvents.send(ExportRequest(uri, "freemind_search_history.csv"))
            }
        }
    }

    fun clearHistory() {
        val userId = authRepository.userId ?: return
        busyState.value = busyState.value.copy(isClearingHistory = true, historyCleared = false)
        viewModelScope.launch {
            placesRepository.clearSearchHistory(userId)
            busyState.value = busyState.value.copy(isClearingHistory = false, historyCleared = true)
        }
    }

    fun acknowledgeHistoryCleared() {
        busyState.value = busyState.value.copy(historyCleared = false)
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun deleteAccount(onDone: () -> Unit) {
        busyState.value = busyState.value.copy(isDeletingAccount = true)
        viewModelScope.launch {
            val userId = authRepository.userId
            if (userId != null) {
                placesRepository.clearSearchHistory(userId)
                placesRepository.clearFavoritePlaces(userId)
                placesRepository.clearFavoriteLists(userId)
                placesRepository.clearSavedItineraries(userId)
                supabaseRepository.deleteProfile(userId)
            }
            authRepository.deleteAccount()
            busyState.value = busyState.value.copy(isDeletingAccount = false)
            onDone()
        }
    }
}
