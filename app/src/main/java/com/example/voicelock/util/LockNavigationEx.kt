package com.example.voicelock.util

import androidx.core.os.bundleOf
import com.example.voicelock.base.navigator.Navigator
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.securityquestion.SecurityQuestionMode
import com.example.voicelock.data.model.voice.VoiceVerificationMode

/** Lock-method screens opened from Home, Settings and the verify screens, with their arguments. */
fun Navigator.navigateToPinLock(
    actionId: Int,
    mode: PinLockMode,
    pinLength: Int = Constant.PIN_LENGTH_DEFAULT,
) {
    navigateTo(actionId, bundleOf(BundleKey.KEY_LOCK_MODE to mode.name, BundleKey.KEY_PIN_LENGTH to pinLength))
}

fun Navigator.navigateToPatternLock(
    actionId: Int,
    mode: PatternLockMode,
) {
    navigateTo(actionId, bundleOf(BundleKey.KEY_LOCK_MODE to mode.name))
}

fun Navigator.navigateToSecurityQuestion(
    actionId: Int,
    mode: SecurityQuestionMode,
) {
    navigateTo(actionId, bundleOf(BundleKey.KEY_LOCK_MODE to mode.name))
}

/** [mode] UNLOCK: the voice passphrase; LOCK: the F1 lock command. */
fun Navigator.navigateToVoiceEnrollment(
    actionId: Int,
    mode: VoiceVerificationMode,
) {
    navigateTo(actionId, bundleOf(BundleKey.KEY_VOICE_MODE to mode.name))
}
