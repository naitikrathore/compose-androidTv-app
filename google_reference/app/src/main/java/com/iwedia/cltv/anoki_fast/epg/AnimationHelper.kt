package com.iwedia.cltv.anoki_fast.epg

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator


/**
 * Utility class for managing view animations.
 * Provides methods for performing fade-in, fade-out, and combined fade-out/fade-in animations.
 * 
 * @author Boris Tirkajla
 */

class AnimationHelper {
    companion object {

        const val ANIMATION_DURATION = 500L
        /**
         * Applies a fade-out animation to a list of views.
         *
         * This method creates a ValueAnimator to animate the alpha property of the given views, gradually reducing their opacity
         * until they become completely transparent. The duration of the animation is set by the constant `ANIMATION_DURATION`.
         * Once the animation is complete, the provided `onAnimationEnded` lambda function is invoked.
         *
         * @param listOfViews A list of views to be animated.
         * @param onAnimationEnded A lambda function to be executed when the animation ends.
         * @param onAnimationStarted A lambda function to be executed when the animation starts.
         */
        fun fadeOutAnimation(
            @SuppressLint("Recycle") valueAnimator: ValueAnimator = ValueAnimator(),
            listOfViews: List<View>,
            onAnimationStarted: () -> Unit = {},
            onAnimationEnded: () -> Unit = {},
            fromAlpha: Float = listOfViews[0].alpha,
            toAlpha: Float = 0f,
            duration: Long = ANIMATION_DURATION
        ): ValueAnimator {

            listOfViews.forEach {
                it.visibility = View.VISIBLE
            }

            // Set the ValueAnimator's values
            valueAnimator.setFloatValues(fromAlpha, toAlpha)
            valueAnimator.duration = duration
            valueAnimator.interpolator = DecelerateInterpolator()

            valueAnimator.addUpdateListener { animator ->
                val alphaValue = animator.animatedValue as Float
                listOfViews.forEach {
                    it.alpha = alphaValue
                }
            }

            valueAnimator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                    onAnimationStarted.invoke()
                }

                override fun onAnimationEnd(p0: Animator) {
                    listOfViews.forEach {
                        it.visibility = View.INVISIBLE
                    }
                    onAnimationEnded.invoke()
                }

                override fun onAnimationCancel(p0: Animator) {
                }

                override fun onAnimationRepeat(p0: Animator) {
                }
            })

            valueAnimator.start()
            return valueAnimator
        }

        /**
         * Applies a fade-out animation to a single view.
         *
         * This method uses the `fadeOutAnimation` function to achieve a fade-out effect for a given view. Optional lambda functions
         * for animation start and end events can be provided.
         *
         * @param view The view to be animated.
         * @param onAnimationStarted A lambda function to be executed when the animation starts.
         * @param onAnimationEnded A lambda function to be executed when the animation ends.
         */
        fun fadeOutAnimation(
            view: View, onAnimationStarted: () -> Unit = {},
            onAnimationEnded: () -> Unit = {},
            fromAlpha: Float = view.alpha,
            toAlpha: Float = 0f,
            duration: Long = ANIMATION_DURATION
        ): ValueAnimator {
            return fadeOutAnimation(
                listOfViews = listOf(view),
                onAnimationStarted = onAnimationStarted,
                onAnimationEnded = onAnimationEnded,
                fromAlpha = fromAlpha,
                toAlpha = toAlpha,
                duration = duration
            )
        }

        /**
         * Applies a fade-in animation to a list of views.
         *
         * This method creates a ValueAnimator to animate the alpha property of the given views, gradually increasing their opacity
         * until they are fully visible. The duration of the animation is set by the constant `ANIMATION_DURATION`. Optional
         * lambda functions for animation start and end events can be provided.
         *
         * @param listOfViews A list of views to be animated.
         * @param onAnimationStarted A lambda function to be executed when the animation starts.
         * @param onAnimationEnded A lambda function to be executed when the animation ends.
         */
        fun fadeInAnimation(
            listOfViews: List<View>,
            onAnimationStarted: () -> Unit = {},
            onAnimationEnded: () -> Unit = {},
            fromAlpha: Float = listOfViews[0].alpha,
            toAlpha: Float = 1f,
            duration: Long = ANIMATION_DURATION
        ): ValueAnimator {

            listOfViews.forEach {
                it.visibility = View.VISIBLE
            }

            // Set the ValueAnimator's values
            val alphaAnimator = ValueAnimator.ofFloat(fromAlpha, toAlpha)
            alphaAnimator.duration = duration
            alphaAnimator.interpolator = AccelerateInterpolator()

            alphaAnimator.addUpdateListener { animator ->
                val alphaValue = animator.animatedValue as Float
                listOfViews.forEach {
                    it.alpha = alphaValue
                }
            }

            alphaAnimator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                    onAnimationStarted.invoke()
                }

                override fun onAnimationEnd(p0: Animator) {
                    onAnimationEnded.invoke()
                }

                override fun onAnimationCancel(p0: Animator) {
                }

                override fun onAnimationRepeat(p0: Animator) {
                }
            })

            alphaAnimator.start()
            return alphaAnimator
        }

        /**
         * Applies a fade-in animation to a single view.
         *
         * This method uses the `fadeOutAnimation` function to achieve a fade-in effect for a given view. Optional lambda functions
         * for animation start and end events can be provided.
         *
         * @param view The view to be animated.
         * @param onAnimationStarted A lambda function to be executed when the animation starts.
         * @param onAnimationEnded A lambda function to be executed when the animation ends.
         */
        fun fadeInAnimation(
            view: View,
            onAnimationStarted: () -> Unit = {},
            onAnimationEnded: () -> Unit = {},
            fromAlpha: Float = view.alpha,
            toAlpha: Float = 1f,
            duration: Long = ANIMATION_DURATION
        ): ValueAnimator {
            return fadeInAnimation(
                listOfViews = listOf(view),
                onAnimationStarted = onAnimationStarted,
                onAnimationEnded = onAnimationEnded,
                fromAlpha = fromAlpha,
                toAlpha = toAlpha,
                duration = duration
            )
        }


        /**
         * Initiates a fade-out followed by a fade-in animation sequence on a list of views.
         *
         * @param listOfViews List of views to apply the animation to.
         * @param onAnimationStarted Callback executed when the animation starts.
         * @param onAnimationEnded Callback executed when the entire animation sequence ends.
         * @param onBetweenAnimation Callback executed after the fade-out and before the fade-in animation.
         * @param fromAlpha Initial alpha value for the animation (default is the alpha value of the first view in the list).
         * @param toAlpha Final alpha value for the animation (default is 1f, fully opaque).
         * @return ValueAnimator used for the animation, which can be canceled externally if desired.
         *
         * This method combines a fade-out animation followed by a fade-in animation. It provides hooks for
         * custom behavior at various stages of the animation sequence.
         *
         * The [onBetweenAnimation] callback is invoked after the fade-out animation completes and before
         * the fade-in animation begins. This allows for custom actions or updates to occur during this intermediate stage.
         *
         */
        fun fadeOutFadeIn(
            listOfViews: List<View>,
            onAnimationStarted: () -> Unit = {},
            onAnimationEnded: () -> Unit = {},
            onBetweenAnimation: () -> Unit = {},
            fromAlpha: Float = listOfViews[0].alpha,
            toAlpha: Float = 1f,
            duration: Long = ANIMATION_DURATION
        ): ValueAnimator {
            val valueAnimator = ValueAnimator()
            fadeOutAnimation(
                valueAnimator = valueAnimator,
                listOfViews = listOfViews,
                onAnimationStarted = onAnimationStarted,
                onAnimationEnded = {
                    onBetweenAnimation.invoke()
                    fadeInAnimation(
                        listOfViews = listOfViews,
                        onAnimationEnded = onAnimationEnded,
                        duration = duration
                    )
                },
                fromAlpha = fromAlpha,
                toAlpha = toAlpha,
                duration = duration
            )
            return valueAnimator
        }

    }
}

enum class Rotation {
    TO_START,
    TO_END
}

fun View.rotate(rotation: Rotation) {
    ObjectAnimator.ofFloat(
        /* target = */ this,
        /* propertyName = */ "rotation",
        /* ...values = (FROM THE VALUE) */ this.rotation,
        /* ...values = (TO THE VALUE) */when (rotation) {
            Rotation.TO_START -> 0f
            Rotation.TO_END -> -180f
        }
    ).apply {
        duration = AnimationHelper.ANIMATION_DURATION
        start()
    }
}