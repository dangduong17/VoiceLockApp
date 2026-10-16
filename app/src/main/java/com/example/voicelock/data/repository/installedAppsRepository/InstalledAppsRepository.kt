package com.example.voicelock.data.repository.installedAppsRepository

import com.example.voicelock.data.model.applock.InstalledAppDtoModel
import com.example.voicelock.util.Result
import kotlinx.coroutines.flow.Flow

interface InstalledAppsRepository {
    fun getInstalledApps(): Flow<Result<List<InstalledAppDtoModel>>>
}
