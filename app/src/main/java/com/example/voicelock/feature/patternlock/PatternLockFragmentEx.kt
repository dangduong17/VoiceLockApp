package com.example.voicelock.feature.patternlock

import android.view.View
import com.example.voicelock.R
import com.example.voicelock.data.model.patternlock.PatternLockStage
import com.example.voicelock.data.model.patternlock.PatternLockUiEvent
import com.example.voicelock.data.model.patternlock.PatternLockUiState
import com.example.voicelock.data.model.patternlock.isInputEnabled
import com.example.voicelock.util.displayToast
import com.example.voicelock.widget.PatternLockView

fun PatternLockFragment.initView() {
    // Basic setup if any
}

fun PatternLockFragment.backEvent() {
    navigator.navigateUp()
}

fun PatternLockFragment.settingEvent() {
    binding.patternLockView.setListener(
        object : PatternLockView.PatternListener {
            override fun onPatternComplete(cells: List<Int>) {
                viewModel.onPatternDrawn(cells)
            }

            override fun onPatternTooShort() {
                viewModel.onPatternTooShort()
            }
        },
    )
}

fun PatternLockFragment.updateUiState(state: PatternLockUiState) {
    val (title, subtitle) =
        when (state.stage) {
            PatternLockStage.VERIFY -> getString(R.string.pattern_title_verify) to getString(R.string.pattern_subtitle_verify)
            PatternLockStage.ENTER -> getString(R.string.pattern_title_enter) to getString(R.string.pattern_subtitle_enter)
            PatternLockStage.CONFIRM -> getString(R.string.pattern_title_confirm) to getString(R.string.pattern_subtitle_confirm)
        }
    binding.tvPatternScreenTitle.text = title
    binding.tvPatternScreenSubtitle.text = subtitle

    binding.tvPatternLockoutMessage.visibility = if (state.isLocked) View.VISIBLE else View.GONE
    if (state.isLocked) {
        binding.tvPatternLockoutMessage.text = getString(R.string.lockout_message, state.lockoutRemainingSeconds)
    }

    binding.patternLockView.setInputEnabled(state.isInputEnabled())
}

fun PatternLockFragment.handleUiEvent(event: PatternLockUiEvent) {
    when (event) {
        is PatternLockUiEvent.VerifyFailed -> {
            displayToast(getString(R.string.pattern_wrong_remaining, event.remainingAttempts))
            binding.patternLockView.showError()
        }

        is PatternLockUiEvent.ConfirmMismatch -> {
            displayToast(getString(R.string.pattern_mismatch))
            binding.patternLockView.showError()
        }

        is PatternLockUiEvent.TooShort -> {
            displayToast(getString(R.string.pattern_too_short))
            binding.patternLockView.clear()
        }

        is PatternLockUiEvent.Success -> {
            displayToast(getString(R.string.pattern_set_success))
            navigator.navigateUp()
        }
    }
}
