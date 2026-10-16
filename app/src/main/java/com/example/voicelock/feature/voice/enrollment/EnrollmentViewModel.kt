package com.example.voicelock.feature.voice.enrollment

import android.annotation.SuppressLint
import com.example.voicelock.base.BaseViewModel
import com.example.voicelock.base.launchIO
import com.example.voicelock.data.model.voice.EnrollmentError
import com.example.voicelock.data.model.voice.EnrollmentPhase
import com.example.voicelock.data.model.voice.EnrollmentUiState
import com.example.voicelock.data.model.voice.VoiceEnrollmentResult
import com.example.voicelock.data.model.voice.VoiceKeywordProfile
import com.example.voicelock.data.model.voice.canRecord
import com.example.voicelock.domain.usecase.voice.EnrollOwnerVoiceUseCase
import com.example.voicelock.domain.usecase.voice.MatchVoiceKeywordUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceUnlockEnabledUseCase
import com.example.voicelock.domain.usecase.voice.WarmUpVoiceModelUseCase
import com.example.voicelock.util.getDataOrDefault
import com.example.voicelock.util.onError
import com.example.voicelock.util.onSuccess
import com.example.voicelock.voice.AcousticPhraseMatcher
import com.example.voicelock.voice.AudioRecorderHelper
import com.example.voicelock.voice.KeywordAudioRecognizer
import com.example.voicelock.voice.VoiceModelMissingException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.Locale

/**
 * Collects [EnrollmentUiState.requiredCount] recordings of the voice password. Every recording is
 * transcribed (speech to text) right away and the text is shown; a recording only counts when:
 *  - its text could be recognized in some language — the first one finds the language by itself:
 *    on-device language detection (Android 14+) guesses it, its offline model is downloaded when
 *    missing, then the guess, the system language and every installed language are tried;
 *  - later recordings are heard as the same phrase as the earlier ones, in that language.
 * Otherwise the recording is discarded and the user is asked to say it again — enrollment never
 * completes without a recognized phrase. The accepted samples then build the speaker centroid and
 * DTW templates via [EnrollOwnerVoiceUseCase], and the transcripts become the Stage-1 reference.
 *
 * Recordings are kept here rather than in the UiState: they are large and must not be diffed.
 */
