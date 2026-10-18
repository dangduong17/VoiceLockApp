package com.example.voicelock.data.repository.appLockSessionRepository

import com.example.voicelock.util.Constant
import com.example.voicelock.util.Result
import com.example.voicelock.util.resultFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-lifetime, in-memory record of "recently verified" packages, so returning to a just
 * unlocked app's internal navigation within [Constant.APP_LOCK_SESSION_TTL_MS] doesn't re-prompt.
 */
class AppLockSessionRepositoryImpl : AppLockSessionRepository {

    private val unlockedAt = ConcurrentHashMap<String, Long>()

    override fun markUnlocked(packageName: String): Flow<Result<Unit>> =
        resultFlow(Dispatchers.Default) {
            unlockedAt[packageName] = System.currentTimeMillis()
            Unit
        }

    override fun isUnlocked(packageName: String): Flow<Result<Boolean>> =
        resultFlow(Dispatchers.Default) {
            val unlockedTimestamp = unlockedAt[packageName]
            val isStillValid =
                unlockedTimestamp != null &&
                    (System.currentTimeMillis() - unlockedTimestamp) < Constant.APP_LOCK_SESSION_TTL_MS
            isStillValid
        }

    override fun clearAll(): Flow<Result<Unit>> =
        resultFlow(Dispatchers.Default) {
            unlockedAt.clear()
            Unit
        }
}
