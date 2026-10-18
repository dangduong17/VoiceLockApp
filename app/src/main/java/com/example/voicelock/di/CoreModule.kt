package com.example.voicelock.di

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.voicelock.data.local.appLockDataStore
import com.example.voicelock.data.local.createSecurePrefs
import com.example.voicelock.data.local.face.FaceRecognitionEngine
import com.example.voicelock.data.local.voice.AudioRecorderHelper
import com.example.voicelock.data.local.voice.KeywordAudioRecognizer
import com.example.voicelock.data.local.voice.VoiceBiometricsManager
import com.example.voicelock.service.AppLockMonitorLauncher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single<SharedPreferences> { createSecurePrefs(androidContext()) }
    single<DataStore<Preferences>> { androidContext().appLockDataStore }
    single { VoiceBiometricsManager(context = androidContext(), securePrefs = get()) }
    single { FaceRecognitionEngine(context = androidContext()) }
    single { AppLockMonitorLauncher(context = androidContext(), getLockedPackagesUseCase = get(), getFloatingLockEnabledUseCase = get()) }
    factory { AudioRecorderHelper() }
    factory { KeywordAudioRecognizer(androidContext()) }
}