class EnrollmentViewModel(
    private val audioRecorderHelper: AudioRecorderHelper,
    private val keywordAudioRecognizer: KeywordAudioRecognizer,
    private val matchVoiceKeywordUseCase: MatchVoiceKeywordUseCase,
    private val enrollOwnerVoiceUseCase: EnrollOwnerVoiceUseCase,
    private val warmUpVoiceModelUseCase: WarmUpVoiceModelUseCase,
    private val setVoiceUnlockEnabledUseCase: SetVoiceUnlockEnabledUseCase,
) : BaseViewModel<EnrollmentUiState, Nothing>(EnrollmentUiState()) {

    private val recordings = mutableListOf<FloatArray>()

    /**
     * Languages in which every accepted sample so far was heard as the same phrase, in preference
     * order, each with the top transcript of every accepted sample. Null before the first sample.
     */
    private var transcriptsByLanguage: LinkedHashMap<String, List<String>>? = null

    private var sampleJob: Job? = null

    init {
        // Load the CAM++ model while the user reads the instructions; failures surface at enrollment.
        launchIO { warmUpVoiceModelUseCase().first() }
    }

    /** The caller must hold RECORD_AUDIO (EnrollmentActivity checks it before calling). */
    @SuppressLint("MissingPermission")
    fun recordNextSample() {
        if (!getCurrentState().canRecord()) return
        setState { copy(phase = EnrollmentPhase.RECORDING, error = EnrollmentError.NONE, rejectedHeardText = "") }

        sampleJob =
            launchIO(onError = { setState { copy(phase = EnrollmentPhase.IDLE, error = EnrollmentError.RECORDING_FAILED) } }) {
                val samples = audioRecorderHelper.record()
                if (!AcousticPhraseMatcher.hasSpeech(samples)) {
                    setState { copy(phase = EnrollmentPhase.IDLE, error = EnrollmentError.NO_SPEECH) }
                    return@launchIO
                }

                setState { copy(phase = EnrollmentPhase.RECOGNIZING) }
                if (!acceptIfRecognized(samples)) return@launchIO

                recordings += samples
                val count = recordings.size
                val leadingTranscripts = transcriptsByLanguage?.values?.firstOrNull().orEmpty()
                setState { copy(phase = EnrollmentPhase.IDLE, recordedCount = count, sampleTranscripts = leadingTranscripts) }
                if (count >= getCurrentState().requiredCount) enroll()
            }
    }

    /**
     * The user gave up on the language-pack download (e.g. declined the system prompt): drop this
     * recording and let them speak again right away; earlier accepted recordings are kept.
     */
    fun cancelLanguageDownload() {
        if (getCurrentState().phase != EnrollmentPhase.DOWNLOADING_LANGUAGE) return
        sampleJob?.cancel()
        sampleJob = null
        setState { copy(phase = EnrollmentPhase.IDLE, error = EnrollmentError.LANGUAGE_DOWNLOAD_CANCELLED) }
    }

    fun restartEnrollment() {
        val phase = getCurrentState().phase
        if (phase == EnrollmentPhase.IDLE || phase == EnrollmentPhase.COMPLETED) resetSamples(EnrollmentError.NONE)
    }

    /**
     * Speech to text for one recording. Returns true (and narrows [transcriptsByLanguage]) when at
     * least one candidate language hears it as the same phrase as the samples accepted so far;
     * otherwise sets the "say it again" error and returns false, leaving earlier samples intact.
     */
    private suspend fun acceptIfRecognized(sample: FloatArray): Boolean {
        val previous = transcriptsByLanguage
        val languages = previous?.keys?.toList() ?: initialLanguages(sample) ?: return false

        val agreeing = LinkedHashMap<String, List<String>>()
        var anySupported = false
        var mismatchHeard: String? = null
        for (languageTag in languages) {
            val earlier = previous?.get(languageTag).orEmpty()
            val outcome = keywordAudioRecognizer.recognize(sample, languageTag, biasingStrings = earlier)
            if (outcome != KeywordAudioRecognizer.Outcome.Unsupported) anySupported = true
            val texts =
                (outcome as? KeywordAudioRecognizer.Outcome.Candidates)?.texts.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
            if (texts.isEmpty()) continue

            val isSamePhrase = earlier.isEmpty() || matchVoiceKeywordUseCase(texts, earlier).getDataOrDefault(false)
            if (isSamePhrase) {
                agreeing[languageTag] = earlier + texts.first()
            } else if (mismatchHeard == null) {
                mismatchHeard = texts.first()
            }
        }
        Timber.d("Enrollment sample #%d languages=%s agreeing=%s mismatch=%s", recordings.size + 1, languages, agreeing, mismatchHeard)

        if (agreeing.isEmpty()) {
            val error =
                when {
                    !anySupported -> EnrollmentError.RECOGNITION_UNAVAILABLE
                    mismatchHeard != null -> EnrollmentError.PHRASE_MISMATCH
                    else -> EnrollmentError.NOT_RECOGNIZED
                }
            setState { copy(phase = EnrollmentPhase.IDLE, error = error, rejectedHeardText = mismatchHeard.orEmpty()) }
            return false
        }
        transcriptsByLanguage = agreeing
        return true
    }

    /**
     * Languages to try for the first sample, best guess first. The on-device language detector also
     * recognizes languages whose model is not installed (verified on device: ja-JP and en-US were
     * detected with only vi-VN installed), so detection runs over every installed AND downloadable
     * language; a detected language that is not on the device yet is downloaded right away.
     * Returns null (with the error set) when that download fails.
     */
    private suspend fun initialLanguages(sample: FloatArray): List<String>? {
        val systemLanguage = Locale.getDefault().toLanguageTag()
        val support = keywordAudioRecognizer.getRecognitionSupport()
        val detectable = (support.installed + support.downloadable).distinctBy { it.lowercase(Locale.ROOT) }
        val detected =
            if (detectable.isNotEmpty()) {
                when (val outcome = keywordAudioRecognizer.recognize(sample, systemLanguage, detectLanguageAmong = detectable)) {
                    is KeywordAudioRecognizer.Outcome.Candidates -> outcome.detectedLanguage
                    is KeywordAudioRecognizer.Outcome.NoSpeech -> outcome.detectedLanguage
                    else -> null
                }?.let { tag -> detectable.firstOrNull { it.equals(tag, ignoreCase = true) } ?: tag }
            } else {
                null
            }

        var installed = support.installed
        if (detected != null && installed.none { it.equals(detected, ignoreCase = true) }) {
            setState { copy(phase = EnrollmentPhase.DOWNLOADING_LANGUAGE, pendingLanguageTag = detected) }
            if (!keywordAudioRecognizer.downloadLanguage(detected)) {
                setState { copy(phase = EnrollmentPhase.IDLE, error = EnrollmentError.LANGUAGE_DOWNLOAD_FAILED) }
                return null
            }
            installed = installed + detected
            setState { copy(phase = EnrollmentPhase.RECOGNIZING) }
        }
        return (listOfNotNull(detected, systemLanguage) + installed + FALLBACK_LANGUAGE)
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(MAX_LANGUAGES_TO_TRY)
            .also { Timber.d("Enrollment language candidates=%s (detected=%s, installed=%s)", it, detected, installed) }
    }

    private suspend fun enroll() {
        setState { copy(phase = EnrollmentPhase.PROCESSING) }
        val samples = recordings.toList()
        val (languageTag, transcripts) = transcriptsByLanguage!!.entries.first().let { it.key to it.value }
        val keywordProfile = VoiceKeywordProfile(languageTag = languageTag, transcripts = transcripts.distinct())

        enrollOwnerVoiceUseCase(samples, keywordProfile).first()
            .onSuccess { result ->
                when (result) {
                    is VoiceEnrollmentResult.Success -> {
                        // Enrolling is how the user opts in, so voice unlock becomes active right away.
                        setVoiceUnlockEnabledUseCase(true).first()
                        setState {
                            copy(
                                phase = EnrollmentPhase.COMPLETED,
                                recognizedPhrase = transcripts.first(),
                                languageTag = languageTag,
                            )
                        }
                    }
                    is VoiceEnrollmentResult.NoSpeechDetected -> resetSamples(EnrollmentError.NO_SPEECH)
                    is VoiceEnrollmentResult.InconsistentSamples -> resetSamples(EnrollmentError.INCONSISTENT_SAMPLES)
                }
            }.onError { throwable ->
                Timber.e(throwable, "Voice enrollment failed")
                resetSamples(
                    if (throwable is VoiceModelMissingException) EnrollmentError.MODEL_MISSING else EnrollmentError.PROCESSING_FAILED,
                )
            }
    }

    private fun resetSamples(error: EnrollmentError) {
        recordings.clear()
        transcriptsByLanguage = null
        setState {
            copy(
                phase = EnrollmentPhase.IDLE,
                recordedCount = 0,
                error = error,
                recognizedPhrase = "",
                rejectedHeardText = "",
                sampleTranscripts = emptyList(),
            )
        }
    }

    private companion object {
        const val MAX_LANGUAGES_TO_TRY = 6
        const val FALLBACK_LANGUAGE = "en-US"
    }
}
