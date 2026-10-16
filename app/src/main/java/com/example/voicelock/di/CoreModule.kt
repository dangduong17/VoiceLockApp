package com.example.voicelock.di

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.voicelock.data.local.appLockDataStore
import com.example.voicelock.data.local.createSecurePrefs
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single<SharedPreferences> { createSecurePrefs(androidContext()) }
    single<DataStore<Preferences>> { androidContext().appLockDataStore }
}
