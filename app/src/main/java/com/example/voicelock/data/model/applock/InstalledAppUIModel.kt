package com.example.voicelock.data.model.applock

import android.graphics.drawable.Drawable

data class InstalledAppUIModel(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)

fun InstalledAppDtoModel.toPresentation(): InstalledAppUIModel =
    InstalledAppUIModel(packageName = packageName, label = label, icon = icon)
