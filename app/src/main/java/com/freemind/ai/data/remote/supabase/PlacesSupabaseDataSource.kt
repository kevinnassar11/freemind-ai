package com.freemind.ai.data.remote.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

private const val SEARCH_HISTORY_LIMIT = 50L

@Singleton
class PlacesSupabaseDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient,
) {
    private val historyTable get() = supabaseClient.from("place_search_history")
    private val favoritesTable get() = supabaseClient.from("favorite_places")

    suspend fun recordSearch(entry: NewPlaceSearchHistoryDto) {
        historyTable.insert(entry)
    }

    suspend fun getSearchHistory(userId: String): List<PlaceSearchHistoryDto> =
        historyTable.select {
            filter { eq("user_id", userId) }
            order(column = "searched_at", order = Order.DESCENDING)
            limit(SEARCH_HISTORY_LIMIT)
        }.decodeList()

    suspend fun deleteSearchHistoryEntry(id: String, userId: String) {
        historyTable.delete {
            filter {
                eq("id", id)
                eq("user_id", userId)
            }
        }
    }

    suspend fun clearSearchHistory(userId: String) {
        historyTable.delete {
            filter { eq("user_id", userId) }
        }
    }

    suspend fun getFavoritePlaces(userId: String): List<FavoritePlaceDto> =
        favoritesTable.select {
            filter { eq("user_id", userId) }
            order(column = "favorited_at", order = Order.DESCENDING)
        }.decodeList()

    suspend fun addFavorite(favorite: NewFavoritePlaceDto) {
        favoritesTable.insert(favorite)
    }

    suspend fun removeFavorite(placeId: String, userId: String) {
        favoritesTable.delete {
            filter {
                eq("place_id", placeId)
                eq("user_id", userId)
            }
        }
    }

    suspend fun clearFavorites(userId: String) {
        favoritesTable.delete {
            filter { eq("user_id", userId) }
        }
    }
}
