package com.example.voicelock.feature.home

import android.os.Bundle
import android.view.View
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
import com.example.voicelock.feature.biometric.confirmBiometric
import com.example.voicelock.feature.biometric.getBiometricActionText
import com.example.voicelock.feature.biometric.showBiometricUnavailable
import com.example.voicelock.feature.biometric.startBiometricEnrollment
import com.example.voicelock.feature.biometric.testBiometric
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.offerVoiceLockTile
import com.example.voicelock.util.MessageType
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.setPreventDoubleClickScaleView
import com.example.voicelock.util.showMessage

fun HomeFragment.initView() {
    // Basic setup if any
}

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

    binding.rowVoiceLock.setPreventDoubleClick {
        viewModel.onVoiceLockRowClicked()
    }

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
        viewModel.onAppLockClicked()
    }
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

    if (binding.switchVoiceUnlock.isChecked != info.isVoiceUnlockEnabled) {
        binding.switchVoiceUnlock.isChecked = info.isVoiceUnlockEnabled
    }

    if (binding.switchVoiceLock.isChecked != info.isVoiceLockEnabled) {
        binding.switchVoiceLock.isChecked = info.isVoiceLockEnabled
    }

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
    binding.btnFingerprint.visibility = if (hasSensor) View.VISIBLE else View.GONE
    binding.dividerFingerprint.visibility = if (hasSensor) View.VISIBLE else View.GONE
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
                message = event.message ?: getString(R.string.common_error_unexpected),
                type = MessageType.ERROR,
                anchorView = binding.layoutBottomNav,
            )
        }

        is HomeUiEvent.NavigateToPinLock -> {
            val bundle =
                Bundle().apply {
                    putString(BundleKey.KEY_LOCK_MODE, event.mode.name)
                    putInt(BundleKey.KEY_PIN_LENGTH, event.pinLength)
                }
            navigator.navigateTo(R.id.action_homeFragment_to_pinLockFragment, bundle)
        }

        is HomeUiEvent.NavigateToPatternLock -> {
            val bundle = Bundle().apply { putString(BundleKey.KEY_LOCK_MODE, event.mode.name) }
            navigator.navigateTo(R.id.action_homeFragment_to_patternLockFragment, bundle)
        }

        is HomeUiEvent.NavigateToSecurityQuestion -> {
            val bundle = Bundle().apply { putString(BundleKey.KEY_LOCK_MODE, event.mode.name) }
            navigator.navigateTo(R.id.action_homeFragment_to_securityQuestionFragment, bundle)
        }

        is HomeUiEvent.NavigateToAppLock -> {
            navigator.navigateTo(R.id.action_homeFragment_to_appLockFragment)
        }

        is HomeUiEvent.NavigateToVoiceEnrollment -> {
            restoreVoiceSwitches()
            val bundle = Bundle().apply { putString(BundleKey.KEY_VOICE_MODE, event.mode.name) }
            navigator.navigateTo(R.id.action_homeFragment_to_enrollmentFragment, bundle)
        }

        is HomeUiEvent.OfferVoiceLockTile -> offerVoiceLockTile()

        is HomeUiEvent.NavigateToVoiceTest -> {
            navigator.navigateTo(R.id.action_homeFragment_to_twoStageAuthFragment)
        }

        is HomeUiEvent.NavigateToFaceEnrollment -> {
            navigator.navigateTo(R.id.action_homeFragment_to_faceEnrollmentFragment)
        }

        is HomeUiEvent.NavigateToFaceTest -> {
            navigator.navigateTo(R.id.action_homeFragment_to_faceAuthFragment)
        }

        is HomeUiEvent.ConfirmBiometric -> confirmBiometric(
                type = event.type,
                anchorView = binding.layoutBottomNav,
                onConfirmed = { viewModel.onBiometricConfirmed(event.type) },
            )

        is HomeUiEvent.TestBiometric -> testBiometric(type = event.type, anchorView = binding.layoutBottomNav)

        is HomeUiEvent.OpenBiometricEnrollment -> startBiometricEnrollment(type = event.type, anchorView = binding.layoutBottomNav)

        is HomeUiEvent.ShowBiometricUnavailable -> showBiometricUnavailable(
                type = event.type,
                availability = event.availability,
                anchorView = binding.layoutBottomNav,
            )
    }
}

/** A switch flipped on before enrollment snaps back to the saved value. */
private fun HomeFragment.restoreVoiceSwitches() {
    val state = viewModel.uiState.value
    binding.switchVoiceUnlock.isChecked = state.isVoiceUnlockEnabled
    binding.switchVoiceLock.isChecked = state.isVoiceLockEnabled
}
