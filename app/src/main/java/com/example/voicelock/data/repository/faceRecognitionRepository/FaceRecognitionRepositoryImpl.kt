package com.example.voicelock.data.repository.faceRecognitionRepository

import android.graphics.Bitmap
import com.example.voicelock.data.local.face.FaceRecognitionEngine
import com.example.voicelock.data.model.face.FaceEngineStatus
import com.example.voicelock.data.model.face.FaceFrameAnalysis
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Detection and inference run on Dispatchers.Default inside [FaceRecognitionEngine]. */
class FaceRecognitionRepositoryImpl(
    private val faceRecognitionEngine: FaceRecognitionEngine,
) : FaceRecognitionRepository {

    override fun initializeEngine(): Flow<Result<FaceEngineStatus>> =
        flow<Result<FaceEngineStatus>> { emit(Result.Success(faceRecognitionEngine.initialize())) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun analyzeFrame(
        bitmap: Bitmap,
        extractTemplate: Boolean,
    ): Flow<Result<FaceFrameAnalysis>> =
        flow<Result<FaceFrameAnalysis>> { emit(Result.Success(faceRecognitionEngine.analyze(bitmap = bitmap, extractTemplate = extractTemplate))) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()
}
