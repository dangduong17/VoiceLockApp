package com.example.voicelock.domain.usecase.patternlock

import com.example.voicelock.data.repository.lockRepository.LockRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class GetPatternInfoUseCase(
    private val lockRepository: LockRepository,
) {
    operator fun invoke(): Flow<Result<Boolean>> = lockRepository.isPatternSet()
}
