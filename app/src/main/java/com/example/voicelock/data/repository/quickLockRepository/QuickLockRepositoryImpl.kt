package com.example.voicelock.data.repository.quickLockRepository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.voicelock.util.Result
import com.example.voicelock.util.resultFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class QuickLockRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : QuickLockRepository {

    override fun isFloatingLockEnabled(): Flow<Result<Boolean>> =
        resultFlow { dataStore.data.first()[KEY_FLOATING_LOCK_ENABLED] ?: false }

    override fun setFloatingLockEnabled(isEnabled: Boolean): Flow<Result<Unit>> =
        resultFlow {
            dataStore.edit { preferences -> preferences[KEY_FLOATING_LOCK_ENABLED] = isEnabled }
            Unit
        }

    companion object {
        private val KEY_FLOATING_LOCK_ENABLED = booleanPreferencesKey("floating_lock_enabled")
    }
}
