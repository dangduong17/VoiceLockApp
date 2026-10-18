package com.example.voicelock.feature.voicecommand

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.example.voicelock.R
import com.example.voicelock.util.BundleKey
import androidx.core.os.bundleOf

/**
 * F1 entry point for the voice lock command, opened from the Quick Settings tile, the launcher
 * shortcut or Home. A plain Activity shell (same status as LockOverlayActivity) hosting the
 * existing TwoStageAuthFragment in lock-command mode: it listens right away, verifies the owner's
 * lock command and locks the phone.
 */
class VoiceCommandActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_command)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutVoiceCommandRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (savedInstanceState == null) {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostVoiceCommand) as NavHostFragment
            val args = bundleOf(BundleKey.KEY_VOICE_LOCK_COMMAND to true)
            navHostFragment.navController.setGraph(R.navigation.nav_graph_voice_command, args)
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, VoiceCommandActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
