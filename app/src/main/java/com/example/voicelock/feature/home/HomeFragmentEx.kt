package com.example.voicelock.feature.home

import com.example.voicelock.data.model.home.HomeUiEvent
import com.example.voicelock.data.model.home.HomeUiState
import com.example.voicelock.util.displayToast
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.setPreventDoubleClickScaleView

fun HomeFragment.initView() {
    // Basic setup if any
}

fun HomeFragment.settingEvent() {
    binding.btnLockNow.setPreventDoubleClickScaleView {
        viewModel.onLockNowClicked()
    }

    binding.switchVoiceUnlock.setOnCheckedChangeListener { _, isChecked ->
        viewModel.toggleVoiceUnlock(isChecked)
    }

    binding.switchVoiceLock.setOnCheckedChangeListener { _, isChecked ->
        viewModel.toggleVoiceLock(isChecked)
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
}

fun HomeFragment.updateUiState(state: HomeUiState) {
    binding.tvGreeting.text = "Chào bạn, ${state.userName}"
    
    val lockNowStatusText = if (state.isProtectionEnabled) "BẢO VỆ ĐANG BẬT" else "BẢO VỆ ĐANG TẮT"
    binding.tvBadgeText.text = lockNowStatusText

    binding.tvVoiceInfo.text = "Đã lưu ${state.voiceSamplesCount} mẫu giọng • PIN dự phòng đã ${if (state.isBackupPinEnabled) "bật" else "tắt"}"

    if (binding.switchVoiceUnlock.isChecked != state.isVoiceUnlockEnabled) {
        binding.switchVoiceUnlock.isChecked = state.isVoiceUnlockEnabled
    }

    if (binding.switchVoiceLock.isChecked != state.isVoiceLockEnabled) {
        binding.switchVoiceLock.isChecked = state.isVoiceLockEnabled
    }

    binding.tvPinSub.text = state.pinStatusText
    binding.tvPatternAction.text = state.patternStatusText
    binding.tvFingerprintAction.text = state.fingerprintStatusText
}

fun HomeFragment.handleUiEvent(event: HomeUiEvent) {
    when (event) {
        is HomeUiEvent.LockNow -> {
            displayToast("Đang tiến hành khóa ngay...")
        }

        is HomeUiEvent.ShowToast -> {
            displayToast(event.message)
        }

        is HomeUiEvent.NavigateToPinSetup -> {
            displayToast("Mở cài đặt Mã PIN")
        }

        is HomeUiEvent.NavigateToPatternSetup -> {
            displayToast("Mở cài đặt Hình vẽ")
        }

        is HomeUiEvent.NavigateToFingerprintSetup -> {
            displayToast("Mở cài đặt Vân tay")
        }

        is HomeUiEvent.NavigateToSecurityQuestions -> {
            displayToast("Mở cài đặt Câu hỏi bảo mật")
        }
    }
}
