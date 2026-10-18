package com.example.voicelock.data.repository.faceRecognitionRepository

import android.graphics.Bitmap
import com.example.voicelock.data.local.face.FaceRecognitionEngine
import com.example.voicelock.data.model.face.FaceEngineStatus
import com.example.voicelock.data.model.face.FaceFrameAnalysis
import com.example.voicelock.util.Result
import com.example.voicelock.util.resultFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/** Detection and inference run on Dispatchers.Default inside [FaceRecognitionEngine]. */
class FaceRecognitionRepositoryImpl(
    private val faceRecognitionEngine: FaceRecognitionEngine,
) : FaceRecognitionRepository {

    override fun initializeEngine(): Flow<Result<FaceEngineStatus>> =
        resultFlow { faceRecognitionEngine.initialize() }

    override fun analyzeFrame(
        bitmap: Bitmap,
        extractTemplate: Boolean,
    ): Flow<Result<FaceFrameAnalysis>> =
        resultFlow { faceRecognitionEngine.analyze(bitmap = bitmap, extractTemplate = extractTemplate) }
}
