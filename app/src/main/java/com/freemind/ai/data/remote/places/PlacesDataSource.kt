package com.freemind.ai.data.remote.places

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.freemind.ai.BuildConfig
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.util.AppError
import com.freemind.ai.util.AppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Places text search, brought over from the standalone map-search project.
 *
 * Unlike the OpenAI key (which is deliberately kept server-side in the chat Edge
 * Function), the Maps key has to live on the device: the Maps SDK renders the map
 * in-process and reads the key from the manifest. That's expected for Maps keys -
 * the protection model is key *restriction* (package name + SHA-1 + enabled-API
 * allowlist in Google Cloud Console), not secrecy. See SETUP.md.
 *
 * Errors are returned as [AppResult.Failure] values rather than thrown, matching
 * how ChatEdgeFunctionClient surfaces AppError to the UI layer.
 */
@Singleton
class PlacesDataSource @Inject constructor(
    private val httpClient: OkHttpClient,
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // A key restricted to "Android apps" in Google Cloud Console is only
    // validated when the request carries these two headers - the Places
    // Android SDK adds them automatically, but this is a raw REST call, so
    // they have to be attached by hand or every request comes back
    // REQUEST_DENIED regardless of how correctly the key is restricted.
    private val androidCertSha1: String by lazy { signingCertSha1(context) }

    suspend fun searchPlaces(
        query: String,
        placeType: String? = null,
    ): AppResult<List<PlaceResult>> = withContext(Dispatchers.IO) {
        if (BuildConfig.MAPS_API_KEY.isBlank()) return@withContext AppResult.Failure(AppError.NotConfigured)

        val url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
            "?query=${URLEncoder.encode(query, "UTF-8")}" +
            (placeType?.let { "&type=$it" } ?: "") +
            "&key=${BuildConfig.MAPS_API_KEY}"

        val request = Request.Builder()
            .url(url)
            .header("X-Android-Package", context.packageName)
            .header("X-Android-Cert", androidCertSha1)
            .build()

        val body = try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext AppResult.Failure(AppError.Unknown("Places search failed (${response.code})."))
                }
                response.body.string()
            }
        } catch (io: IOException) {
            return@withContext AppResult.Failure(AppError.Network)
        }

        val parsed = try {
            json.decodeFromString<PlacesSearchResponse>(body)
        } catch (e: Exception) {
            return@withContext AppResult.Failure(AppError.Unknown("Couldn't read the Places response."))
        }

        when (parsed.status) {
            "OK", "ZERO_RESULTS" -> Unit
            "REQUEST_DENIED" -> {
                android.util.Log.w("PlacesDataSource", "REQUEST_DENIED: ${parsed.errorMessage}")
                return@withContext AppResult.Failure(AppError.Unauthorized)
            }
            "OVER_QUERY_LIMIT" -> return@withContext AppResult.Failure(AppError.RateLimited)
            else -> return@withContext AppResult.Failure(
                AppError.Unknown(parsed.errorMessage ?: "Places search failed (${parsed.status})."),
            )
        }

        AppResult.Success(
            parsed.results.map { place ->
                PlaceResult(
                    id = place.placeId,
                    title = place.name,
                    description = place.formattedAddress
                        ?: place.types.firstOrNull()?.replace('_', ' ')
                        ?: "Unknown place",
                    latitude = place.geometry?.location?.lat,
                    longitude = place.geometry?.location?.lng,
                )
            },
        )
    }
}

/** Uppercase, colon-free hex SHA-1 of the APK's signing certificate - the format Google expects in X-Android-Cert. */
private fun signingCertSha1(context: Context): String {
    val signature: Signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES,
        )
        val signingInfo = packageInfo.signingInfo
        if (signingInfo != null && signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners.first()
        } else {
            signingInfo?.signingCertificateHistory?.first()
                ?: error("No signing certificate found for ${context.packageName}")
        }
    } else {
        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        @Suppress("DEPRECATION")
        packageInfo.signatures?.first() ?: error("No signing certificate found for ${context.packageName}")
    }
    val digest = MessageDigest.getInstance("SHA-1").digest(signature.toByteArray())
    return digest.joinToString("") { "%02X".format(it) }
}

@Serializable
private data class PlacesSearchResponse(
    val results: List<PlaceDto> = emptyList(),
    val status: String = "UNKNOWN_ERROR",
    @SerialName("error_message") val errorMessage: String? = null,
)

@Serializable
private data class PlaceDto(
    @SerialName("place_id") val placeId: String,
    val name: String,
    @SerialName("formatted_address") val formattedAddress: String? = null,
    val types: List<String> = emptyList(),
    val geometry: GeometryDto? = null,
)

@Serializable
private data class GeometryDto(val location: LocationDto? = null)

@Serializable
private data class LocationDto(val lat: Double, val lng: Double)
