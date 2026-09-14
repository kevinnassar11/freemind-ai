package com.freemind.ai.data.repository

import com.freemind.ai.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    /** Whether place searches get recorded to history at all. */
    val historyEnabled: Flow<Boolean>
    suspend fun setHistoryEnabled(enabled: Boolean)
}
