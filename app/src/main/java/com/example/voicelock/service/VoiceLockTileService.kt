package com.example.voicelock.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.voicelock.MainActivity
import com.example.voicelock.R
import com.example.voicelock.data.model.voice.VoiceSettingsDtoModel
import com.example.voicelock.data.model.voice.isVoiceLockActive
import com.example.voicelock.domain.usecase.voice.GetVoiceSettingsUseCase
import com.example.voicelock.feature.voicecommand.VoiceCommandActivity
import com.example.voicelock.util.getDataOrDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * F1 Quick Settings tile "Khóa bằng giọng nói" (framework entry point, like AppLockMonitorService).
 * Active while voice lock is on: a tap opens the voice lock command. Inactive: a tap opens VoiceLock
 * so the user can record the lock command / turn the feature on.
 */
class VoiceLockTileService : TileService() {

    private val getVoiceSettingsUseCase: GetVoiceSettingsUseCase by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var isVoiceLockActive = false

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch {
            isVoiceLockActive = getVoiceSettingsUseCase().getDataOrDefault(VoiceSettingsDtoModel()).isVoiceLockActive()
            qsTile?.apply {
                // An action tile: always tappable; the subtitle says whether voice lock is ready.
                state = Tile.STATE_INACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    subtitle =
                        getString(if (isVoiceLockActive) R.string.voice_lock_tile_subtitle_ready else R.string.voice_lock_tile_subtitle_off)
                }
                updateTile()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = if (isVoiceLockActive) VoiceCommandActivity.createIntent(this) else Intent(this, MainActivity::class.java)
        launchAndCollapse(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchAndCollapse(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
