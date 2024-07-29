package com.iwedia.cltv.scene.home_scene.rail

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.HorizontalGridView
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.custom_card.CustomCard
import com.iwedia.cltv.components.CustomDetails
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.utils.Utils

const val ANIMATION_DURATION = 500L

enum class AnimationScale {
    DEFAULT,
    LARGER
}
/**
 * For you rail item view holder
 *
 * Used to hoist one, main Rail in SearchScene or ForYouWidget. Every single rail have railLabel (On Now, Channel, Watchlist, Recordings), horizontalGridView which hoists events and
 * customDetails which will display event's details such as Title, Description, Progress, Time etc.
 *
 * @author Boris Tirkajla
 */
class RailsViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    //rail label
    var railLabelTextView: TextView? = null

    /**isRailExpanded flag is important for handling when to animate CustomDetails and when no.
     *
     * For example, it is important when user scrolls left and right through rail,
     * if this is not handled with the flag CustomDetails would be animated whenever
     * user changes focus to another Card.
     * */
    private var isRailExpanded = false

    //Events recycler
    var eventRecycler: HorizontalGridView? = null
    private var railItemMainConstraintLayout: ConstraintLayout? = null
    var customDetails: CustomDetails.CustomDetailsSearch

    /**
     * @param railLabelTextViewScale used for handling scale of the RailLabel
     */
    private var railLabelTextViewScale = 1f

    init {
        customDetails = view.findViewById(R.id.custom_details)
        eventRecycler = view.findViewById(R.id.events_recycler)
        railLabelTextView = view.findViewById(R.id.rail_name_text_view)
        railLabelTextView!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular")
        )
        railItemMainConstraintLayout = view.findViewById(R.id.rail_item_main_constraint_layout)
        eventRecycler!!.apply {
            //make focus fixed on th left side of the screen
            itemAlignmentOffset =
                Utils.getContext().resources.getDimensionPixelSize(R.dimen.custom_dim_n10)
            itemAlignmentOffsetPercent = HorizontalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED
            windowAlignmentOffset =
                Utils.getDimensInPixelSize(R.dimen.custom_dim_82) // first item should be offset 80dp from the start edge.
            windowAlignmentOffsetPercent = HorizontalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
            windowAlignment = HorizontalGridView.WINDOW_ALIGN_HIGH_EDGE

            setItemSpacing(Utils.getDimensInPixelSize(CustomCard.CustomCardInfoBanner.SPACE_BETWEEN_ITEMS))
        }
        railLabelTextView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
    }

    /**
     * method used to collapse the CustomDetails when user hasn't navigated to any CustomCard in the corresponding Rail.
     *
     * Example of usage: when user press DPAD_UP or DPAD_DOWN this method is called in order to collapse corresponding Rail (hide CustomDetails from user).
     */
    fun collapseDetailsContainer(railLabelTextColor: String) {
        isRailExpanded = false // enable expanding of DetailsContainer.

        railLabelTextView!!.setTextColor(Color.parseColor(railLabelTextColor))

        animateTextViewScale(AnimationScale.DEFAULT) // when CustomDetails is collapsed rail name have normal/not animated size.

        animateForYouItemConstraintLayoutHeight(R.dimen.custom_dim_170) // RailItem should have height which crops CustomDetails when collapsed.

        customDetails.animateVisibility( // HIDE CustomDetails because Card in RailItem hasn't gained focus yet.
            shouldBeVisible = false, duration = ANIMATION_DURATION
        )
    }

    /**
     * method used to expand the CustomDetails when user navigates to a CustomCard in the corresponding Rail.
     */
    fun expandDetailsContainer(isRailDetailsEnabled: Boolean, railLabelTextColor: String) {
        /*
        The following if statement addresses a critical scenario when a user rapidly scrolls horizontally through rails.
        Its removal could lead to a specific issue: if the user navigates to a particular rail and quickly swipes right,
        the CustomDetails may exhibit a blinking behavior.
        */
        if (isRailExpanded) return // avoid expanding already expanded card.


        railLabelTextView!!.setTextColor(Color.parseColor(railLabelTextColor))

        animateTextViewScale(AnimationScale.LARGER) // when CustomDetails is expanded rail name should appear bigger and with different color.

        isRailExpanded =
            true // used to disable expanding of RailItem until Rail is not collapsed first.

        if (isRailDetailsEnabled) {
            animateForYouItemConstraintLayoutHeight(R.dimen.custom_dim_322_5) // RailItem should have height which DOESN'T crop CustomDetails when expanded.
            customDetails.animateVisibility( // SHOW CustomDetails - CustomCard has focus, so it's details should be visible.
                shouldBeVisible = true, duration = ANIMATION_DURATION
            )
        }
    }

    /**
     * private method used for handing the MainRail's height. This is key method for animating the CustomDetails visibility.
     *
     * This method is used in 2 cases:
     *
    1) when collapsing - height is set to 170dp and CustomDetails is NOT visible
    2) when expanding - height is set to 300dp and CustomDetails is visible
     */
    private fun animateForYouItemConstraintLayoutHeight(customDim: Int) {
        Utils.animateContentHeight(
            railItemMainConstraintLayout!!,
            currentHeight = railItemMainConstraintLayout!!.height,
            newHeight = Utils.getDimensInPixelSize(customDim),
            duration = ANIMATION_DURATION
        )
    }

    /**
     * Animates the scale of the railLabelTextView based on the provided `animationScale`.
     * When the Rail is focused, the rail's name should appear bigger; otherwise, it should have a 'normal' size.
     *
     * @param animationScale The desired animation scale. Use `AnimationScale.NORMAL` for normal size,
     *                       and `AnimationScale.ANIMATED` for a larger, animated size.
     */
    fun animateTextViewScale(animationScale: AnimationScale) {
        // Create a ValueAnimator to smoothly transition between current scale and target scale
        val valueAnimator = ValueAnimator.ofFloat(
            railLabelTextViewScale, // Starting scale
            when (animationScale) {
                AnimationScale.DEFAULT -> 1f // Target scale for normal size
                AnimationScale.LARGER -> 1.2f // Target scale for animated size
            }
        )

        // Set the duration of the animation
        valueAnimator.duration = ANIMATION_DURATION

        // Add an update listener to respond to animation progress
        valueAnimator.addUpdateListener {
            // Update railLabelTextViewScale with the animated value
            railLabelTextViewScale = it.animatedValue as Float

            // Apply the scale to both X and Y axes of the railLabelTextView
            railLabelTextView!!.scaleX = railLabelTextViewScale
            railLabelTextView!!.scaleY = railLabelTextViewScale
        }

        // Start the animation
        valueAnimator.start()
    }
}