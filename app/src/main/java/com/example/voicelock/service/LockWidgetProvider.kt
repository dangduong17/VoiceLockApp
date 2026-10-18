package com.example.voicelock.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.voicelock.R
import com.example.voicelock.feature.voicecommand.VoiceCommandActivity

/**
 * Home-screen widget "Khóa bằng giọng nói": one tap opens the voice lock command
 * (VoiceCommandActivity), which listens right away and locks only after the owner's voice and lock
 * command are verified.
 */
class LockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                VoiceCommandActivity.createIntent(context),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val views =
            RemoteViews(context.packageName, R.layout.widget_lock).apply {
                setOnClickPendingIntent(R.id.layoutWidgetLock, pendingIntent)
            }
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
    }
}
