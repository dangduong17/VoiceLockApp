package com.example.voicelock.domain.usecase.home

import com.example.voicelock.data.model.home.HomeUiState
import com.example.voicelock.data.model.pinlock.PinInfo
import com.example.voicelock.domain.usecase.patternlock.GetPatternInfoUseCase
import com.example.voicelock.domain.usecase.pinlock.GetPinInfoUseCase
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import com.example.voicelock.util.getDataOrDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GetHomeStatusUseCase(
    private val getPinInfoUseCase: GetPinInfoUseCase,
    private val getPatternInfoUseCase: GetPatternInfoUseCase,
) {
    operator fun invoke(): Flow<Result<HomeUiState>> =
        flow<Result<HomeUiState>> {
            val pinInfo = getPinInfoUseCase().getDataOrDefault(PinInfo(isPinSet = false, pinLength = 6))
            val isPatternSet = getPatternInfoUseCase().getDataOrDefault(false)

            emit(
                Result.Success(
                    HomeUiState(
                        isPinSet = pinInfo.isPinSet,
                        pinLength = pinInfo.pinLength,
                        isPatternSet = isPatternSet,
                        pinStatusText =
                            if (pinInfo.isPinSet) "${pinInfo.pinLength} chữ số • Đã thiết lập" else "Chưa thiết lập",
                        patternStatusText = if (isPatternSet) "Đã thiết lập" else "Thiết lập",
                    ),
                ),
            )
        }.flowOn(Dispatchers.IO)
            .catchAsResult()
}
