package com.example.voicelock.domain.usecase.lock

import com.example.voicelock.data.model.lock.LockType
import com.example.voicelock.data.model.lock.LockoutState
import com.example.voicelock.data.repository.lockRepository.LockRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class GetLockoutStateUseCase(
    private val lockRepository: LockRepository,
) {
    operator fun invoke(type: LockType): Flow<Result<LockoutState>> = lockRepository.getLockoutState(type)
}
