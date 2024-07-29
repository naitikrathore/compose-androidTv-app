package com.iwedia.cltv.scene.custom_recording.repeat

import world.widget.GWidgetListener

interface RepeatSceneWidgetListener : GWidgetListener {
    fun repeatdata(repeat:Int)
//    0-None
//    1-Daily
//    2-Weekly
}