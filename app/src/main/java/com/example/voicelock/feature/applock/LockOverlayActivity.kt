package com.example.voicelock.feature.applock

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.voicelock.R
import com.example.voicelock.data.model.applock.LockOverlayUiEvent
import com.example.voicelock.data.model.applock.LockOverlayUiState
import com.example.voicelock.data.model.applock.hasFallbackMethod
import com.example.voicelock.data.model.lock.LockMethod
import com.example.voicelock.data.model.patternlock.PatternLockMode
import com.example.voicelock.data.model.pinlock.PinLockMode
import com.example.voicelock.feature.voice.auth.TwoStageAuthActivity
import com.example.voicelock.util.BundleKey
import com.example.voicelock.util.collectFlowOnView
import com.example.voicelock.util.setPreventDoubleClick
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Plain Activity shell (same "outside the BaseFragment mandate" status as MainActivity) that
 * hosts a small, dynamically-chosen nav graph reusing the existing PinLockFragment/PatternLockFragment
 * in UNLOCK mode, so a locked app's foreground can be gated by the existing verification stack.
 * Started by AppLockMonitorService with FLAG_ACTIVITY_NEW_TASK.
 *
 * Voice (TwoStageAuthActivity, launched for result) comes first when enrolled, then Pattern,
 * with PIN as the backup (see LockOverlayViewModel); this
 * screen offers a "try another method" button so a user who forgets the primary method can
 * still fall back to whichever other method they've configured.
 */
class LockOverlayActivity : AppCompatActivity() {

    private val viewModel: LockOverlayViewModel by viewModel()
    private var targetPackageName: String = ""

    /**
     * TwoStageAuthActivity marks the app unlocked itself; RESULT_OK just closes the overlay.
     * Any other result (back, "use PIN or pattern") falls back to the next configured method.
     */
    private val voiceUnlockLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when {
                result.resultCode == RESULT_OK -> finish()
                viewModel.uiState.value.hasFallbackMethod() -> viewModel.onTryAnotherMethod()
                else -> finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_overlay)
        applyLockScreenWindowFlags()

        targetPackageName = intent.getStringExtra(BundleKey.KEY_LOCK_TARGET_PACKAGE).orEmpty()

        findViewById<View>(R.id.btnTryAnotherMethod).setPreventDoubleClick {
            viewModel.onTryAnotherMethod()
        }

        viewModel.uiState.collectFlowOnView(this) { state -> updateUiState(state) }
        viewModel.uiEvent.collectFlowOnView(this) { event -> handleEvent(event) }
        viewModel.decideVerificationMethod()
    }

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
                val intent =
                    Intent(this, TwoStageAuthActivity::class.java)
                        .putExtra(BundleKey.KEY_LOCK_TARGET_PACKAGE, targetPackageName)
                voiceUnlockLauncher.launch(intent)
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

    private fun applyLockScreenWindowFlags() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        )
    }
}
