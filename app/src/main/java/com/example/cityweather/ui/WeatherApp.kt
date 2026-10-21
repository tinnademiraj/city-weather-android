package com.example.cityweather.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.example.cityweather.domain.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val Ink = Color(0xFF123B42)
private val Mist = Color(0xFFF3F6F2)
private val Muted = Color(0xFF5C7476)
private val Gold = Color(0xFFF0CB87)
private val scheme = lightColorScheme(primary = Ink, secondary = Color(0xFF55796B),
    background = Mist, surface = Color.White, surfaceVariant = Color(0xFFE5EDE6),
    onPrimary = Color.White, onSurface = Ink, onBackground = Ink)
private val dayFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

@Composable
fun WeatherApp(vm: WeatherViewModel) {
    MaterialTheme(colorScheme = scheme) {
        val state by vm.state.collectAsStateWithLifecycle()
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route ?: "today"
        Scaffold(containerColor = Mist, bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf(Triple("cities", "◎", "Cities"), Triple("today", "☀", "Today"),
                    Triple("forecast", "▥", "Forecast"), Triple("settings", "⚙", "Settings")).forEach { (target, icon, title) ->
                    NavigationBarItem(selected = route == target, onClick = {
                        nav.navigate(target) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true }
                    }, icon = { Text(icon, fontSize = 23.sp) }, label = { Text(title) })
                }
            }
        }) { padding ->
            NavHost(nav, startDestination = "today", modifier = Modifier.padding(padding)) {
                composable("today") { WeatherScreen(state, vm::refresh, { nav.navigate("forecast") { launchSingleTop = true } }, { nav.navigate("cities") }) }
                composable("forecast") { ForecastScreen(state, vm::refresh, { nav.navigate("cities") }) }
                composable("cities") { CitiesScreen(state, vm, onSelect = {
                    vm.selectCity(it)
                    nav.navigate("today") { popUpTo(nav.graph.startDestinationId); launchSingleTop = true }
                }) }
                composable("settings") { SettingsScreen(state.unit, vm::setUnit) }
            }
        }
    }
}

