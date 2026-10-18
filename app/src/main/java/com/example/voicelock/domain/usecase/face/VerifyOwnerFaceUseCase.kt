package com.example.voicelock.domain.usecase.face

import com.example.voicelock.data.model.face.FaceVerifyResult
import com.example.voicelock.data.repository.faceProfileRepository.FaceProfileRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class VerifyOwnerFaceUseCase(
    private val faceProfileRepository: FaceProfileRepository,
) {
    operator fun invoke(template: FloatArray): Flow<Result<FaceVerifyResult>> = faceProfileRepository.verifyOwnerFace(template)
}
