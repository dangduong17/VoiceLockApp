package com.example.voicelock.data.repository.voiceProfileRepository

import com.example.voicelock.data.model.voice.VoiceEnrollmentResult
import com.example.voicelock.data.model.voice.VoiceSettings
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.data.model.voice.VoiceVerifyResult
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

interface VoiceProfileRepository {
    fun isVoiceEnrolled(): Flow<Result<Boolean>>

    fun getVoiceSettings(): Flow<Result<VoiceSettings>>

    fun setVoiceUnlockEnabled(isEnabled: Boolean): Flow<Result<Unit>>

    fun setVoiceLockEnabled(isEnabled: Boolean): Flow<Result<Unit>>

    fun warmUpModel(): Flow<Result<Unit>>

    fun enrollOwnerVoice(recordings: List<FloatArray>): Flow<Result<VoiceEnrollmentResult>>

    fun verifyOwnerVoice(
        samples: FloatArray,
        mode: VoiceVerificationMode,
    ): Flow<Result<VoiceVerifyResult>>
}
