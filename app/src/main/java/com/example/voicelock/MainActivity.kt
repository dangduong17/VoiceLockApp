package com.example.voicelock

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.voicelock.service.AppLockMonitorLauncher
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val appLockMonitorLauncher: AppLockMonitorLauncher by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // After a force stop (no boot broadcast arrives) opening VoiceLock brings the app lock back.
        appLockMonitorLauncher.syncWithSavedSettingsAsync()
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}