package com.example.voicelock.data.repository.deviceLockRepository

import android.app.admin.DevicePolicyManager
import android.content.Context
import com.example.voicelock.util.Result
import com.example.voicelock.util.resultFlow
import com.example.voicelock.util.isDeviceAdminActive
import kotlinx.coroutines.flow.Flow

class DeviceLockRepositoryImpl(
    private val context: Context,
) : DeviceLockRepository {

    override fun isDeviceAdminActive(): Flow<Result<Boolean>> =
        resultFlow { context.isDeviceAdminActive() }

    override fun lockScreen(): Flow<Result<Unit>> =
        resultFlow {
            check(context.isDeviceAdminActive()) { "VoiceLock is not a device admin" }
            context.getSystemService(DevicePolicyManager::class.java).lockNow()
            Unit
        }
}
