package com.example.voicelock.base.navigator

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions

class NavigatorImpl(
    private val navController: NavController,
    private val lifecycle: Lifecycle,
    private val currentDestinationId: Int,
) : Navigator {
    private var navObserver: LifecycleEventObserver? = null

    private fun isAtCurrentDestination(): Boolean = navController.currentDestination?.id == currentDestinationId

    private fun safeAction(action: () -> Unit) {
        if (!isAtCurrentDestination()) return
        // addObserver on an already-resumed lifecycle delivers ON_RESUME immediately, and an
        // <activity> destination never changes currentDestination, so without this guard both the
        // observer and the direct call below would navigate (e.g. starting an Activity twice).
        var hasRun = false
        val runOnce = {
            if (!hasRun) {
                hasRun = true
                action()
            }
        }
        runCatching {
            navObserver =
                object : LifecycleEventObserver {
                    override fun onStateChanged(
                        source: LifecycleOwner,
                        event: Lifecycle.Event,
                    ) {
                        if (event == Lifecycle.Event.ON_RESUME) {
                            lifecycle.removeObserver(this)
                            runCatching {
                                if (navController.currentDestination?.id == currentDestinationId) {
                                    runOnce()
                                }
                            }
                        }
                    }
                }
            lifecycle.addObserver(navObserver!!)

            navController.addOnDestinationChangedListener(
                object : NavController.OnDestinationChangedListener {
                    override fun onDestinationChanged(
                        controller: NavController,
                        destination: NavDestination,
                        arguments: Bundle?,
                    ) {
                        if (destination.id != currentDestinationId) {
                            navController.removeOnDestinationChangedListener(this)
                            navObserver?.let { lifecycle.removeObserver(it) }
                        }
                    }
                },
            )

            if (navController.currentDestination?.id == currentDestinationId) {
                runOnce()
            }
        }
    }

    override fun getCurrentDestinationId(): Int = navController.currentDestination?.id ?: 0

    override fun navigateTo(
        actionId: Int,
        bundle: Bundle?,
    ) {
        safeAction { navController.navigate(actionId, bundle, null) }
    }

    override fun navigateTo(
        actionId: Int,
        bundle: Bundle?,
        clearBackStack: Boolean,
    ) {
        val navOptions =
            if (clearBackStack) {
                NavOptions
                    .Builder()
                    .setPopUpTo(navController.graph.startDestinationId, true)
                    .build()
            } else {
                null
            }
        safeAction { navController.navigate(actionId, bundle, navOptions) }
    }

    override fun navigateUp() {
        safeAction { navController.navigateUp() }
    }

    override fun addOnDestinationChangedListener(listener: NavController.OnDestinationChangedListener) {
        navController.addOnDestinationChangedListener(listener)
    }

    override fun removeOnDestinationChangedListener(listener: NavController.OnDestinationChangedListener) {
        navController.removeOnDestinationChangedListener(listener)
    }

    override fun isCameFrom(destinationId: Int): Boolean = navController.previousBackStackEntry?.destination?.id == destinationId

    override fun popBackStack(
        destinationId: Int,
        inclusive: Boolean,
    ) {
        safeAction { navController.popBackStack(destinationId, inclusive) }
    }
}
