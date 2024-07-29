package com.iwedia.cltv.scene.home_scene.guideVertical

import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigFontManager

/**
 * Vertical guide channel list view holder
 *
 * @author Thanvandh Natarajan
 */
class VerticalGuideChannelListViewHolder (view: View) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: ConstraintLayout? = null

    //Channel logo
    var channelLogo: ImageView? = null

    //Channel name
    var channelName: TextView?= null

    //Channel index
    var channelIndex: TextView? = null

    //Channel separator view
    var separatorView: View? = null

    var isSkipped: ImageView? = null

    var isLocked:ImageView?=null

    init {
        //Set references
        rootView = view.findViewById(R.id.guide_channel_list_root_view)
        channelLogo = view.findViewById(R.id.guide_channel_list_logo)
        channelName = view.findViewById(R.id.guide_channel_list_no_logo)
        channelIndex = view.findViewById(R.id.guide_channel_list_index)
        separatorView = view.findViewById(R.id.guide_channel_list_separator_view)
        isLocked = view.findViewById(R.id.guide_is_locked)
        isSkipped = view.findViewById(R.id.guide_is_skipped)

        //Set typeface
        channelIndex!!.typeface =
            TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
        channelName!!.typeface =
            TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_bold"))
    }
}