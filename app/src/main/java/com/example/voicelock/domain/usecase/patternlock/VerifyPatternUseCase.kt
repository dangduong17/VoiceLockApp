package com.example.voicelock.domain.usecase.patternlock

import com.example.voicelock.data.model.lock.LockType
import com.example.voicelock.data.model.patternlock.PatternVerifyResult
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

class VerifyPatternUseCase(
    private val lockRepository: LockRepository,
) {
    operator fun invoke(cells: List<Int>): Flow<Result<PatternVerifyResult>> =
        flow<Result<PatternVerifyResult>> {
            val lockoutState = lockRepository.getLockoutState(LockType.PATTERN).getDataOrNull()
            if (lockoutState != null && lockoutState.isLocked) {
                emit(Result.Success(PatternVerifyResult.Locked(remainingSeconds = lockoutState.remainingSeconds)))
                return@flow
            }

            val isMatch = lockRepository.verifyPatternHash(cells).getDataOrDefault(false)
            if (isMatch) {
                lockRepository.resetFailedAttempts(LockType.PATTERN).getDataOrNull()
                emit(Result.Success(PatternVerifyResult.Success))
            } else {
                val updatedState = lockRepository.recordFailedAttempt(LockType.PATTERN).getDataOrNull()
                emit(
                    Result.Success(
                        when {
                            updatedState == null -> PatternVerifyResult.Failed(remainingAttempts = Constant.LOCK_MAX_ATTEMPTS)
                            updatedState.isLocked -> PatternVerifyResult.Locked(remainingSeconds = updatedState.remainingSeconds)
                            else ->
                                PatternVerifyResult.Failed(
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
