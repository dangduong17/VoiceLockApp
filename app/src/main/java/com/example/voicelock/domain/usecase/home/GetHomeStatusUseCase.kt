package com.example.voicelock.domain.usecase.home

import com.example.voicelock.data.model.home.HomeUiState
import com.example.voicelock.data.model.biometric.BiometricSettingsDtoModel
import com.example.voicelock.data.model.face.FaceSettingsDtoModel
import com.example.voicelock.data.model.pinlock.PinInfo
import com.example.voicelock.data.model.voice.VoiceSettingsDtoModel
import com.example.voicelock.data.model.voice.isVoiceLockActive
import com.example.voicelock.data.model.voice.isVoiceUnlockActive
import com.example.voicelock.domain.usecase.biometric.GetBiometricSettingsUseCase
import com.example.voicelock.domain.usecase.face.GetFaceSettingsUseCase
import com.example.voicelock.domain.usecase.patternlock.GetPatternInfoUseCase
import com.example.voicelock.domain.usecase.pinlock.GetPinInfoUseCase
import com.example.voicelock.domain.usecase.securityquestion.GetSecurityQuestionUseCase
import com.example.voicelock.domain.usecase.voice.GetVoiceSettingsUseCase
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.util.getDataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GetHomeStatusUseCase(
    private val getPinInfoUseCase: GetPinInfoUseCase,
    private val getPatternInfoUseCase: GetPatternInfoUseCase,
    private val getVoiceSettingsUseCase: GetVoiceSettingsUseCase,
    private val getBiometricSettingsUseCase: GetBiometricSettingsUseCase,
    private val getFaceSettingsUseCase: GetFaceSettingsUseCase,
    private val getSecurityQuestionUseCase: GetSecurityQuestionUseCase,
) {
    operator fun invoke(): Flow<Result<HomeUiState>> =
        flow<Result<HomeUiState>> {
            val pinInfo = getPinInfoUseCase().getDataOrDefault(PinInfo(isPinSet = false, pinLength = 6))
            val isPatternSet = getPatternInfoUseCase().getDataOrDefault(false)
            val isSecurityQuestionSet = getSecurityQuestionUseCase().getDataOrNull() != null
            val voiceSettings = getVoiceSettingsUseCase().getDataOrDefault(VoiceSettingsDtoModel())
            val biometricSettings = getBiometricSettingsUseCase().getDataOrDefault(BiometricSettingsDtoModel())
            val faceSettings = getFaceSettingsUseCase().getDataOrDefault(FaceSettingsDtoModel())

            emit(
                Result.Success(
                    HomeUiState(
                        isVoiceEnrolled = voiceSettings.isEnrolled,
                        voiceSamplesCount = voiceSettings.sampleCount,
                        isBackupPinEnabled = pinInfo.isPinSet,
                        isVoiceUnlockEnabled = voiceSettings.isVoiceUnlockActive(),
                        isVoiceLockEnabled = voiceSettings.isVoiceLockActive(),
                        isVoiceLockCommandEnrolled = voiceSettings.isLockCommandEnrolled,
                        voiceLockPhrase = voiceSettings.lockCommandPhrase,
                        biometricSettings = biometricSettings,
                        faceSettings = faceSettings,
                        isPinSet = pinInfo.isPinSet,
                        pinLength = pinInfo.pinLength,
                        isPatternSet = isPatternSet,
                        isSecurityQuestionSet = isSecurityQuestionSet,
                    ),
                ),
            )
        }.flowOn(Dispatchers.IO)
            .catchAsResult()
}
