package com.example.voicelock.feature.settings

import android.os.Bundle
import com.example.voicelock.R
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
        viewModel.onFingerprintClicked()
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

    binding.tvPinSub.text = state.pinStatusText
    binding.tvPatternSub.text = state.patternStatusText
}

/** A switch flipped on before enrollment snaps back to the saved value. */
private fun SettingsFragment.restoreVoiceSwitches() {
    val state = viewModel.uiState.value
    binding.switchVoiceUnlock.isChecked = state.isVoiceUnlockEnabled
    binding.switchVoiceLock.isChecked = state.isVoiceLockEnabled
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
            navigator.navigateTo(R.id.action_settingsFragment_to_enrollmentActivity)
        }

        is SettingsUiEvent.NavigateToVoiceTest -> {
            navigator.navigateTo(R.id.action_settingsFragment_to_twoStageAuthActivity)
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

        is SettingsUiEvent.NavigateToFingerprintSetup -> {
            displayToast("Mở Vân tay")
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
