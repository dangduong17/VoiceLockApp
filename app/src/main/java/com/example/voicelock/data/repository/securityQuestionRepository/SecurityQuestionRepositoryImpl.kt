package com.example.voicelock.data.repository.securityQuestionRepository

import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.example.voicelock.data.model.securityquestion.SecurityQuestionDtoModel
import com.example.voicelock.util.HashUtils
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import com.example.voicelock.util.normalizeForComparison
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.security.MessageDigest

/**
 * The question is kept as typed (it is shown on the lock screen); the answer is normalized
 * (see [normalizeForComparison]) and stored only as a salted SHA-256 hash, like the PIN.
 * Attempt counting / lockout lives in LockRepository (LockType.SECURITY_QUESTION).
 */
class SecurityQuestionRepositoryImpl(
    private val securePrefs: SharedPreferences,
) : SecurityQuestionRepository {

    override fun getSecurityQuestion(): Flow<Result<SecurityQuestionDtoModel?>> =
        flow<Result<SecurityQuestionDtoModel?>> {
            val question = securePrefs.getString(KEY_QUESTION, null)
            val isSet = question != null && securePrefs.contains(KEY_ANSWER_HASH)
            emit(Result.Success(if (isSet) SecurityQuestionDtoModel(question = question!!) else null))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun saveSecurityQuestion(
        question: String,
        answer: String,
    ): Flow<Result<Unit>> =
        flow<Result<Unit>> {
            val salt = HashUtils.generateSalt()
            val hash = HashUtils.hash(value = answer.normalizeForComparison(), salt = salt)
            securePrefs.edit {
                putString(KEY_QUESTION, question.trim())
                putString(KEY_ANSWER_SALT, salt)
                putString(KEY_ANSWER_HASH, hash)
            }
            emit(Result.Success(Unit))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun verifyAnswerHash(answer: String): Flow<Result<Boolean>> =
        flow<Result<Boolean>> {
            val salt = securePrefs.getString(KEY_ANSWER_SALT, null)
            val storedHash = securePrefs.getString(KEY_ANSWER_HASH, null)
            val isMatch =
                salt != null &&
                    storedHash != null &&
                    MessageDigest.isEqual(
                        Base64.decode(HashUtils.hash(value = answer.normalizeForComparison(), salt = salt), Base64.NO_WRAP),
                        Base64.decode(storedHash, Base64.NO_WRAP),
                    )
            emit(Result.Success(isMatch))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    companion object {
        private const val KEY_QUESTION = "security_question"
        private const val KEY_ANSWER_SALT = "security_answer_salt"
        private const val KEY_ANSWER_HASH = "security_answer_hash"
    }
}
