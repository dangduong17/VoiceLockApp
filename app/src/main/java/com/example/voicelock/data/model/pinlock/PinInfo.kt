package com.example.voicelock.data.model.pinlock

import com.example.voicelock.util.Constant

data class PinInfo(
    val isPinSet: Boolean,
    val pinLength: Int,
) {
    companion object {
        /** No PIN saved yet: setup starts at the default length. */
        val NOT_SET = PinInfo(isPinSet = false, pinLength = Constant.PIN_LENGTH_DEFAULT)
    }
}
