package com.example.voicelock.data.model.voice

/** The CAM++ .onnx file is not in app/src/main/assets. */
class VoiceModelMissingException(
    modelAssetPath: String,
) : IllegalStateException("Speaker model not found in assets: $modelAssetPath")
