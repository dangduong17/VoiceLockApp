package com.example.voicelock.domain.usecase.patternlock

import com.example.voicelock.data.repository.lockRepository.LockRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class SetPatternUseCase(
    private val lockRepository: LockRepository,
) {
    operator fun invoke(cells: List<Int>): Flow<Result<Unit>> = lockRepository.savePattern(cells)
}
