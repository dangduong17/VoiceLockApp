package com.example.voicelock.feature.settings

import com.example.voicelock.data.model.settings.SettingsUiEvent
import com.example.voicelock.data.model.settings.SettingsUiState
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
}

fun SettingsFragment.updateUiState(state: SettingsUiState) {
    if (binding.switchVoiceUnlock.isChecked != state.isVoiceUnlockEnabled) {
        binding.switchVoiceUnlock.isChecked = state.isVoiceUnlockEnabled
    }

    if (binding.switchVoiceLock.isChecked != state.isVoiceLockEnabled) {
        binding.switchVoiceLock.isChecked = state.isVoiceLockEnabled
    }
}

fun SettingsFragment.handleUiEvent(event: SettingsUiEvent) {
    when (event) {
        is SettingsUiEvent.NavigateToHome -> {
            navigator.navigateUp()
        }

        is SettingsUiEvent.ShowToast -> {
            displayToast(event.message)
        }

        is SettingsUiEvent.NavigateToVoiceCommands -> {
            displayToast("Mở Lệnh và mẫu giọng nói")
        }

        is SettingsUiEvent.NavigateToPermissions -> {
            displayToast("Mở Quyền ứng dụng")
        }

        is SettingsUiEvent.NavigateToPinSetup -> {
            displayToast("Mở Mã PIN")
        }

        is SettingsUiEvent.NavigateToPatternSetup -> {
            displayToast("Mở Hình vẽ")
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
    }
}
