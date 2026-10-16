package com.example.voicelock.domain.usecase.applock

import com.example.voicelock.data.repository.appLockRepository.AppLockRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class SetAppLockedUseCase(
    private val appLockRepository: AppLockRepository,
) {
    operator fun invoke(
        packageName: String,
        isLocked: Boolean,
    ): Flow<Result<Unit>> = appLockRepository.setLocked(packageName = packageName, isLocked = isLocked)
}
