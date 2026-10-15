package com.example.voicelock.di

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

val networkModule = module {
    single<Gson> {
        GsonBuilder()
            .setLenient()
            .create()
    }

    single<OkHttpClient> {
        val cacheDir = File(androidContext().cacheDir, "http_cache")
        val cache = Cache(cacheDir, 10L * 1024L * 1024L)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }
}
