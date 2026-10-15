package com.example.voicelock.base.navigator

import android.os.Bundle
import androidx.navigation.NavController

interface Navigator {
    fun getCurrentDestinationId(): Int

    /**
     * Navigate to a specific route with optional arguments
     */
    fun navigateTo(
        actionId: Int,
        bundle: Bundle? = null,
    )

    /**
     * Navigate to a specific route and optionally clear back stack
     */
    fun navigateTo(
        actionId: Int,
        bundle: Bundle? = null,
        clearBackStack: Boolean = false,
    )

    /**
     * Navigate back to previous screen
     */
    fun navigateUp()

    /**
     * Add listener for navigation events
     */
    fun addOnDestinationChangedListener(listener: NavController.OnDestinationChangedListener)

    /**
     * Remove listener for navigation events
     */
    fun removeOnDestinationChangedListener(listener: NavController.OnDestinationChangedListener)

    /**
     * Check if current screen was navigated from specific destination
     */
    fun isCameFrom(destinationId: Int): Boolean

    /**
     * Pop back stack to specific destination
     */
    fun popBackStack(
        destinationId: Int,
        inclusive: Boolean,
    )
}
