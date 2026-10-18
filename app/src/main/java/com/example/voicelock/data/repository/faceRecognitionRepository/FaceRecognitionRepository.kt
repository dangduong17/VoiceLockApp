package com.example.voicelock.data.repository.faceRecognitionRepository

import android.graphics.Bitmap
import com.example.voicelock.data.model.face.FaceEngineStatus
import com.example.voicelock.data.model.face.FaceFrameAnalysis
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

interface FaceRecognitionRepository {
    fun initializeEngine(): Flow<Result<FaceEngineStatus>>

    fun analyzeFrame(
        bitmap: Bitmap,
        extractTemplate: Boolean,
    ): Flow<Result<FaceFrameAnalysis>>
}
