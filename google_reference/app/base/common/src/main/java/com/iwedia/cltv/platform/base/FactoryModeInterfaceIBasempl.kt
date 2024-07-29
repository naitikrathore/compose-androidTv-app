package com.iwedia.cltv.platform.base

import android.content.Context
import android.media.tv.TvView
import android.net.Uri
import com.iwedia.cltv.platform.`interface`.FactoryModeInterface

open class FactoryModeInterfaceIBasempl : FactoryModeInterface {

    override fun hasSignal(): Boolean {
        return false
    }

    override fun restoreEdidVersion() {
    }

    override fun tuneByChannelNameOrNum(
        channelName: String,
        isName: Boolean,
        tvView: TvView?
    ): Int {
       return 0
    }

    override fun tuneByUri(uri: Uri, context: Context, tvView: TvView?): String {
        return ""
    }

    override fun getVendorMtkAutoTest(): Int {
        return 0
    }

    override fun getFirstChannelAndTune(context: Context, liveTvView: TvView?) {
    }

    override fun tuneToActiveChannel(tvView: TvView?): String {
        return ""
    }

    override fun loadChannels(context: Context) {
    }

    override fun channelUpOrDown(isUp: Boolean, liveTvView: TvView?): String {
        return ""
    }

    override fun getChannelDisplayName(): String {
        return ""
    }

    override fun deleteInstance() {
    }
}