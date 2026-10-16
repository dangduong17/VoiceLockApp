package com.example.voicelock.domain.usecase.applock

import com.example.voicelock.data.model.applock.InstalledAppDtoModel
import com.example.voicelock.data.repository.installedAppsRepository.InstalledAppsRepository
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

class GetInstalledAppsUseCase(
    private val installedAppsRepository: InstalledAppsRepository,
) {
    operator fun invoke(): Flow<Result<List<InstalledAppDtoModel>>> = installedAppsRepository.getInstalledApps()
}
