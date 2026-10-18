package com.example.voicelock.feature.face

import androidx.fragment.app.Fragment
import com.example.voicelock.R
import com.example.voicelock.data.model.face.FaceEngineStatus
import com.example.voicelock.data.model.face.FaceHint

/** Texts shared by the face enrollment and face verification screens. */
fun Fragment.getFaceHintText(hint: FaceHint): String? =
    when (hint) {
        FaceHint.NONE -> null
        FaceHint.NO_FACE -> getString(R.string.face_hint_no_face)
        FaceHint.MULTIPLE_FACES -> getString(R.string.face_hint_multiple_faces)
        FaceHint.MOVE_CLOSER -> getString(R.string.face_hint_move_closer)
        FaceHint.LOOK_STRAIGHT -> getString(R.string.face_hint_look_straight)
        FaceHint.BLINK -> getString(R.string.face_hint_blink)
        FaceHint.HOLD_STILL -> getString(R.string.face_hint_hold_still)
        FaceHint.FACE_MISMATCH -> getString(R.string.face_hint_face_mismatch)
    }

fun Fragment.getFaceEngineErrorText(status: FaceEngineStatus): String =
    when (status) {
        FaceEngineStatus.MODEL_MISSING -> getString(R.string.face_engine_error_model_missing)
        FaceEngineStatus.READY, FaceEngineStatus.INIT_FAILED -> getString(R.string.face_engine_error_init)
    }
