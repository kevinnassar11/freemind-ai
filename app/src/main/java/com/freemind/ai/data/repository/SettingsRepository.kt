package com.freemind.ai.data.repository

import com.freemind.ai.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    /** Whether place searches get recorded to history at all. */
    val historyEnabled: Flow<Boolean>
    suspend fun setHistoryEnabled(enabled: Boolean)

    /** Whether notifications are scheduled for the days of saved trip plans. Kept on this
     *  device only: it is about this phone's notification permission, not the account. */
    val tripRemindersEnabled: Flow<Boolean>
    suspend fun setTripRemindersEnabled(enabled: Boolean)
}
