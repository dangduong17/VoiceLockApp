package com.example.voicelock.domain.usecase.pinlock

import com.example.voicelock.data.repository.lockRepository.LockRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class SetPinUseCase(
    private val lockRepository: LockRepository,
) {
    operator fun invoke(
        pin: String,
        length: Int,
    ): Flow<Result<Unit>> = lockRepository.savePin(pin = pin, length = length)
}
