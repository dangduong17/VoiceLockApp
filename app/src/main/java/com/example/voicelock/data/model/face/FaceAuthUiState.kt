package com.example.voicelock.data.model.face

enum class FaceAuthStage {
    INITIALIZING,
    ENGINE_ERROR,
    NOT_ENROLLED,
    CAMERA_PERMISSION_REQUIRED,

    /** Ready but not scanning (screen in the background, or waiting for the user). */
    IDLE,
    SCANNING,
    SUCCESS,

    /** No matching, blinking face before the scan timed out. */
    FAILED,
}

data class FaceAuthUiState(
    val stage: FaceAuthStage = FaceAuthStage.INITIALIZING,
    val engineStatus: FaceEngineStatus = FaceEngineStatus.READY,
    /** True when started by LockOverlayActivity to unlock another app. */
    val isAppUnlock: Boolean = false,
    val hint: FaceHint = FaceHint.NONE,
    /** Best similarity of the current scan; null until a face was compared. */
    val bestSimilarity: Float? = null,
)

fun FaceAuthUiState.canStartScan(): Boolean =
    stage == FaceAuthStage.IDLE || stage == FaceAuthStage.FAILED || stage == FaceAuthStage.SUCCESS

/** Stage, hint and score render together in the status area. */
data class FaceAuthStatusInfo(
    val stage: FaceAuthStage,
    val engineStatus: FaceEngineStatus,
    val hint: FaceHint,
    val bestSimilarity: Float?,
)

fun FaceAuthUiState.getStatusInfo(): FaceAuthStatusInfo =
    FaceAuthStatusInfo(stage = stage, engineStatus = engineStatus, hint = hint, bestSimilarity = bestSimilarity)

sealed class FaceAuthUiEvent {
    /** [isAppUnlock]: the screen was opened by the lock overlay and should close it. */
    data class Authenticated(
        val isAppUnlock: Boolean,
    ) : FaceAuthUiEvent()

    data object ProfileDeleted : FaceAuthUiEvent()
}
