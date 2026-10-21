package com.example.cityweather

import android.app.Application
import com.example.cityweather.data.*
import com.example.cityweather.domain.WeatherRepository
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class WeatherApplication : Application() {
    private val gson by lazy { Gson() }
    private val client by lazy { OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build() }
    private fun retrofit(url: String) = Retrofit.Builder().baseUrl(url).client(client)
        .addConverterFactory(GsonConverterFactory.create(gson)).build()
    val repository: WeatherRepository by lazy {
        LiveWeatherRepository(retrofit("https://api.open-meteo.com/").create(WeatherApi::class.java),
            retrofit("https://geocoding-api.open-meteo.com/").create(GeocodingApi::class.java),
            getSharedPreferences("forecast_cache", MODE_PRIVATE), gson)
    }
    val preferences by lazy { LocalPreferences(getSharedPreferences("preferences", MODE_PRIVATE), gson) }
}
