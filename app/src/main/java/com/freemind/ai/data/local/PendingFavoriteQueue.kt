package com.freemind.ai.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** A favorite add/remove that couldn't reach Supabase and is waiting for connectivity to return. */
@Serializable
sealed interface PendingFavoriteAction {
    val placeId: String

    @Serializable
    data class Add(
        override val placeId: String,
        val title: String,
        val description: String,
        val latitude: Double?,
        val longitude: Double?,
    ) : PendingFavoriteAction

    @Serializable
    data class Remove(override val placeId: String) : PendingFavoriteAction
}

private val PENDING_FAVORITE_ACTIONS_KEY = stringPreferencesKey("pending_favorite_actions")

/**
 * DataStore-backed so a pending write survives the app being killed while
 * offline, not just a screen navigation - the same durability concern that
 * theme_mode already relies on DataStore for in SettingsRepositoryImpl.
 */
@Singleton
class PendingFavoriteQueue @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val pendingActions: Flow<List<PendingFavoriteAction>> = dataStore.data.map { prefs ->
        decode(prefs[PENDING_FAVORITE_ACTIONS_KEY])
    }

    suspend fun enqueue(action: PendingFavoriteAction) {
        dataStore.edit { prefs ->
            // A later action for the same place supersedes an earlier one -
            // e.g. add then remove before ever reconnecting should just net
            // out to remove, not replay both.
            val next = decode(prefs[PENDING_FAVORITE_ACTIONS_KEY])
                .filterNot { it.placeId == action.placeId } + action
            prefs[PENDING_FAVORITE_ACTIONS_KEY] = json.encodeToString(next)
        }
    }

    suspend fun remove(placeId: String) {
        dataStore.edit { prefs ->
            val next = decode(prefs[PENDING_FAVORITE_ACTIONS_KEY]).filterNot { it.placeId == placeId }
            prefs[PENDING_FAVORITE_ACTIONS_KEY] = json.encodeToString(next)
        }
    }

    suspend fun snapshot(): List<PendingFavoriteAction> = decode(dataStore.data.first()[PENDING_FAVORITE_ACTIONS_KEY])

    private fun decode(raw: String?): List<PendingFavoriteAction> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<PendingFavoriteAction>>(raw) }.getOrDefault(emptyList())
    }
}
