package com.iwedia.cltv.platform.refplus5.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.content_provider.TifEpgDataProvider
import com.iwedia.cltv.platform.base.content_provider.createTvEventFromCursor
import com.iwedia.cltv.platform.base.content_provider.getInputIds
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.refplus5.parental.TvRrt5Rating
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.view.rating.MtkTvRRTRatingRegionInfo
import java.util.stream.Collectors

class EpgDataProvider(context: Context, val utilsInterface: UtilsInterface) :
    TifEpgDataProvider(context) {

    private val TAG = javaClass.simpleName

    private val COLUMN_DIRECT_TUNE_NUMBER = "direct_tune_number"
    private val COLUMN_BANDWIDTH = "bandwidth"
    private val COLUMN_RF_NUMBER = "rf_number"
    private val COLUMN_FREQUENCY = "frequency"
    private val COLUMN_CENTRAL_FREQUENCY = "frequency_central";
    private val COLUMN_MODULATION = "modulation"
    private val MTK_CHANNEL_RATING_COLUMN = "mtk_channel_rating"

    //RRT5 parental control
    private val COLUMN_MTKTVDB_PROG_RRT5_DIMEMSION = "mtk_prog_rrt5_dimension"
    private val COLUMN_MTKTVDB_PROG_RRT5_DIMEMSION_VALUE = "mtk_prog_rrt5_dimension_value"

    @SuppressLint("Range")
    @RequiresApi(Build.VERSION_CODES.S)
    @Synchronized
    override fun loadEvents() {
        CoroutineHelper.runCoroutine({
            val retEvents = HashMap<String, ArrayList<TvEvent>>()
            val contentResolver: ContentResolver = context.contentResolver
            val inputList = getInputIds(context)
            if (inputList!!.isNotEmpty()) {
                for (input in inputList) {
                    //Skip google movies events
                    if (input.contains("com.google.android.videos")) {
                        continue
                    }
                    val channelCursor = contentResolver.query(
                        TvContract.buildChannelsUriForInput(input),
                        null,
                        null,
                        null,
                        null
                    )
                    if (channelCursor!!.count > 0) {
                        channelCursor.moveToFirst()
                        do {
                            val tvChannel =
                                com.iwedia.cltv.platform.base.content_provider.createChannelFromCursor(
                                    context,
                                    channelCursor
                                )
                            if (!tvChannel.inputId.contains("anoki", ignoreCase = true)) {
                                try {
                                    tvChannel.platformSpecific = getPlatformData(channelCursor)
                                } catch (E: Exception) {
                                    println(E)
                                }
                            }

                            val uri: Uri = TvContract.buildProgramsUriForChannel(
                                tvChannel.id.toLong()
                            )
                            //Create query
                            var epgCursor: Cursor? = null
                            epgCursor = context.contentResolver
                                .query(uri, null, null, null, null)

                            val eventList = arrayListOf<TvEvent>()
                            if (epgCursor != null) {
                                epgCursor.moveToFirst()
                                var endTime = 0L
                                var firstEvent = true
                                while (!epgCursor.isAfterLast) {
                                    val event = createTvEventFromCursor(tvChannel, epgCursor)

                                    try {
                                        var dim = ""
                                        var dimValue = ""
                                        //dim
                                        val indexOfColumnRRT5Dim = epgCursor.getColumnIndex(
                                            COLUMN_MTKTVDB_PROG_RRT5_DIMEMSION
                                        )
                                        if (indexOfColumnRRT5Dim != -1 && epgCursor.getString(
                                                indexOfColumnRRT5Dim
                                            ) != null
                                        ) {
                                            dim = epgCursor.getString(indexOfColumnRRT5Dim)
                                        }
                                        //dimValue
                                        val indexOfColumnRRT5DimValue =
                                            epgCursor.getColumnIndex(
                                                COLUMN_MTKTVDB_PROG_RRT5_DIMEMSION_VALUE
                                            )
                                        if (indexOfColumnRRT5DimValue != -1 && epgCursor.getString(
                                                indexOfColumnRRT5DimValue
                                            ) != null
                                        ) {
                                            dimValue =
                                                epgCursor.getString(indexOfColumnRRT5DimValue)
                                        }
                                        event.rrt5Rating =
                                            if (dim.isNotEmpty() && dimValue.isNotEmpty())
                                                parseRRT5ContentRating(dim, dimValue)
                                            else ""
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    //Remove overlapped items
                                    if (!firstEvent && event.startTime < endTime) {
                                        firstEvent = false
                                        epgCursor.moveToNext()
                                    } else {
                                        firstEvent = false
                                        if (event.endTime > endTime) {
                                            eventList.add(event)
                                        }
                                        endTime = event.endTime
                                        epgCursor.moveToNext()
                                    }
                                }
                                epgCursor.close()
                            }
                            retEvents[tvChannel.getUniqueIdentifier()] = eventList
                        } while (channelCursor.moveToNext())
                    }
                    channelCursor.close()
                }
                events.clear()
                events = retEvents
                InformationBus.informationBusEventListener.submitEvent(Events.EVENTS_LOADED)
            } else {
                InformationBus.informationBusEventListener.submitEvent(Events.EVENTS_LOADED)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("Range")
    fun getPlatformData(cursor: Cursor): Any {
        var channelListId = ""
        var directTuneNumber = -1
        var bandwidth = -1
        var rfNumber = -1
        var frequency = -1
        var modulation: String? = null
        var mtkChannelRating = ""

        val platformSpecificData = PlatformSpecificData()
        try {
            if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_CHANNEL_LIST_ID)) != null) {
                channelListId =
                    cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_CHANNEL_LIST_ID))
            }
            platformSpecificData.channelListId = channelListId
            val indexOfColumnDirectTuneNumber = cursor.getColumnIndex(COLUMN_DIRECT_TUNE_NUMBER)
            if (indexOfColumnDirectTuneNumber != -1 && cursor.getString(
                    indexOfColumnDirectTuneNumber
                ) != null
            ) {
                directTuneNumber = cursor.getInt(indexOfColumnDirectTuneNumber)
            }
            platformSpecificData.directTuneNumber = directTuneNumber
            val indexOfColumnBandwidth = cursor.getColumnIndex(COLUMN_BANDWIDTH)
            if (indexOfColumnBandwidth != -1 && cursor.getString(indexOfColumnBandwidth) != null) {
                bandwidth = cursor.getInt(indexOfColumnBandwidth)
            }
            platformSpecificData.bandwidth = bandwidth
            val indexOfColumnRfNumber = cursor.getColumnIndex(COLUMN_RF_NUMBER)
            if (indexOfColumnRfNumber != -1 && cursor.getString(indexOfColumnRfNumber) != null) {
                rfNumber = cursor.getInt(indexOfColumnRfNumber)
            }
            platformSpecificData.rfNumber = rfNumber

            val indexOfColumnFrequency = cursor.getColumnIndex(COLUMN_FREQUENCY)
            if (indexOfColumnFrequency != -1 && cursor.getString(indexOfColumnFrequency) != null) {
                val frequencyString = cursor.getString(indexOfColumnFrequency)
                frequency = frequencyString.toInt()
            }
            if ((frequency == 0) || (frequency == -1)) {
                val indexOfColumnCentralFrequency = cursor.getColumnIndex(COLUMN_CENTRAL_FREQUENCY)
                frequency = if (indexOfColumnCentralFrequency != -1) cursor.getInt(
                    indexOfColumnCentralFrequency
                ) else frequency
            }

            if ((frequency == 0) || (frequency == -1)) {
                frequency =
                    cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1))
            }
            platformSpecificData.frequency = frequency

            try {
                val indexOfColumnModulation = cursor.getColumnIndex(COLUMN_MODULATION)
                if (indexOfColumnModulation != -1 && cursor.getString(indexOfColumnModulation) != null) {
                    modulation = cursor.getString(indexOfColumnModulation)
                }
            } catch (e: Exception) {
                modulation = ""
            }
            platformSpecificData.modulation = modulation

            val indexOfMtkChannelRating = cursor.getColumnIndex(MTK_CHANNEL_RATING_COLUMN)
            if (indexOfMtkChannelRating != -1 && cursor.getString(indexOfMtkChannelRating) != null) {
                mtkChannelRating = cursor.getString(indexOfMtkChannelRating)
            }
            platformSpecificData.mtkChannelRating = mtkChannelRating
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to parse platform specific data : ${e.message}")
        }

        return platformSpecificData
    }

    private fun parseRRT5ContentRating(dim: String, dimValue: String): String {
        val ratingValue: MutableList<String> = java.util.ArrayList()
        val dimArray = dim.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val dimValueArray = dimValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()

        val rrt5Ratings: java.util.ArrayList<MtkTvRRTRatingRegionInfo> =
            TvRrt5Rating.getRRTRatingInfo(
                context,
                com.mediatek.dtv.tvinput.client.scan.Constants.TYPE_ATSC
            )
        if (rrt5Ratings.size > 0) {
            val dimInfoList = rrt5Ratings[0].ratingDimInfoList
            for (i in dimArray.indices) {
                val dimNum = dimArray[i].toInt()
                val dimValueNum = dimValueArray[i].toInt()
                val levelList = dimInfoList[dimNum].ratingLevelList
                val abbrText = levelList[dimValueNum].lvlAbbrText
                if (abbrText != null && "" != abbrText) {
                    ratingValue.add(abbrText)
                }
            }
        }
        return ratingValue.stream().collect(Collectors.joining("-"))
    }
}