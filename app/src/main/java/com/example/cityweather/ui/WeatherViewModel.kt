package com.example.cityweather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityweather.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(val cities: List<City>, val selected: City?, val unit: TemperatureUnit,
    val weather: Weather? = null, val loading: Boolean = false, val error: String? = null,
    val query: String = "", val results: List<City> = emptyList(), val searching: Boolean = false,
    val searchError: String? = null, val searched: Boolean = false)

class WeatherViewModel(private val repository: WeatherRepository, private val preferences: Preferences) : ViewModel() {
    private val initialCities = preferences.cities()
    private val _state = MutableStateFlow(WeatherUiState(initialCities, initialCities.firstOrNull(), preferences.unit()))
    val state = _state.asStateFlow()
    private var weatherJob: Job? = null
    private var searchJob: Job? = null
    init { _state.value.selected?.let(::selectCity) }
    fun selectCity(city: City) {
        weatherJob?.cancel()
        _state.update { it.copy(selected = city, weather = if (it.selected?.id == city.id) it.weather else null,
            loading = true, error = null) }
        weatherJob = viewModelScope.launch {
            try {
                val weather = repository.weather(city)
                _state.update { it.copy(weather = weather, loading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                _state.update { it.copy(loading = false, error = "Couldn’t load weather. Check your connection and try again.") }
            }
        }
    }
    fun refresh() { _state.value.selected?.let(::selectCity) }
    fun queryChanged(query: String) {
        searchJob?.cancel()
        _state.update { it.copy(query = query, searching = false, searched = false, results = emptyList(), searchError = null) }
    }
    fun search() {
        searchJob?.cancel()
        val query = _state.value.query.trim()
        if (query.length < 2) return
        searchJob = viewModelScope.launch {
            _state.update { it.copy(searching = true, searchError = null, searched = true) }
            try {
                val results = repository.search(query)
                _state.update { it.copy(results = results, searching = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                _state.update { it.copy(searching = false, searchError = "Search is unavailable. Try again or choose a featured city.") }
            }
        }
    }
    fun saveCity(city: City) {
        val cities = (_state.value.cities + city).distinctBy { it.id }
        preferences.saveCities(cities)
        _state.update { it.copy(cities = cities) }
    }
    fun removeCity(city: City) {
        val cities = _state.value.cities.filterNot { it.id == city.id }
        preferences.saveCities(cities)
        _state.update { it.copy(cities = cities) }
    }
    fun setUnit(unit: TemperatureUnit) {
        preferences.saveUnit(unit)
        _state.update { it.copy(unit = unit) }
    }
}
