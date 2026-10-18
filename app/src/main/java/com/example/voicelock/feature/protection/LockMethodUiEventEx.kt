package com.example.voicelock.feature.protection

import android.view.View
import androidx.annotation.IdRes
import com.example.voicelock.base.BaseFragment
import com.example.voicelock.data.model.biometric.BiometricType
import com.example.voicelock.data.model.lock.LockMethodUiEvent
import com.example.voicelock.feature.biometric.confirmBiometric
import com.example.voicelock.feature.biometric.showBiometricUnavailable
import com.example.voicelock.feature.biometric.startBiometricEnrollment
import com.example.voicelock.feature.biometric.testBiometric
import com.example.voicelock.util.navigateToPatternLock
import com.example.voicelock.util.navigateToPinLock
import com.example.voicelock.util.navigateToSecurityQuestion
import com.example.voicelock.util.navigateToVoiceEnrollment
import com.example.voicelock.util.offerVoiceLockTile

/** The nav graph actions a screen with lock-method rows (Home, Settings) uses for each destination. */
data class LockMethodRoutes(
    @param:IdRes val pinLockActionId: Int,
    @param:IdRes val patternLockActionId: Int,
    @param:IdRes val securityQuestionActionId: Int,
    @param:IdRes val voiceEnrollmentActionId: Int,
    @param:IdRes val voiceTestActionId: Int,
    @param:IdRes val faceEnrollmentActionId: Int,
    @param:IdRes val faceTestActionId: Int,
)

/** Shared handling of [LockMethodUiEvent]; [anchorView] keeps messages above the screen's bottom bar. */
fun BaseFragment<*, *>.handleLockMethodEvent(
    event: LockMethodUiEvent,
    routes: LockMethodRoutes,
    anchorView: View?,
    onBiometricConfirmed: (BiometricType) -> Unit,
) {
    when (event) {
        is LockMethodUiEvent.NavigateToPinLock ->
            navigator.navigateToPinLock(actionId = routes.pinLockActionId, mode = event.mode, pinLength = event.pinLength)

        is LockMethodUiEvent.NavigateToPatternLock ->
            navigator.navigateToPatternLock(actionId = routes.patternLockActionId, mode = event.mode)

        is LockMethodUiEvent.NavigateToSecurityQuestion ->
            navigator.navigateToSecurityQuestion(actionId = routes.securityQuestionActionId, mode = event.mode)

        is LockMethodUiEvent.NavigateToVoiceEnrollment ->
            navigator.navigateToVoiceEnrollment(actionId = routes.voiceEnrollmentActionId, mode = event.mode)

        is LockMethodUiEvent.NavigateToVoiceTest -> navigator.navigateTo(routes.voiceTestActionId)

        is LockMethodUiEvent.OfferVoiceLockTile -> offerVoiceLockTile()

        is LockMethodUiEvent.NavigateToFaceEnrollment -> navigator.navigateTo(routes.faceEnrollmentActionId)

        is LockMethodUiEvent.NavigateToFaceTest -> navigator.navigateTo(routes.faceTestActionId)

        is LockMethodUiEvent.ConfirmBiometric ->
            confirmBiometric(type = event.type, anchorView = anchorView, onConfirmed = { onBiometricConfirmed(event.type) })

        is LockMethodUiEvent.TestBiometric -> testBiometric(type = event.type, anchorView = anchorView)

        is LockMethodUiEvent.OpenBiometricEnrollment -> startBiometricEnrollment(type = event.type, anchorView = anchorView)

        is LockMethodUiEvent.ShowBiometricUnavailable ->
            showBiometricUnavailable(type = event.type, availability = event.availability, anchorView = anchorView)
    }
}
