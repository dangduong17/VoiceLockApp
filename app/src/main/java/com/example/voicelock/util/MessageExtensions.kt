package com.example.voicelock.util

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.voicelock.R
import com.google.android.material.snackbar.Snackbar

enum class MessageType(
    @param:ColorRes val backgroundColorRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    INFO(R.color.message_info_bg, R.drawable.ic_message_info),
    SUCCESS(R.color.message_success_bg, R.drawable.ic_message_success),
    ERROR(R.color.message_error_bg, R.drawable.ic_message_error),
}

/**
 * In-app message: a floating, rounded Material Snackbar with an icon and a color per [type],
 * replacing system toasts. It is attached to the activity content, so it stays visible when the
 * screen navigates up right after showing it (e.g. "PIN saved"). Screens with a bottom bar pass
 * it as [anchorView] so the message floats above it. An optional action (e.g. open settings)
 * keeps the message on screen longer.
 */
fun Fragment.showMessage(
    message: String,
    type: MessageType = MessageType.INFO,
    anchorView: View? = null,
    actionText: String? = null,
    onAction: () -> Unit = {},
) {
    val host = activity?.findViewById<View>(android.R.id.content) ?: return
    val context = host.context
    val duration = if (actionText != null || type == MessageType.ERROR) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
    val snackbar = Snackbar.make(host, message, duration).setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
    anchorView?.takeIf { it.isAttachedToWindow }?.let { snackbar.anchorView = it }

    snackbar.view.apply {
        background = ContextCompat.getDrawable(context, R.drawable.bg_message)
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, type.backgroundColorRes))
    }
    snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.apply {
        setTextColor(ContextCompat.getColor(context, R.color.white))
        maxLines = MESSAGE_MAX_LINES
        setCompoundDrawablesRelativeWithIntrinsicBounds(type.iconRes, 0, 0, 0)
        compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen._12dp)
    }
    if (actionText != null) {
        snackbar.setAction(actionText) { onAction() }
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.message_action))
    }
    snackbar.show()
}

/** Error for a permission the user denied, with a shortcut to this app's Android settings page. */
fun Fragment.showPermissionDeniedMessage(message: String) {
    showMessage(
        message = message,
        type = MessageType.ERROR,
        actionText = getString(R.string.message_action_open_settings),
        onAction = { openAppSettings() },
    )
}

fun Fragment.openAppSettings() {
    val context = context ?: return
    runCatching {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private const val MESSAGE_MAX_LINES = 3
