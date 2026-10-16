package com.example.voicelock.domain.usecase.home

import com.example.voicelock.data.model.home.HomeUiState
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetHomeStatusUseCase {
    operator fun invoke(): Flow<Result<HomeUiState>> = flow {
        emit(Result.Success(HomeUiState()))
    }
}
