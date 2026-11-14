package com.freemind.ai.data.remote.weather

import com.freemind.ai.domain.model.DayForecast
import com.freemind.ai.domain.model.weatherKindOf
import com.freemind.ai.util.AppError
import com.freemind.ai.util.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** Open-Meteo reaches 16 days ahead; asking for a date past that returns an error. */
const val FORECAST_HORIZON_DAYS = 16

/**
 * Daily forecasts from Open-Meteo (https://open-meteo.com) - free and keyless, so no
 * secret ships in the app and no account is needed.
 */
@Singleton
class WeatherDataSource @Inject constructor(
    private val httpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Forecasts for [start]..[end] inclusive, clipped to what Open-Meteo can supply. */
    suspend fun forecast(
        latitude: Double,
        longitude: Double,
        start: LocalDate,
        end: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): AppResult<List<DayForecast>> = withContext(Dispatchers.IO) {
        val from = maxOf(start, today)
        val to = minOf(end, today.plusDays((FORECAST_HORIZON_DAYS - 1).toLong()))
        if (from > to) return@withContext AppResult.Success(emptyList())

        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=auto&start_date=$from&end_date=$to"
        val body = try {
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext AppResult.Failure(AppError.Unknown("Forecast unavailable (${response.code})."))
                }
                response.body.string()
            }
        } catch (io: IOException) {
            return@withContext AppResult.Failure(AppError.Network)
        }

        val parsed = try {
            json.decodeFromString<ForecastResponse>(body)
        } catch (e: Exception) {
            return@withContext AppResult.Failure(AppError.Unknown("Couldn't read the forecast."))
        }
        val daily = parsed.daily ?: return@withContext AppResult.Success(emptyList())

        AppResult.Success(
            daily.time.mapIndexedNotNull { i, date ->
                val max = daily.temperatureMax.getOrNull(i) ?: return@mapIndexedNotNull null
                val min = daily.temperatureMin.getOrNull(i) ?: return@mapIndexedNotNull null
                DayForecast(
                    date = LocalDate.parse(date),
                    tempMaxC = max.roundToInt(),
                    tempMinC = min.roundToInt(),
                    rainChance = daily.rainChance.getOrNull(i) ?: 0,
                    kind = weatherKindOf(daily.weatherCode.getOrNull(i) ?: 3),
                )
            },
        )
    }
}

@Serializable
private data class ForecastResponse(val daily: DailyDto? = null)

@Serializable
private data class DailyDto(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val rainChance: List<Int?> = emptyList(),
)
