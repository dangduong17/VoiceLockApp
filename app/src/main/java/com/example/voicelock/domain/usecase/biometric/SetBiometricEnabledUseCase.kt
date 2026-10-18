package com.example.voicelock.domain.usecase.biometric

import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.repository.biometricRepository.BiometricRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class SetBiometricEnabledUseCase(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(
        type: BiometricType,
        isEnabled: Boolean,
    ): Flow<Result<Unit>> = biometricRepository.setBiometricEnabled(type, isEnabled)
}
