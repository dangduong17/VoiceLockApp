package com.example.voicelock.domain.usecase.face

import com.example.voicelock.data.repository.faceProfileRepository.FaceProfileRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class SetFaceUnlockEnabledUseCase(
    private val faceProfileRepository: FaceProfileRepository,
) {
    operator fun invoke(isEnabled: Boolean): Flow<Result<Unit>> = faceProfileRepository.setFaceUnlockEnabled(isEnabled)
}
