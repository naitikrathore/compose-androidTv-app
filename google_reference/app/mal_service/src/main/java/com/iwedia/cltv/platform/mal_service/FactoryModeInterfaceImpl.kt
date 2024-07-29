package com.iwedia.cltv.platform.mal_service

import android.content.Context
import android.media.tv.TvView
import android.net.Uri
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.entities.ServiceTvView
import com.iwedia.cltv.platform.`interface`.FactoryModeInterface

class FactoryModeInterfaceImpl(private val serviceImpl: IServiceAPI) : FactoryModeInterface {
    override fun hasSignal(): Boolean {
        return serviceImpl.hasSignal()
    }

    override fun restoreEdidVersion() {
        serviceImpl.restoreEdidVersion()
    }

    override fun tuneByChannelNameOrNum(
        channelName: String,
        isName: Boolean,
        tvView: TvView?
    ): Int {
        return serviceImpl.tuneByChannelNameOrNum(
            channelName,
            isName,
            tvView as ServiceTvView
        )
    }

    override fun tuneByUri(uri: Uri, context: Context, tvView: TvView?): String {
        return serviceImpl.tuneByUri(uri, tvView as ServiceTvView)
    }

    override fun getVendorMtkAutoTest(): Int {
        return serviceImpl.vendorMtkAutoTest
    }

    override fun getFirstChannelAndTune(context: Context, liveTvView: TvView?) {
        serviceImpl.getFirstChannelAndTune(liveTvView as ServiceTvView)
    }

    override fun tuneToActiveChannel(tvView: TvView?): String {
        return serviceImpl.tuneToActiveChannel(tvView as ServiceTvView)
    }

    override fun loadChannels(context: Context) {
        serviceImpl.loadChannels()
    }

    override fun channelUpOrDown(isUp: Boolean, liveTvView: TvView?): String {
        return serviceImpl.channelUpOrDown(isUp, liveTvView as ServiceTvView)
    }

    override fun getChannelDisplayName(): String {
        return serviceImpl.channelDisplayName
    }

    override fun deleteInstance() {
        serviceImpl.deleteInstance()
    }

}