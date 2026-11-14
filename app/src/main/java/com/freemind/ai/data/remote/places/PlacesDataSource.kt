package com.freemind.ai.data.remote.places

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.freemind.ai.BuildConfig
import com.freemind.ai.domain.model.DeviceLocation
import com.freemind.ai.domain.model.PlaceDetails
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

    private companion object {
        /** Wide enough to cover a city, narrow enough that "near me" means it. */
        const val BIAS_RADIUS_METRES = 30_000
    }

    suspend fun searchPlaces(
        query: String,
        placeType: String? = null,
        /** Device position, when known, so results are ranked around the user rather
         *  than around whatever location Google infers from the request IP. */
        bias: DeviceLocation? = null,
    ): AppResult<List<PlaceResult>> = withContext(Dispatchers.IO) {
        if (BuildConfig.MAPS_API_KEY.isBlank()) return@withContext AppResult.Failure(AppError.NotConfigured)

        val url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
            "?query=${URLEncoder.encode(query, "UTF-8")}" +
            (placeType?.let { "&type=$it" } ?: "") +
            (bias?.let { "&location=${it.latitude},${it.longitude}&radius=$BIAS_RADIUS_METRES" } ?: "") +
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
                    description = place.readableAddress()
                        ?: place.types.firstOrNull()?.replace('_', ' ')
                        ?: "Unknown place",
                    latitude = place.geometry?.location?.lat,
                    longitude = place.geometry?.location?.lng,
                    types = place.types,
                    rating = place.rating,
                    ratingCount = place.userRatingsTotal,
                    openNow = place.openingHours?.openNow,
                    priceLevel = place.priceLevel,
                    photoReference = place.photos.firstOrNull()?.photoReference,
                )
            },
        )
    }

    /** Phone, website and weekly hours - not returned by text search, so fetched on demand. */
    suspend fun getPlaceDetails(placeId: String): AppResult<PlaceDetails> = withContext(Dispatchers.IO) {
        if (BuildConfig.MAPS_API_KEY.isBlank()) return@withContext AppResult.Failure(AppError.NotConfigured)
        val url = "https://maps.googleapis.com/maps/api/place/details/json" +
            "?place_id=${URLEncoder.encode(placeId, "UTF-8")}" +
            "&fields=formatted_phone_number,international_phone_number,website,opening_hours,url" +
            "&key=${BuildConfig.MAPS_API_KEY}"
        val body = try {
            httpClient.newCall(authorized(url)).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext AppResult.Failure(AppError.Unknown("Place details failed (${response.code})."))
                }
                response.body.string()
            }
        } catch (io: IOException) {
            return@withContext AppResult.Failure(AppError.Network)
        }
        val parsed = try {
            json.decodeFromString<PlaceDetailsResponse>(body)
        } catch (e: Exception) {
            return@withContext AppResult.Failure(AppError.Unknown("Couldn't read the place details."))
        }
        when (parsed.status) {
            "OK" -> Unit
            "OVER_QUERY_LIMIT" -> return@withContext AppResult.Failure(AppError.RateLimited)
            "REQUEST_DENIED" -> return@withContext AppResult.Failure(AppError.Unauthorized)
            else -> return@withContext AppResult.Failure(
                AppError.Unknown(parsed.errorMessage ?: "Place details failed (${parsed.status})."),
            )
        }
        val result = parsed.result
        AppResult.Success(
            PlaceDetails(
                phone = result?.formattedPhoneNumber,
                dialNumber = result?.internationalPhoneNumber ?: result?.formattedPhoneNumber,
                website = result?.website,
                mapsUrl = result?.url,
                weekdayHours = result?.openingHours?.weekdayText.orEmpty(),
            ),
        )
    }

    /** A place photo scaled to about [maxWidthPx] wide, or null if it can't be loaded. */
    suspend fun fetchPhoto(photoReference: String, maxWidthPx: Int = 800): Bitmap? = withContext(Dispatchers.IO) {
        if (BuildConfig.MAPS_API_KEY.isBlank()) return@withContext null
        val url = "https://maps.googleapis.com/maps/api/place/photo" +
            "?maxwidth=$maxWidthPx&photo_reference=${URLEncoder.encode(photoReference, "UTF-8")}" +
            "&key=${BuildConfig.MAPS_API_KEY}"
        try {
            httpClient.newCall(authorized(url)).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                BitmapFactory.decodeStream(response.body.byteStream())
            }
        } catch (io: IOException) {
            null
        }
    }

    private fun authorized(url: String): Request = Request.Builder()
        .url(url)
        .header("X-Android-Package", context.packageName)
        .header("X-Android-Cert", androidCertSha1)
        .build()
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
    /** Set on results that have no street address; usually just the neighbourhood. */
    val vicinity: String? = null,
    val types: List<String> = emptyList(),
    val geometry: GeometryDto? = null,
    val rating: Double? = null,
    @SerialName("user_ratings_total") val userRatingsTotal: Int? = null,
    @SerialName("price_level") val priceLevel: Int? = null,
    @SerialName("opening_hours") val openingHours: OpeningHoursDto? = null,
    val photos: List<PhotoDto> = emptyList(),
)

@Serializable
private data class OpeningHoursDto(
    @SerialName("open_now") val openNow: Boolean? = null,
    @SerialName("weekday_text") val weekdayText: List<String> = emptyList(),
)

@Serializable
private data class PhotoDto(@SerialName("photo_reference") val photoReference: String)

@Serializable
private data class PlaceDetailsResponse(
    val result: PlaceDetailsDto? = null,
    val status: String = "UNKNOWN_ERROR",
    @SerialName("error_message") val errorMessage: String? = null,
)

@Serializable
private data class PlaceDetailsDto(
    @SerialName("formatted_phone_number") val formattedPhoneNumber: String? = null,
    @SerialName("international_phone_number") val internationalPhoneNumber: String? = null,
    val website: String? = null,
    val url: String? = null,
    @SerialName("opening_hours") val openingHours: OpeningHoursDto? = null,
)

/**
 * An Open Location Code ("plus code") as it leads a `formatted_address` component -
 * e.g. the "H99M+X77" in "H99M+X77, Sidon, Lebanon", or, for a place with no mapped
 * street at all, glued straight onto a description with no comma, as in
 * "VFRM+84M Near Swiss Butter Verdun, Bayrut, Lebanon". Google substitutes one
 * whenever a place has no street address. It's a valid location and a useless label,
 * so it gets stripped in favour of whatever readable text is left in that component.
 */
private val PLUS_CODE_PREFIX = Regex("""^[23456789CFGHJMPQRVWX]{4,8}\+[23456789CFGHJMPQRVWX]{2,3}\s*""")

/**
 * The most human-readable address available: the formatted address with any leading
 * plus code stripped from each component, falling back to `vicinity` when stripping
 * leaves nothing behind.
 */
private fun PlaceDto.readableAddress(): String? {
    val withoutPlusCode = formattedAddress
        ?.split(',')
        ?.map { PLUS_CODE_PREFIX.replace(it.trim(), "").trim() }
        ?.filter { it.isNotEmpty() }
        ?.joinToString(", ")
        ?.takeIf { it.isNotBlank() }
    return withoutPlusCode ?: vicinity?.takeIf { it.isNotBlank() }
}

@Serializable
private data class GeometryDto(val location: LocationDto? = null)

@Serializable
private data class LocationDto(val lat: Double, val lng: Double)
