package com.example.voicelock.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.voicelock.R
import com.example.voicelock.feature.voicecommand.VoiceCommandActivity
import com.example.voicelock.util.Constant
import com.example.voicelock.util.hasOverlayPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Plain foreground Service (framework entry point, like AppLockMonitorService) that shows the
 * floating voice-lock button over every app ("Display over other apps"). Tapping it
 * [Constant.FLOATING_LOCK_TAP_COUNT] times within [Constant.FLOATING_LOCK_TAP_WINDOW_MS] opens the
 * voice lock command (VoiceCommandActivity): the phone is only locked after the owner's voice and
 * lock command are verified. Only touches on the button itself are seen, so typing elsewhere can
 * never trigger it. Drag to move; it snaps to the nearest screen edge and remembers the position.
 */
class FloatingLockService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var windowManager: WindowManager
    private var buttonView: ImageView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val tapTimes = ArrayDeque<Long>()
    private var dimJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WindowManager::class.java)
        if (!hasOverlayPermission()) {
            Timber.tag(TAG).w("No overlay permission: floating lock button not shown")
            stopSelf()
            return
        }
        addButton()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        buttonView?.let { runCatching { windowManager.removeView(it) } }
        buttonView = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun addButton() {
        val size = resources.getDimensionPixelSize(R.dimen._52dp)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val params =
            WindowManager.LayoutParams(
                size,
                size,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = prefs.getInt(KEY_X, 0)
                y = prefs.getInt(KEY_Y, resources.displayMetrics.heightPixels / 3)
            }
        val view =
            ImageView(this).apply {
                setImageResource(R.drawable.ic_lock)
                background = ContextCompat.getDrawable(context, R.drawable.bg_floating_lock)
                val padding = resources.getDimensionPixelSize(R.dimen._14dp)
                setPadding(padding, padding, padding, padding)
                contentDescription = getString(R.string.floating_lock_description)
                elevation = resources.getDimension(R.dimen._6dp)
                setOnTouchListener(ButtonTouchListener())
            }
        windowManager.addView(view, params)
        buttonView = view
        layoutParams = params
        scheduleDim()
    }

    private inner class ButtonTouchListener : View.OnTouchListener {
        private val touchSlop = ViewConfiguration.get(this@FloatingLockService).scaledTouchSlop
        private var downRawX = 0f
        private var downRawY = 0f
        private var startX = 0
        private var startY = 0
        private var isDragging = false

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(
            view: View,
            event: MotionEvent,
        ): Boolean {
            val params = layoutParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    isDragging = false
                    wakeUp()
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) isDragging = true
                    if (isDragging) {
                        params.x = startX + dx.roundToInt()
                        params.y = startY + dy.roundToInt()
                        windowManager.updateViewLayout(view, params)
                    }
                }
                MotionEvent.ACTION_UP -> if (isDragging) snapToEdge() else onTap()
                MotionEvent.ACTION_CANCEL -> if (isDragging) snapToEdge()
            }
            return true
        }
    }

    private fun onTap() {
        val now = SystemClock.elapsedRealtime()
        while (tapTimes.isNotEmpty() && now - tapTimes.first() > Constant.FLOATING_LOCK_TAP_WINDOW_MS) tapTimes.removeFirst()
        tapTimes.addLast(now)
        buttonView?.animate()?.scaleX(TAP_SCALE)?.scaleY(TAP_SCALE)?.setDuration(TAP_ANIMATION_MS)
            ?.withEndAction { buttonView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(TAP_ANIMATION_MS)?.start() }
            ?.start()
        if (tapTimes.size >= Constant.FLOATING_LOCK_TAP_COUNT) {
            tapTimes.clear()
            openVoiceLockCommand()
        }
    }

    /** F1: the button is a shortcut to the voice lock command, never a lock without the owner's voice. */
    private fun openVoiceLockCommand() {
        // Allowed from the background: the app holds "Display over other apps".
        runCatching { startActivity(VoiceCommandActivity.createIntent(this)) }
            .onFailure { Timber.tag(TAG).e(it, "Voice lock command could not be opened") }
    }

    private fun snapToEdge() {
        val view = buttonView ?: return
        val params = layoutParams ?: return
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        params.x = if (params.x + view.width / 2 < screenWidth / 2) 0 else screenWidth - view.width
        params.y = params.y.coerceIn(0, (screenHeight - view.height).coerceAtLeast(0))
        windowManager.updateViewLayout(view, params)
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_X, params.x)
            putInt(KEY_Y, params.y)
        }
        scheduleDim()
    }

    /** Fully visible while touched, faded when idle so it does not cover the content. */
    private fun wakeUp() {
        dimJob?.cancel()
        buttonView?.alpha = 1f
    }

    private fun scheduleDim() {
        dimJob?.cancel()
        dimJob =
            serviceScope.launch {
                delay(Constant.FLOATING_LOCK_IDLE_DIM_MS)
                buttonView?.animate()?.alpha(IDLE_ALPHA)?.setDuration(DIM_ANIMATION_MS)?.start()
            }
    }

    @Suppress("DEPRECATION")
    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(CHANNEL_ID, getString(R.string.floating_lock_title), NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.floating_lock_title))
            .setContentText(getString(R.string.floating_lock_notification_text))
            .setSmallIcon(R.drawable.ic_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val TAG = "FloatingLockService"
        private const val CHANNEL_ID = "floating_lock_channel"
        private const val NOTIFICATION_ID = 1002
        private const val PREFS_NAME = "floating_lock_button"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val TAP_SCALE = 0.85f
        private const val TAP_ANIMATION_MS = 70L
        private const val IDLE_ALPHA = 0.55f
        private const val DIM_ANIMATION_MS = 300L

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, FloatingLockService::class.java))
            } catch (e: IllegalStateException) {
                Timber.tag(TAG).e(e, "Floating lock button could not be started")
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingLockService::class.java))
        }
    }
}
