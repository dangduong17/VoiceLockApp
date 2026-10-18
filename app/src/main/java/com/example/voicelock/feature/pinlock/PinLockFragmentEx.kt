package com.example.voicelock.feature.pinlock

import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.example.voicelock.R
import com.example.voicelock.data.model.pinlock.PinLengthToggleInfo
import com.example.voicelock.data.model.pinlock.PinLockDotsInfo
import com.example.voicelock.data.model.pinlock.PinLockHeaderInfo
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.pinlock.PinLockStage
import com.example.voicelock.data.model.pinlock.PinLockUiEvent
import com.example.voicelock.data.model.securityquestion.SecurityQuestionMode
import com.example.voicelock.util.MessageType
import com.example.voicelock.util.navigateToSecurityQuestion
import com.example.voicelock.util.setPreventDoubleClick
import com.example.voicelock.util.shakeError
import com.example.voicelock.util.showMessage
import com.example.voicelock.util.updateLockoutMessage

fun PinLockFragment.backEvent() {
    navigator.navigateUp()
}

fun PinLockFragment.settingEvent() {
    getDigitButtons().forEachIndexed { digit, button ->
        button.setPreventDoubleClick { viewModel.onDigitPressed(digit) }
    }

    binding.btnDelete.setPreventDoubleClick {
        viewModel.onDeletePressed()
    }

    binding.tvToggleLength.setPreventDoubleClick {
        viewModel.onToggleLength()
    }

    binding.tvForgotPin.setPreventDoubleClick { forgotPinEvent() }
}

/** Keypad buttons in digit order, so a button's index is the digit it types. */
private fun PinLockFragment.getDigitButtons() =
    with(binding) { listOf(btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9) }

/** Answer the security question instead of the forgotten PIN, then set a new one. */
fun PinLockFragment.forgotPinEvent() {
    navigator.navigateToSecurityQuestion(
        actionId = R.id.action_pinLockFragment_to_securityQuestionFragment,
        mode = SecurityQuestionMode.RESET_PIN,
    )
}

fun PinLockFragment.updateHeader(info: PinLockHeaderInfo) {
    val (title, subtitle) =
        when (info.stage) {
            PinLockStage.VERIFY ->
                if (info.mode == PinLockMode.UNLOCK) {
                    getString(R.string.pin_title_unlock) to getString(R.string.pin_subtitle_unlock)
                } else {
                    getString(R.string.pin_title_verify) to getString(R.string.pin_subtitle_verify)
                }
            PinLockStage.ENTER -> getString(R.string.pin_title_enter) to getString(R.string.pin_subtitle_enter, info.pinLength)
            PinLockStage.CONFIRM -> getString(R.string.pin_title_confirm) to getString(R.string.pin_subtitle_confirm)
        }
    binding.tvPinScreenTitle.text = title
    binding.tvPinScreenSubtitle.text = subtitle
}

fun PinLockFragment.updateLengthToggle(info: PinLengthToggleInfo) {
    binding.tvToggleLength.isInvisible = !info.isVisible
    binding.tvToggleLength.text = getString(R.string.pin_toggle_use_length, info.toggledLength)
}

fun PinLockFragment.updateForgotPin(isVisible: Boolean) {
    binding.tvForgotPin.isVisible = isVisible
}

fun PinLockFragment.updateLockout(remainingSeconds: Int) {
    binding.tvLockoutMessage.updateLockoutMessage(remainingSeconds)
}

fun PinLockFragment.updateKeypadEnabled(isEnabled: Boolean) {
    (getDigitButtons() + binding.btnDelete).forEach { it.isEnabled = isEnabled }
}

fun PinLockFragment.updateDots(info: PinLockDotsInfo) {
    val dots = with(binding) { listOf(dot1, dot2, dot3, dot4, dot5, dot6) }
    dots.forEachIndexed { index, dot ->
        dot.isVisible = index < info.pinLength
        dot.setBackgroundResource(
            when {
                info.isError -> R.drawable.dot_error
                index < info.enteredDigits -> R.drawable.dot_filled
                else -> R.drawable.dot_empty
            },
        )
    }
}

fun PinLockFragment.handleUiEvent(event: PinLockUiEvent) {
    when (event) {
        is PinLockUiEvent.VerifyFailed -> {
            showMessage(message = getString(R.string.pin_wrong_remaining, event.remainingAttempts), type = MessageType.ERROR)
            binding.layoutDots.shakeError()
        }

        is PinLockUiEvent.ConfirmMismatch -> {
            showMessage(message = getString(R.string.pin_mismatch), type = MessageType.ERROR)
            binding.layoutDots.shakeError()
        }

        is PinLockUiEvent.Success -> {
            if (event.mode == PinLockMode.UNLOCK) {
                // The unlocked app coming to the front is the feedback; the overlay just closes.
                requireActivity().finish()
            } else {
                showMessage(message = getString(R.string.pin_set_success), type = MessageType.SUCCESS)
                navigator.navigateUp()
            }
        }
    }
}
