package com.example.voicelock.data.repository.quickLockRepository

import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

/** Quick ways to lock without the voice command: the floating lock button (the widget needs no setting). */
interface QuickLockRepository {
    fun isFloatingLockEnabled(): Flow<Result<Boolean>>

    fun setFloatingLockEnabled(isEnabled: Boolean): Flow<Result<Unit>>
}
