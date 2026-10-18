package com.example.voicelock.data.repository.biometricRepository

import android.content.Context
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.voicelock.data.model.biometric.BiometricAvailability
import com.example.voicelock.data.model.biometric.BiometricSettingsDtoModel
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.util.Constant
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Android keeps the biometric data itself; the app only stores its on/off switch. The prompt
 * accepts any Class 3 biometric enrolled on the device, the app cannot tell which finger was used.
 */
class BiometricRepositoryImpl(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
) : BiometricRepository {

    override fun getBiometricSettings(): Flow<Result<BiometricSettingsDtoModel>> =
        flow<Result<BiometricSettingsDtoModel>> {
            val preferences = dataStore.data.first()
            emit(
                Result.Success(
                    BiometricSettingsDtoModel(
                        fingerprintAvailability = getAvailability(BiometricType.FINGERPRINT),
                        isFingerprintPreferenceOn = preferences[KEY_FINGERPRINT_ENABLED] ?: false,
                    ),
                ),
            )
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun setBiometricEnabled(
        type: BiometricType,
        isEnabled: Boolean,
    ): Flow<Result<Unit>> =
        flow<Result<Unit>> {
            val key =
                when (type) {
                    BiometricType.FINGERPRINT -> KEY_FINGERPRINT_ENABLED
                }
            dataStore.edit { preferences -> preferences[key] = isEnabled }
            emit(Result.Success(Unit))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    private fun getAvailability(type: BiometricType): BiometricAvailability {
        if (!hasSensor(type)) return BiometricAvailability.NO_HARDWARE
        return when (BiometricManager.from(context).canAuthenticate(Constant.BIOMETRIC_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    private fun hasSensor(type: BiometricType): Boolean {
        val packageManager = context.packageManager
        return when (type) {
            BiometricType.FINGERPRINT -> packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        }
    }

    companion object {
        private val KEY_FINGERPRINT_ENABLED = booleanPreferencesKey("biometric_fingerprint_enabled")
    }
}
