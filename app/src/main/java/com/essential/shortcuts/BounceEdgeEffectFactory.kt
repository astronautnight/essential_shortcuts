package com.essential.shortcuts

import android.graphics.Canvas
import android.widget.EdgeEffect
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView
import java.util.WeakHashMap

/**
 * iOS-style smooth elastic overscroll for RecyclerView.
 */
class BounceEdgeEffectFactory : RecyclerView.EdgeEffectFactory() {

    companion object {
        private val animMap = WeakHashMap<RecyclerView, SpringAnimation>()

        private fun getSpring(view: RecyclerView): SpringAnimation {
            return animMap.getOrPut(view) {
                SpringAnimation(view, SpringAnimation.TRANSLATION_Y).apply {
                    spring = SpringForce(0f).apply {
                        dampingRatio = 0.75f // tighter, more controlled (closer to iOS)
                        stiffness = SpringForce.STIFFNESS_VERY_LOW
                    }
                }
            }
        }
    }

    override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
        return object : EdgeEffect(view.context) {

            override fun onPull(deltaDistance: Float) {
                handlePull(deltaDistance)
            }

            override fun onPull(deltaDistance: Float, displacement: Float) {
                handlePull(deltaDistance)
            }

            private fun handlePull(deltaDistance: Float) {
                val sign = if (direction == DIRECTION_BOTTOM) -1 else 1

                // Slightly heavier drag (more iOS-like resistance)
                val translation = sign * view.height * deltaDistance * 0.3f

                val anim = getSpring(view)
                anim.cancel()

                view.translationY += translation
            }

            override fun onRelease() {
                if (view.translationY != 0f) {
                    getSpring(view).start()
                }
            }

            override fun onAbsorb(velocity: Int) {
                val sign = if (direction == DIRECTION_BOTTOM) -1 else 1

                // Softer fling response
                val velocityY = sign * velocity * 0.35f

                val anim = getSpring(view)
                anim.cancel()
                anim.setStartVelocity(velocityY).start()
            }

            override fun draw(canvas: Canvas?): Boolean = false

            override fun isFinished(): Boolean {
                val anim = animMap[view]
                return anim == null || (!anim.isRunning && view.translationY == 0f)
            }
        }
    }
}