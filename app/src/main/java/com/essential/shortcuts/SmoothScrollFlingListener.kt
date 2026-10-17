package com.essential.shortcuts

import androidx.recyclerview.widget.RecyclerView

/**
 * A custom OnFlingListener for RecyclerView that creates a heavier, more iOS-like scroll friction.
 */
class SmoothScrollFlingListener(private val recyclerView: RecyclerView) : RecyclerView.OnFlingListener() {

    override fun onFling(velocityX: Int, velocityY: Int): Boolean {
        // Temporarily detach listener to prevent infinite recursion
        recyclerView.onFlingListener = null

        // iOS scrolling feels "heavier" and deceleration is smoother.
        // Dampen the fling velocity manually.
        val frictionMultiplier = 0.7f

        val newVelX = (velocityX * frictionMultiplier).toInt()
        val newVelY = (velocityY * frictionMultiplier).toInt()

        // Perform the fling with reduced velocity
        recyclerView.fling(newVelX, newVelY)

        // Reattach listener
        recyclerView.onFlingListener = this

        return true // We handled the fling
    }
}
