package com.example.voicelock.util

import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.Toast
import androidx.fragment.app.Fragment

fun View.setPreventDoubleClick(
    debounceTime: Long = 500,
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
 * Debounced click with 0.9x press-scale animation, for primary CTAs
 */
fun View.setPreventDoubleClickScaleView(
    debounceTime: Long = 500,
    action: () -> Unit,
) {
    setOnTouchListener(
        object : View.OnTouchListener {
            private var lastClickTime: Long = 0
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
                        setScale(0.9f)
                    }

                    rect?.contains(v.left + event.x.toInt(), v.top + event.y.toInt()) == false -> {
                        // moved out of the view
                        setScale(1f)
                        return false
                    }

                    event.action == MotionEvent.ACTION_UP -> {
                        setScale(1f)
                        if (SystemClock.elapsedRealtime() - lastClickTime >= debounceTime) {
                            lastClickTime = SystemClock.elapsedRealtime()
                            action()
                        }
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
        TranslateAnimation(0f, 12f, 0f, 0f).apply {
            duration = 60
            repeatCount = 4
            repeatMode = TranslateAnimation.REVERSE
        }
    startAnimation(animation)
}
