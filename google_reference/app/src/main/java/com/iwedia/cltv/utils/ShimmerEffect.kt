package com.iwedia.cltv.utils

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * Shimmer Effect helper class - creates Shimmer Effect View.
 * @author Boris Tirkajla
 */
sealed class ShimmerEffect(
    private val shimmerConstraintLayout: ConstraintLayout,
    private val parentConstraintLayout: ConstraintLayout,
) {
    private var isCurrentlyShowed = false

    /**
    returns true if ShimmerEffect Element is being shown, otherwise it returns false
     */
    fun isCurrentStateShow() = isCurrentlyShowed

    /**
     * @param onShimmerShow executes right before showing Shimmer. Example: visibility of some Views can be changed here.
     * */
    open fun showShimmer(onShimmerShow: () -> Unit) {
        onShimmerShow()
        isCurrentlyShowed = true
        shimmerConstraintLayout.visibility = View.VISIBLE
    }

    /**
     * @param onShimmerHide executes right before hiding Shimmer. Example: visibility of some Views can be changed here.
     * */
    open fun hideShimmer(onShimmerHide: () -> Unit) {
        onShimmerHide()
        isCurrentlyShowed = false
        shimmerConstraintLayout.visibility = View.INVISIBLE
    }

    protected fun initializeShimmerObject() {
        shimmerConstraintLayout.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        shimmerConstraintLayout.visibility = View.INVISIBLE
        shimmerConstraintLayout.layoutParams.width = parentConstraintLayout.width
        shimmerConstraintLayout.layoutParams.height = parentConstraintLayout.height
        parentConstraintLayout.addView(shimmerConstraintLayout)
        (shimmerConstraintLayout.layoutParams as ConstraintLayout.LayoutParams).apply {
            startToStart = parentConstraintLayout.id
            endToEnd = parentConstraintLayout.id
            bottomToBottom = parentConstraintLayout.id
            topToTop = parentConstraintLayout.id
        }
    }

    /**
     * ShimmerEffectWithOneCall class - creates Shimmer Effect View which can be shown and hidden only ONCE. Initially Shimmer Effect is not shown - in order to be shown, showShimmer() method must be called.
     * @param shimmerId is value that will be stored internally in HashMap and after it will be used to check whether ShimmerEffect Element has already been created with passed shimmerId. If Shimmer Effect Element with passed shimmerId has already been created before, both methods showShimmer() and hideShimmer() won't be executed - function onShimmerShow() and onShimmerHide() passed as their parameters won't be executed. RECOMMENDATION: If ShimmerEffect is bound to a Widget - Widget ID can be used as shimmerId. If ShimmerEffect is bound to Scene - Scene ID can be used as shimmerId.
     * @param shimmerConstraintLayout ConstraintLayout which has ShimmerFrameLayout and within it a View that will be used as placeholder when Shimmer Effect is being shown.
     * @param parentView ConstraintLayout which will be used to host shimmerConstraintLayout.
     * */
    class ShimmerEffectWithOneCall(
        private val shimmerId: Int,
        private val shimmerConstraintLayout: ConstraintLayout,
        private val parentView: ConstraintLayout
    ) : ShimmerEffect(shimmerConstraintLayout, parentView) {
        init {
            if (allShimmersMap[shimmerId] != true) { // if shimmer with shimmerId hasn't been created yet - create it
                initializeShimmerObject()
                allShimmersMap[shimmerId] = true // marks that Shimmer with shimmerId is created
            }
        }

        /**
         * @param onShimmerShow executes right before showing Shimmer but JUST ONCE. Example: visibility of some Views can be changed here.
         * */
        override fun showShimmer(onShimmerShow: () -> Unit) {
            if (shownShimmersMap[shimmerId] != true) { // if shimmer with shimmerId hasn't been shown yet
                super.showShimmer(onShimmerShow)
                shownShimmersMap[shimmerId] = true
            }
        }

        /**
         * @param onShimmerHide executes right before hiding Shimmer but JUST ONCE. Example: visibility of some Views can be changed here.
         * */
        override fun hideShimmer(onShimmerHide: () -> Unit) {
            if (hiddenShimmersMap[shimmerId] != true) { // hide Shimmer only if it hasn't been hidden yet on this instance
                super.hideShimmer(onShimmerHide)
                hiddenShimmersMap[shimmerId] = true
            }
        }

        companion object {
            private val shownShimmersMap = HashMap<Int, Boolean>()
            private val hiddenShimmersMap = HashMap<Int, Boolean>()
            private val allShimmersMap = HashMap<Int, Boolean>()
        }
    }

    /**
     * ShimmerEffectWithMultipleCalls - creates Shimmer Effect View which can be shown and hidden MULTIPLE TIMES. Initially Shimmer Effect is not shown - in order to be shown, showShimmer() method must be called.
     * @param shimmerConstraintLayout ConstraintLayout which has ShimmerFrameLayout and within it a View that will be used as placeholder when Shimmer Effect is being shown.
     * @param parentView ConstraintLayout which will be used to host shimmerConstraintLayout
     * */
    class ShimmerEffectWithMultipleCalls(
        private val shimmerConstraintLayout: ConstraintLayout,
        private val parentView: ConstraintLayout
    ) : ShimmerEffect(shimmerConstraintLayout, parentView) {
        init {
            initializeShimmerObject()
        }
    }
}