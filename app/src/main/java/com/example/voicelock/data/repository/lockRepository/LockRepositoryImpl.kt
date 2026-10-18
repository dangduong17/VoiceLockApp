package com.example.voicelock.data.repository.lockRepository

import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.example.voicelock.data.model.lock.LockType
import com.example.voicelock.data.model.lock.LockoutState
import com.example.voicelock.util.Constant
import com.example.voicelock.util.HashUtils
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.security.MessageDigest
import kotlin.math.ceil

class LockRepositoryImpl(
    private val securePrefs: SharedPreferences,
) : LockRepository {

    override fun isPinSet(): Flow<Result<Boolean>> =
        flow<Result<Boolean>> { emit(Result.Success(securePrefs.contains(KEY_PIN_HASH))) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun getPinLength(): Flow<Result<Int>> =
        flow<Result<Int>> { emit(Result.Success(securePrefs.getInt(KEY_PIN_LENGTH, DEFAULT_PIN_LENGTH))) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun savePin(
        pin: String,
        length: Int,
    ): Flow<Result<Unit>> =
        flow<Result<Unit>> {
            val salt = HashUtils.generateSalt()
            val hash = HashUtils.hash(value = pin, salt = salt)
            securePrefs.edit {
                putString(KEY_PIN_SALT, salt)
                putString(KEY_PIN_HASH, hash)
                putInt(KEY_PIN_LENGTH, length)
            }
            resetAttempts(LockType.PIN)
            emit(Result.Success(Unit))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun verifyPinHash(pin: String): Flow<Result<Boolean>> =
        flow<Result<Boolean>> {
            val salt = securePrefs.getString(KEY_PIN_SALT, null)
            val storedHash = securePrefs.getString(KEY_PIN_HASH, null)
            emit(Result.Success(matchesHash(value = pin, salt = salt, storedHash = storedHash)))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun isPatternSet(): Flow<Result<Boolean>> =
        flow<Result<Boolean>> { emit(Result.Success(securePrefs.contains(KEY_PATTERN_HASH))) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun savePattern(cells: List<Int>): Flow<Result<Unit>> =
        flow<Result<Unit>> {
            val salt = HashUtils.generateSalt()
            val hash = HashUtils.hash(value = cells.joinToString(","), salt = salt)
            securePrefs.edit {
                putString(KEY_PATTERN_SALT, salt)
                putString(KEY_PATTERN_HASH, hash)
            }
            resetAttempts(LockType.PATTERN)
            emit(Result.Success(Unit))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun verifyPatternHash(cells: List<Int>): Flow<Result<Boolean>> =
        flow<Result<Boolean>> {
            val salt = securePrefs.getString(KEY_PATTERN_SALT, null)
            val storedHash = securePrefs.getString(KEY_PATTERN_HASH, null)
            emit(Result.Success(matchesHash(value = cells.joinToString(","), salt = salt, storedHash = storedHash)))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun getLockoutState(type: LockType): Flow<Result<LockoutState>> =
        flow<Result<LockoutState>> { emit(Result.Success(currentLockoutState(type))) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun recordFailedAttempt(type: LockType): Flow<Result<LockoutState>> =
        flow<Result<LockoutState>> {
            val attemptsKey = attemptsKey(type)
            val lockedUntilKey = lockedUntilKey(type)
            val newAttempts = securePrefs.getInt(attemptsKey, 0) + 1
            val state =
                if (newAttempts >= Constant.LOCK_MAX_ATTEMPTS) {
                    val lockedUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                    securePrefs.edit {
                        putInt(attemptsKey, newAttempts)
                        putLong(lockedUntilKey, lockedUntil)
                    }
                    LockoutState(
                        failedAttempts = newAttempts,
                        isLocked = true,
                        remainingSeconds = Constant.LOCK_LOCKOUT_DURATION_SECONDS,
                    )
                } else {
                    securePrefs.edit { putInt(attemptsKey, newAttempts) }
                    LockoutState(failedAttempts = newAttempts, isLocked = false, remainingSeconds = 0)
                }
            emit(Result.Success(state))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun resetFailedAttempts(type: LockType): Flow<Result<Unit>> =
        flow<Result<Unit>> {
            resetAttempts(type)
            emit(Result.Success(Unit))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()

    private fun resetAttempts(type: LockType) {
        securePrefs.edit {
            putInt(attemptsKey(type), 0)
            putLong(lockedUntilKey(type), 0L)
        }
    }

    private fun currentLockoutState(type: LockType): LockoutState {
        val attemptsKey = attemptsKey(type)
        val lockedUntilKey = lockedUntilKey(type)
        val failedAttempts = securePrefs.getInt(attemptsKey, 0)
        val lockedUntil = securePrefs.getLong(lockedUntilKey, 0L)
        val now = System.currentTimeMillis()

        return when {
            lockedUntil > now -> {
                val remainingSeconds = ceil((lockedUntil - now) / 1000.0).toInt()
                LockoutState(failedAttempts = failedAttempts, isLocked = true, remainingSeconds = remainingSeconds)
            }
            lockedUntil != 0L -> {
                resetAttempts(type)
                LockoutState(failedAttempts = 0, isLocked = false, remainingSeconds = 0)
            }
            else -> LockoutState(failedAttempts = failedAttempts, isLocked = false, remainingSeconds = 0)
        }
    }

    private fun matchesHash(
        value: String,
        salt: String?,
        storedHash: String?,
    ): Boolean {
        if (salt == null || storedHash == null) return false
        val computedHash = HashUtils.hash(value = value, salt = salt)
        return MessageDigest.isEqual(
            Base64.decode(computedHash, Base64.NO_WRAP),
            Base64.decode(storedHash, Base64.NO_WRAP),
        )
    }

    private fun attemptsKey(type: LockType): String =
        when (type) {
            LockType.PIN -> KEY_PIN_FAILED_ATTEMPTS
            LockType.PATTERN -> KEY_PATTERN_FAILED_ATTEMPTS
            LockType.SECURITY_QUESTION -> KEY_QUESTION_FAILED_ATTEMPTS
        }

    private fun lockedUntilKey(type: LockType): String =
        when (type) {
            LockType.PIN -> KEY_PIN_LOCKED_UNTIL
            LockType.PATTERN -> KEY_PATTERN_LOCKED_UNTIL
            LockType.SECURITY_QUESTION -> KEY_QUESTION_LOCKED_UNTIL
        }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_PIN_FAILED_ATTEMPTS = "pin_failed_attempts"
        private const val KEY_PIN_LOCKED_UNTIL = "pin_locked_until"

        private const val KEY_PATTERN_HASH = "pattern_hash"
        private const val KEY_PATTERN_SALT = "pattern_salt"
        private const val KEY_PATTERN_FAILED_ATTEMPTS = "pattern_failed_attempts"
        private const val KEY_PATTERN_LOCKED_UNTIL = "pattern_locked_until"

        private const val KEY_QUESTION_FAILED_ATTEMPTS = "security_question_failed_attempts"
        private const val KEY_QUESTION_LOCKED_UNTIL = "security_question_locked_until"

        private const val DEFAULT_PIN_LENGTH = 6
        private const val LOCKOUT_DURATION_MS = Constant.LOCK_LOCKOUT_DURATION_SECONDS * 1000L
    }
}
