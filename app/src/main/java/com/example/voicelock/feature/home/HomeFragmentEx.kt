package com.example.voicelock.feature.home

import androidx.core.view.isVisible
import com.example.voicelock.R
import com.example.voicelock.data.model.biometric.BiometricAvailability
import com.example.voicelock.data.model.lock.DeviceLockResult
import com.example.voicelock.data.model.biometric.BiometricSettingsDtoModel
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.biometric.isActive
import com.example.voicelock.data.model.face.FaceSettingsDtoModel
import com.example.voicelock.data.model.face.isFaceUnlockActive
import com.example.voicelock.data.model.home.HomeLockInfo
import com.example.voicelock.data.model.home.HomeUiEvent
import com.example.voicelock.data.model.home.HomeVoiceInfo
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.feature.biometric.getBiometricActionText
import com.example.voicelock.feature.protection.LockMethodRoutes
import com.example.voicelock.feature.protection.handleLockMethodEvent
import com.example.voicelock.util.MessageType
import com.example.voicelock.util.navigateToVoiceEnrollment
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.setPreventDoubleClickScaleView
import com.example.voicelock.util.showMessage
import com.example.voicelock.util.syncChecked

fun HomeFragment.settingEvent() {
    binding.tabSettings.setPreventDoubleClick {
        navigator.navigateTo(R.id.action_homeFragment_to_settingsFragment)
    }

    binding.btnLockNow.setPreventDoubleClickScaleView {
        viewModel.onLockNowClicked()
    }

    binding.switchVoiceUnlock.setOnCheckedChangeListener { _, isChecked ->
        viewModel.toggleVoiceUnlock(isChecked)
    }

    binding.switchVoiceLock.setOnCheckedChangeListener { _, isChecked ->
        viewModel.toggleVoiceLock(isChecked)
    }

    // F1: tapping the voice lock row records (or re-records) the lock command.
    binding.rowVoiceLock.setPreventDoubleClick { navigateToVoiceEnrollment(VoiceVerificationMode.LOCK) }

    binding.rowVoiceUnlock.setPreventDoubleClick {
        viewModel.onVoiceUnlockRowClicked()
    }

    binding.tvVoiceReadyTitle.setPreventDoubleClick {
        viewModel.onVoiceUnlockRowClicked()
    }

    binding.btnPin.setPreventDoubleClick {
        viewModel.onPinClicked()
    }

    binding.btnPattern.setPreventDoubleClick {
        viewModel.onPatternClicked()
    }

    binding.btnFingerprint.setPreventDoubleClick {
        viewModel.onBiometricRowClicked(BiometricType.FINGERPRINT)
    }

    binding.btnFace.setPreventDoubleClick {
        viewModel.onFaceClicked()
    }

    binding.btnQuestions.setPreventDoubleClick {
        viewModel.onQuestionsClicked()
    }

    binding.btnAppLock.setPreventDoubleClick {
        navigator.navigateTo(R.id.action_homeFragment_to_appLockFragment)
    }
}

private fun HomeFragment.navigateToVoiceEnrollment(mode: VoiceVerificationMode) {
    navigator.navigateToVoiceEnrollment(actionId = R.id.action_homeFragment_to_enrollmentFragment, mode = mode)
}

fun HomeFragment.updateProtection(isProtectionEnabled: Boolean) {
    binding.tvBadgeText.setText(
        if (isProtectionEnabled) R.string.protection_status_enabled else R.string.protection_status_disabled,
    )
}

