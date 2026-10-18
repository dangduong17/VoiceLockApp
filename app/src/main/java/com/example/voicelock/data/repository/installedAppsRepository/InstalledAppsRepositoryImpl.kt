package com.example.voicelock.data.repository.installedAppsRepository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.voicelock.data.model.applock.InstalledAppDtoModel
import com.example.voicelock.util.Result
import com.example.voicelock.util.resultFlow
import kotlinx.coroutines.flow.Flow

class InstalledAppsRepositoryImpl(
    private val context: Context,
) : InstalledAppsRepository {

    override fun getInstalledApps(): Flow<Result<List<InstalledAppDtoModel>>> =
        resultFlow {
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
            resolvedApps
        }
}
