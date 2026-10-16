package com.example.voicelock.data.model.home

sealed class HomeUiEvent {
    data object LockNow : HomeUiEvent()
    data class ShowToast(val message: String) : HomeUiEvent()
    data object NavigateToPinSetup : HomeUiEvent()
    data object NavigateToPatternSetup : HomeUiEvent()
    data object NavigateToFingerprintSetup : HomeUiEvent()
    data object NavigateToSecurityQuestions : HomeUiEvent()
}
