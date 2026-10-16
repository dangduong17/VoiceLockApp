package com.example.voicelock.data.model.lock

/**
 * An unlock method that can be offered on the lock screen. New methods (fingerprint, face,
 * voice, ...) are added here as the app grows; [com.example.voicelock.feature.applock.LockOverlayViewModel]
 * builds the ordered list of methods the user has actually configured and lets them switch
 * between them, so adding a method here plus its own verification screen is all that's needed.
 */
enum class LockMethod {
    PATTERN,
    PIN,
}
