package com.iwedia.cltv.scene.custom_recording.channel


import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.components.CategoryAdapter
import world.widget.GWidgetListener

interface ChannelSceneWidgetListener : GWidgetListener {
    //    fun getoptionsinstance(): VerticalGridView
//    fun getTvChannel(position: Int)
    fun setFocusPosition(position:Int)
    fun getFocusPosition():Int
    fun onChannelItemClicked(position: Int)
    fun onLeftClicked()

}