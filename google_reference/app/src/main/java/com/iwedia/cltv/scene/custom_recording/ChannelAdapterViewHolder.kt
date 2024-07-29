package com.iwedia.cltv.scene.custom_recording

import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R

class ChannelAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)  {
    //Root view


    //Category text
    var channelNum: TextView? = null
    var channelImage : ImageView? = null
    var rootView: RelativeLayout? = null
    var channelName: TextView? = null
    var checkBox:ImageView?=null

    init {


        channelNum = view.findViewById(R.id.channel_num)
        channelImage = view.findViewById(R.id.channel_image)
        rootView = view.findViewById(R.id.channel_item_root_view)
        channelName = view!!.findViewById(R.id.channelname)
        checkBox = view!!.findViewById(R.id.checkboxcr)

    }
}