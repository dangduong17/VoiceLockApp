package com.example.voicelock.data.repository.appLockSessionRepository

import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

interface AppLockSessionRepository {
    fun markUnlocked(packageName: String): Flow<Result<Unit>>

    fun isUnlocked(packageName: String): Flow<Result<Boolean>>
}
