package com.example.voicelock.data.repository.deviceLockRepository

import android.app.admin.DevicePolicyManager
import android.content.Context
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import com.example.voicelock.util.isDeviceAdminActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class DeviceLockRepositoryImpl(
    private val context: Context,
) : DeviceLockRepository {

    override fun isDeviceAdminActive(): Flow<Result<Boolean>> =
        flow<Result<Boolean>> { emit(Result.Success(context.isDeviceAdminActive())) }
            .flowOn(Dispatchers.IO)
            .catchAsResult()

    override fun lockScreen(): Flow<Result<Unit>> =
        flow<Result<Unit>> {
            check(context.isDeviceAdminActive()) { "VoiceLock is not a device admin" }
            context.getSystemService(DevicePolicyManager::class.java).lockNow()
            emit(Result.Success(Unit))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()
}
