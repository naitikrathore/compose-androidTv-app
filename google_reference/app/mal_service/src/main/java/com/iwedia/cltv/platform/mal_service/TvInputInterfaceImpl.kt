package com.iwedia.cltv.platform.mal_service

import android.content.Context
import android.media.tv.ContentRatingSystem
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.TvInputInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvEvent

class TvInputInterfaceImpl(private val context: Context, private val serviceImpl: IServiceAPI) :
    TvInputInterface {
    override fun getTvInputManager(): TvInputManager {
        return context!!.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
    }

    override fun getTvInputList(callback: IAsyncDataCallback<ArrayList<TvInputInfo>>) {
        var result = arrayListOf<TvInputInfo>()
        result.addAll(serviceImpl.tvInputList)
        callback.onReceive(result)
    }

    override fun getTvInputFilteredList(
        filter: String,
        callback: IAsyncDataCallback<ArrayList<TvInputInfo>>
    ) {
        var result = arrayListOf<TvInputInfo>()
        result.addAll(serviceImpl.getTvInputFilteredList(filter))
        callback.onReceive(result)
    }

    override fun startSetupActivity(input: TvInputInfo, callback: IAsyncCallback) {
        serviceImpl.startSetupActivity(input)
        callback.onSuccess()
    }

    override fun triggerScanCallback(isSuccessful: Boolean) {
        serviceImpl.triggerScanCallback(isSuccessful)
    }

    override fun getChannelCountForInput(input: TvInputInfo, callback: IAsyncDataCallback<Int>) {
        val res = serviceImpl.getChannelCountForInput(input)
        callback.onReceive(res)
    }

    override fun isParentalEnabled(): Boolean {
        return serviceImpl.isParentalEnabled
    }

    override fun getContentRatingSystems(): List<ContentRatingSystem> {
        var result = arrayListOf<ContentRatingSystem>()
        serviceImpl.contentRatingSystems.forEach {
            result.add(fromServiceContentRatingSystem(it))
        }
        return result
    }

    override fun getContentRatingSystemsList(): MutableList<ContentRatingSystem> {
        var retList = arrayListOf<ContentRatingSystem>()
        serviceImpl.contentRatingSystemsList.forEach {
            retList.add(fromServiceContentRatingSystem(it))
        }
        return retList
    }

    override fun getContentRatingSystemDisplayName(contentRatingSystem: ContentRatingSystem): String {
        return serviceImpl.getContentRatingSystemDisplayName(
            toServiceContentRatingSystem(contentRatingSystem)
        )
    }

    override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
        return serviceImpl.getParentalRatingDisplayName(parentalRating, toServiceTvEvent(tvEvent))
    }

}