package com.example.voicelock.data.repository.installedAppsRepository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.voicelock.data.model.applock.InstalledAppDtoModel
import com.example.voicelock.util.Result
import com.example.voicelock.util.catchAsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class InstalledAppsRepositoryImpl(
    private val context: Context,
) : InstalledAppsRepository {

    override fun getInstalledApps(): Flow<Result<List<InstalledAppDtoModel>>> =
        flow<Result<List<InstalledAppDtoModel>>> {
            val packageManager = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolvedApps =
                packageManager
                    .queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
                    .asSequence()
                    .map { resolveInfo ->
                        InstalledAppDtoModel(
                            packageName = resolveInfo.activityInfo.packageName,
                            label = resolveInfo.loadLabel(packageManager).toString(),
                            icon = resolveInfo.loadIcon(packageManager),
                        )
                    }
                    .filterNot { it.packageName == context.packageName }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
                    .toList()
            emit(Result.Success(resolvedApps))
        }.flowOn(Dispatchers.IO)
            .catchAsResult()
}
