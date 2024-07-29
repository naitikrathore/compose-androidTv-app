package com.iwedia.cltv.components

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.TimeTextView
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.platform.model.Constants

class RecommendationViewHolder(view : View) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: ConstraintLayout? = null

    //Event images
    var eventImage: ImageView? = null

    //Event name when event has no image
    var titlNoImage : TextView? =  null

    //Event time when event has no image
    var channelName : TextView? = null

    //Event time when event has no image
    var time : TimeTextView? = null

    //Channel lock icon
    var channelLockedIcon: ImageView? = null

    //Channel skip icon
    var channelSkipIcon: ImageView? = null

    //No image channel name
    var channelLogo : ImageView ?= null

    val TAG = javaClass.simpleName
    init {
        //Set references
        rootView = view.findViewById(R.id.launcher_recommendation_item_root_view)
        eventImage = view.findViewById(R.id.event_list_image)
        channelLogo = view.findViewById(R.id.channel_logo)


        var drawable = GradientDrawable()
        val colorStart = Color.parseColor(
            ConfigColorManager.getColor("color_background")
                .replace("#", ConfigColorManager.alfa_fifty_per)
        )
        val colorEnd = Color.parseColor(
            ConfigColorManager.getColor("color_background")
                .replace("#", ConfigColorManager.alfa_zero_per)
        )
        drawable.setShape(GradientDrawable.RECTANGLE)
        drawable.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP)
        drawable.setColors(
            intArrayOf(
                colorStart,
                colorEnd
            )
        )

        channelLogo!!.background = drawable

        titlNoImage = view.findViewById(R.id.title_no_image)
        channelName = view.findViewById(R.id.channel_name)
        time = view.findViewById(R.id.time)
        try {
            channelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: Exception color rdb $ex")
        }

        rootView!!.setBackgroundColor(Color.parseColor((ConfigColorManager.getColor("color_not_selected"))))

        channelLockedIcon = view.findViewById(R.id.channel_list_lock_icon)
        channelLockedIcon?.elevation = 10f
        channelSkipIcon = view.findViewById(R.id.channel_list_skip_icon)
        channelSkipIcon?.elevation = 10f
    }
}