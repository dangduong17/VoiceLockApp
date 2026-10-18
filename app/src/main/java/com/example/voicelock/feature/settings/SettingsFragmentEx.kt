package com.example.voicelock.feature.settings

import androidx.core.view.isVisible
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
import com.example.voicelock.data.model.voice.VoiceVerificationMode
import com.example.voicelock.feature.biometric.getBiometricSubText
import com.example.voicelock.feature.protection.LockMethodRoutes
import com.example.voicelock.feature.protection.handleLockMethodEvent
import com.example.voicelock.service.FloatingLockService
import com.example.voicelock.util.MessageType
import com.example.voicelock.util.hasOverlayPermission
import com.example.voicelock.util.navigateToVoiceEnrollment
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.showMessage
import com.example.voicelock.util.syncChecked

fun SettingsFragment.settingEvent() {
    binding.tabProtection.setPreventDoubleClick { navigator.navigateUp() }

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

    binding.switchFingerprint.setOnCheckedChangeListener { _, isChecked ->
        viewModel.toggleBiometric(type = BiometricType.FINGERPRINT, isEnabled = isChecked)
    }

    binding.switchFaceUnlock.setOnCheckedChangeListener { _, isChecked ->
        viewModel.toggleFaceUnlock(isChecked)
    }

    binding.rowFaceUnlock.setPreventDoubleClick {
        viewModel.onFaceUnlockRowClicked()
    }

    // "Voice commands & samples" = (re-)record the voice password.
    binding.btnVoiceCommands.setPreventDoubleClick { navigateToVoiceEnrollment(VoiceVerificationMode.UNLOCK) }

    binding.btnPermissions.setPreventDoubleClick { navigateToPermissions() }

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

    binding.btnThemeLanguage.setPreventDoubleClick { showComingSoon(R.string.coming_soon_theme_language) }

    binding.btnHelp.setPreventDoubleClick { showComingSoon(R.string.coming_soon_help_feedback) }

    binding.btnAbout.setPreventDoubleClick { showComingSoon(R.string.coming_soon_about) }

    binding.btnAppLock.setPreventDoubleClick {
        navigator.navigateTo(R.id.action_settingsFragment_to_appLockFragment)
    }

    binding.switchFloatingLock.setOnCheckedChangeListener { _, isChecked ->
        viewModel.toggleFloatingLock(isEnabled = isChecked, canDrawOverlays = requireContext().hasOverlayPermission())
    }
}

private fun SettingsFragment.showComingSoon(messageRes: Int) {
    showMessage(message = getString(messageRes), anchorView = binding.layoutBottomNav)
}

private fun SettingsFragment.navigateToPermissions() {
    navigator.navigateTo(R.id.action_settingsFragment_to_permissionsFragment)
}

private fun SettingsFragment.navigateToVoiceEnrollment(mode: VoiceVerificationMode) {
    navigator.navigateToVoiceEnrollment(actionId = R.id.action_settingsFragment_to_enrollmentFragment, mode = mode)
}

fun SettingsFragment.updateFloatingLock(isEnabled: Boolean) {
    binding.switchFloatingLock.syncChecked(isEnabled)
}

fun SettingsFragment.updateVoice(info: SettingsVoiceInfo) {
    binding.switchVoiceUnlock.syncChecked(info.isVoiceUnlockEnabled)
    binding.switchVoiceLock.syncChecked(info.isVoiceLockEnabled)

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
    binding.btnFingerprint.isVisible = hasSensor
    binding.dividerFingerprint.isVisible = hasSensor
    val isFingerprintActive = biometric.isActive(BiometricType.FINGERPRINT)
    binding.switchFingerprint.syncChecked(isFingerprintActive)
    binding.tvFingerprintSub.text = getBiometricSubText(availability = biometric.fingerprintAvailability, isActive = isFingerprintActive)
}

fun SettingsFragment.updateFace(face: FaceSettingsDtoModel) {
    binding.switchFaceUnlock.syncChecked(face.isFaceUnlockActive())
    binding.tvFaceUnlockSub.text =
        when {
            !face.isEnrolled -> getString(R.string.face_unlock_sub_not_enrolled)
            face.isFaceUnlockActive() -> getString(R.string.face_unlock_sub_enrolled, face.sampleCount)
            else -> getString(R.string.face_unlock_sub_disabled, face.sampleCount)
        }
}

/**
 * A switch flipped on is only saved once its setup or prompt succeeds, so every switch snaps back
 * to the saved value before the lock-method effect runs.
 */
private fun SettingsFragment.restoreSwitches() {
    val state = viewModel.uiState.value
    binding.switchVoiceUnlock.syncChecked(state.isVoiceUnlockEnabled)
    binding.switchVoiceLock.syncChecked(state.isVoiceLockEnabled)
    binding.switchFaceUnlock.syncChecked(state.faceSettings.isFaceUnlockActive())
    state.biometricSettings?.let { binding.switchFingerprint.syncChecked(it.isActive(BiometricType.FINGERPRINT)) }
}

fun SettingsFragment.handleUiEvent(event: SettingsUiEvent) {
    when (event) {
        is SettingsUiEvent.ShowLoadError -> {
            showMessage(
                message = getString(R.string.common_error_unexpected),
                type = MessageType.ERROR,
                anchorView = binding.layoutBottomNav,
            )
        }

        is SettingsUiEvent.NavigateToPermissions -> navigateToPermissions()

        is SettingsUiEvent.LockMethod -> {
            restoreSwitches()
            handleLockMethodEvent(
                event = event.event,
                routes = SETTINGS_LOCK_METHOD_ROUTES,
                anchorView = binding.layoutBottomNav,
                onBiometricConfirmed = { type -> viewModel.onBiometricConfirmed(type) },
            )
        }

        is SettingsUiEvent.SyncFloatingLock ->
            if (event.isEnabled) FloatingLockService.start(requireContext()) else FloatingLockService.stop(requireContext())

        is SettingsUiEvent.ShowFloatingLockNeedsOverlay -> {
            binding.switchFloatingLock.isChecked = false
            showMessage(message = getString(R.string.floating_lock_needs_overlay), anchorView = binding.layoutBottomNav)
        }
    }
}

private val SETTINGS_LOCK_METHOD_ROUTES =
    LockMethodRoutes(
        pinLockActionId = R.id.action_settingsFragment_to_pinLockFragment,
        patternLockActionId = R.id.action_settingsFragment_to_patternLockFragment,
        securityQuestionActionId = R.id.action_settingsFragment_to_securityQuestionFragment,
        voiceEnrollmentActionId = R.id.action_settingsFragment_to_enrollmentFragment,
        voiceTestActionId = R.id.action_settingsFragment_to_twoStageAuthFragment,
        faceEnrollmentActionId = R.id.action_settingsFragment_to_faceEnrollmentFragment,
        faceTestActionId = R.id.action_settingsFragment_to_faceAuthFragment,
    )
