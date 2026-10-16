package com.example.voicelock.domain.usecase.applock

import com.example.voicelock.data.repository.appLockSessionRepository.AppLockSessionRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class IsAppUnlockedUseCase(
    private val appLockSessionRepository: AppLockSessionRepository,
) {
    operator fun invoke(packageName: String): Flow<Result<Boolean>> = appLockSessionRepository.isUnlocked(packageName)
}
