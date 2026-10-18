package com.example.voicelock.domain.usecase.quicklock

import com.example.voicelock.data.repository.quickLockRepository.QuickLockRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class GetFloatingLockEnabledUseCase(
    private val quickLockRepository: QuickLockRepository,
) {
    operator fun invoke(): Flow<Result<Boolean>> = quickLockRepository.isFloatingLockEnabled()
}
