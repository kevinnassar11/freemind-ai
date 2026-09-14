package com.freemind.ai.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.freemind.ai.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val HISTORY_ENABLED_KEY = booleanPreferencesKey("history_enabled")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val authRepository: AuthRepository,
    private val supabaseRepository: SupabaseRepository,
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = mode.name }
        // Best-effort cross-device sync; local DataStore is the source of truth
        // for rendering the current session regardless of whether this succeeds.
        authRepository.userId?.let { uid -> supabaseRepository.setThemePref(uid, mode) }
    }

    override val historyEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[HISTORY_ENABLED_KEY] ?: true
    }

    override suspend fun setHistoryEnabled(enabled: Boolean) {
        dataStore.edit { it[HISTORY_ENABLED_KEY] = enabled }
        authRepository.userId?.let { uid -> supabaseRepository.setHistoryEnabled(uid, enabled) }
    }
}
