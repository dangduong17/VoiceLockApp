package com.example.voicelock.domain.usecase.applock

import com.example.voicelock.data.repository.appLockRepository.AppLockRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class GetLockedPackagesUseCase(
    private val appLockRepository: AppLockRepository,
) {
    operator fun invoke(): Flow<Result<Set<String>>> = appLockRepository.getLockedPackages()
}
