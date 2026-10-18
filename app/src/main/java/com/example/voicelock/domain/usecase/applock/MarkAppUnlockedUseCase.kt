package com.example.voicelock.domain.usecase.applock

import com.example.voicelock.data.repository.appLockSessionRepository.AppLockSessionRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Records a successful unlock of [packageName] so the monitor lets that app through. A blank or
 * null package (verify screens opened from Home / Settings, not the lock overlay) does nothing.
 */
class MarkAppUnlockedUseCase(
    private val appLockSessionRepository: AppLockSessionRepository,
) {
    operator fun invoke(packageName: String?): Flow<Result<Unit>> =
        if (packageName.isNullOrBlank()) flowOf(Result.Success(Unit)) else appLockSessionRepository.markUnlocked(packageName)
}
