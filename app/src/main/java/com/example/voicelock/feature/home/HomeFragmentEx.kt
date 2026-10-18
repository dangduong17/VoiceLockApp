package com.example.voicelock.feature.home

import android.os.Bundle
import com.example.voicelock.R
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.biometric.isActive
import com.example.voicelock.data.model.face.isFaceUnlockActive
import com.example.voicelock.feature.biometric.confirmBiometric
import com.example.voicelock.feature.biometric.getBiometricActionText
import com.example.voicelock.feature.biometric.showBiometricUnavailable
import com.example.voicelock.feature.biometric.startBiometricEnrollment
import com.example.voicelock.feature.biometric.testBiometric
import com.example.voicelock.data.model.home.HomeUiEvent
import com.example.voicelock.data.model.home.HomeUiState
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.displayToast
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.setPreventDoubleClickScaleView

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

fun HomeFragment.updateUiState(state: HomeUiState) {
    
    val lockNowStatusText = if (state.isProtectionEnabled) "BẢO VỆ ĐANG BẬT" else "BẢO VỆ ĐANG TẮT"
    binding.tvBadgeText.text = lockNowStatusText

    binding.tvVoiceReadyTitle.setText(if (state.isVoiceEnrolled) R.string.voice_ready else R.string.voice_not_enrolled)
    binding.tvVoiceInfo.text =
        if (state.isVoiceEnrolled) {
            getString(
                R.string.voice_info_enrolled,
                state.voiceSamplesCount,
                getString(if (state.isBackupPinEnabled) R.string.voice_info_backup_pin_on else R.string.voice_info_backup_pin_off),
            )
        } else {
            getString(R.string.voice_not_enrolled_info)
        }
    binding.tvVoiceUnlockSub.setText(
        if (state.isVoiceEnrolled) R.string.unlock_by_voice_sub else R.string.voice_not_enrolled_info,
    )

    if (binding.switchVoiceUnlock.isChecked != state.isVoiceUnlockEnabled) {
        binding.switchVoiceUnlock.isChecked = state.isVoiceUnlockEnabled
    }

    if (binding.switchVoiceLock.isChecked != state.isVoiceLockEnabled) {
        binding.switchVoiceLock.isChecked = state.isVoiceLockEnabled
    }

    binding.tvPinSub.text = state.pinStatusText
    binding.tvPatternAction.text = state.patternStatusText
    val biometric = state.biometricSettings
    binding.tvFingerprintAction.text =
        getBiometricActionText(biometric.fingerprintAvailability, biometric.isActive(BiometricType.FINGERPRINT))
    val face = state.faceSettings
    binding.tvFaceAction.text =
        getString(
            when {
                !face.isEnrolled -> R.string.setup
                face.isFaceUnlockActive() -> R.string.biometric_action_enabled
                else -> R.string.face_action_disabled
            },
        )
}

fun HomeFragment.handleUiEvent(event: HomeUiEvent) {
    when (event) {
        is HomeUiEvent.LockNow -> {
            displayToast("Đang tiến hành khóa ngay...")
        }

        is HomeUiEvent.ShowToast -> {
            displayToast(event.message)
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

        is HomeUiEvent.NavigateToSecurityQuestions -> {
            displayToast("Mở cài đặt Câu hỏi bảo mật")
        }

        is HomeUiEvent.NavigateToAppLock -> {
            navigator.navigateTo(R.id.action_homeFragment_to_appLockFragment)
        }

        is HomeUiEvent.NavigateToVoiceEnrollment -> {
            restoreVoiceSwitches()
            navigator.navigateTo(R.id.action_homeFragment_to_enrollmentFragment)
        }

        is HomeUiEvent.NavigateToVoiceTest -> {
            navigator.navigateTo(R.id.action_homeFragment_to_twoStageAuthFragment)
        }

        is HomeUiEvent.NavigateToFaceEnrollment -> {
            navigator.navigateTo(R.id.action_homeFragment_to_faceEnrollmentFragment)
        }

        is HomeUiEvent.NavigateToFaceTest -> {
            navigator.navigateTo(R.id.action_homeFragment_to_faceAuthFragment)
        }

        is HomeUiEvent.ConfirmBiometric -> confirmBiometric(event.type) { viewModel.onBiometricConfirmed(event.type) }

        is HomeUiEvent.TestBiometric -> testBiometric(event.type)

        is HomeUiEvent.OpenBiometricEnrollment -> startBiometricEnrollment(event.type)

        is HomeUiEvent.ShowBiometricUnavailable -> showBiometricUnavailable(event.type, event.availability)
    }
}

/** A switch flipped on before enrollment snaps back to the saved value. */
private fun HomeFragment.restoreVoiceSwitches() {
    val state = viewModel.uiState.value
    binding.switchVoiceUnlock.isChecked = state.isVoiceUnlockEnabled
    binding.switchVoiceLock.isChecked = state.isVoiceLockEnabled
}
