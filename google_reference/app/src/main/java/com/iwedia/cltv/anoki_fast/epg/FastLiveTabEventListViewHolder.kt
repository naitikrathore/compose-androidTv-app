package com.iwedia.cltv.anoki_fast.epg

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
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
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.Utils

/**
 * Guide event list view holder
 *
 * @author Dejan Nadj
 */
class FastLiveTabEventListViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val TAG = javaClass.simpleName
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

    //playing indicator
     var currentlyPlayingIcon: ImageView?=null

    val lockImageView: ImageView

    // Record indicator
    var recordIndicator: ImageView?=null

    // Watchlist indicator
    var watchlistIndicator: ImageView?=null

    // Watchlist indicator
    var indicatorContainer: LinearLayoutCompat?=null

    //to check recyclerview pool
    var channelIndex = -1

    init {

        //Set references
        rootView = view.findViewById(R.id.guide_event_list_item_root_view)
        backgroundView = view.findViewById(R.id.guide_event_background)
        nameTextView = view.findViewById(R.id.guide_event_name)
        timeTextView = view.findViewById(R.id.guide_event_time_info)
        separator = view.findViewById(R.id.guide_event_separator)
        currentlyPlayingIcon = view.findViewById(R.id.status_icon)
        lockImageView = view.findViewById(R.id.lock_image_view)
        recordIndicator = view.findViewById(R.id.record_indicator)
        watchlistIndicator = view.findViewById(R.id.watchlist_indicator)
        indicatorContainer = view.findViewById(R.id.indicator_container)
        val drawableFadingEdgeColorMid = Color.parseColor(ConfigColorManager.getColor("color_text_description").replace("#","#26"))
        separator!!.backgroundTintList = ColorStateList.valueOf(drawableFadingEdgeColorMid)

        //Set typeface
        nameTextView!!.typeface =
            TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))

        //Set typeface
        timeTextView!!.typeface =
            TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))

        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: Exception color_context $color_context")
            backgroundView!!.setBackgroundColor(color_context)
        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: Exception color rdb $ex")
        }
        nameTextView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        var layoutParams = LinearLayoutCompat.LayoutParams(Utils.getDimensInPixelSize(R.dimen.custom_dim_30), Utils.getDimensInPixelSize(R.dimen.custom_dim_30))
        layoutParams.topMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_12)
        layoutParams.marginStart = Utils.getDimensInPixelSize(R.dimen.custom_dim_5)
        layoutParams.marginEnd = Utils.getDimensInPixelSize(R.dimen.custom_dim_10)
        currentlyPlayingIcon!!.layoutParams = layoutParams
        currentlyPlayingIcon!!.setImageResource(0)
        currentlyPlayingIcon!!.setImageBitmap(null)
        currentlyPlayingIcon!!.setImageResource(R.drawable.ic_fast_play_icon)
        currentlyPlayingIcon!!.setPadding(0, -4, 0, 0)
    }

    fun setParentalRestriction() {
        if (currentlyPlayingIcon!!.visibility == View.VISIBLE) {
            var layoutParams = LinearLayoutCompat.LayoutParams(Utils.getDimensInPixelSize(R.dimen.custom_dim_20), Utils.getDimensInPixelSize(R.dimen.custom_dim_20))
            layoutParams.topMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_2)
            layoutParams.marginStart = Utils.getDimensInPixelSize(R.dimen.custom_dim_3)
            layoutParams.marginEnd = Utils.getDimensInPixelSize(R.dimen.custom_dim_10)
            currentlyPlayingIcon!!.layoutParams = layoutParams
            nameTextView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        }
        nameTextView!!.text = ConfigStringsManager.getStringById("parental_control_restriction")
        lockImageView.visibility = View.VISIBLE
//        nameTextView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_progress")))
    }
}
