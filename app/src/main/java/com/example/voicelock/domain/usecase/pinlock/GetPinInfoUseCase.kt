package com.example.voicelock.domain.usecase.pinlock

import com.example.voicelock.data.model.pinlock.PinInfo
import com.example.voicelock.data.repository.lockRepository.LockRepository
import com.example.voicelock.util.Constant
import com.example.voicelock.util.Result
import com.example.voicelock.util.resultFlow
import com.example.voicelock.util.getDataOrDefault
import kotlinx.coroutines.flow.Flow

class GetPinInfoUseCase(
    private val lockRepository: LockRepository,
) {
    operator fun invoke(): Flow<Result<PinInfo>> =
        resultFlow {
            val isPinSet = lockRepository.isPinSet().getDataOrDefault(false)
            val pinLength = lockRepository.getPinLength().getDataOrDefault(Constant.PIN_LENGTH_DEFAULT)
            PinInfo(isPinSet = isPinSet, pinLength = pinLength)
        }
}
