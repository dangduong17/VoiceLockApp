package com.example.voicelock.di

import com.example.voicelock.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

private const val HTTP_CACHE_DIR = "http_cache"
private const val HTTP_CACHE_SIZE_BYTES = 10L * 1024L * 1024L
private const val HTTP_TIMEOUT_SECONDS = 30L

val networkModule = module {
    single<Gson> {
        GsonBuilder()
            .setLenient()
            .create()
    }

    single<OkHttpClient> {
        val cache = Cache(File(androidContext().cacheDir, HTTP_CACHE_DIR), HTTP_CACHE_SIZE_BYTES)

        // Request / response bodies are logged in debug builds only.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }
}
