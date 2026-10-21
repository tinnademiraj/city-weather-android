# City Weather

A Kotlin weather app with Jetpack Compose, MVVM, live forecasts, city search, and offline caching.

## Open and run

1. Open **this CityWeather folder** in Android Studio (File → Open).
2. Let Gradle sync. Install Android SDK Platform 36 and Build Tools 36.0.0 if prompted.
3. Under Settings → Build, Execution, Deployment → Build Tools → Gradle, select **JDK 17**. A newer bundled JDK may not work with Gradle 8.13.
4. Choose an emulator or USB device running Android 8.0 / API 26 or newer. Press Run ▶.
5. Internet is needed for the first forecast. No API key or account is needed.

Android Studio generates `local.properties` for your Android SDK. Local SDK paths, signing keys, IDE files, and build output are excluded from version control.

## What to demonstrate

- **Today:** real current weather, feels-like temperature, humidity, wind, and the next 24 hours.
- **Cities:** London, Paris, New York, Tokyo, Belgrade, and Sydney; search for more cities through the geocoding API. Tap a city to view it; Save/Remove manages favorites.
- **Forecast:** seven days, high/low temperatures, rain probability, sunrise, and sunset.
- **Settings:** switch Celsius/Fahrenheit; the choice and saved cities survive app restarts.
- **Resilience:** visible loading and retry states, cancellation when changing city/search, and a clearly labeled last-successful forecast when offline.

## Architecture

```
Compose screen → ViewModel action → WeatherRepository → Retrofit → Open-Meteo
Compose screen ← lifecycle-aware StateFlow ← immutable WeatherUiState
                                         ↘ device preferences / forecast cache
```

- `domain/Models.kt`: UI-independent models, repository/preferences contracts, units, WMO weather conditions.
- `data/WeatherApi.kt`: Retrofit endpoints and network DTOs.
- `data/LiveWeatherRepository.kt`: DTO mapping, network calls, persistent cache, local preferences.
- `ui/WeatherViewModel.kt`: immutable state, coroutine jobs, loading/errors, city and unit actions.
- `ui/WeatherApp.kt`: Compose UI and four Navigation Compose destinations.
- `WeatherApplication.kt`: small manual dependency container. Dependencies are injected through constructors so fakes work in unit tests.
- `MainActivity.kt`: app entry point and ViewModel creation.

The UI never invokes Retrofit directly. The repository does not know about Compose. Domain values stay in Celsius; display conversion happens at the edge. Forecast times are local to the chosen city, not the device.

## Build and test

From a terminal in this folder, with JDK 17 selected:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
sh ./gradlew assembleDebug testDebugUnitTest lintDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
Tests cover canceled requests, keeping visible weather on failed refresh, stale-search cancellation, saved-city deduplication, persisted unit choice, weather codes and conversions, and mapping an actual API response fixture.

## APIs and attribution

- Weather: https://open-meteo.com/en/docs
- City search: https://open-meteo.com/en/docs/geocoding-api
- Weather data by Open-Meteo, CC BY 4.0: https://creativecommons.org/licenses/by/4.0/
- Geocoding data originates from GeoNames via Open-Meteo.

Open-Meteo's free endpoint is intended for non-commercial use; check its terms before commercial deployment. Current weather comes from weather model data, rather than a guaranteed nearby physical station reading.

## Deliberate scope and next steps

This is a small interview app. A single activity-scoped ViewModel shares the selected city across Today and Forecast and survives rotation. Process death restores saved cities/units but resets temporary navigation/search and selects the first saved city. The cache is a last-successful snapshot with an explicit timestamp, not a background sync engine; it has no automatic expiry or eviction. A city must have loaded once before offline viewing works.

Preferences/cache use SharedPreferences for a small, readable demo. For a larger app, use DataStore for settings, Room for cached forecasts, inject dispatchers/clock, split screen ViewModels, add paging/cache eviction, localization, adaptive layouts and instrumented accessibility/UI tests. There is no precise device-location feature or API secret to protect.
