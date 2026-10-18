package com.example.voicelock.data.repository.quickLockRepository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class QuickLockRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : QuickLockRepository {

    override fun isFloatingLockEnabled(): Flow<Result<Boolean>> =
        flow<Result<Boolean>> { emit(Result.Success(dataStore.data.first()[KEY_FLOATING_LOCK_ENABLED] ?: false)) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun setFloatingLockEnabled(isEnabled: Boolean): Flow<Result<Unit>> =
        flow<Result<Unit>> {
            dataStore.edit { preferences -> preferences[KEY_FLOATING_LOCK_ENABLED] = isEnabled }
            emit(Result.Success(Unit))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    companion object {
        private val KEY_FLOATING_LOCK_ENABLED = booleanPreferencesKey("floating_lock_enabled")
    }
}
