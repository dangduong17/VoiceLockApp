package com.example.voicelock.domain.usecase.face

import com.example.voicelock.data.model.face.FaceEnrollmentResult
import com.example.voicelock.data.repository.faceProfileRepository.FaceProfileRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class EnrollOwnerFaceUseCase(
    private val faceProfileRepository: FaceProfileRepository,
) {
    operator fun invoke(templates: List<FloatArray>): Flow<Result<FaceEnrollmentResult>> = faceProfileRepository.enrollOwnerFace(templates)
}
