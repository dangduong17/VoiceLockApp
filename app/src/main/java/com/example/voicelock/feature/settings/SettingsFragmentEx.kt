package com.example.voicelock.feature.settings

import android.os.Bundle
import com.example.voicelock.R
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.biometric.isActive
import com.example.voicelock.data.model.face.isFaceUnlockActive
import com.example.voicelock.feature.biometric.confirmBiometric
import com.example.voicelock.feature.biometric.getBiometricSubText
import com.example.voicelock.feature.biometric.showBiometricUnavailable
import com.example.voicelock.feature.biometric.startBiometricEnrollment
import com.example.voicelock.feature.biometric.testBiometric
import com.example.voicelock.data.model.settings.SettingsUiEvent
import com.example.voicelock.data.model.settings.SettingsUiState
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.displayToast
import com.example.voicelock.util.setPreventDoubleClick

fun SettingsFragment.initView() {
    // Basic setup if any
}

fun SettingsFragment.settingEvent() {
    binding.tabProtection.setPreventDoubleClick {
        viewModel.onHomeTabClicked()
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

    binding.switchFingerprint.setOnCheckedChangeListener { _, isChecked ->
        viewModel.toggleBiometric(BiometricType.FINGERPRINT, isChecked)
    }

    binding.switchFaceUnlock.setOnCheckedChangeListener { _, isChecked ->
        viewModel.toggleFaceUnlock(isChecked)
    }

    binding.rowFaceUnlock.setPreventDoubleClick {
        viewModel.onFaceUnlockRowClicked()
    }

    binding.btnVoiceCommands.setPreventDoubleClick {
        viewModel.onVoiceCommandsClicked()
    }

    binding.btnPermissions.setPreventDoubleClick {
        viewModel.onPermissionsClicked()
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

    binding.btnQuestions.setPreventDoubleClick {
        viewModel.onQuestionsClicked()
    }

    binding.btnThemeLanguage.setPreventDoubleClick {
        viewModel.onThemeLanguageClicked()
    }

    binding.btnHelp.setPreventDoubleClick {
        viewModel.onHelpFeedbackClicked()
    }

    binding.btnAbout.setPreventDoubleClick {
        viewModel.onAboutClicked()
    }

    binding.btnAppLock.setPreventDoubleClick {
        viewModel.onAppLockClicked()
    }
}

fun SettingsFragment.updateUiState(state: SettingsUiState) {
    if (binding.switchVoiceUnlock.isChecked != state.isVoiceUnlockEnabled) {
        binding.switchVoiceUnlock.isChecked = state.isVoiceUnlockEnabled
    }

    if (binding.switchVoiceLock.isChecked != state.isVoiceLockEnabled) {
        binding.switchVoiceLock.isChecked = state.isVoiceLockEnabled
    }

    binding.tvVoiceUnlockSub.text =
        if (state.isVoiceEnrolled) {
            getString(R.string.voice_unlock_settings_sub_enrolled, state.voiceSamplesCount)
        } else {
            getString(R.string.voice_not_enrolled_info)
        }

    val biometric = state.biometricSettings
    val isFingerprintActive = biometric.isActive(BiometricType.FINGERPRINT)
    if (binding.switchFingerprint.isChecked != isFingerprintActive) binding.switchFingerprint.isChecked = isFingerprintActive
    binding.tvFingerprintSub.text = getBiometricSubText(biometric.fingerprintAvailability, isFingerprintActive)

    val face = state.faceSettings
    if (binding.switchFaceUnlock.isChecked != face.isFaceUnlockActive()) binding.switchFaceUnlock.isChecked = face.isFaceUnlockActive()
    binding.tvFaceUnlockSub.text =
        when {
            !face.isEnrolled -> getString(R.string.face_unlock_sub_not_enrolled)
            face.isFaceUnlockActive() -> getString(R.string.face_unlock_sub_enrolled, face.sampleCount)
            else -> getString(R.string.face_unlock_sub_disabled, face.sampleCount)
        }

    binding.tvPinSub.text = state.pinStatusText
    binding.tvPatternSub.text = state.patternStatusText
}

/** A switch flipped on before enrollment snaps back to the saved value. */
private fun SettingsFragment.restoreVoiceSwitches() {
    val state = viewModel.uiState.value
    binding.switchVoiceUnlock.isChecked = state.isVoiceUnlockEnabled
    binding.switchVoiceLock.isChecked = state.isVoiceLockEnabled
}

/** A biometric switch flipped on is only saved after the prompt succeeds, so it snaps back first. */
private fun SettingsFragment.restoreBiometricSwitches() {
    val biometric = viewModel.uiState.value.biometricSettings
    binding.switchFingerprint.isChecked = biometric.isActive(BiometricType.FINGERPRINT)
}

/** A face switch flipped on before enrolling snaps back; enrollment turns it on. */
private fun SettingsFragment.restoreFaceSwitch() {
    binding.switchFaceUnlock.isChecked = viewModel.uiState.value.faceSettings.isFaceUnlockActive()
}

fun SettingsFragment.handleUiEvent(event: SettingsUiEvent) {
    when (event) {
        is SettingsUiEvent.NavigateToHome -> {
            navigator.navigateUp()
        }

        is SettingsUiEvent.ShowToast -> {
            displayToast(event.message)
        }

        is SettingsUiEvent.NavigateToVoiceEnrollment -> {
            restoreVoiceSwitches()
            navigator.navigateTo(R.id.action_settingsFragment_to_enrollmentFragment)
        }

        is SettingsUiEvent.NavigateToVoiceTest -> {
            navigator.navigateTo(R.id.action_settingsFragment_to_twoStageAuthFragment)
        }

        is SettingsUiEvent.NavigateToFaceEnrollment -> {
            restoreFaceSwitch()
            navigator.navigateTo(R.id.action_settingsFragment_to_faceEnrollmentFragment)
        }

        is SettingsUiEvent.NavigateToFaceTest -> {
            navigator.navigateTo(R.id.action_settingsFragment_to_faceAuthFragment)
        }

        is SettingsUiEvent.ConfirmBiometric -> {
            restoreBiometricSwitches()
            confirmBiometric(event.type) { viewModel.onBiometricConfirmed(event.type) }
        }

        is SettingsUiEvent.TestBiometric -> testBiometric(event.type)

        is SettingsUiEvent.OpenBiometricEnrollment -> {
            restoreBiometricSwitches()
            startBiometricEnrollment(event.type)
        }

        is SettingsUiEvent.ShowBiometricUnavailable -> {
            restoreBiometricSwitches()
            showBiometricUnavailable(event.type, event.availability)
        }

        is SettingsUiEvent.NavigateToPermissions -> {
            navigator.navigateTo(R.id.action_settingsFragment_to_permissionsFragment)
        }

        is SettingsUiEvent.NavigateToPinLock -> {
            val bundle =
                Bundle().apply {
                    putString(BundleKey.KEY_LOCK_MODE, event.mode.name)
                    putInt(BundleKey.KEY_PIN_LENGTH, event.pinLength)
                }
            navigator.navigateTo(R.id.action_settingsFragment_to_pinLockFragment, bundle)
        }

        is SettingsUiEvent.NavigateToPatternLock -> {
            val bundle = Bundle().apply { putString(BundleKey.KEY_LOCK_MODE, event.mode.name) }
            navigator.navigateTo(R.id.action_settingsFragment_to_patternLockFragment, bundle)
        }

        is SettingsUiEvent.NavigateToSecurityQuestions -> {
            displayToast("Mở Câu hỏi bảo mật")
        }

        is SettingsUiEvent.NavigateToThemeLanguage -> {
            displayToast("Mở Giao diện và ngôn ngữ")
        }

        is SettingsUiEvent.NavigateToHelpFeedback -> {
            displayToast("Mở Trợ giúp và phản hồi")
        }

        is SettingsUiEvent.NavigateToAbout -> {
            displayToast("Mở Về VoiceLock")
        }

        is SettingsUiEvent.NavigateToAppLock -> {
            navigator.navigateTo(R.id.action_settingsFragment_to_appLockFragment)
        }
    }
}
