package com.example.cityweather

import com.example.cityweather.data.ForecastDto
import com.example.cityweather.data.toWeather
import com.example.cityweather.domain.featuredCities
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ForecastMappingTest {
    @Test fun realApiFixtureMapsDailyAndHourlyValues() {
        val json = javaClass.getResource("/forecast.json")!!.readText()
        val dto = Gson().fromJson(json, ForecastDto::class.java)
        val weather = dto.toWeather(featuredCities[0], 123L)
        assertEquals("Europe/London", weather.timezone)
        assertEquals(7, weather.days.size)
        assertEquals(168, weather.hours.size)
        assertEquals(dto.current.temperature, weather.current.temperature, 0.001)
        assertEquals(dto.daily.highs[0], weather.days[0].high, 0.001)
        assertEquals(dto.hourly.temperatures[24], weather.hours[24].temperature, 0.001)
        assertEquals(123L, weather.fetchedAt)
        assertFalse(weather.cached)
    }
}
