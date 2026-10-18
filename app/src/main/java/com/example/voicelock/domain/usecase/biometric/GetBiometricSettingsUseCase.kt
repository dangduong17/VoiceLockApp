package com.example.voicelock.domain.usecase.biometric

import com.example.voicelock.data.model.biometric.BiometricSettings
import com.example.voicelock.data.repository.biometricRepository.BiometricRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class GetBiometricSettingsUseCase(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(): Flow<Result<BiometricSettings>> = biometricRepository.getBiometricSettings()
}
