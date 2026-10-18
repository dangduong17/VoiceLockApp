package com.example.voicelock.domain.usecase.securityquestion

import com.example.voicelock.data.model.lock.LockType
import com.example.voicelock.data.repository.lockRepository.LockRepository
import com.example.voicelock.data.repository.securityQuestionRepository.SecurityQuestionRepository
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import com.example.voicelock.util.getDataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Saves a new question + answer and clears any wrong-answer lockout left from the old one. */
class SetSecurityQuestionUseCase(
    private val securityQuestionRepository: SecurityQuestionRepository,
    private val lockRepository: LockRepository,
) {
    operator fun invoke(
        question: String,
        answer: String,
    ): Flow<Result<Unit>> =
        flow {
            val saved = securityQuestionRepository.saveSecurityQuestion(question = question, answer = answer).first()
            if (saved is Result.Success) lockRepository.resetFailedAttempts(LockType.SECURITY_QUESTION).getDataOrNull()
            emit(saved)
        }.flowOn(Dispatchers.IO)
            .catchAsResult()
}
