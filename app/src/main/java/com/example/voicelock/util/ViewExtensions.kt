package com.example.voicelock.util

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

private const val DEFAULT_DEBOUNCE_MS = 500L
private const val PRESSED_SCALE = 0.9f
private const val NORMAL_SCALE = 1f
private const val DISABLED_ALPHA = 0.5f
private const val ENABLED_ALPHA = 1f
private const val SHAKE_OFFSET_PX = 12f
private const val SHAKE_DURATION_MS = 60L
private const val SHAKE_REPEAT_COUNT = 4

fun View.setPreventDoubleClick(
    debounceTime: Long = DEFAULT_DEBOUNCE_MS,
    action: () -> Unit,
) {
    setOnClickListener(
        object : View.OnClickListener {
            private var lastClickTime: Long = 0

            override fun onClick(v: View?) {
                if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime) return
                action()
                lastClickTime = SystemClock.elapsedRealtime()
            }
        },
    )
}

/**
 * Debounced click with 0.9x press-scale animation, for primary CTAs. The touch listener only
 * animates and forwards the tap to [View.performClick], so TalkBack's double-tap still works.
 */
@SuppressLint("ClickableViewAccessibility")
fun View.setPreventDoubleClickScaleView(
    debounceTime: Long = DEFAULT_DEBOUNCE_MS,
    action: () -> Unit,
) {
    setPreventDoubleClick(debounceTime = debounceTime, action = action)
    setOnTouchListener(
        object : View.OnTouchListener {
            private var rect: Rect? = null

            override fun onTouch(
                v: View,
                event: MotionEvent,
            ): Boolean {
                fun setScale(scale: Float) {
                    v.scaleX = scale
                    v.scaleY = scale
                }

                when {
                    event.action == MotionEvent.ACTION_DOWN -> {
                        rect = Rect(v.left, v.top, v.right, v.bottom)
                        setScale(PRESSED_SCALE)
                    }

                    rect?.contains(v.left + event.x.toInt(), v.top + event.y.toInt()) == false -> {
                        // moved out of the view
                        setScale(NORMAL_SCALE)
                        return false
                    }

                    event.action == MotionEvent.ACTION_UP -> {
                        setScale(NORMAL_SCALE)
                        if (v.isEnabled) v.performClick()
                    }

                    event.action == MotionEvent.ACTION_CANCEL -> setScale(NORMAL_SCALE)

                    else -> {
                        // Do nothing
                    }
                }
                return true
            }
        },
    )
}

fun Fragment.displayToast(msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

/**
 * Horizontal shake animation for invalid-input feedback (wrong PIN/pattern)
 */
fun View.shakeError() {
    val animation =
        TranslateAnimation(0f, SHAKE_OFFSET_PX, 0f, 0f).apply {
            duration = SHAKE_DURATION_MS
            repeatCount = SHAKE_REPEAT_COUNT
            repeatMode = TranslateAnimation.REVERSE
        }
    startAnimation(animation)
}

/** Enables / disables a button and dims it while disabled. */
fun View.setEnabledWithAlpha(isEnabled: Boolean) {
    this.isEnabled = isEnabled
    alpha = if (isEnabled) ENABLED_ALPHA else DISABLED_ALPHA
}

fun TextView.setTextColorRes(
    @ColorRes colorRes: Int,
) {
    setTextColor(ContextCompat.getColor(context, colorRes))
}

/** Sets the switch only when it differs, so a render does not re-fire its listener for nothing. */
fun CompoundButton.syncChecked(isChecked: Boolean) {
    if (this.isChecked != isChecked) this.isChecked = isChecked
}

/** Opens a system screen; some ROMs lack it, which must not crash the app. */
fun Fragment.startActivitySafely(intent: Intent): Boolean = runCatching { startActivity(intent) }.isSuccess
