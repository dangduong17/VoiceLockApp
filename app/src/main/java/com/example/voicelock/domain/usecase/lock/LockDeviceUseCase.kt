package com.example.voicelock.domain.usecase.lock

import com.example.voicelock.data.model.lock.DeviceLockResult
import com.example.voicelock.data.repository.appLockSessionRepository.AppLockSessionRepository
import com.example.voicelock.data.repository.deviceLockRepository.DeviceLockRepository
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.util.getDataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * F1 "lock now" (voice command or the Home button). Locked apps always ask again (their unlock
 * sessions are cleared); with the device admin granted the phone screen is locked as well.
 */
class LockDeviceUseCase(
    private val deviceLockRepository: DeviceLockRepository,
    private val appLockSessionRepository: AppLockSessionRepository,
) {
    operator fun invoke(): Flow<Result<DeviceLockResult>> =
        flow<Result<DeviceLockResult>> {
            appLockSessionRepository.clearAll().getDataOrNull()
            val isAdminActive = deviceLockRepository.isDeviceAdminActive().getDataOrDefault(false)
            val result =
                if (isAdminActive && deviceLockRepository.lockScreen().first() is Result.Success) {
                    DeviceLockResult.SCREEN_LOCKED
                } else {
                    DeviceLockResult.APPS_LOCKED
                }
            emit(Result.Success(result))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()
}
