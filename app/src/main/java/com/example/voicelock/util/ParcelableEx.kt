package com.example.voicelock.util

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelable<T>(key)
    }

/** Enum stored by name (e.g. `KEY_LOCK_MODE`); a missing or unknown name falls back to [defaultValue]. */
inline fun <reified T : Enum<T>> Bundle?.getEnum(
    key: String,
    defaultValue: T,
): T = this?.getString(key)?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: defaultValue
