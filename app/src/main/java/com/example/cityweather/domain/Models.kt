package com.example.cityweather.domain

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

data class City(val id: Int, val name: String, val country: String, val latitude: Double, val longitude: Double) {
    val subtitle get() = country
}
val featuredCities = listOf(
    City(2643743, "London", "United Kingdom", 51.5085, -0.1257),
    City(2988507, "Paris", "France", 48.8534, 2.3488),
    City(5128581, "New York", "United States", 40.7143, -74.006),
    City(1850147, "Tokyo", "Japan", 35.6895, 139.6917),
    City(786714, "Prishtina", "Kosovo", 42.6727, 21.1669),
    City(2147714, "Sydney", "Australia", -33.8679, 151.2073)
)
data class CurrentWeather(val time: LocalDateTime, val temperature: Double, val feelsLike: Double,
    val humidity: Int, val wind: Double, val code: Int, val isDay: Boolean)
data class DayForecast(val date: LocalDate, val code: Int, val high: Double, val low: Double,
    val rainChance: Int?, val sunrise: String, val sunset: String)
data class HourForecast(val time: LocalDateTime, val temperature: Double, val code: Int)
data class Weather(val city: City, val timezone: String, val current: CurrentWeather,
    val days: List<DayForecast>, val hours: List<HourForecast>, val fetchedAt: Long,
    val cached: Boolean = false)
enum class TemperatureUnit(val label: String) {
    CELSIUS("Celsius"), FAHRENHEIT("Fahrenheit");
    fun format(celsius: Double): String = "${(if (this == CELSIUS) celsius else celsius * 9 / 5 + 32).roundToInt()}°${if (this == CELSIUS) "C" else "F"}"
}
data class Condition(val label: String, val symbol: String)
fun condition(code: Int, isDay: Boolean = true): Condition = when (code) {
    0 -> Condition("Clear sky", if (isDay) "☀" else "☾")
    1, 2 -> Condition("Partly cloudy", "⛅")
    3 -> Condition("Overcast", "☁")
    45, 48 -> Condition("Foggy", "≋")
    51, 53, 55, 56, 57 -> Condition("Drizzle", "☂")
    61, 63, 65, 66, 67, 80, 81, 82 -> Condition("Rain", "☂")
    71, 73, 75, 77, 85, 86 -> Condition("Snow", "❄")
    95, 96, 99 -> Condition("Thunderstorm", "ϟ")
    else -> Condition("Unknown conditions", "—")
}
interface WeatherRepository {
    suspend fun weather(city: City): Weather
    suspend fun search(query: String): List<City>
}
interface Preferences {
    fun cities(): List<City>
    fun saveCities(cities: List<City>)
    fun unit(): TemperatureUnit
    fun saveUnit(unit: TemperatureUnit)
}
