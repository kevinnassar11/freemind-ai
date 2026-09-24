package com.freemind.ai.data.remote.supabase

import com.freemind.ai.BuildConfig
import com.freemind.ai.data.remote.firebase.FirebaseAuthDataSource
import com.freemind.ai.util.AppError
import com.freemind.ai.util.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls the `generate-itinerary` Edge Function (see supabase/functions/generate-itinerary)
 * to turn a free-form trip-planning prompt into a Claude-generated paragraph itinerary.
 *
 * Deployed, but inert until the project has an `ANTHROPIC_API_KEY` secret set - see that
 * file's header comment. Until then it 503s and PlacesRepositoryImpl falls back to the
 * on-device ItineraryComposer, same as before it was deployed.
 *
 * Same shape as RoleStampClient: a thin call to a Supabase Edge Function, authenticated
 * with the caller's own Firebase ID token.
 */
@Singleton
class ItineraryEdgeFunctionClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val functionUrl = "${BuildConfig.SUPABASE_URL}/functions/v1/generate-itinerary"

    suspend fun generateItinerary(prompt: String): AppResult<String> = withContext(Dispatchers.IO) {
        val idToken = firebaseAuthDataSource.getIdToken(forceRefresh = false)
            ?: return@withContext AppResult.Failure(AppError.Unauthorized)

        val request = Request.Builder()
            .url(functionUrl)
            .addHeader("Authorization", "Bearer $idToken")
            .post(
                json.encodeToString(ItineraryRequestDto(prompt))
                    .toRequestBody("application/json".toMediaType()),
            )
            .build()

        val response = try {
            okHttpClient.newCall(request).execute()
        } catch (io: IOException) {
            return@withContext AppResult.Failure(AppError.Network)
        }

        response.use {
            val bodyString = it.body.string()
            if (!it.isSuccessful) {
                return@withContext AppResult.Failure(
                    if (it.code == 503) {
                        AppError.Unknown("Trip planning isn't set up yet - the app owner needs to add an Anthropic API key.")
                    } else {
                        AppError.Unknown("Trip planning failed (${it.code}).")
                    },
                )
            }
            val parsed = try {
                json.decodeFromString<ItineraryResponseDto>(bodyString)
            } catch (e: Exception) {
                return@withContext AppResult.Failure(AppError.Unknown("Couldn't read the trip plan response."))
            }
            AppResult.Success(parsed.itinerary)
        }
    }
}

@Serializable
private data class ItineraryRequestDto(val prompt: String)

@Serializable
private data class ItineraryResponseDto(val itinerary: String)
