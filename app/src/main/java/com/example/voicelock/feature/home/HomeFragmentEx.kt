package com.example.voicelock.feature.home

import android.os.Bundle
import com.example.voicelock.R
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

    binding.btnAppLock.setPreventDoubleClick {
        viewModel.onAppLockClicked()
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

        is HomeUiEvent.NavigateToFingerprintSetup -> {
            displayToast("Mở cài đặt Vân tay")
        }

        is HomeUiEvent.NavigateToSecurityQuestions -> {
            displayToast("Mở cài đặt Câu hỏi bảo mật")
        }

        is HomeUiEvent.NavigateToAppLock -> {
            navigator.navigateTo(R.id.action_homeFragment_to_appLockFragment)
        }
    }
}
