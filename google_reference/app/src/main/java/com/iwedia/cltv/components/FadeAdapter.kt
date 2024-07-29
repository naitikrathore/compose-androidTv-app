package com.iwedia.cltv.components

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bosphere.fadingedgelayout.FadingEdgeLayout

/**
 *
 * FadeAdapter is an abstract class used for automatically handling edge fading of GridView.
 *
 * Whenever having adapter which inherits RecyclerView.Adapter<>() - it should inherit
 * FadeAdapter which inherits RecyclerView.Adapter<>().
 *
 * This enables having one step between our adapters and RecyclerView.Adapter<>() where logic for
 * automatic fade effect is enabled overriding some of the Adapters methods.
 *
 * When implementing this functionality in app there are 2 fundamental steps to be taken:
 *
 *  1) few steps when inheriting this class should be followed. This step is explained in details in FadeAdapterExample class.
 *  2) another important thing is to create FadingEdgeLayout in corresponding xml file as following:
 *
 *
 *              <com.bosphere.fadingedgelayout.FadingEdgeLayout
 *                 android:id="@+id/fading_edge_layout"
 *                 android:layout_width="wrap_content"
 *                 android:layout_height="wrap_content"
 *                 app:fel_edge="bottom|top"
 *                 app:fel_size_bottom="@dimen/custom_dim_200"
 *                 app:fel_size_top="@dimen/custom_dim_200">
 *
 *                 <androidx.leanback.widget.VerticalGridView
 *                     android:id="@+id/side_view_vertical_grid_view"
 *                     android:clipToPadding="false"
 *                     android:layout_marginEnd="@dimen/custom_dim_66"
 *                     android:layout_width="match_parent"
 *                     android:gravity="end"
 *                     android:layout_height="match_parent"/>
 *
 *             </com.bosphere.fadingedgelayout.FadingEdgeLayout>
 *
 * For additional information about FadingEdgeLayout visit: https://github.com/bosphere/Android-FadingEdgeLayout
 * @author Boris Tirkajla
 */
abstract class FadeAdapter<T : RecyclerView.ViewHolder>() : RecyclerView.Adapter<T>() {
    protected var fadingEdgeLayout: FadingEdgeLayout?= null
    protected var fadeAdapterType: FadeAdapterType? =null
    constructor(fadingEdgeLayout: FadingEdgeLayout?, fadeAdapterType: FadeAdapterType) : this() {
        this.fadingEdgeLayout = fadingEdgeLayout
        this.fadeAdapterType = fadeAdapterType
    }

    /**
     * itemsCount is variable used to store information about total items in adapter.
     *
     * this is essential property of the BaseFadeAdapter and it is extremely important to follow
     * steps in following sample in FadeAdapterExample class.
     *
     * @sample FadeAdapterExample
     */
    abstract val itemsCount: Int

    private var isLeftFadeEnabled: Boolean = true
    private var isRightFadeEnabled: Boolean = true
    private var isTopFadeEnabled: Boolean = true
    private var isBottomFadeEnabled: Boolean = true

