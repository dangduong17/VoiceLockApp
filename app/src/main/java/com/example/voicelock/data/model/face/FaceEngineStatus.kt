package com.example.voicelock.data.model.face

/** Result of loading the on-device face recognition model (see FaceRecognitionEngine). */
enum class FaceEngineStatus {
    READY,

    /** assets/mobilefacenet.tflite is not in the APK. */
    MODEL_MISSING,
    INIT_FAILED,
}

class FaceEngineNotReadyException(
    val status: FaceEngineStatus,
) : IllegalStateException("Face engine is not ready: $status")
