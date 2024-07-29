package com.iwedia.cltv.platform.`interface`

import android.content.Context
import android.media.tv.TvView
import android.net.Uri

interface FactoryModeInterface {

    fun hasSignal(): Boolean

    fun restoreEdidVersion()

    fun tuneByChannelNameOrNum(
        channelName: String,
        isName: Boolean,
        tvView: TvView?
    ): Int

    fun tuneByUri(uri: Uri, context: Context, tvView: TvView?): String

    fun getVendorMtkAutoTest(): Int
    fun getFirstChannelAndTune(context: Context, liveTvView: TvView?)

    fun tuneToActiveChannel(tvView: TvView?): String
    fun loadChannels(context: Context)
    fun channelUpOrDown(isUp: Boolean,liveTvView: TvView?):String
    fun getChannelDisplayName():String
    fun deleteInstance()
}