package com.example.voicelock.data.repository.appLockSessionRepository

import com.example.voicelock.util.Constant
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-lifetime, in-memory record of "recently verified" packages, so returning to a just
 * unlocked app's internal navigation within [Constant.APP_LOCK_SESSION_TTL_MS] doesn't re-prompt.
 */
class AppLockSessionRepositoryImpl : AppLockSessionRepository {

    private val unlockedAt = ConcurrentHashMap<String, Long>()

    override fun markUnlocked(packageName: String): Flow<Result<Unit>> =
        flow<Result<Unit>> {
            unlockedAt[packageName] = System.currentTimeMillis()
            emit(Result.Success(Unit))
        }.flowOn(Dispatchers.Default)
            .catchAsResult()

    override fun isUnlocked(packageName: String): Flow<Result<Boolean>> =
        flow<Result<Boolean>> {
            val unlockedTimestamp = unlockedAt[packageName]
            val isStillValid =
                unlockedTimestamp != null &&
                    (System.currentTimeMillis() - unlockedTimestamp) < Constant.APP_LOCK_SESSION_TTL_MS
            emit(Result.Success(isStillValid))
        }.flowOn(Dispatchers.Default)
            .catchAsResult()

    override fun clearAll(): Flow<Result<Unit>> =
        flow<Result<Unit>> {
            unlockedAt.clear()
            emit(Result.Success(Unit))
        }.flowOn(Dispatchers.Default)
            .catchAsResult()
}
