package com.example.voicelock.data.repository.deviceLockRepository

import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

interface DeviceLockRepository {
    fun isDeviceAdminActive(): Flow<Result<Boolean>>

    /** Locks the phone screen; fails when the device admin is not active. */
    fun lockScreen(): Flow<Result<Unit>>
}
