package com.freemind.ai.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.freemind.ai.domain.model.FavoritePlace
import com.freemind.ai.domain.model.PlaceSearchHistoryEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

/** Formats the user's own data as CSV and hands back a shareable content:// Uri. */
@Singleton
class ExportService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun exportFavorites(favorites: List<FavoritePlace>): Uri = writeCsv(
        fileName = "freemind_favorites.csv",
        header = listOf("title", "description", "latitude", "longitude", "favorited_at"),
        rows = favorites.map { place ->
            listOf(
                place.title,
                place.description,
                place.latitude?.toString().orEmpty(),
                place.longitude?.toString().orEmpty(),
                place.favoritedAt.toString(),
            )
        },
    )

    fun exportSearchHistory(entries: List<PlaceSearchHistoryEntry>): Uri = writeCsv(
        fileName = "freemind_search_history.csv",
        header = listOf("query", "mode", "result_title", "latitude", "longitude", "searched_at"),
        rows = entries.map { entry ->
            listOf(
                entry.query,
                entry.mode.name.lowercase(),
                entry.resultTitle.orEmpty(),
                entry.latitude?.toString().orEmpty(),
                entry.longitude?.toString().orEmpty(),
                entry.searchedAt.toString(),
            )
        },
    )

    private fun writeCsv(fileName: String, header: List<String>, rows: List<List<String>>): Uri {
        val file = File(context.cacheDir, fileName)
        FileWriter(file).use { writer ->
            writer.append(header.joinToString(",") { csvField(it) }).append('\n')
            rows.forEach { row -> writer.append(row.joinToString(",") { csvField(it) }).append('\n') }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun csvField(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
