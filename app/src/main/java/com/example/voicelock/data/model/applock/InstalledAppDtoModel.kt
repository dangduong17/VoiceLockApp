package com.example.voicelock.data.model.applock

import android.graphics.drawable.Drawable

data class InstalledAppDtoModel(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)