    override fun onViewDetachedFromWindow(holder: T) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position == 0) { // when first item is detached add top or left fade
            when (fadeAdapterType!!) {
                FadeAdapterType.VERTICAL -> addTopFade()
                FadeAdapterType.HORIZONTAL -> addLeftFade()
            }
        }
        if (position == itemsCount - 1) { // when last item is detached add bottom fade
            when (fadeAdapterType!!) {
                FadeAdapterType.VERTICAL -> addBottomFade()
                FadeAdapterType.HORIZONTAL -> addRightFade()
            }
        }
    }


    override fun onViewAttachedToWindow(holder: T) {
        val position = holder.bindingAdapterPosition
        if (position == 0) { // when first item is attached remove top fade
            when (fadeAdapterType!!) {
                FadeAdapterType.VERTICAL -> removeTopFade()
                FadeAdapterType.HORIZONTAL -> removeLeftFade()
            }
        }
        if (position == itemsCount - 1) { // when last item is attached remove bottom fade
            when (fadeAdapterType!!) {
                FadeAdapterType.VERTICAL -> removeBottomFade()
                FadeAdapterType.HORIZONTAL -> removeRightFade()
            }
        }
    }

    /**
     * callback used for ADDING LEFT FADE in HorizontalGridView when first item is not visible any more.
     */
    private fun addLeftFade() {
        isLeftFadeEnabled = true
        handleHorizontalFade()
    }

    /**
     * callback used for REMOVING LEFT FADE in HorizontalGridView when first item item starts being visible.
     */
    private fun removeLeftFade() {
        isLeftFadeEnabled = false
        handleHorizontalFade()
    }

    /**
     * callback used for ADDING RIGHT FADE in HorizontalGridView when last item is not visible any more.
     */
    private fun addRightFade() {
        isRightFadeEnabled = true
        handleHorizontalFade()
    }

    /**
     * callback used for REMOVING RIGHT FADE in HorizontalGridView when last item starts being visible.
     */
    private fun removeRightFade() {
        isRightFadeEnabled = false
        handleHorizontalFade()
    }

    /**
     * callback used for ADDING TOP FADE in VerticalGridView when first item is not visible any more.
     */
    private fun addTopFade() {
        isTopFadeEnabled = true
        fadingEdgeLayout?.setFadeEdges(
            isTopFadeEnabled, false, isBottomFadeEnabled, false
        )
    }

    /**
     * callback used for REMOVING TOP FADE in VerticalGridView when first item item starts being visible.
     */
    private fun removeTopFade() {
        isTopFadeEnabled = false
        fadingEdgeLayout?.setFadeEdges(
            isTopFadeEnabled, false, isBottomFadeEnabled, false
        )
    }

    /**
     * callback used for ADDING BOTTOM FADE in VerticalGridView when last item is not visible any more.
     */
    private fun addBottomFade() {
        isBottomFadeEnabled = true
        fadingEdgeLayout?.setFadeEdges(
            isTopFadeEnabled, false, isBottomFadeEnabled, false
        )
    }

    /**
     * callback used for REMOVING BOTTOM FADE in VerticalGridView when last item starts being visible.
     */
    private fun removeBottomFade() {
        isBottomFadeEnabled = false
        fadingEdgeLayout?.setFadeEdges(
            isTopFadeEnabled, false, isBottomFadeEnabled, false
        )
    }

    private fun handleVerticalFade() {
        fadingEdgeLayout?.setFadeEdges(
            isTopFadeEnabled, false, isBottomFadeEnabled, false
        )

    }

    private fun handleHorizontalFade() {
        fadingEdgeLayout?.setFadeEdges(
            false, isLeftFadeEnabled, false, isRightFadeEnabled
        )
    }

    enum class FadeAdapterType {
        VERTICAL, HORIZONTAL
    }

}


/**
 * Following class is an example how to properly
 *
 * Note: This function is for documentation purposes only and should not be used in the code.
 */
class FadeAdapterExample<T : RecyclerView.ViewHolder>(fadingEdgeLayout: FadingEdgeLayout? = null) :
    FadeAdapter<T>(fadingEdgeLayout, FadeAdapterType.VERTICAL) {

    // Every adapter should have variable that is some kind of list which holds all items needed to be shown through adapter.
    private var items = mutableListOf<String>()

    override var itemsCount: Int
        get() = items.size // THIS IS CRUCIAL. itemsCount MUST HAVE getter which will return total number of items related to the adapter.
        set(value) {}
    

    // Code below is not relevant for example, those are 3 methods that must be overridden when using Adapters
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T {
        TODO("Not yet implemented") // this is not relevant for this example
    }

    override fun onBindViewHolder(holder: T, position: Int) {
        TODO("Not yet implemented") // this is not relevant for this example
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented") // this is not relevant for this example
    }
}