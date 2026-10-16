package com.example.voicelock.domain.usecase.pinlock

import com.example.voicelock.data.model.lock.LockType
import com.example.voicelock.data.model.pinlock.PinVerifyResult
import com.example.voicelock.data.repository.lockRepository.LockRepository
import com.example.voicelock.util.Constant
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.util.getDataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class VerifyPinUseCase(
    private val lockRepository: LockRepository,
) {
    operator fun invoke(pin: String): Flow<Result<PinVerifyResult>> =
        flow<Result<PinVerifyResult>> {
            val lockoutState = lockRepository.getLockoutState(LockType.PIN).getDataOrNull()
            if (lockoutState != null && lockoutState.isLocked) {
                emit(Result.Success(PinVerifyResult.Locked(remainingSeconds = lockoutState.remainingSeconds)))
                return@flow
            }

            val isMatch = lockRepository.verifyPinHash(pin).getDataOrDefault(false)
            if (isMatch) {
                lockRepository.resetFailedAttempts(LockType.PIN).getDataOrNull()
                emit(Result.Success(PinVerifyResult.Success))
            } else {
                val updatedState = lockRepository.recordFailedAttempt(LockType.PIN).getDataOrNull()
                emit(
                    Result.Success(
                        when {
                            updatedState == null -> PinVerifyResult.Failed(remainingAttempts = Constant.LOCK_MAX_ATTEMPTS)
                            updatedState.isLocked -> PinVerifyResult.Locked(remainingSeconds = updatedState.remainingSeconds)
                            else ->
                                PinVerifyResult.Failed(
                                    remainingAttempts =
                                        (Constant.LOCK_MAX_ATTEMPTS - updatedState.failedAttempts).coerceAtLeast(0),
                                )
                        },
                    ),
                )
            }
        }.flowOn(Dispatchers.IO)
            .catchAsResult()
}
