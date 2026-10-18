package com.example.voicelock.data.local.face

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.voicelock.data.model.face.FaceEngineStatus
import com.example.voicelock.data.model.face.FaceFrameAnalysis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device checks of the face pipeline that need no person in front of the camera:
 * the bundled MobileFaceNet model loads, and ML Kit sees no face in an empty frame.
 */
@RunWith(AndroidJUnit4::class)
class FaceRecognitionEngineTest {

    private val engine = FaceRecognitionEngine(context = InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun modelFromAssets_loads() =
        runBlocking {
            assertEquals(FaceEngineStatus.READY, engine.initialize())
        }

    @Test
    fun emptyFrame_hasNoFace() =
        runBlocking {
            val frame = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
            assertEquals(FaceFrameAnalysis.NoFace, engine.analyze(bitmap = frame, extractTemplate = true))
        }

    @Test
    fun similarity_ofNormalizedVectors_isCosine() {
        val first = floatArrayOf(1f, 0f, 0f)
        val second = floatArrayOf(0f, 1f, 0f)
        assertEquals(1f, engine.similarity(first = first, second = first), 1e-6f)
        assertEquals(0f, engine.similarity(first = first, second = second), 1e-6f)
    }
}
