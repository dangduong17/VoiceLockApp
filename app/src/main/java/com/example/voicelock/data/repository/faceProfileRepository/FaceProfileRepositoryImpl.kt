package com.example.voicelock.data.repository.faceProfileRepository

import android.content.SharedPreferences
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.voicelock.data.local.face.FaceRecognitionEngine
import com.example.voicelock.data.model.face.FaceEnrollmentResult
import com.example.voicelock.data.model.face.FaceSettingsDtoModel
import com.example.voicelock.data.model.face.FaceVerifyResult
import com.example.voicelock.util.Constant
import com.example.voicelock.util.Result
import com.example.voicelock.util.resultFlow
import com.example.voicelock.util.catchAsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.core.content.edit

/**
 * The owner's face embeddings (never images) live in encrypted prefs; the on/off switch lives
 * in DataStore, like the voice profile.
 */
class FaceProfileRepositoryImpl(
    private val faceRecognitionEngine: FaceRecognitionEngine,
    private val securePrefs: SharedPreferences,
    private val dataStore: DataStore<Preferences>,
) : FaceProfileRepository {

    override fun getFaceSettings(): Flow<Result<FaceSettingsDtoModel>> =
        flow<Result<FaceSettingsDtoModel>> {
            val templates = loadTemplates()
            val preferences = dataStore.data.first()
            emit(
                Result.Success(
                    FaceSettingsDtoModel(
                        isEnrolled = templates.isNotEmpty(),
                        sampleCount = templates.size,
                        isUnlockPreferenceOn = preferences[KEY_FACE_UNLOCK_ENABLED] ?: true,
                    ),
                ),
            )
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun setFaceUnlockEnabled(isEnabled: Boolean): Flow<Result<Unit>> =
        resultFlow {
            dataStore.edit { preferences -> preferences[KEY_FACE_UNLOCK_ENABLED] = isEnabled }
            Unit
        }

    /** Every pair of samples must match each other, so a profile never mixes two people. */
    override fun enrollOwnerFace(templates: List<FloatArray>): Flow<Result<FaceEnrollmentResult>> =
        flow<Result<FaceEnrollmentResult>> {
            val isConsistent =
                templates.isNotEmpty() &&
                    templates.indices.all { i ->
                        (i + 1 until templates.size).all { j ->
                            faceRecognitionEngine.similarity(first = templates[i], second = templates[j]) >= Constant.FACE_MATCH_THRESHOLD
                        }
                    }
            if (!isConsistent) {
                emit(Result.Success(FaceEnrollmentResult.InconsistentSamples))
                return@flow
            }
            saveTemplates(templates)
            emit(Result.Success(FaceEnrollmentResult.Success(sampleCount = templates.size)))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun verifyOwnerFace(template: FloatArray): Flow<Result<FaceVerifyResult>> =
        flow<Result<FaceVerifyResult>> {
            val enrolled = loadTemplates()
            if (enrolled.isEmpty()) {
                emit(Result.Success(FaceVerifyResult.NotEnrolled))
                return@flow
            }
            val best = enrolled.maxOf { faceRecognitionEngine.similarity(first = template, second = it) }
            val result =
                if (best >= Constant.FACE_MATCH_THRESHOLD) FaceVerifyResult.Verified(best) else FaceVerifyResult.Rejected(best)
            emit(Result.Success(result))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun deleteFaceProfile(): Flow<Result<Unit>> =
        resultFlow {
            // Written synchronously: the caller reports success only once the profile is really gone.
            securePrefs.edit(commit = true) { remove(KEY_FACE_TEMPLATES) }
            Unit
        }

    private fun loadTemplates(): List<FloatArray> =
        securePrefs.getString(KEY_FACE_TEMPLATES, null)
            ?.split(TEMPLATE_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?.map { decode(it) }
            .orEmpty()

    private fun saveTemplates(templates: List<FloatArray>) {
        securePrefs.edit(commit = true) { putString(KEY_FACE_TEMPLATES, templates.joinToString(TEMPLATE_SEPARATOR) { encode(it) }) }
    }

    private fun encode(template: FloatArray): String {
        val buffer = ByteBuffer.allocate(template.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        template.forEach { buffer.putFloat(it) }
        return Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
    }

    private fun decode(encoded: String): FloatArray {
        val buffer = ByteBuffer.wrap(Base64.decode(encoded, Base64.NO_WRAP)).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(buffer.remaining() / Float.SIZE_BYTES) { buffer.getFloat() }
    }

    companion object {
        /** New key: embeddings from the earlier FaceSDK build are not compatible. */
        private const val KEY_FACE_TEMPLATES = "face_embeddings_mobilefacenet"
        private const val TEMPLATE_SEPARATOR = ","
        private val KEY_FACE_UNLOCK_ENABLED = booleanPreferencesKey("face_unlock_enabled")
    }
}
