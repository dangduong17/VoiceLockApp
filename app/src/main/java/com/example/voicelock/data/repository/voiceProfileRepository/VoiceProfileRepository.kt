package com.example.voicelock.data.repository.voiceProfileRepository

import com.example.voicelock.data.model.voice.VoiceEnrollmentResult
import com.example.voicelock.data.model.voice.VoiceKeywordProfile
import com.example.voicelock.data.model.voice.VoiceSettingsDtoModel
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.model.voice.VoiceVerifyResult
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

interface VoiceProfileRepository {
    fun isVoiceEnrolled(mode: VoiceVerificationMode = VoiceVerificationMode.UNLOCK): Flow<Result<Boolean>>

    fun getVoiceSettings(): Flow<Result<VoiceSettingsDtoModel>>

    fun setVoiceUnlockEnabled(isEnabled: Boolean): Flow<Result<Unit>>

    fun setVoiceLockEnabled(isEnabled: Boolean): Flow<Result<Unit>>

    fun warmUpModel(): Flow<Result<Unit>>

    fun getKeywordProfile(mode: VoiceVerificationMode = VoiceVerificationMode.UNLOCK): Flow<Result<VoiceKeywordProfile?>>

    fun enrollOwnerVoice(
        recordings: List<FloatArray>,
        keywordProfile: VoiceKeywordProfile,
        mode: VoiceVerificationMode = VoiceVerificationMode.UNLOCK,
    ): Flow<Result<VoiceEnrollmentResult>>

    fun verifyOwnerVoice(
        samples: FloatArray,
        mode: VoiceVerificationMode,
        isKeywordChecked: Boolean,
    ): Flow<Result<VoiceVerifyResult>>
}
