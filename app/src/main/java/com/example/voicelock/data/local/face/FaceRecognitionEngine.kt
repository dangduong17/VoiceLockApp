package com.example.voicelock.data.local.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.core.graphics.scale
import com.example.voicelock.data.model.face.EyeState
import com.example.voicelock.data.model.face.FaceEngineNotReadyException
import com.example.voicelock.data.model.face.FaceEngineStatus
import com.example.voicelock.data.model.face.FaceFrameAnalysis
import com.example.voicelock.util.Constant
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Fully on-device face recognition:
 *  1. ML Kit finds the face, its head angles and eye-open probabilities (for the blink check);
 *  2. the face crop is resized to the model input (112x112) and normalized to [-1, 1];
 *  3. MobileFaceNet turns it into an embedding, L2-normalized so that the dot product of two
 *     embeddings is their cosine similarity.
 * The TFLite interpreter is not thread-safe, so inference is serialized on [lock].
 */
class FaceRecognitionEngine(
    private val context: Context,
) {
    private val lock = Any()
    private var interpreter: Interpreter? = null
    private var inputSize = DEFAULT_INPUT_SIZE
    private var embeddingSize = DEFAULT_EMBEDDING_SIZE

    @Volatile
    private var status: FaceEngineStatus? = null

    private val detector: FaceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setMinFaceSize(MIN_DETECTED_FACE_SIZE)
                .build(),
        )
    }

    /** Loads the model once; the result is cached for the process. */
    suspend fun initialize(): FaceEngineStatus =
        withContext(Dispatchers.Default) {
            synchronized(lock) { getOrInitStatus() }
        }

    /** [extractTemplate]: also compute the embedding (only done for open-eye frames). */
    suspend fun analyze(
        bitmap: Bitmap,
        extractTemplate: Boolean,
    ): FaceFrameAnalysis =
        withContext(Dispatchers.Default) {
            val faces = Tasks.await(detector.process(InputImage.fromBitmap(bitmap, 0)))
            if (faces.isEmpty()) return@withContext FaceFrameAnalysis.NoFace
            if (faces.size > 1) return@withContext FaceFrameAnalysis.MultipleFaces

            val face = faces.first()
            val box = face.boundingBox
            when {
                box.width().toFloat() / bitmap.width < Constant.FACE_MIN_WIDTH_RATIO -> FaceFrameAnalysis.TooFar
                abs(face.headEulerAngleY) > Constant.FACE_MAX_ANGLE_DEGREES ||
                    abs(face.headEulerAngleX) > Constant.FACE_MAX_ANGLE_DEGREES -> FaceFrameAnalysis.NotFrontal
                else -> {
                    val eyeState = face.getEyeState()
                    val template = if (extractTemplate && eyeState == EyeState.OPEN) embed(bitmap = bitmap, box = box) else null
                    FaceFrameAnalysis.Face(eyeState = eyeState, template = template)
                }
            }
        }

    /** Cosine similarity of two L2-normalized embeddings, in [-1, 1]. */
    fun similarity(
        first: FloatArray,
        second: FloatArray,
    ): Float {
        if (first.size != second.size) return 0f
        var dot = 0f
        for (i in first.indices) dot += first[i] * second[i]
        return dot
    }

    private fun Face.getEyeState(): EyeState {
        val left = leftEyeOpenProbability ?: return EyeState.UNKNOWN
        val right = rightEyeOpenProbability ?: return EyeState.UNKNOWN
        return when {
            left > Constant.FACE_EYE_OPEN_PROBABILITY && right > Constant.FACE_EYE_OPEN_PROBABILITY -> EyeState.OPEN
            left < Constant.FACE_EYE_CLOSED_PROBABILITY && right < Constant.FACE_EYE_CLOSED_PROBABILITY -> EyeState.CLOSED
            else -> EyeState.UNKNOWN
        }
    }

    private fun embed(
        bitmap: Bitmap,
        box: Rect,
    ): FloatArray? =
        synchronized(lock) {
            val current = getOrInitStatus()
            if (current != FaceEngineStatus.READY) throw FaceEngineNotReadyException(current)
            val model = interpreter ?: return null
            val crop = cropSquare(bitmap = bitmap, box = box) ?: return null
            val input = crop.scale(width = inputSize, height = inputSize).toInputBuffer()
            val output = Array(1) { FloatArray(embeddingSize) }
            model.run(input, output)
            output[0].normalized()
        }

    private fun cropSquare(
        bitmap: Bitmap,
        box: Rect,
    ): Bitmap? {
        val side = max(box.width(), box.height())
        val left = (box.centerX() - side / 2).coerceIn(0, bitmap.width - 1)
        val top = (box.centerY() - side / 2).coerceIn(0, bitmap.height - 1)
        val width = side.coerceAtMost(bitmap.width - left)
        val height = side.coerceAtMost(bitmap.height - top)
        if (width < MIN_CROP_PX || height < MIN_CROP_PX) return null
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    private fun Bitmap.toInputBuffer(): ByteBuffer {
        val pixels = IntArray(inputSize * inputSize)
        getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        val buffer = ByteBuffer.allocateDirect(FLOAT_BYTES * inputSize * inputSize * CHANNELS).order(ByteOrder.nativeOrder())
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16 and 0xFF) - PIXEL_MEAN) / PIXEL_STD)
            buffer.putFloat(((pixel shr 8 and 0xFF) - PIXEL_MEAN) / PIXEL_STD)
            buffer.putFloat(((pixel and 0xFF) - PIXEL_MEAN) / PIXEL_STD)
        }
        buffer.rewind()
        return buffer
    }

    private fun FloatArray.normalized(): FloatArray {
        var sum = 0f
        for (value in this) sum += value * value
        val norm = sqrt(sum)
        if (norm == 0f) return this
        return FloatArray(size) { this[it] / norm }
    }

    private fun getOrInitStatus(): FaceEngineStatus = status ?: loadModel().also { status = it }

    private fun loadModel(): FaceEngineStatus =
        try {
            val model = Interpreter(loadModelFile(), Interpreter.Options().setNumThreads(INTERPRETER_THREADS))
            inputSize = model.getInputTensor(0).shape()[1]
            embeddingSize = model.getOutputTensor(0).shape().last()
            interpreter = model
            Timber.tag(TAG).d("Face model loaded: input=%d embedding=%d", inputSize, embeddingSize)
            FaceEngineStatus.READY
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "Face model asset missing")
            FaceEngineStatus.MODEL_MISSING
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Face model failed to load")
            FaceEngineStatus.INIT_FAILED
        } catch (e: LinkageError) {
            Timber.tag(TAG).e(e, "TFLite native library unavailable")
            FaceEngineStatus.INIT_FAILED
        }

    private fun loadModelFile(): MappedByteBuffer =
        context.assets.openFd(Constant.FACE_MODEL_ASSET).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, descriptor.startOffset, descriptor.declaredLength)
            }
        }

    private companion object {
        const val TAG = "FaceRecognitionEngine"
        const val DEFAULT_INPUT_SIZE = 112
        const val DEFAULT_EMBEDDING_SIZE = 192
        const val CHANNELS = 3
        const val FLOAT_BYTES = 4
        const val PIXEL_MEAN = 128f
        const val PIXEL_STD = 128f
        const val INTERPRETER_THREADS = 4
        const val MIN_CROP_PX = 32
        const val MIN_DETECTED_FACE_SIZE = 0.15f
    }
}
