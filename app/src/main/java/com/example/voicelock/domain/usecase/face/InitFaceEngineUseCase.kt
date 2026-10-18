package com.example.voicelock.domain.usecase.face

import com.example.voicelock.data.model.face.FaceEngineStatus
import com.example.voicelock.data.repository.faceRecognitionRepository.FaceRecognitionRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class InitFaceEngineUseCase(
    private val faceRecognitionRepository: FaceRecognitionRepository,
) {
    operator fun invoke(): Flow<Result<FaceEngineStatus>> = faceRecognitionRepository.initializeEngine()
}
