package com.example.voicelock.util

import android.widget.TextView
import androidx.core.view.isVisible
import com.example.voicelock.R

/** "Try again in N seconds" under a PIN / pattern / security question, hidden when not locked out. */
fun TextView.updateLockoutMessage(remainingSeconds: Int) {
    isVisible = remainingSeconds > 0
    if (isVisible) text = context.getString(R.string.lockout_message, remainingSeconds)
}
