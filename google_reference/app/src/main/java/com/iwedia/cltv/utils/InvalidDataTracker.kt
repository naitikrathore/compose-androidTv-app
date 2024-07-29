package com.iwedia.cltv.utils

import com.iwedia.cltv.platform.model.recording.Recording

object InvalidDataTracker {

    var invalidChannelLogosNew: ArrayList<com.iwedia.cltv.platform.model.TvChannel> = arrayListOf()
    var invalidTvEventImagesNew: ArrayList<com.iwedia.cltv.platform.model.TvEvent> = arrayListOf()
    var invalidRecordingItemImagesNew: ArrayList<Recording> = arrayListOf()

    fun hasValidData(item: Any): Boolean {
        if (item is com.iwedia.cltv.platform.model.TvChannel) {
            invalidChannelLogosNew.forEach { existingItem ->
                if (existingItem.id == (item as com.iwedia.cltv.platform.model.TvChannel).id) {
                    return false
                }
            }
            return true
        }
        if (item is com.iwedia.cltv.platform.model.TvEvent) {
            invalidTvEventImagesNew.forEach { existingItem ->
                if (existingItem.id == (item as com.iwedia.cltv.platform.model.TvEvent).id) {
                    return false
                }
            }
            return true
        }
        if (item is Recording) {
            invalidRecordingItemImagesNew.forEach { existingItem ->
                if (existingItem.id == (item as Recording).id) {
                    return false
                }
            }
            return true
        }
        return false
    }

    fun setValidData(item: Any) {
        if (item is com.iwedia.cltv.platform.model.TvChannel) {
            invalidChannelLogosNew.remove(item)
        }
        if (item is com.iwedia.cltv.platform.model.TvEvent) {
            invalidTvEventImagesNew.remove(item)
        }
        if (item is Recording) {
            invalidRecordingItemImagesNew.remove(item as Recording)
        }
    }

    fun setInvalidData(item: Any) {


        if (item is com.iwedia.cltv.platform.model.TvChannel) {
            invalidChannelLogosNew.add(item)
        }
        if (item is com.iwedia.cltv.platform.model.TvEvent) {
            invalidTvEventImagesNew.add(item)
        }
        if (item is Recording) {
            invalidRecordingItemImagesNew.add(item)
        }
    }

    fun dispose() {
        invalidChannelLogosNew.clear()
        invalidTvEventImagesNew.clear()
        invalidRecordingItemImagesNew.clear()
    }

}