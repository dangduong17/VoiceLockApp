package com.example.voicelock.feature.pinlock

import android.view.View
import com.example.voicelock.R
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.pinlock.PinLockStage
import com.example.voicelock.data.model.pinlock.PinLockUiEvent
import com.example.voicelock.data.model.pinlock.PinLockUiState
import com.example.voicelock.data.model.pinlock.canToggleLength
import com.example.voicelock.data.model.pinlock.isKeypadEnabled
import com.example.voicelock.util.displayToast
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.shakeError

fun PinLockFragment.initView() {
    // Basic setup if any
}

fun PinLockFragment.backEvent() {
    navigator.navigateUp()
}

fun PinLockFragment.settingEvent() {
    val digitButtons =
        listOf(
            binding.btn0 to 0,
            binding.btn1 to 1,
            binding.btn2 to 2,
            binding.btn3 to 3,
            binding.btn4 to 4,
            binding.btn5 to 5,
            binding.btn6 to 6,
            binding.btn7 to 7,
            binding.btn8 to 8,
            binding.btn9 to 9,
        )
    digitButtons.forEach { (button, digit) ->
        button.setPreventDoubleClick { viewModel.onDigitPressed(digit) }
    }

    binding.btnDelete.setPreventDoubleClick {
        viewModel.onDeletePressed()
    }

    binding.tvToggleLength.setPreventDoubleClick {
        viewModel.onToggleLength()
    }
}

fun PinLockFragment.updateUiState(state: PinLockUiState) {
    val (titleRes, subtitleText) =
        when (state.stage) {
            PinLockStage.VERIFY ->
                if (state.mode == PinLockMode.UNLOCK) {
                    getString(R.string.pin_title_unlock) to getString(R.string.pin_subtitle_unlock)
                } else {
                    getString(R.string.pin_title_verify) to getString(R.string.pin_subtitle_verify)
                }
            PinLockStage.ENTER -> getString(R.string.pin_title_enter) to getString(R.string.pin_subtitle_enter, state.pinLength)
            PinLockStage.CONFIRM -> getString(R.string.pin_title_confirm) to getString(R.string.pin_subtitle_confirm)
        }
    binding.tvPinScreenTitle.text = titleRes
    binding.tvPinScreenSubtitle.text = subtitleText

    val canShowToggle = state.canToggleLength() && state.enteredDigits == 0
    binding.tvToggleLength.visibility = if (canShowToggle) View.VISIBLE else View.INVISIBLE
    binding.tvToggleLength.text =
        if (state.pinLength == 4) getString(R.string.pin_toggle_use_6) else getString(R.string.pin_toggle_use_4)

    updateDots(state)

    binding.tvLockoutMessage.visibility = if (state.isLocked) View.VISIBLE else View.GONE
    if (state.isLocked) {
        binding.tvLockoutMessage.text = getString(R.string.lockout_message, state.lockoutRemainingSeconds)
    }

    val keypadButtons =
        listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9, binding.btnDelete,
        )
    keypadButtons.forEach { it.isEnabled = state.isKeypadEnabled() }
}

private fun PinLockFragment.updateDots(state: PinLockUiState) {
    val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4, binding.dot5, binding.dot6)
    dots.forEachIndexed { index, dot ->
        when {
            index >= state.pinLength -> dot.visibility = View.GONE
            else -> {
                dot.visibility = View.VISIBLE
                dot.setBackgroundResource(
                    when {
                        state.isError -> R.drawable.dot_error
                        index < state.enteredDigits -> R.drawable.dot_filled
                        else -> R.drawable.dot_empty
                    },
                )
            }
        }
    }
}

fun PinLockFragment.handleUiEvent(event: PinLockUiEvent) {
    when (event) {
        is PinLockUiEvent.VerifyFailed -> {
            displayToast(getString(R.string.pin_wrong_remaining, event.remainingAttempts))
            binding.layoutDots.shakeError()
        }

        is PinLockUiEvent.ConfirmMismatch -> {
            displayToast(getString(R.string.pin_mismatch))
            binding.layoutDots.shakeError()
        }

        is PinLockUiEvent.Success -> {
            if (event.mode == PinLockMode.UNLOCK) {
                displayToast(getString(R.string.pin_unlock_success))
                requireActivity().finish()
            } else {
                displayToast(getString(R.string.pin_set_success))
                navigator.navigateUp()
            }
        }
    }
}
