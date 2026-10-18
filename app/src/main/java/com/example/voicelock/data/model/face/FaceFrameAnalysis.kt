package com.example.voicelock.data.model.face

enum class EyeState { OPEN, CLOSED, UNKNOWN }

/** What a single camera frame contains, from the enrollment / verification point of view. */
sealed class FaceFrameAnalysis {
    data object NoFace : FaceFrameAnalysis()

    data object MultipleFaces : FaceFrameAnalysis()

    /** The face is too small in the frame for a reliable embedding. */
    data object TooFar : FaceFrameAnalysis()

    /** Head turned or tilted too much (yaw / pitch beyond the allowed angle). */
    data object NotFrontal : FaceFrameAnalysis()

    /**
     * A single, frontal, close-enough face. [template] (L2-normalized MobileFaceNet embedding) is
     * only computed when it was asked for and the eyes are open; closed-eye frames feed the blink check.
     */
    class Face(
        val eyeState: EyeState,
        val template: FloatArray?,
    ) : FaceFrameAnalysis()
}

/** Guidance shown under the camera preview. */
enum class FaceHint {
    NONE,
    NO_FACE,
    MULTIPLE_FACES,
    MOVE_CLOSER,
    LOOK_STRAIGHT,

    /** The face is fine, the liveness check still needs a blink. */
    BLINK,
    HOLD_STILL,
    FACE_MISMATCH,
}

fun FaceFrameAnalysis.toHint(): FaceHint =
    when (this) {
        FaceFrameAnalysis.NoFace -> FaceHint.NO_FACE
        FaceFrameAnalysis.MultipleFaces -> FaceHint.MULTIPLE_FACES
        FaceFrameAnalysis.TooFar -> FaceHint.MOVE_CLOSER
        FaceFrameAnalysis.NotFrontal -> FaceHint.LOOK_STRAIGHT
        is FaceFrameAnalysis.Face -> FaceHint.HOLD_STILL
    }

/**
 * Active liveness: a printed photo or a still screen never blinks. A blink is the eyes seen open,
 * then closed, then open again, on a single face; seeing nobody or several faces starts over.
 * A replayed video can still pass, which is the known limit of this check.
 */
class BlinkTracker {
    private var hasSeenOpen = false
    private var hasSeenClosedAfterOpen = false

    var hasBlinked = false
        private set

    fun onFrame(analysis: FaceFrameAnalysis) {
        if (analysis !is FaceFrameAnalysis.Face) {
            if (analysis == FaceFrameAnalysis.NoFace || analysis == FaceFrameAnalysis.MultipleFaces) reset()
            return
        }
        when (analysis.eyeState) {
            EyeState.OPEN -> {
                if (hasSeenClosedAfterOpen) hasBlinked = true
                hasSeenOpen = true
            }
            EyeState.CLOSED -> if (hasSeenOpen) hasSeenClosedAfterOpen = true
            EyeState.UNKNOWN -> Unit
        }
    }

    fun reset() {
        hasSeenOpen = false
        hasSeenClosedAfterOpen = false
        hasBlinked = false
    }
}
