package com.example.voicelock.data.model.face

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlinkTrackerTest {

    private fun face(eyeState: EyeState) = FaceFrameAnalysis.Face(eyeState = eyeState, template = null)

    @Test
    fun openClosedOpen_isBlink() {
        val tracker = BlinkTracker()
        tracker.onFrame(face(EyeState.OPEN))
        tracker.onFrame(face(EyeState.CLOSED))
        assertFalse(tracker.hasBlinked)
        tracker.onFrame(face(EyeState.OPEN))
        assertTrue(tracker.hasBlinked)
    }

    @Test
    fun eyesAlwaysOpen_photo_neverBlinks() {
        val tracker = BlinkTracker()
        repeat(20) { tracker.onFrame(face(EyeState.OPEN)) }
        assertFalse(tracker.hasBlinked)
    }

    @Test
    fun closedBeforeAnyOpenFrame_doesNotCount() {
        val tracker = BlinkTracker()
        tracker.onFrame(face(EyeState.CLOSED))
        tracker.onFrame(face(EyeState.OPEN))
        assertFalse(tracker.hasBlinked)
    }

    @Test
    fun losingTheFace_startsOver() {
        val tracker = BlinkTracker()
        tracker.onFrame(face(EyeState.OPEN))
        tracker.onFrame(face(EyeState.CLOSED))
        tracker.onFrame(FaceFrameAnalysis.NoFace)
        tracker.onFrame(face(EyeState.OPEN))
        assertFalse(tracker.hasBlinked)
    }

    @Test
    fun secondPersonInFrame_startsOver() {
        val tracker = BlinkTracker()
        tracker.onFrame(face(EyeState.OPEN))
        tracker.onFrame(face(EyeState.CLOSED))
        tracker.onFrame(FaceFrameAnalysis.MultipleFaces)
        tracker.onFrame(face(EyeState.OPEN))
        assertFalse(tracker.hasBlinked)
    }

    @Test
    fun unknownEyeFrames_areIgnored() {
        val tracker = BlinkTracker()
        tracker.onFrame(face(EyeState.OPEN))
        tracker.onFrame(face(EyeState.UNKNOWN))
        tracker.onFrame(face(EyeState.CLOSED))
        tracker.onFrame(face(EyeState.UNKNOWN))
        tracker.onFrame(face(EyeState.OPEN))
        assertTrue(tracker.hasBlinked)
    }

    @Test
    fun badPoseFrames_keepProgress() {
        val tracker = BlinkTracker()
        tracker.onFrame(face(EyeState.OPEN))
        tracker.onFrame(face(EyeState.CLOSED))
        tracker.onFrame(FaceFrameAnalysis.NotFrontal)
        tracker.onFrame(face(EyeState.OPEN))
        assertTrue(tracker.hasBlinked)
    }
}
