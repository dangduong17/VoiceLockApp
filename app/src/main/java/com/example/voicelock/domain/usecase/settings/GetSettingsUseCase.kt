package com.example.voicelock.domain.usecase.settings

import com.example.voicelock.data.model.settings.SettingsUiState
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetSettingsUseCase {
    operator fun invoke(): Flow<Result<SettingsUiState>> = flow {
        emit(Result.Success(SettingsUiState()))
    }
}
