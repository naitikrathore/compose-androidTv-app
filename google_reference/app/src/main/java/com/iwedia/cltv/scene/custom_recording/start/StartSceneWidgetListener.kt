package com.iwedia.cltv.scene.custom_recording.start

import world.widget.GWidgetListener

interface StartSceneWidgetListener : GWidgetListener {
    fun gettimedata(hr:Int,min:Int,day:Int,month:Int,year:Int)
    fun getCurrentTime(): Long
}