package com.example.voicelock.feature.face.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX preview + frame analysis for the face screens (front camera when available). Frames are
 * delivered upright as Bitmaps on a background thread, and only converted when [shouldAnalyze]
 * says the ViewModel wants one, so busy frames are dropped cheaply. Bound to [lifecycleOwner]
 * (the fragment's view lifecycle), so the camera stops with the screen.
 */
class FaceCameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val shouldAnalyze: () -> Boolean,
    private val onFrame: (Bitmap) -> Unit,
) {
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var analysis: ImageAnalysis? = null
    private var isReleased = false

    fun start() {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                if (isReleased) return@addListener
                try {
                    val provider = providerFuture.get()
                    cameraProvider = provider
                    bindUseCases(provider)
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Camera start failed")
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    /** Unbinds only this screen's use cases: another face screen may already own the camera. */
    fun release() {
        isReleased = true
        val boundUseCases = listOfNotNull(preview, analysis)
        if (boundUseCases.isNotEmpty()) cameraProvider?.unbind(*boundUseCases.toTypedArray())
        analysis?.clearAnalyzer()
        cameraProvider = null
        preview = null
        analysis = null
        analysisExecutor.shutdown()
    }

    private fun bindUseCases(provider: ProcessCameraProvider) {
        val newPreview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val newAnalysis =
            ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(ANALYSIS_SIZE, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                        ).build(),
                ).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
        newAnalysis.setAnalyzer(analysisExecutor) { image ->
            image.use { if (shouldAnalyze()) onFrame(it.toUprightBitmap()) }
        }
        provider.bindToLifecycle(lifecycleOwner, selectCamera(provider), newPreview, newAnalysis)
        preview = newPreview
        analysis = newAnalysis
    }

    private fun selectCamera(provider: ProcessCameraProvider): CameraSelector {
        val hasFrontCamera = runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)
        return if (hasFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    }

    private fun ImageProxy.toUprightBitmap(): Bitmap {
        val bitmap = toBitmap()
        val rotation = imageInfo.rotationDegrees
        if (rotation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private companion object {
        const val TAG = "FaceCameraController"

        /** Landscape sensor size; FaceSDK needs no more than VGA for a face at arm's length. */
        val ANALYSIS_SIZE = Size(640, 480)
    }
}
