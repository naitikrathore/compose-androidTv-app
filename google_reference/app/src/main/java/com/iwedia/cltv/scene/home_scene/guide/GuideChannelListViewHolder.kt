package com.iwedia.cltv.scene.home_scene.guide

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.platform.model.TvChannel

/**
 * Guide channel list view holder
 *
 * @author Dejan Nadj
 */
class GuideChannelListViewHolder (view: View) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: ConstraintLayout? = null

    //Channel logo
    var channelLogo: ImageView? = null

    //Channel name
    var channelName: TextView?= null

    //Channel index
    var channelIndex: TextView? = null

    var tvChannel: TvChannel? = null

    //source type
    var channelSource : TextView? =null

    //Channel separator view
    var separatorView: View? = null
    var isSkipped: ImageView? = null
    var isLocked:ImageView?=null
    var isScrambled: ImageView? = null

    init {
        //Set references
        isLocked = view!!.findViewById(R.id.guide_is_locked)
        isLocked!!.visibility=View.GONE
        isSkipped = view!!.findViewById(R.id.guide_is_skipped)
        isSkipped!!.visibility=View.GONE
        rootView = view.findViewById(R.id.guide_channel_list_root_view)
        channelLogo = view.findViewById(R.id.guide_channel_list_logo)
        channelName = view.findViewById(R.id.guide_channel_list_no_logo)
        channelIndex = view.findViewById(R.id.guide_channel_list_index)
        channelSource = view.findViewById(R.id.guide_channel_source_type)
        separatorView = view.findViewById(R.id.guide_channel_list_separator_view)
        isScrambled = view.findViewById(R.id.guide_is_scrambled)

        //Set typeface
        channelIndex!!.typeface =
            TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
        channelSource!!.typeface =
            TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
        channelName!!.typeface =
            TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_bold"))
    }
}