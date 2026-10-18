package com.example.voicelock.domain.usecase.face

import android.graphics.Bitmap
import com.example.voicelock.data.model.face.FaceFrameAnalysis
import com.example.voicelock.data.repository.faceRecognitionRepository.FaceRecognitionRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class AnalyzeFaceFrameUseCase(
    private val faceRecognitionRepository: FaceRecognitionRepository,
) {
    operator fun invoke(
        bitmap: Bitmap,
        extractTemplate: Boolean,
    ): Flow<Result<FaceFrameAnalysis>> = faceRecognitionRepository.analyzeFrame(bitmap, extractTemplate)
}
