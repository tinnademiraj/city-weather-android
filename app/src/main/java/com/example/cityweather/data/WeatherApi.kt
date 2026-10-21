package com.example.cityweather.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset",
        @Query("hourly") hourly: String = "temperature_2m,weather_code",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") days: Int = 7
    ): ForecastDto
}
interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(@Query("name") name: String, @Query("count") count: Int = 12,
        @Query("language") language: String = "en"): SearchDto
}
data class SearchDto(val results: List<CityDto>?)
data class CityDto(val id: Int, val name: String, val country: String?, val latitude: Double, val longitude: Double)
data class ForecastDto(val timezone: String, val current: CurrentDto, val daily: DailyDto, val hourly: HourlyDto)
data class CurrentDto(val time: String,
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("apparent_temperature") val feelsLike: Double,
    @SerializedName("relative_humidity_2m") val humidity: Int,
    @SerializedName("wind_speed_10m") val wind: Double,
    @SerializedName("weather_code") val code: Int,
    @SerializedName("is_day") val isDay: Int)
data class DailyDto(val time: List<String>,
    @SerializedName("weather_code") val codes: List<Int>,
    @SerializedName("temperature_2m_max") val highs: List<Double>,
    @SerializedName("temperature_2m_min") val lows: List<Double>,
    @SerializedName("precipitation_probability_max") val rain: List<Int?>,
    val sunrise: List<String>, val sunset: List<String>)
data class HourlyDto(val time: List<String>,
    @SerializedName("temperature_2m") val temperatures: List<Double>,
    @SerializedName("weather_code") val codes: List<Int>)
