package com.example.voicelock.domain.usecase.securityquestion

import com.example.voicelock.data.model.lock.LockType
import com.example.voicelock.data.model.securityquestion.SecurityAnswerVerifyResult
import com.example.voicelock.data.repository.lockRepository.LockRepository
import com.example.voicelock.data.repository.securityQuestionRepository.SecurityQuestionRepository
import com.example.voicelock.util.Constant
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.util.getDataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Same attempt / lockout rules as the PIN (see VerifyPinUseCase), on LockType.SECURITY_QUESTION. */
class VerifySecurityAnswerUseCase(
    private val securityQuestionRepository: SecurityQuestionRepository,
    private val lockRepository: LockRepository,
) {
    operator fun invoke(answer: String): Flow<Result<SecurityAnswerVerifyResult>> =
        flow<Result<SecurityAnswerVerifyResult>> {
            if (securityQuestionRepository.getSecurityQuestion().getDataOrNull() == null) {
                emit(Result.Success(SecurityAnswerVerifyResult.NotSet))
                return@flow
            }
            val lockoutState = lockRepository.getLockoutState(LockType.SECURITY_QUESTION).getDataOrNull()
            if (lockoutState != null && lockoutState.isLocked) {
                emit(Result.Success(SecurityAnswerVerifyResult.Locked(remainingSeconds = lockoutState.remainingSeconds)))
                return@flow
            }

            if (securityQuestionRepository.verifyAnswerHash(answer).getDataOrDefault(false)) {
                lockRepository.resetFailedAttempts(LockType.SECURITY_QUESTION).getDataOrNull()
                emit(Result.Success(SecurityAnswerVerifyResult.Success))
                return@flow
            }
            val updatedState = lockRepository.recordFailedAttempt(LockType.SECURITY_QUESTION).getDataOrNull()
            val result =
                when {
                    updatedState == null -> SecurityAnswerVerifyResult.Failed(remainingAttempts = Constant.LOCK_MAX_ATTEMPTS)
                    updatedState.isLocked -> SecurityAnswerVerifyResult.Locked(remainingSeconds = updatedState.remainingSeconds)
                    else ->
                        SecurityAnswerVerifyResult.Failed(
                            remainingAttempts = (Constant.LOCK_MAX_ATTEMPTS - updatedState.failedAttempts).coerceAtLeast(0),
                        )
                }
            emit(Result.Success(result))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()
}
