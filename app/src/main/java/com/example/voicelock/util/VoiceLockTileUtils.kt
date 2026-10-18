package com.example.voicelock.util

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.voicelock.R
import com.example.voicelock.service.VoiceLockTileService

/**
 * F1: on Android 13+ ask the system to add the "Khóa bằng giọng nói" Quick Settings tile (the user
 * confirms in a system dialog; nothing happens if it is already added). Older Android versions:
 * the user adds the tile by editing the Quick Settings panel.
 */
fun Fragment.offerVoiceLockTile() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = context ?: return
    runCatching {
        context.getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(context, VoiceLockTileService::class.java),
            getString(R.string.lock_by_voice),
            Icon.createWithResource(context, R.drawable.ic_lock),
            ContextCompat.getMainExecutor(context),
        ) { }
    }
}
