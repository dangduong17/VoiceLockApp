package com.example.voicelock.feature.securityquestion

import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.voicelock.R
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.securityquestion.SecurityQuestionInputError
import com.example.voicelock.data.model.securityquestion.SecurityQuestionMode
import com.example.voicelock.data.model.securityquestion.SecurityQuestionScreenInfo
import com.example.voicelock.data.model.securityquestion.SecurityQuestionStage
import com.example.voicelock.data.model.securityquestion.SecurityQuestionUiEvent
import com.example.voicelock.util.MessageType
import com.example.voicelock.util.navigateToPinLock
import com.example.voicelock.util.setEnabledWithAlpha
import com.example.voicelock.util.setPreventDoubleClickScaleView
import com.example.voicelock.util.showMessage
import com.example.voicelock.util.updateLockoutMessage

fun SecurityQuestionFragment.initView() {
    val presets = resources.getStringArray(R.array.security_question_presets).toList()
    binding.actvQuestion.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, presets))
    binding.actvQuestion.setText(presets[selectedQuestionIndex], false)
    updateCustomQuestionVisibility()
}

fun SecurityQuestionFragment.backEvent() {
    navigator.navigateUp()
}

fun SecurityQuestionFragment.settingEvent() {
    binding.actvQuestion.setOnItemClickListener { _, _, position, _ -> questionSelectedEvent(position) }
    listOf(binding.etAnswer, binding.etCustomQuestion, binding.etNewAnswer, binding.etConfirmAnswer).forEach { editText ->
        editText.doAfterTextChanged { viewModel.onInputChanged() }
    }
    binding.btnQuestionAction.setPreventDoubleClickScaleView { actionEvent() }
}

fun SecurityQuestionFragment.questionSelectedEvent(position: Int) {
    selectedQuestionIndex = position
    updateCustomQuestionVisibility()
    viewModel.onInputChanged()
}

fun SecurityQuestionFragment.actionEvent() {
    when (viewModel.uiState.value.stage) {
        SecurityQuestionStage.VERIFY -> viewModel.verifyAnswer(binding.etAnswer.text?.toString().orEmpty())
        SecurityQuestionStage.SETUP ->
            viewModel.saveQuestion(
                question = getSelectedQuestion(),
                answer = binding.etNewAnswer.text?.toString().orEmpty(),
                confirmAnswer = binding.etConfirmAnswer.text?.toString().orEmpty(),
            )
        SecurityQuestionStage.NOT_SET -> navigator.navigateUp()
        SecurityQuestionStage.LOADING -> {
            // Busy: the button is disabled
        }
    }
}

private fun SecurityQuestionFragment.isCustomQuestionSelected(): Boolean =
    selectedQuestionIndex == resources.getStringArray(R.array.security_question_presets).lastIndex

private fun SecurityQuestionFragment.getSelectedQuestion(): String =
    if (isCustomQuestionSelected()) {
        binding.etCustomQuestion.text?.toString().orEmpty()
    } else {
        resources.getStringArray(R.array.security_question_presets)[selectedQuestionIndex]
    }

private fun SecurityQuestionFragment.updateCustomQuestionVisibility() {
    binding.tilCustomQuestion.isVisible = isCustomQuestionSelected()
}

fun SecurityQuestionFragment.updateScreen(info: SecurityQuestionScreenInfo) = with(binding) {
    val stage = info.stage
    tvQuestionTitle.setText(
        when (stage) {
            SecurityQuestionStage.SETUP ->
                if (info.mode == SecurityQuestionMode.CHANGE) R.string.security_question_title_change else R.string.security_question_title_setup
            SecurityQuestionStage.VERIFY, SecurityQuestionStage.LOADING, SecurityQuestionStage.NOT_SET ->
                R.string.security_question_title_verify
        },
    )
    tvQuestionSubtitle.setText(
        when (stage) {
            SecurityQuestionStage.SETUP -> R.string.security_question_subtitle_setup
            SecurityQuestionStage.VERIFY ->
                when (info.mode) {
                    SecurityQuestionMode.UNLOCK -> R.string.security_question_subtitle_unlock
                    SecurityQuestionMode.RESET_PIN -> R.string.security_question_subtitle_reset_pin
                    SecurityQuestionMode.SETUP, SecurityQuestionMode.CHANGE -> R.string.security_question_subtitle_change
                }
            SecurityQuestionStage.NOT_SET -> R.string.security_question_subtitle_not_set
            SecurityQuestionStage.LOADING -> R.string.common_loading
        },
    )
    layoutVerify.isVisible = stage == SecurityQuestionStage.VERIFY
    layoutSetup.isVisible = stage == SecurityQuestionStage.SETUP
    tvStoredQuestion.text = info.question
    btnQuestionAction.setText(
        when (stage) {
            SecurityQuestionStage.SETUP -> R.string.security_question_action_save
            SecurityQuestionStage.NOT_SET -> R.string.common_back
            SecurityQuestionStage.VERIFY, SecurityQuestionStage.LOADING -> R.string.security_question_action_verify
        },
    )
    if (stage == SecurityQuestionStage.SETUP) etAnswer.text = null
}

fun SecurityQuestionFragment.updateInputError(error: SecurityQuestionInputError) = with(binding) {
    tilAnswer.error = if (error == SecurityQuestionInputError.ANSWER_EMPTY) getString(R.string.security_question_error_answer_empty) else null
    tilCustomQuestion.error =
        if (error == SecurityQuestionInputError.QUESTION_EMPTY) getString(R.string.security_question_error_question_empty) else null
    tilNewAnswer.error =
        if (error == SecurityQuestionInputError.ANSWER_TOO_SHORT) getString(R.string.security_question_error_answer_short) else null
    tilConfirmAnswer.error =
        if (error == SecurityQuestionInputError.ANSWER_MISMATCH) getString(R.string.security_question_error_mismatch) else null
}

fun SecurityQuestionFragment.updateLockout(remainingSeconds: Int) {
    binding.tvQuestionLockout.updateLockoutMessage(remainingSeconds)
}

fun SecurityQuestionFragment.updateInputEnabled(isEnabled: Boolean) = with(binding) {
    listOf(tilAnswer, tilQuestion, tilCustomQuestion, tilNewAnswer, tilConfirmAnswer).forEach { it.isEnabled = isEnabled }
    btnQuestionAction.setEnabledWithAlpha(isEnabled)
}

fun SecurityQuestionFragment.handleUiEvent(event: SecurityQuestionUiEvent) {
    when (event) {
        is SecurityQuestionUiEvent.VerifyFailed -> {
            binding.etAnswer.text = null
            showMessage(message = getString(R.string.security_question_wrong_remaining, event.remainingAttempts), type = MessageType.ERROR)
        }

        is SecurityQuestionUiEvent.Saved -> {
            showMessage(message = getString(R.string.security_question_saved), type = MessageType.SUCCESS)
            navigator.navigateUp()
        }

        // The unlocked app coming to the front is the feedback; the overlay just closes.
        is SecurityQuestionUiEvent.Unlocked -> requireActivity().finish()

        is SecurityQuestionUiEvent.NavigateToPinSetup -> navigateToPinSetup()
    }
}

private fun SecurityQuestionFragment.navigateToPinSetup() {
    navigator.navigateToPinLock(actionId = R.id.action_securityQuestionFragment_to_pinLockFragment, mode = PinLockMode.SETUP)
}
