package com.example.voicelock.feature.patternlock

import com.example.voicelock.R
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.patternlock.PatternLockStage
import com.example.voicelock.data.model.patternlock.PatternLockUiEvent
import com.example.voicelock.data.model.patternlock.PatternLockHeaderInfo
import com.example.voicelock.util.MessageType
import com.example.voicelock.util.showMessage
import com.example.voicelock.util.updateLockoutMessage
import com.example.voicelock.widget.PatternLockView

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

fun PatternLockFragment.updateHeader(info: PatternLockHeaderInfo) {
    val (title, subtitle) =
        when (info.stage) {
            PatternLockStage.VERIFY ->
                if (info.mode == PatternLockMode.UNLOCK) {
                    getString(R.string.pattern_title_unlock) to getString(R.string.pattern_subtitle_unlock)
                } else {
                    getString(R.string.pattern_title_verify) to getString(R.string.pattern_subtitle_verify)
                }
            PatternLockStage.ENTER -> getString(R.string.pattern_title_enter) to getString(R.string.pattern_subtitle_enter)
            PatternLockStage.CONFIRM -> getString(R.string.pattern_title_confirm) to getString(R.string.pattern_subtitle_confirm)
        }
    binding.tvPatternScreenTitle.text = title
    binding.tvPatternScreenSubtitle.text = subtitle
}

fun PatternLockFragment.updateLockout(remainingSeconds: Int) {
    binding.tvPatternLockoutMessage.updateLockoutMessage(remainingSeconds)
}

fun PatternLockFragment.updateInputEnabled(isEnabled: Boolean) {
    binding.patternLockView.setInputEnabled(isEnabled)
}

fun PatternLockFragment.handleUiEvent(event: PatternLockUiEvent) {
    when (event) {
        is PatternLockUiEvent.VerifyFailed -> {
            showMessage(message = getString(R.string.pattern_wrong_remaining, event.remainingAttempts), type = MessageType.ERROR)
            binding.patternLockView.showError()
        }

        is PatternLockUiEvent.ConfirmMismatch -> {
            showMessage(message = getString(R.string.pattern_mismatch), type = MessageType.ERROR)
            binding.patternLockView.showError()
        }

        is PatternLockUiEvent.TooShort -> {
            showMessage(message = getString(R.string.pattern_too_short), type = MessageType.ERROR)
            binding.patternLockView.clear()
        }

        is PatternLockUiEvent.Success -> {
            if (event.mode == PatternLockMode.UNLOCK) {
                // The unlocked app coming to the front is the feedback; the overlay just closes.
                requireActivity().finish()
            } else {
                showMessage(message = getString(R.string.pattern_set_success), type = MessageType.SUCCESS)
                navigator.navigateUp()
            }
        }
    }
}
