package com.example.voicelock.domain.usecase.lock

import com.example.voicelock.data.model.biometric.BiometricSettingsDtoModel
import com.example.voicelock.data.model.face.FaceSettingsDtoModel
import com.example.voicelock.data.model.lock.ProtectionStatusDtoModel
import com.example.voicelock.data.model.pinlock.PinInfo
import com.example.voicelock.data.model.voice.VoiceSettingsDtoModel
import com.example.voicelock.domain.usecase.biometric.GetBiometricSettingsUseCase
import com.example.voicelock.domain.usecase.face.GetFaceSettingsUseCase
import com.example.voicelock.domain.usecase.patternlock.GetPatternInfoUseCase
import com.example.voicelock.domain.usecase.pinlock.GetPinInfoUseCase
import com.example.voicelock.domain.usecase.quicklock.GetFloatingLockEnabledUseCase
import com.example.voicelock.domain.usecase.securityquestion.GetSecurityQuestionUseCase
import com.example.voicelock.domain.usecase.voice.GetVoiceSettingsUseCase
import com.example.voicelock.util.Result
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.util.getDataOrNull
import com.example.voicelock.util.resultFlow
import kotlinx.coroutines.flow.Flow

/** Home and Settings: the status of every lock method, each falling back to "not set up" on failure. */
class GetProtectionStatusUseCase(
    private val getPinInfoUseCase: GetPinInfoUseCase,
    private val getPatternInfoUseCase: GetPatternInfoUseCase,
    private val getVoiceSettingsUseCase: GetVoiceSettingsUseCase,
    private val getBiometricSettingsUseCase: GetBiometricSettingsUseCase,
    private val getFaceSettingsUseCase: GetFaceSettingsUseCase,
    private val getSecurityQuestionUseCase: GetSecurityQuestionUseCase,
    private val getFloatingLockEnabledUseCase: GetFloatingLockEnabledUseCase,
) {
    operator fun invoke(): Flow<Result<ProtectionStatusDtoModel>> =
        resultFlow {
            ProtectionStatusDtoModel(
                pinInfo = getPinInfoUseCase().getDataOrDefault(PinInfo.NOT_SET),
                isPatternSet = getPatternInfoUseCase().getDataOrDefault(false),
                isSecurityQuestionSet = getSecurityQuestionUseCase().getDataOrNull() != null,
                voiceSettings = getVoiceSettingsUseCase().getDataOrDefault(VoiceSettingsDtoModel()),
                biometricSettings = getBiometricSettingsUseCase().getDataOrDefault(BiometricSettingsDtoModel()),
                faceSettings = getFaceSettingsUseCase().getDataOrDefault(FaceSettingsDtoModel()),
                isFloatingLockEnabled = getFloatingLockEnabledUseCase().getDataOrDefault(false),
            )
        }
}
