package com.iwedia.cltv.platform.t56

import android.content.Context
import com.iwedia.cltv.platform.base.FavoritesInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.favorite.FavoriteItem
import com.iwedia.cltv.platform.model.favorite.FavoriteItemType
import com.iwedia.cltv.platform.t56.provider.PlatformSpecificData
import com.mediatek.opapp.OpAppUtils.getSvl
import com.mediatek.twoworlds.tv.MtkTvChannelList
import com.mediatek.twoworlds.tv.common.MtkTvChCommonBase
import com.mediatek.twoworlds.tv.model.MtkTvChannelInfoBase


internal class FavoritesInterfaceImpl constructor(
    applicationContext: Context,
    channelDataProvider: ChannelDataProviderInterface,
    utilsInterface: UtilsInterface
) : FavoritesInterfaceBaseImpl(applicationContext, channelDataProvider, utilsInterface) {

    private val favMask = intArrayOf(
        MtkTvChCommonBase.SB_VNET_FAVORITE1, MtkTvChCommonBase.SB_VNET_FAVORITE2,
        MtkTvChCommonBase.SB_VNET_FAVORITE3, MtkTvChCommonBase.SB_VNET_FAVORITE4
    )
    private var favCategoryList = arrayListOf(FAVORITE_1, FAVORITE_2, FAVORITE_3, FAVORITE_4)
    private val PREFS_TAG = "LiveTVPrefs"
    private val KEY_PREFERRED_FAVOURITES_SET = "preferred_favourites_set_from_mtk"

    override fun setup() {
        val favSet = context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getBoolean(KEY_PREFERRED_FAVOURITES_SET, false)
        println("FavF "+"first time setup ${favSet}")
        if(!favSet){
            getFavouriteChannelsFromMtkApp()
            context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).edit().putBoolean(KEY_PREFERRED_FAVOURITES_SET, true).apply()
        }
    }

    private val mtkTvChList = MtkTvChannelList.getInstance()

    private fun getFavoriteListByFilter(
        filter: Int, channelId: Int,
        isCurChannel: Boolean, prevCount: Int, nextCount: Int, favtype: Int
    ): List<MtkTvChannelInfoBase>? {
        val chLen: Int = mtkTvChList.getChannelCountByFilter(getSvl(), filter)
        if (chLen <= 0) {
            return null
        }

        return mtkTvChList.getFavoriteListByFilter(
            getSvl(), filter,
            channelId, isCurChannel, prevCount, nextCount, favtype
        )
    }

    private fun getFavouriteChannelCount(filter: Int): Int {
        return mtkTvChList.getChannelCountByFilter(getSvl(), filter)
    }

    private fun getFavouriteChannelsFromMtkApp(){
        for (favIter in 0 until 4) {
            val preNum: Int = getFavouriteChannelCount(favMask[favIter])
            val mfavChannelList: List<MtkTvChannelInfoBase>? = getFavoriteListByFilter(
                favMask[favIter], 0, false, 0, preNum, favIter
            )

            if (mfavChannelList != null) {
                val favList = getChannels(mfavChannelList)
                addListToDb(favList, favIter)
            }
        }
    }

    private fun getChannels(tempApiChList: List<MtkTvChannelInfoBase>): List<TvChannel?> {
        val data: ArrayList<TvChannel> = channelDataProvider.getChannelList()
        var chlist: ArrayList<TvChannel> = ArrayList()
        tempApiChList.forEach {mtkTvChannelInfoBase ->
            kotlin.run data@{
                data.forEach {channel ->
                    //this might need to be added also
        //                if(it.channelId == channel.serviceId){
                    if ((channel.platformSpecific as PlatformSpecificData).internalServiceIndex == mtkTvChannelInfoBase.svlRecId && (channel.platformSpecific as PlatformSpecificData).internalServiceListID == mtkTvChannelInfoBase.svlId) {
                        chlist.add(channel)
                        return@data
                    }
                }
            }
        }
        return chlist
    }

    private fun addListToDb(favList: List<TvChannel?>, i: Int) {
        favList.forEachIndexed { index, item ->
            var favListIds = ArrayList<String>()

            if (item != null) {
                item.favListIds.let { favListIds.addAll(it) }
                favListIds.add(favCategoryList[i])
                val favItem = FavoriteItem(
                    item.id,
                    FavoriteItemType.TV_CHANNEL,
                    item.favListIds,
                    item,
                    favListIds
                )
                dataProvider.addToFavorites(
                    favItem,
                    object : IAsyncCallback {
                        override fun onSuccess() {
                            println("addToFavorites success ${favItem.favListIds}")
                        }
                        override fun onFailed(error: Error) {
                            println("addToFavorites failed $error")
                        }
                    })
            }
        }
    }


}