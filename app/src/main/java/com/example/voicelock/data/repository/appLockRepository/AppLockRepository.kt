package com.example.voicelock.data.repository.appLockRepository

import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    fun getLockedPackages(): Flow<Result<Set<String>>>

    fun setLocked(
        packageName: String,
        isLocked: Boolean,
    ): Flow<Result<Unit>>
}
