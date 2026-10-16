package com.example.voicelock.data.model.applock

import android.graphics.drawable.Drawable

data class AppLockItemUiModel(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isLocked: Boolean,
)

fun InstalledAppUIModel.toAppLockItem(lockedPackages: Set<String>): AppLockItemUiModel =
    AppLockItemUiModel(
        packageName = packageName,
        label = label,
        icon = icon,
        isLocked = packageName in lockedPackages,
    )