@Composable
private fun PageTitle(eyebrow: String, title: String, subtitle: String) {
    Text(eyebrow.uppercase(Locale.ENGLISH), color = Muted, letterSpacing = 2.sp, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ink)
    Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
}
@Composable
private fun SectionTitle(title: String) { Text(title, fontSize = 20.sp, color = Ink, fontWeight = FontWeight.SemiBold) }
@Composable
private fun Notice(message: String, action: String? = null, onAction: () -> Unit = {}) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEED1))) {
        Column(Modifier.padding(16.dp)) {
            Text(message, color = Ink)
            if (action != null) TextButton(onClick = onAction) { Text(action) }
        }
    }
}
@Composable
private fun WeatherScreen(state: WeatherUiState, refresh: () -> Unit, forecast: () -> Unit, cities: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { PageTitle("City Weather / Today", state.selected?.name ?: "Your weather",
                    state.selected?.country ?: "Choose a city to begin") }
                TextButton(onClick = cities) { Text("Change") }
            }
        }
        item { WeatherStatus(state, refresh, cities) }
        state.weather?.let { weather ->
            item { Hero(weather, state.unit) }
            if (weather.cached) item { Notice("Offline · showing saved weather from ${java.time.Instant.ofEpochMilli(weather.fetchedAt).atZone(java.time.ZoneId.of(weather.timezone)).format(DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH))}. Reconnect and refresh for an updated forecast.", "Try again", refresh) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("Feels like", state.unit.format(weather.current.feelsLike), Modifier.weight(1f))
                    Metric("Humidity", "${weather.current.humidity}%", Modifier.weight(1f))
                    Metric("Wind", "${weather.current.wind.roundToInt()} km/h", Modifier.weight(1f))
                }
            }
            item { SectionTitle("The next 24 hours") }
            item {
                val hours = weather.hours.filter { !it.time.isBefore(weather.current.time.withMinute(0).withSecond(0)) }.take(24)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(hours, key = { it.time.toString() }) { hour ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(hour.time.format(timeFormat), color = Muted, fontSize = 12.sp)
                                Text(condition(hour.code).symbol, fontSize = 25.sp)
                                Text(state.unit.format(hour.temperature), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                Card(onClick = forecast, colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E9DD))) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Plan the week ahead", fontWeight = FontWeight.Bold); Text("Explore the 7-day forecast", color = Muted) }
                        Text("→", fontSize = 28.sp)
                    }
                }
            }
            item { Attribution() }
        }
    }
}
@Composable
private fun WeatherStatus(state: WeatherUiState, refresh: () -> Unit, cities: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.selected == null) Notice("Choose a city to see its live weather.", "Explore cities", cities)
        if (state.loading) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Updating forecast…", color = Muted) }
        state.error?.let { Notice(it, "Retry", refresh) }
        if (!state.loading && state.selected != null && state.error == null) {
            TextButton(onClick = refresh, contentPadding = PaddingValues(0.dp)) { Text("↻  Refresh weather") }
        }
    }
}
@Composable
private fun Hero(weather: Weather, unit: TemperatureUnit) {
    val current = weather.current
    val condition = condition(current.code, current.isDay)
    Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Ink, Color(0xFF376D6C))), RoundedCornerShape(28.dp)).padding(26.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(current.time.format(dayFormat), color = Color(0xFFD3E2DA))
                Text(condition.symbol, fontSize = 54.sp, color = Gold)
            }
            Text(unit.format(current.temperature), color = Color.White, fontSize = 68.sp, fontWeight = FontWeight.Light)
            Text(condition.label, color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Medium)
            weather.days.firstOrNull()?.let { Text("High ${unit.format(it.high)}  /  Low ${unit.format(it.low)}", color = Color.White) }
            HorizontalDivider(color = Color.White.copy(alpha = .2f))
            Text("As of ${current.time.format(timeFormat)} · ${weather.timezone}", color = Color(0xFFD3E2DA), fontSize = 12.sp)
        }
    }
}
@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = Muted, fontSize = 12.sp)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
@Composable
private fun ForecastScreen(state: WeatherUiState, refresh: () -> Unit, cities: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageTitle("Look ahead", "The next 7 days", state.selected?.let { "${it.name} · ${it.country}" } ?: "Choose a city first") }
        item { WeatherStatus(state, refresh, cities) }
        state.weather?.let { weather ->
            if (weather.cached) item { Notice("Saved forecast · this data may be out of date. Reconnect and refresh.", "Refresh", refresh) }
            items(weather.days, key = { it.date.toString() }) { day ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(condition(day.code).symbol, fontSize = 30.sp, modifier = Modifier.width(45.dp))
                            Column(Modifier.weight(1f)) {
                                Text(day.date.format(dayFormat), fontWeight = FontWeight.Bold)
                                Text(condition(day.code).label, color = Muted, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(state.unit.format(day.high), fontWeight = FontWeight.Bold)
                                Text(state.unit.format(day.low), color = Muted)
                            }
                        }
                        HorizontalDivider(color = Mist)
                        Text("Rain ${day.rainChance?.let { "$it%" } ?: "—"}  ·  Sunrise ${day.sunrise}  ·  Sunset ${day.sunset}", color = Muted, fontSize = 12.sp)
                    }
                }
            }
            item { Text("Times are local to ${weather.city.name} (${weather.timezone}). Wind is shown in km/h.", color = Muted, fontSize = 12.sp) }
            item { Attribution() }
        }
    }
}
@Composable
private fun CitiesScreen(state: WeatherUiState, vm: WeatherViewModel, onSelect: (City) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageTitle("Around the world", "Find your forecast", "Save the places that matter to you.") }
        item {
            OutlinedTextField(value = state.query, onValueChange = vm::queryChanged, label = { Text("Search for a city") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { vm.search() }))
        }
        item { Button(onClick = vm::search, enabled = state.query.trim().length >= 2 && !state.searching, modifier = Modifier.fillMaxWidth()) { Text(if (state.searching) "Searching…" else "Search cities") } }
        if (state.searching) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.searchError?.let { message -> item { Notice(message, "Retry", vm::search) } }
        if (state.searched && !state.searching && state.searchError == null && state.results.isEmpty()) item { Text("No cities found. Try another name.", color = Muted) }
        if (state.results.isNotEmpty()) {
            item { SectionTitle("Search results") }
            items(state.results, key = { "result_${it.id}" }) { city -> CityRow(city, state.cities.any { it.id == city.id }, onSelect,
                { if (state.cities.any { it.id == city.id }) vm.removeCity(city) else vm.saveCity(city) }) }
        }
        item { SectionTitle("Saved places") }
        if (state.cities.isEmpty()) item { Text("No saved cities yet. Add one from search or the featured list.", color = Muted) }
        items(state.cities, key = { "saved_${it.id}" }) { city -> CityRow(city, true, onSelect, { vm.removeCity(city) }) }
        item { SectionTitle("A little inspiration") }
        items(featuredCities, key = { "featured_${it.id}" }) { city -> CityRow(city, state.cities.any { it.id == city.id }, onSelect,
            { if (state.cities.any { it.id == city.id }) vm.removeCity(city) else vm.saveCity(city) }) }
        item { Text("Tap a city to open its weather. Save adds it to your list for your next visit.", color = Muted, fontSize = 12.sp) }
    }
}
@Composable
private fun CityRow(city: City, saved: Boolean, onSelect: (City) -> Unit, toggle: () -> Unit) {
    Card(onClick = { onSelect(city) }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(city.name, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(city.subtitle, color = Muted, fontSize = 13.sp) }
            TextButton(onClick = toggle) { Text(if (saved) "Remove" else "+ Save") }
            Text("›", fontSize = 25.sp, modifier = Modifier.padding(end = 8.dp))
        }
    }
}
@Composable
private fun SettingsScreen(unit: TemperatureUnit, onUnit: (TemperatureUnit) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { PageTitle("Make it yours", "Settings", "A small forecast, a wider perspective.") }
        item { SectionTitle("Temperature") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp)) {
                    TemperatureUnit.entries.forEach { option ->
                        Surface(onClick = { onUnit(option) }, color = Color.Transparent) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = option == unit, onClick = { onUnit(option) })
                                Text("${option.label} (${if (option == TemperatureUnit.CELSIUS) "°C" else "°F"})")
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle("About the forecast") }
        item { Text("Real weather data from Open-Meteo. Current conditions are weather model estimates. Forecasts can change, so refresh before making plans.", color = Muted) }
        item { Text("Saved cities and units stay on this device. The last successful forecast for each city is stored for offline viewing. No account or location permission is required.", color = Muted) }
        item { Text("Search sends the city name to Open-Meteo’s geocoding service. Forecast requests send the selected city’s coordinates. Your device’s location is never requested.", color = Muted) }
        item { Attribution() }
        item { Text("CITY WEATHER   /   1.0", letterSpacing = 2.sp, color = Muted, fontSize = 11.sp) }
    }
}
@Composable
private fun Attribution() {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    Column {
        Text("Weather data: Open-Meteo · CC BY 4.0", color = Muted, fontSize = 12.sp)
        Text("City search: GeoNames via Open-Meteo", color = Muted, fontSize = 12.sp)
        TextButton(onClick = { uri.openUri("https://open-meteo.com/") }, contentPadding = PaddingValues(0.dp)) { Text("About the data ↗", fontSize = 12.sp) }
    }
}
