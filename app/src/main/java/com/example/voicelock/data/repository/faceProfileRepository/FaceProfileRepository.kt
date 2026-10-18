package com.example.voicelock.data.repository.faceProfileRepository

import com.example.voicelock.data.model.face.FaceEnrollmentResult
import com.example.voicelock.data.model.face.FaceSettingsDtoModel
import com.example.voicelock.data.model.face.FaceVerifyResult
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

interface FaceProfileRepository {
    fun getFaceSettings(): Flow<Result<FaceSettingsDtoModel>>

    fun setFaceUnlockEnabled(isEnabled: Boolean): Flow<Result<Unit>>

    fun enrollOwnerFace(templates: List<FloatArray>): Flow<Result<FaceEnrollmentResult>>

    fun verifyOwnerFace(template: FloatArray): Flow<Result<FaceVerifyResult>>

    fun deleteFaceProfile(): Flow<Result<Unit>>
}
