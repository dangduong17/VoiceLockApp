package com.example.voicelock.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val APP_LOCK_DATASTORE_NAME = "app_lock_prefs"

val Context.appLockDataStore: DataStore<Preferences> by preferencesDataStore(name = APP_LOCK_DATASTORE_NAME)
