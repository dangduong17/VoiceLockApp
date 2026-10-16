package com.example.voicelock.domain.usecase.pinlock

import com.example.voicelock.data.model.pinlock.PinInfo
import com.example.voicelock.data.repository.lockRepository.LockRepository
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import com.example.voicelock.util.getDataOrDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GetPinInfoUseCase(
    private val lockRepository: LockRepository,
) {
    operator fun invoke(): Flow<Result<PinInfo>> =
        flow<Result<PinInfo>> {
            val isPinSet = lockRepository.isPinSet().getDataOrDefault(false)
            val pinLength = lockRepository.getPinLength().getDataOrDefault(6)
            emit(Result.Success(PinInfo(isPinSet = isPinSet, pinLength = pinLength)))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()
}
