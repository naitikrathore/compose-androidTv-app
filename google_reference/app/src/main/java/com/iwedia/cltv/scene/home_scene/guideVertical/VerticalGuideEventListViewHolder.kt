package com.iwedia.cltv.scene.home_scene.guideVertical

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TimeTextView
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.guide.android.widgets.helpers.UtilsLib

/**
 * Vertical guide event list view holder
 *
 * @author Thanvandh Natarajan
 */
class VerticalGuideEventListViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: ConstraintLayout? = null

    //Background view
    var backgroundView: View?= null

    //Name text view
    var nameTextView: TextView? = null

    //Time text view
    var timeTextView: TimeTextView? = null

    //Event separator view
    var separator: View?= null

    //holder height
    var availableHeight = 0

    //single line height name
    var lineHeightName = 0f;

    //single line height time
    var lineHeightTime = 0f;

    //required line count name
    var requiredLineCountName = 0;

    //required line count time
    var requiredLineCountTime = 0;

    //required line count name
    var channelIndex = -1;

    //last bind position
    var bindPosition = -1;

    //playing indicator
    var currentlyPlayingIcon: ImageView?=null

    // Record indicator
    var recordIndicator: ImageView?=null

    // Watchlist indicator
    var watchlistIndicator: ImageView?=null

    // Watchlist indicator
    var indicatorContainer: LinearLayoutCompat?=null

    init {
        //Set references
        rootView = view.findViewById(R.id.guide_event_list_item_root_view)
        backgroundView = view.findViewById(R.id.guide_event_background)
        nameTextView = view.findViewById(R.id.guide_event_name)
        timeTextView = view.findViewById(R.id.guide_event_time_info)
        separator = view.findViewById(R.id.guide_event_separator)
        currentlyPlayingIcon = view.findViewById(R.id.status_icon)
        recordIndicator = view.findViewById(R.id.record_indicator)
        watchlistIndicator = view.findViewById(R.id.watchlist_indicator)
        indicatorContainer = view.findViewById(R.id.indicator_container)

        //Set typeface
        nameTextView!!.typeface =
            TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_medium"))

        //Set typeface
        timeTextView!!.typeface =
            TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_medium"))

        backgroundView!!.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background")))

        val drawableFadingEdgeColorMid = Color.parseColor(ConfigColorManager.getColor("color_text_description").replace("#","#26"))
        separator!!.backgroundTintList = ColorStateList.valueOf(drawableFadingEdgeColorMid)

        clearFocus()
    }

    /**
     * Remove focus from holder
     */
    fun clearFocus() {
        separator!!.visibility = View.VISIBLE
        nameTextView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        timeTextView!!.setTextColor(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_text_description"), 0.8)))
        backgroundView!!.background = ConfigColorManager.generateGuideDrawable()
    }

    /*
     * Show focus on the item inside the list
     */
    fun showFocus() {
        nameTextView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
        timeTextView!!.setTextColor(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_background"), 0.8)))
        backgroundView!!.background = ConfigColorManager.generateGuideFocusDrawable()
    }

    /**
     * Mark as selected current focused item inside the list
     */
    fun staySelected() {
        clearFocus()
        separator?.visibility = View.INVISIBLE
        backgroundView!!.background = ConfigColorManager.generateGuideSelectedDrawable()
    }
}