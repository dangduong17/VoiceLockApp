package com.example.voicelock.feature.settings

import android.os.Bundle
import android.view.View
import com.example.voicelock.R
import com.example.voicelock.data.model.biometric.BiometricAvailability
import com.example.voicelock.data.model.biometric.BiometricSettingsDtoModel
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.biometric.isActive
import com.example.voicelock.data.model.face.FaceSettingsDtoModel
import com.example.voicelock.data.model.face.isFaceUnlockActive
import com.example.voicelock.data.model.settings.SettingsLockInfo
import com.example.voicelock.data.model.settings.SettingsUiEvent
import com.example.voicelock.data.model.settings.SettingsVoiceInfo
import com.example.voicelock.feature.biometric.confirmBiometric
import com.example.voicelock.feature.biometric.getBiometricSubText
import com.example.voicelock.feature.biometric.showBiometricUnavailable
import com.example.voicelock.feature.biometric.startBiometricEnrollment
import com.example.voicelock.feature.biometric.testBiometric
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.offerVoiceLockTile
import com.example.voicelock.util.MessageType
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.showMessage

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

    binding.rowVoiceLock.setPreventDoubleClick {
        viewModel.onVoiceLockRowClicked()
    }

    binding.rowVoiceUnlock.setPreventDoubleClick {
        viewModel.onVoiceUnlockRowClicked()
    }

    binding.switchFingerprint.setOnCheckedChangeListener { _, isChecked ->
        viewModel.toggleBiometric(type = BiometricType.FINGERPRINT, isEnabled = isChecked)
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

fun SettingsFragment.updateVoice(info: SettingsVoiceInfo) {
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

    binding.tvVoiceUnlockSub.text =
        if (info.isVoiceEnrolled) {
            getString(R.string.voice_unlock_settings_sub_enrolled, info.voiceSamplesCount)
        } else {
            getString(R.string.voice_not_enrolled_info)
        }
}

fun SettingsFragment.updateLockMethods(info: SettingsLockInfo) {
    binding.tvPinSub.text =
        if (info.isPinSet) getString(R.string.pin_status_set, info.pinLength) else getString(R.string.pin_not_set_status)
    binding.tvPatternSub.setText(if (info.isPatternSet) R.string.pattern_set_status else R.string.pattern_not_set_status)
    binding.tvQuestionSub.setText(if (info.isSecurityQuestionSet) R.string.security_question_sub else R.string.security_question_sub_not_set)
}

fun SettingsFragment.updateFingerprint(biometric: BiometricSettingsDtoModel?) {
    if (biometric == null) return
    // F4: a device without a fingerprint sensor never sees the option.
    val hasSensor = biometric.fingerprintAvailability != BiometricAvailability.NO_HARDWARE
    binding.btnFingerprint.visibility = if (hasSensor) View.VISIBLE else View.GONE
    binding.dividerFingerprint.visibility = if (hasSensor) View.VISIBLE else View.GONE
    val isFingerprintActive = biometric.isActive(BiometricType.FINGERPRINT)
    if (binding.switchFingerprint.isChecked != isFingerprintActive) binding.switchFingerprint.isChecked = isFingerprintActive
    binding.tvFingerprintSub.text = getBiometricSubText(availability = biometric.fingerprintAvailability, isActive = isFingerprintActive)
}

fun SettingsFragment.updateFace(face: FaceSettingsDtoModel) {
    if (binding.switchFaceUnlock.isChecked != face.isFaceUnlockActive()) binding.switchFaceUnlock.isChecked = face.isFaceUnlockActive()
    binding.tvFaceUnlockSub.text =
        when {
            !face.isEnrolled -> getString(R.string.face_unlock_sub_not_enrolled)
            face.isFaceUnlockActive() -> getString(R.string.face_unlock_sub_enrolled, face.sampleCount)
            else -> getString(R.string.face_unlock_sub_disabled, face.sampleCount)
        }
}

/** A switch flipped on before enrollment snaps back to the saved value. */
private fun SettingsFragment.restoreVoiceSwitches() {
    val state = viewModel.uiState.value
    binding.switchVoiceUnlock.isChecked = state.isVoiceUnlockEnabled
    binding.switchVoiceLock.isChecked = state.isVoiceLockEnabled
}

/** A biometric switch flipped on is only saved after the prompt succeeds, so it snaps back first. */
private fun SettingsFragment.restoreBiometricSwitches() {
    val biometric = viewModel.uiState.value.biometricSettings ?: return
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

        is SettingsUiEvent.ShowLoadError -> {
            showMessage(
                message = event.message ?: getString(R.string.common_error_unexpected),
                type = MessageType.ERROR,
                anchorView = binding.layoutBottomNav,
            )
        }

        is SettingsUiEvent.NavigateToVoiceEnrollment -> {
            restoreVoiceSwitches()
            val bundle = Bundle().apply { putString(BundleKey.KEY_VOICE_MODE, event.mode.name) }
            navigator.navigateTo(R.id.action_settingsFragment_to_enrollmentFragment, bundle)
        }

        is SettingsUiEvent.OfferVoiceLockTile -> offerVoiceLockTile()

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
            confirmBiometric(
                type = event.type,
                anchorView = binding.layoutBottomNav,
                onConfirmed = { viewModel.onBiometricConfirmed(event.type) },
            )
        }

        is SettingsUiEvent.TestBiometric -> testBiometric(type = event.type, anchorView = binding.layoutBottomNav)

        is SettingsUiEvent.OpenBiometricEnrollment -> {
            restoreBiometricSwitches()
            startBiometricEnrollment(type = event.type, anchorView = binding.layoutBottomNav)
        }

        is SettingsUiEvent.ShowBiometricUnavailable -> {
            restoreBiometricSwitches()
            showBiometricUnavailable(
                type = event.type,
                availability = event.availability,
                anchorView = binding.layoutBottomNav,
            )
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

        is SettingsUiEvent.NavigateToSecurityQuestion -> {
            val bundle = Bundle().apply { putString(BundleKey.KEY_LOCK_MODE, event.mode.name) }
            navigator.navigateTo(R.id.action_settingsFragment_to_securityQuestionFragment, bundle)
        }

        is SettingsUiEvent.NavigateToThemeLanguage -> {
            showMessage(message = getString(R.string.coming_soon_theme_language), anchorView = binding.layoutBottomNav)
        }

        is SettingsUiEvent.NavigateToHelpFeedback -> {
            showMessage(message = getString(R.string.coming_soon_help_feedback), anchorView = binding.layoutBottomNav)
        }

        is SettingsUiEvent.NavigateToAbout -> {
            showMessage(message = getString(R.string.coming_soon_about), anchorView = binding.layoutBottomNav)
        }

        is SettingsUiEvent.NavigateToAppLock -> {
            navigator.navigateTo(R.id.action_settingsFragment_to_appLockFragment)
        }
    }
}
