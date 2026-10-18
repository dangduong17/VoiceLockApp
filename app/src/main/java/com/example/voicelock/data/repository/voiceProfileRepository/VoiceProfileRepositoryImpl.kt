package com.example.voicelock.data.repository.voiceProfileRepository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.voicelock.data.model.voice.VoiceEnrollmentResult
import com.example.voicelock.data.model.voice.VoiceKeywordProfile
import com.example.voicelock.data.model.voice.VoiceSettingsDtoModel
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.model.voice.VoiceVerifyResult
import com.example.voicelock.util.Result
import com.example.voicelock.util.resultFlow
import com.example.voicelock.util.catchAsResult
import com.example.voicelock.data.local.voice.VoiceBiometricsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * CPU-heavy work (model inference, MFCC, DTW) is moved to Dispatchers.Default inside
 * [VoiceBiometricsManager]; these flows only guarantee they never run on the main thread.
 * The voiceprint lives in encrypted prefs (via the manager); the on/off switches in DataStore.
 */
class VoiceProfileRepositoryImpl(
    private val voiceBiometricsManager: VoiceBiometricsManager,
    private val dataStore: DataStore<Preferences>,
) : VoiceProfileRepository {

    override fun isVoiceEnrolled(mode: VoiceVerificationMode): Flow<Result<Boolean>> =
        resultFlow { voiceBiometricsManager.isEnrolled(mode) }

    override fun getVoiceSettings(): Flow<Result<VoiceSettingsDtoModel>> =
        flow<Result<VoiceSettingsDtoModel>> {
            val preferences = dataStore.data.first()
            emit(
                Result.Success(
                    VoiceSettingsDtoModel(
                        isEnrolled = voiceBiometricsManager.isEnrolled(),
                        sampleCount = voiceBiometricsManager.getEnrolledSampleCount(),
                        isUnlockPreferenceOn = preferences[KEY_VOICE_UNLOCK_ENABLED] ?: true,
                        isLockPreferenceOn = preferences[KEY_VOICE_LOCK_ENABLED] ?: false,
                        isLockCommandEnrolled = voiceBiometricsManager.isEnrolled(VoiceVerificationMode.LOCK),
                        lockCommandPhrase =
                            voiceBiometricsManager.getKeywordProfile(VoiceVerificationMode.LOCK)?.transcripts?.firstOrNull().orEmpty(),
                    ),
                ),
            )
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun setVoiceUnlockEnabled(isEnabled: Boolean): Flow<Result<Unit>> = setPreference(KEY_VOICE_UNLOCK_ENABLED, isEnabled)

    override fun setVoiceLockEnabled(isEnabled: Boolean): Flow<Result<Unit>> = setPreference(KEY_VOICE_LOCK_ENABLED, isEnabled)

    private fun setPreference(
        key: Preferences.Key<Boolean>,
        value: Boolean,
    ): Flow<Result<Unit>> =
        resultFlow {
            dataStore.edit { preferences -> preferences[key] = value }
            Unit
        }

    override fun warmUpModel(): Flow<Result<Unit>> =
        resultFlow {
            voiceBiometricsManager.warmUp()
            Unit
        }

    override fun getKeywordProfile(mode: VoiceVerificationMode): Flow<Result<VoiceKeywordProfile?>> =
        resultFlow { voiceBiometricsManager.getKeywordProfile(mode) }

    override fun enrollOwnerVoice(
        recordings: List<FloatArray>,
        keywordProfile: VoiceKeywordProfile,
        mode: VoiceVerificationMode,
    ): Flow<Result<VoiceEnrollmentResult>> =
        resultFlow {
            voiceBiometricsManager.enrollOwnerVoice(recordings = recordings, keywordProfile = keywordProfile, mode = mode)
        }

    override fun verifyOwnerVoice(
        samples: FloatArray,
        mode: VoiceVerificationMode,
        isKeywordChecked: Boolean,
    ): Flow<Result<VoiceVerifyResult>> =
        resultFlow { voiceBiometricsManager.verify(samples, mode, isKeywordChecked) }

    companion object {
        private val KEY_VOICE_UNLOCK_ENABLED = booleanPreferencesKey("voice_unlock_enabled")
        private val KEY_VOICE_LOCK_ENABLED = booleanPreferencesKey("voice_lock_enabled")
    }
}
