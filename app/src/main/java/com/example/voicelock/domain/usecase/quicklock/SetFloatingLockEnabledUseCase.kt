package com.example.voicelock.domain.usecase.quicklock

import com.example.voicelock.data.repository.quickLockRepository.QuickLockRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class SetFloatingLockEnabledUseCase(
    private val quickLockRepository: QuickLockRepository,
) {
    operator fun invoke(isEnabled: Boolean): Flow<Result<Unit>> = quickLockRepository.setFloatingLockEnabled(isEnabled)
}
