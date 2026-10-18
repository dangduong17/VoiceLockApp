package com.example.voicelock.data.repository.securityQuestionRepository

import com.example.voicelock.data.model.securityquestion.SecurityQuestionDtoModel
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

interface SecurityQuestionRepository {
    /** Null when no security question has been set up. */
    fun getSecurityQuestion(): Flow<Result<SecurityQuestionDtoModel?>>

    fun saveSecurityQuestion(
        question: String,
        answer: String,
    ): Flow<Result<Unit>>

    fun verifyAnswerHash(answer: String): Flow<Result<Boolean>>
}
