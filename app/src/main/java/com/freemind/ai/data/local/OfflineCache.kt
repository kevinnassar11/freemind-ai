package com.freemind.ai.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** A place's phone/website/hours as last fetched, so its details sheet still opens offline. */
@Entity(tableName = "cached_place_details")
data class CachedPlaceDetailsEntity(
    @PrimaryKey val placeId: String,
    val detailsJson: String,
    val cachedAt: Long,
)

@Dao
interface PlaceDetailsCacheDao {
    @Query("SELECT * FROM cached_place_details WHERE placeId = :placeId LIMIT 1")
    suspend fun get(placeId: String): CachedPlaceDetailsEntity?

    @Upsert
    suspend fun upsert(entity: CachedPlaceDetailsEntity)
}

private const val MAX_PHOTO_FILES = 150

/**
 * Place photos kept on disk, so a saved plan still shows its pictures with no signal.
 * Kept in app-private storage (not the cache dir, which Android may empty at any time
 * and which would defeat the point) and trimmed to the most recent [MAX_PHOTO_FILES].
 */
@Singleton
class PhotoDiskCache @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dir = File(context.filesDir, "place_photos").apply { mkdirs() }

    private fun fileFor(photoReference: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(photoReference.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir, "$hash.jpg")
    }

    fun get(photoReference: String): Bitmap? {
        val file = fileFor(photoReference)
        if (!file.exists()) return null
        file.setLastModified(System.currentTimeMillis())
        return BitmapFactory.decodeFile(file.path)
    }

    fun put(photoReference: String, bitmap: Bitmap) {
        runCatching {
            fileFor(photoReference).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            trim()
        }
    }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun trim() {
        val files = dir.listFiles() ?: return
        if (files.size <= MAX_PHOTO_FILES) return
        files.sortedBy { it.lastModified() }.take(files.size - MAX_PHOTO_FILES).forEach { it.delete() }
    }
}
