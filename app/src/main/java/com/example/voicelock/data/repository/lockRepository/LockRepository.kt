package com.example.voicelock.data.repository.lockRepository

import com.example.voicelock.data.model.lock.LockType
import com.example.voicelock.data.model.lock.LockoutState
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

interface LockRepository {
    fun isPinSet(): Flow<Result<Boolean>>

    fun getPinLength(): Flow<Result<Int>>

    fun savePin(
        pin: String,
        length: Int,
    ): Flow<Result<Unit>>

    fun verifyPinHash(pin: String): Flow<Result<Boolean>>

    fun isPatternSet(): Flow<Result<Boolean>>

    fun savePattern(cells: List<Int>): Flow<Result<Unit>>

    fun verifyPatternHash(cells: List<Int>): Flow<Result<Boolean>>

    fun getLockoutState(type: LockType): Flow<Result<LockoutState>>

    fun recordFailedAttempt(type: LockType): Flow<Result<LockoutState>>

    fun resetFailedAttempts(type: LockType): Flow<Result<Unit>>
}
