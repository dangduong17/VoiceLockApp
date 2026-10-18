package com.example.voicelock.data.repository.appLockRepository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.voicelock.util.Result
import com.example.voicelock.util.resultFlow
import com.example.voicelock.util.catchAsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AppLockRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : AppLockRepository {

    override fun getLockedPackages(): Flow<Result<Set<String>>> =
        dataStore.data
            .map<Preferences, Result<Set<String>>> { preferences -> Result.Success(preferences[KEY_LOCKED_PACKAGES] ?: emptySet()) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun setLocked(
        packageName: String,
        isLocked: Boolean,
    ): Flow<Result<Unit>> =
        resultFlow {
            dataStore.edit { preferences ->
                val current = preferences[KEY_LOCKED_PACKAGES] ?: emptySet()
                preferences[KEY_LOCKED_PACKAGES] = if (isLocked) current + packageName else current - packageName
            }
            Unit
        }

    companion object {
        private val KEY_LOCKED_PACKAGES = stringSetPreferencesKey("locked_packages")
    }
}
