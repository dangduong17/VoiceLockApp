package com.example.voicelock.domain.usecase.securityquestion

import com.example.voicelock.data.model.securityquestion.SecurityQuestionDtoModel
import com.example.voicelock.data.repository.securityQuestionRepository.SecurityQuestionRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class GetSecurityQuestionUseCase(
    private val securityQuestionRepository: SecurityQuestionRepository,
) {
    operator fun invoke(): Flow<Result<SecurityQuestionDtoModel?>> = securityQuestionRepository.getSecurityQuestion()
}
