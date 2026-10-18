package com.example.voicelock.feature.applock

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.example.voicelock.R
import com.example.voicelock.data.model.applock.LockOverlayUiEvent
import com.example.voicelock.data.model.applock.LockOverlayUiState
import com.example.voicelock.data.model.applock.hasFallbackMethod
import com.example.voicelock.data.model.applock.isVoiceAvailable
import com.example.voicelock.data.model.lock.LockMethod
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.data.model.securityquestion.SecurityQuestionMode
import com.example.voicelock.feature.voice.auth.TwoStageAuthFragment
import com.example.voicelock.feature.voice.auth.volumeKeyEvent
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.collectFlowOnView
import com.example.voicelock.util.setPreventDoubleClick
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/**
 * Plain Activity shell (same "outside the BaseFragment mandate" status as MainActivity) that
 * hosts a small, dynamically-chosen nav graph reusing the existing TwoStageAuthFragment (voice),
 * PinLockFragment and PatternLockFragment in unlock mode, so a locked app's foreground can be gated by
 * the existing verification stack.
 * Started by AppLockMonitorService with FLAG_ACTIVITY_NEW_TASK.
 *
 * Voice comes first when enrolled, then Pattern, with PIN as the backup (see LockOverlayViewModel); this
 * screen offers a "try another method" button so a user who forgets the primary method can
 * still fall back to whichever other method they've configured.
 */
class LockOverlayActivity : AppCompatActivity() {

    private val viewModel: LockOverlayViewModel by viewModel()
    private var targetPackageName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_overlay)
        applySystemBarInsets()
        applyLockScreenWindowFlags()

        targetPackageName = intent.getStringExtra(BundleKey.KEY_LOCK_TARGET_PACKAGE).orEmpty()

        findViewById<View>(R.id.btnTryAnotherMethod).setPreventDoubleClick {
            viewModel.onTryAnotherMethod()
        }

        viewModel.uiState.collectFlowOnView(this) { state -> updateUiState(state) }
        viewModel.uiEvent.collectFlowOnView(this) { event -> handleEvent(event) }
        viewModel.decideVerificationMethod()
    }

    /**
     * F0: on the lock screen either volume key starts voice verification instead of tapping the mic.
     * While voice unlock is configured the keys never change the volume here (holding them neither).
     */
    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (keyCode !in VOLUME_KEYS || !viewModel.uiState.value.isVoiceAvailable()) {
            return super.onKeyDown(keyCode, event)
        }
        if (event.repeatCount == 0) volumeKeyEvent()
        return true
    }

    private fun volumeKeyEvent() {
        val currentScreen = getCurrentScreen()
        Timber.tag(TAG).d("Volume key on lock screen, current screen=%s", currentScreen?.javaClass?.simpleName)
        val voiceScreen = currentScreen as? TwoStageAuthFragment
        if (voiceScreen != null) voiceScreen.volumeKeyEvent() else viewModel.switchToVoiceMethod()
    }

    private fun getCurrentScreen(): Fragment? =
        (supportFragmentManager.findFragmentById(R.id.navHostOverlay) as? NavHostFragment)
            ?.childFragmentManager
            ?.primaryNavigationFragment

    private fun updateUiState(state: LockOverlayUiState) {
        findViewById<View>(R.id.btnTryAnotherMethod).visibility =
            if (state.hasFallbackMethod()) View.VISIBLE else View.GONE
    }

    private fun handleEvent(event: LockOverlayUiEvent) {
        when (event) {
            is LockOverlayUiEvent.ShowMethod -> showMethod(event.method, event.pinLength)
            is LockOverlayUiEvent.NothingConfigured -> finish()
        }
    }

    private fun showMethod(
        method: LockMethod,
        pinLength: Int,
    ) {
        when (method) {
            LockMethod.VOICE -> {
                val args = Bundle().apply { putString(BundleKey.KEY_LOCK_TARGET_PACKAGE, targetPackageName) }
                showDestination(R.id.twoStageAuthFragment, args)
            }

            LockMethod.FACE -> {
                val args = Bundle().apply { putString(BundleKey.KEY_LOCK_TARGET_PACKAGE, targetPackageName) }
                showDestination(R.id.faceAuthFragment, args)
            }

            LockMethod.BIOMETRIC -> {
                val args = Bundle().apply { putString(BundleKey.KEY_LOCK_TARGET_PACKAGE, targetPackageName) }
                showDestination(R.id.biometricAuthFragment, args)
            }

            LockMethod.PATTERN -> {
                val args =
                    Bundle().apply {
                        putString(BundleKey.KEY_LOCK_MODE, PatternLockMode.UNLOCK.name)
                        putString(BundleKey.KEY_LOCK_TARGET_PACKAGE, targetPackageName)
                    }
                showDestination(R.id.patternLockFragment, args)
            }

            LockMethod.PIN -> {
                val args =
                    Bundle().apply {
                        putString(BundleKey.KEY_LOCK_MODE, PinLockMode.UNLOCK.name)
                        putInt(BundleKey.KEY_PIN_LENGTH, pinLength)
                        putString(BundleKey.KEY_LOCK_TARGET_PACKAGE, targetPackageName)
                    }
                showDestination(R.id.pinLockFragment, args)
            }

            LockMethod.SECURITY_QUESTION -> {
                val args =
                    Bundle().apply {
                        putString(BundleKey.KEY_LOCK_MODE, SecurityQuestionMode.UNLOCK.name)
                        putString(BundleKey.KEY_LOCK_TARGET_PACKAGE, targetPackageName)
                    }
                showDestination(R.id.securityQuestionFragment, args)
            }
        }
    }

    private fun showDestination(
        destinationId: Int,
        args: Bundle,
    ) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostOverlay) as NavHostFragment
        val navController = navHostFragment.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph_lock_overlay)
        graph.setStartDestination(destinationId)
        navController.setGraph(graph, args)
    }

    /**
     * Target SDK 35+ draws edge-to-edge: without this padding the "try another method" button sits
     * under the navigation bar (same handling as MainActivity).
     */
    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutOverlayRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun applyLockScreenWindowFlags() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        )
    }

    private companion object {
        const val TAG = "LockOverlayActivity"
        val VOLUME_KEYS = setOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN)
    }
}