fun HomeFragment.updateVoice(info: HomeVoiceInfo) {
    binding.tvVoiceReadyTitle.setText(if (info.isVoiceEnrolled) R.string.voice_ready else R.string.voice_not_enrolled)
    binding.tvVoiceInfo.text =
        if (info.isVoiceEnrolled) {
            getString(
                R.string.voice_info_enrolled,
                info.voiceSamplesCount,
                getString(if (info.isBackupPinEnabled) R.string.voice_info_backup_pin_on else R.string.voice_info_backup_pin_off),
            )
        } else {
            getString(R.string.voice_not_enrolled_info)
        }
    binding.tvVoiceUnlockSub.setText(
        if (info.isVoiceEnrolled) R.string.unlock_by_voice_sub else R.string.voice_not_enrolled_info,
    )

    binding.switchVoiceUnlock.syncChecked(info.isVoiceUnlockEnabled)
    binding.switchVoiceLock.syncChecked(info.isVoiceLockEnabled)

    binding.tvVoiceLockSub.text =
        if (info.isVoiceLockCommandEnrolled) {
            getString(R.string.voice_lock_sub_enrolled, info.voiceLockPhrase)
        } else {
            getString(R.string.voice_lock_sub_not_enrolled)
        }
}

fun HomeFragment.updateLockMethods(info: HomeLockInfo) {
    binding.tvPinSub.text =
        if (info.isPinSet) getString(R.string.pin_status_set, info.pinLength) else getString(R.string.pin_not_set_status)
    binding.tvPatternAction.setText(if (info.isPatternSet) R.string.pattern_set_status else R.string.setup)
}

fun HomeFragment.updateFingerprint(biometric: BiometricSettingsDtoModel?) {
    if (biometric == null) return
    // F4: a device without a fingerprint sensor never sees the option.
    val hasSensor = biometric.fingerprintAvailability != BiometricAvailability.NO_HARDWARE
    binding.btnFingerprint.isVisible = hasSensor
    binding.dividerFingerprint.isVisible = hasSensor
    binding.tvFingerprintAction.text =
        getBiometricActionText(
            availability = biometric.fingerprintAvailability,
            isActive = biometric.isActive(BiometricType.FINGERPRINT),
        )
}

fun HomeFragment.updateFace(face: FaceSettingsDtoModel) {
    binding.tvFaceAction.setText(
        when {
            !face.isEnrolled -> R.string.setup
            face.isFaceUnlockActive() -> R.string.biometric_action_enabled
            else -> R.string.face_action_disabled
        },
    )
}

fun HomeFragment.handleUiEvent(event: HomeUiEvent) {
    when (event) {
        is HomeUiEvent.LockNowDone ->
            // With the device admin the screen is already off; otherwise say that only the apps were locked.
            if (event.result == DeviceLockResult.APPS_LOCKED) {
                showMessage(message = getString(R.string.voice_lock_apps_locked), type = MessageType.SUCCESS, anchorView = binding.layoutBottomNav)
            }

        is HomeUiEvent.ShowLoadError -> {
            showMessage(
                message = getString(R.string.common_error_unexpected),
                type = MessageType.ERROR,
                anchorView = binding.layoutBottomNav,
            )
        }

        is HomeUiEvent.LockMethod -> {
            // A voice switch flipped on before enrollment snaps back to the saved value.
            restoreVoiceSwitches()
            handleLockMethodEvent(
                event = event.event,
                routes = HOME_LOCK_METHOD_ROUTES,
                anchorView = binding.layoutBottomNav,
                onBiometricConfirmed = { type -> viewModel.onBiometricConfirmed(type) },
            )
        }
    }
}

/** A switch flipped on before enrollment snaps back to the saved value. */
private fun HomeFragment.restoreVoiceSwitches() {
    val state = viewModel.uiState.value
    binding.switchVoiceUnlock.syncChecked(state.isVoiceUnlockEnabled)
    binding.switchVoiceLock.syncChecked(state.isVoiceLockEnabled)
}

private val HOME_LOCK_METHOD_ROUTES =
    LockMethodRoutes(
        pinLockActionId = R.id.action_homeFragment_to_pinLockFragment,
        patternLockActionId = R.id.action_homeFragment_to_patternLockFragment,
        securityQuestionActionId = R.id.action_homeFragment_to_securityQuestionFragment,
        voiceEnrollmentActionId = R.id.action_homeFragment_to_enrollmentFragment,
        voiceTestActionId = R.id.action_homeFragment_to_twoStageAuthFragment,
        faceEnrollmentActionId = R.id.action_homeFragment_to_faceEnrollmentFragment,
        faceTestActionId = R.id.action_homeFragment_to_faceAuthFragment,
    )
