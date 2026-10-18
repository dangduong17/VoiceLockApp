package com.example.voicelock.data.repository.biometricRepository

import com.example.voicelock.data.model.biometric.BiometricSettings
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

interface BiometricRepository {
    fun getBiometricSettings(): Flow<Result<BiometricSettings>>

    fun setBiometricEnabled(
        type: BiometricType,
        isEnabled: Boolean,
    ): Flow<Result<Unit>>
}
