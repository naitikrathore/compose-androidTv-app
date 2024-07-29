package com.iwedia.cltv.scene.custom_recording.duration

import world.widget.GWidgetListener

interface DurationSceneWidgetListener : GWidgetListener {
    fun getDurationdata(hours:Int,minutes:Int)
}