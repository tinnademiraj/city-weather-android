package com.example.cityweather

import com.example.cityweather.domain.*
import com.example.cityweather.ui.WeatherViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    private class MemoryPreferences : Preferences {
        var saved = featuredCities.take(2)
        var temperatureUnit = TemperatureUnit.CELSIUS
        override fun cities() = saved
        override fun saveCities(cities: List<City>) { saved = cities }
        override fun unit() = temperatureUnit
        override fun saveUnit(unit: TemperatureUnit) { temperatureUnit = unit }
    }
    private fun sample(city: City) = Weather(city, "Europe/London",
        CurrentWeather(LocalDateTime.parse("2026-10-22T12:00"), 15.0, 14.0, 70, 8.0, 0, true),
        emptyList(), emptyList(), 1)
    @Test fun changingCityCancelsOlderRequest() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = object : WeatherRepository {
                override suspend fun weather(city: City): Weather {
                    delay(if (city == featuredCities[0]) 1000 else 10)
                    return sample(city)
                }
                override suspend fun search(query: String) = emptyList<City>()
            }
            val vm = WeatherViewModel(repo, MemoryPreferences())
            runCurrent()
            vm.selectCity(featuredCities[1])
            advanceUntilIdle()
            assertEquals(featuredCities[1], vm.state.value.weather?.city)
            assertFalse(vm.state.value.loading)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun failedRefreshKeepsVisibleWeatherAndOffersError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var fail = false
            val repo = object : WeatherRepository {
                override suspend fun weather(city: City): Weather {
                    if (fail) throw java.io.IOException("offline")
                    return sample(city)
                }
                override suspend fun search(query: String) = emptyList<City>()
            }
            val vm = WeatherViewModel(repo, MemoryPreferences())
            advanceUntilIdle()
            fail = true
            vm.refresh()
            advanceUntilIdle()
            assertNotNull(vm.state.value.weather)
            assertNotNull(vm.state.value.error)
            assertFalse(vm.state.value.loading)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun changingSearchCancelsOldResults() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = object : WeatherRepository {
                override suspend fun weather(city: City) = sample(city)
                override suspend fun search(query: String): List<City> { delay(1000); return featuredCities }
            }
            val vm = WeatherViewModel(repo, MemoryPreferences())
            vm.queryChanged("London")
            vm.search()
            runCurrent()
            vm.queryChanged("Tokyo")
            advanceUntilIdle()
            assertTrue(vm.state.value.results.isEmpty())
            assertFalse(vm.state.value.searching)
            assertEquals("Tokyo", vm.state.value.query)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun citiesAreDeduplicatedAndUnitIsPersisted() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val prefs = MemoryPreferences()
            val repo = object : WeatherRepository {
                override suspend fun weather(city: City) = sample(city)
                override suspend fun search(query: String) = emptyList<City>()
            }
            val vm = WeatherViewModel(repo, prefs)
            vm.saveCity(featuredCities[0])
            assertEquals(2, prefs.saved.size)
            vm.setUnit(TemperatureUnit.FAHRENHEIT)
            assertEquals(TemperatureUnit.FAHRENHEIT, prefs.temperatureUnit)
            advanceUntilIdle()
        } finally { Dispatchers.resetMain() }
    }
    @Test fun temperaturesAndWeatherCodesHaveCorrectMeaning() {
        assertEquals("32°F", TemperatureUnit.FAHRENHEIT.format(0.0))
        assertEquals("-40°F", TemperatureUnit.FAHRENHEIT.format(-40.0))
        assertEquals("100°C", TemperatureUnit.CELSIUS.format(100.0))
        assertEquals("Thunderstorm", condition(99).label)
        assertEquals("Unknown conditions", condition(999).label)
        assertEquals("☾", condition(0, false).symbol)
    }
}
