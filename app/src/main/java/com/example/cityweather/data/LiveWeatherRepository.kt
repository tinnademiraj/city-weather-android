package com.example.cityweather.data

import android.content.SharedPreferences
import com.example.cityweather.domain.*
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

class LiveWeatherRepository(private val api: WeatherApi, private val geo: GeocodingApi,
    private val cache: SharedPreferences, private val gson: Gson) : WeatherRepository {
    override suspend fun weather(city: City): Weather = withContext(Dispatchers.IO) {
        val key = "weather_${city.id}"
        try {
            val dto = api.forecast(city.latitude, city.longitude)
            val fetchedAt = System.currentTimeMillis()
            val mapped = dto.toWeather(city, fetchedAt)
            cache.edit().putString(key, gson.toJson(dto)).putLong("${key}_time", fetchedAt).apply()
            mapped
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val stored = cache.getString(key, null) ?: throw error
            try {
                gson.fromJson(stored, ForecastDto::class.java)
                    .toWeather(city, cache.getLong("${key}_time", 0L)).copy(cached = true)
            } catch (invalidCache: Exception) {
                throw error
            }
        }
    }
    override suspend fun search(query: String): List<City> = geo.search(query).results.orEmpty().map {
        City(it.id, it.name, it.country ?: "", it.latitude, it.longitude)
    }
}
fun ForecastDto.toWeather(city: City, fetchedAt: Long): Weather {
    val c = current
    return Weather(city, timezone,
        CurrentWeather(LocalDateTime.parse(c.time), c.temperature, c.feelsLike, c.humidity, c.wind, c.code, c.isDay == 1),
        daily.time.indices.map { i -> DayForecast(LocalDate.parse(daily.time[i]), daily.codes[i],
            daily.highs[i], daily.lows[i], daily.rain[i], daily.sunrise[i].substringAfter('T'), daily.sunset[i].substringAfter('T')) },
        hourly.time.indices.map { i -> HourForecast(LocalDateTime.parse(hourly.time[i]), hourly.temperatures[i], hourly.codes[i]) }, fetchedAt)
}
class LocalPreferences(private val prefs: SharedPreferences, private val gson: Gson) : Preferences {
    override fun cities(): List<City> = runCatching {
        prefs.getString("cities", null)?.let { gson.fromJson(it, Array<City>::class.java).toList() }
    }.getOrNull() ?: featuredCities.take(3)
    override fun saveCities(cities: List<City>) { prefs.edit().putString("cities", gson.toJson(cities)).apply() }
    override fun unit(): TemperatureUnit = runCatching {
        TemperatureUnit.valueOf(prefs.getString("unit", null) ?: "CELSIUS")
    }.getOrDefault(TemperatureUnit.CELSIUS)
    override fun saveUnit(unit: TemperatureUnit) { prefs.edit().putString("unit", unit.name).apply() }
}
