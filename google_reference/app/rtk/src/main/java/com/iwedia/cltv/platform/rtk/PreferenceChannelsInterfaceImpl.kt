package com.iwedia.cltv.platform.rtk

import android.content.ContentValues
import android.content.Context
import android.media.tv.TvContract
import com.iwedia.cltv.platform.`interface`.PreferenceChannelsInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.TvChannel
import java.util.ArrayList

class PreferenceChannelsInterfaceImpl(private var context: Context, tvModule: TvInterface) : PreferenceChannelsInterface {

    private var mChannelList: ArrayList<TvChannel> = tvModule.getChannelList()

    override fun swapChannel(
        firstChannel: TvChannel,
        secondChannel: TvChannel,
        previousPosition: Int,
        newPosition: Int
    ): Boolean {

        var ret1 = 0
        var ret2 = 0

        mChannelList?.forEach { item ->
            if (item.id == firstChannel.id) {
                val values = ContentValues()
                values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, secondChannel.displayNumber)
                values.put(TvContract.Channels.COLUMN_BROWSABLE, secondChannel.isBrowsable)
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1, secondChannel.providerFlag1)
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, secondChannel.providerFlag2)
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG3, secondChannel.providerFlag3)
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4, secondChannel.providerFlag4)

                ret1 = context.contentResolver.update(
                    TvContract.buildChannelUri(firstChannel.channelId),
                    values,
                    null,
                    null
                )
            }

            if (item.id == secondChannel.id) {
                val values = ContentValues()
                values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, firstChannel.displayNumber)
                values.put(TvContract.Channels.COLUMN_BROWSABLE, firstChannel.isBrowsable)
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1, firstChannel.providerFlag1)
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, firstChannel.providerFlag2)
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG3, firstChannel.providerFlag3)
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4, firstChannel.providerFlag4)

                ret2 = context.contentResolver.update(
                    TvContract.buildChannelUri(secondChannel.channelId),
                    values,
                    null,
                    null
                )
            }
        }
        mChannelList?.sortBy { it.displayNumber.toInt() }
        return (ret1>0 && ret2>0)
    }

    override fun moveChannel(
        moveChannelList: ArrayList<TvChannel>,
        previousIndex: Int,
        newIndex: Int,
        channelMap: HashMap<Int, String>
    ): Boolean {
        var index2 = newIndex
        var removeList: ArrayList<TvChannel> = arrayListOf()

        CoroutineHelper.runCoroutine ({

            moveChannelList.forEach {
                for (tvChannel in mChannelList) {
                    if (it.channelId == tvChannel.channelId) {
                        removeList.add(tvChannel)
                        break
                    }
                }
            }

            mChannelList.removeAll(removeList.toSet())

            if (index2 < mChannelList.size) {
                mChannelList.addAll(index2, moveChannelList)
            } else {
                mChannelList.addAll(moveChannelList)
            }

            for (i in 0 until channelMap.size) {
                mChannelList[i].displayNumber = channelMap[i]!!

                var contentValues = ContentValues()
                contentValues.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, mChannelList[i].displayNumber)
                contentValues.put(TvContract.Channels.COLUMN_BROWSABLE, mChannelList[i].isBrowsable)
                contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1, mChannelList[i].providerFlag1)
                contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, mChannelList[i].providerFlag2)
                contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG3, mChannelList[i].providerFlag3)
                contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4, mChannelList[i].providerFlag4)
                var ret = context.contentResolver.update(
                    TvContract.buildChannelUri(mChannelList[i].channelId),
                    contentValues,
                    null,
                    null
                )
            }

            mChannelList.sortBy { it.displayNumber.toInt() }
        })
        return false
    }

    override fun deleteAllChannels() {
        //Delete only broadcast channels, cuz option is selected from broadcast tab
        mChannelList.forEach { tvChannel ->
            context.contentResolver.delete(TvContract.buildChannelUri(tvChannel.channelId), null, null)
        }
    }
}