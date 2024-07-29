package com.iwedia.cltv.sdk

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.provider.BaseColumns
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.text.isDigitsOnly
import com.iwedia.cltv.RunningDataProvider
import com.iwedia.cltv.sdk.ReferenceSdk.context
import com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider
import com.iwedia.cltv.sdk.content_provider.ReferenceContract
import com.iwedia.cltv.sdk.entities.*
import com.iwedia.cltv.sdk.handlers.*
import core_entities.Error
import core_entities.GenericEntity
import data_type.GList
import data_type.GLong
import default_sdk.backend.DefaultEntityBuilder
import handlers.DataProvider
import handlers.PrefsHandler
import kotlinx.coroutines.*
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashMap


/**
 * Tif data provider
 *
 * @author Dejan Nadj
 */
open class TifDataProvider : DataProvider<DefaultEntityBuilder>(DefaultEntityBuilder()) {

    private val TAG = "TifDataProvider"
    private var channels = GList<ReferenceTvChannel>()
    private lateinit var favoriteHelper: ReferenceFavoriteHelper
    private val epgUpdateTimerTimeout = 10000L
    private var channelListUpdateJob: Job? = null
    private val PACKAGE_NAME = "com.iwedia.cltv"

    var channelListUpdateTimer: CountDownTimer? = null
    var epgUpdateTimer: CountDownTimer? = null
    var activeChannelDeleted = false

    var scanPerformed = false

    /**
     * Start channel list update timer
     */
    private fun startUpdateTimer() {
        //Cancel timer if it's already started
        stopUpdateTimer()

        //Start new count down timer
        channelListUpdateTimer = object :
            CountDownTimer(
                4000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                if (channelListUpdateJob != null) {
                    channelListUpdateJob?.cancel()
                    channelListUpdateJob = null
                }
                channelListUpdateJob = GlobalScope.async {
                    if (!scanPerformed) {
                        delay(1000)
                    }
                    channelListUpdate()
                }
            }
        }
        channelListUpdateTimer!!.start()
    }

    /**
     * Start epg data update timer
     */
    private fun startEpgUpdateTimer() {
        //Cancel timer if it's already started
        stopEpgUpdateTimer()

        //Start new count down timer
        epgUpdateTimer = object :
            CountDownTimer(
                epgUpdateTimerTimeout,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                InformationBus.submitEvent(Event(ReferenceEvents.EPG_DATA_UPDATED))
            }
        }
        epgUpdateTimer!!.start()
    }

    private fun isAppOnForeground(context: Context, appPackageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == appPackageName) {
                return true
            }
        }
        return false
    }

    /**
     * Stop channel list udpate timer if it is already started
     */
    private fun stopUpdateTimer() {
        if (channelListUpdateTimer != null) {
            channelListUpdateTimer!!.cancel()
            channelListUpdateTimer = null
        }
    }

    /**
     * Stop epg data update timer if it is already started
     */
    private fun stopEpgUpdateTimer() {
        if (epgUpdateTimer != null) {
            epgUpdateTimer!!.cancel()
            epgUpdateTimer = null
        }
    }

    /**
     * Enable/disable lcn
     */
    fun lcnConfigUpdate() {
        if (channelListUpdateJob != null) {
            channelListUpdateJob?.cancel()
            channelListUpdateJob = null
        }
        channelListUpdateJob = GlobalScope.async {
            delay(1000)
            channelListUpdate()
        }
    }

    fun setScanPerformed() {
        scanPerformed = true
    }

    /**
     * Channel list update
     */
    private suspend fun channelListUpdate() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ TifDataProvider channel list update")
        init(object : AsyncDataReceiver<Int> {
            override fun onReceive(data: Int) {
                if (data == DataEvent.CHANNELS_LOADED) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ TifDataProvider channel list update channels loaded")
                    //Check is active channel locked
                    var isActiveChannelLocked = false

                    var activeTvChannel =
                        (ReferenceSdk.tvHandler as ReferenceTvHandler).activeChannel

                    if (activeTvChannel != null) {
                        var position = 0
                        var i = 0
                        channels.value.forEach { item ->
                            if (ReferenceTvChannel.compare(activeTvChannel!!, item)) {
                                position = i
                            }
                            i++
                        }

                        (ReferenceSdk.tvHandler as ReferenceTvHandler).activeChannel =
                            channels.get(position)
                        ReferenceSdk.prefsHandler!!.storeValue(
                            PrefsHandler.ACTIVE_CHANNEL_TAG,
                            position
                        )
                    }

                    // Update tv handler with the new channel list
                    ReferenceSdk.tvHandler!!.setup()

                    channels.value.forEach { tvChannel ->
                        if (tvChannel.isLocked && ReferenceTvChannel.compare(
                                tvChannel,
                                activeTvChannel!!
                            )
                        ) {
                            isActiveChannelLocked = true
                        }
                    }

                    // Trigger active channel is locked event
                    if (isActiveChannelLocked && !(ReferenceSdk.playerHandler as ReferencePlayerHandler).isChannelUnlocked) {
                        InformationBus.submitEvent(
                            Event(
                                ReferenceEvents.ACTIVE_CHANNEL_LOCKED_EVENT
                            )
                        )
                    } else {
                        InformationBus.submitEvent(
                            Event(
                                ReferenceEvents.ACTIVE_CHANNEL_UNLOCKED_EVENT
                            )
                        )

                    }

                    if (activeChannelDeleted) {
                        activeChannelDeleted = false
                        if (channels.value.isNotEmpty()) {
                            // Play next channel if the active is deleted
                            CoroutineHelper.runCoroutineWithDelay({

                                var index =
                                    (ReferenceSdk.tvHandler as ReferenceTvHandler).desiredChannelIndex++
                                if (index >= channels.value.size) {
                                    index = 0
                                }
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ active channel is deleted ")
                                (ReferenceSdk.tvHandler as ReferenceTvHandler).changeChannel(
                                    index,
                                    object : AsyncReceiver {
                                        override fun onFailed(error: Error?) {}

                                        override fun onSuccess() {
                                            (ReferenceSdk.tvHandler as ReferenceTvHandler).activeChannel =
                                                channels.get(index)
                                        }
                                    })
                                InformationBus.submitEvent(
                                    Event(
                                        ReferenceEvents.ACTIVE_CHANNEL_DELETED_EVENT
                                    )
                                )
                            }, 1000)
                        }
                    }

                    if (scanPerformed) {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "scanPerformed"
                        )
                        CoroutineHelper.runCoroutineWithDelay({
                            if (channels.value.isNotEmpty()){
                                // Reset active channel index
                                ReferenceSdk.prefsHandler!!.storeValue(
                                    PrefsHandler.ACTIVE_CHANNEL_TAG, 0
                                )
                                (ReferenceSdk.tvHandler!! as ReferenceTvHandler).desiredChannelIndex =
                                    0
                                (ReferenceSdk.tvHandler!! as ReferenceTvHandler).activeChannel =
                                    (ReferenceSdk.tvHandler!! as ReferenceTvHandler).getChannelByIndex(
                                        0
                                    )
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    "scanPerformed channel list is not empty"
                                )

                                channels.get(0)?.let {
                                    if (it.isRadioChannel) {
                                        InformationBus.submitEvent(
                                            Event(
                                                ReferenceEvents.PLAYBACK_SHOW_RADIO_OVERLAY,
                                            )
                                        )
                                    }
                                }

                                // Start first channel playback
                                if (isAppOnForeground(context, PACKAGE_NAME)){
                                    ReferenceSdk.tvHandler!!.startInitialPlayback(object :
                                        AsyncReceiver {
                                        override fun onFailed(error: Error?) {
                                        }

                                        @RequiresApi(Build.VERSION_CODES.N)
                                        override fun onSuccess() {
                                            Log.d(Constants.LogTag.CLTV_TAG +
                                                TAG,
                                                "onSuccess: SCAN COMPLETED, STARTING PLAYBACK"
                                            )
                                            var activeRunning =
                                                (ReferenceSdk.tvHandler!! as ReferenceTvHandler).activeChannel
                                            if (activeRunning != null) {
                                                RunningDataProvider.initChannelObserver(
                                                    activeRunning
                                                )
                                                RunningDataProvider.checkRunningStatus(activeRunning)
                                            }
                                            ReferenceSdk.recentlyHandler!!.getRecentlyWatched()!!
                                                .clear()
                                        }
                                    })
                                }
                            } else {
                                InformationBus.submitEvent(
                                    Event(
                                        ReferenceEvents.CHANNEL_LIST_IS_EMPTY
                                    )
                                )
                            }
                        }, 1000)
                    } else {
                        if (channels.value.isEmpty()) {
                            InformationBus.submitEvent(
                                Event(
                                    ReferenceEvents.CHANNEL_LIST_IS_EMPTY
                                )
                            )
                        }
                        CoroutineHelper.runCoroutineWithDelay({
                            (ReferenceSdk.tvHandler as ReferenceTvHandler).updateDesiredChannelIndex()
                        }, 1000)
                    }

                    scanPerformed = false
                    InformationBus.submitEvent(Event(ReferenceEvents.CHANNEL_LIST_UPDATED))
                }
            }

            override fun onFailed(error: Error?) {
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun setup() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ TifDataProvider setup")
    }
    
    override fun dispose() {
        stopUpdateTimer()
        var previousFavString = ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource()
        ReferenceSdk.prefsHandler?.storeValue(
            ReferenceFavoriteHandler.FAVORITE_STRING_TAG, previousFavString
        )!!
    }

    override fun init(callback: AsyncDataReceiver<Int>) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ TifDataProvider init")
    }

    /**
     * Get list of the available input ids
     *
     * @return available input ids
     */
    private fun getInputIds(): ArrayList<String>? {
        val retList = ArrayList<String>()
        //Get all TV inputs
        for (input in (context.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager).tvInputList) {
            val inputId = input.id
            retList.add(inputId)
        }
        return retList
    }

    override fun <T : GenericEntity> getData(dataType: Int, vararg param: Any?): GList<T> {
        return if (dataType == DataType.TV_CHANNEL) {
            channels as GList<T>
        } else {
            super.getData(dataType, *param)
        }
    }

    fun loadCurrentEpgData( callback: AsyncDataReceiver<HashMap<ReferenceTvChannel,GList<ReferenceTvEvent>>>) {
        var currentTime = GLong(System.currentTimeMillis().toString())
        var startTime = currentTime.value.toLong() - (1 * 24 * 60 * 60 * 1000)
        var endTime = currentTime.value.toLong() + (1 * 24 * 60 * 60 * 1000)
        var retMap = HashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>()
        channels.value.forEach { channel ->
            loadEvents(channel, startTime, endTime, object : AsyncDataReceiver<GList<ReferenceTvEvent>> {
                override fun onReceive(data: GList<ReferenceTvEvent>) {
                    retMap.put(channel, data)
                }

                override fun onFailed(error: Error?) {
                    retMap.put(channel, GList())
                }
            })
        }
        callback.onReceive(retMap)
    }

    override fun <T : GenericEntity> getDataAsync(
        dataType: Int,
        callback: AsyncDataReceiver<GList<T>>,
        vararg param: Any?
    ) {
        if (dataType == DataType.TV_CHANNEL) {
            callback.onReceive(channels as GList<T>)
        } else if (dataType == DataType.TV_EVENT) {
            //Set tv channel
            var tvChannel: ReferenceTvChannel? = null
            if (param.isNotEmpty() && param[0] is ReferenceTvChannel) {
                tvChannel = param[0] as ReferenceTvChannel
            }

            //Init start and end time
            var startTime: Long = 0
            var endTime: Long = 0
            if (param.isNotEmpty() && param.size > 1 && param[1] is GLong) {
                startTime = (param[1] as GLong).value.toLong()
            }

            if (param.isNotEmpty() && param.size > 2 && param[2] is GLong) {
                endTime = (param[2] as GLong).value.toLong()
            }

            if (startTime == 0L) {
                var currentTime = GLong(System.currentTimeMillis().toString())
                startTime = currentTime.value.toLong() - (7 * 24 * 60 * 60 * 1000)
                endTime = currentTime.value.toLong() + (7 * 24 * 60 * 60 * 1000)
                loadEvents(tvChannel!!, startTime, endTime,
                    object : AsyncDataReceiver<GList<ReferenceTvEvent>> {
                        override fun onReceive(data: GList<ReferenceTvEvent>) {
                            callback.onReceive(data as GList<T>)
                        }

                        override fun onFailed(error: Error?) {
                            callback.onFailed(error)
                        }
                    })
            } else {
                loadEvents(tvChannel!!, startTime, endTime,
                    object : AsyncDataReceiver<GList<ReferenceTvEvent>> {
                        override fun onReceive(data: GList<ReferenceTvEvent>) {
                            callback.onReceive(data as GList<T>)
                        }

                        override fun onFailed(error: Error?) {
                            callback.onFailed(error)
                        }
                    })
            }
        } else if (dataType == DataType.FAVORITE) {
            favoriteHelper.getFavoriteItems(object :
                AsyncDataReceiver<List<ReferenceFavoriteItem>> {
                override fun onReceive(data: List<ReferenceFavoriteItem>) {
                    val list = GList<T>()
                    for (favItem in data) {
                        if (favItem != null) {
                            list.add(favItem as T)
                        }
                    }
                    callback.onReceive(list)
                }

                override fun onFailed(error: Error?) {
                    callback.onFailed(error)
                }
            })
        } /*else if (dataType == DataType.SCHEDULED_RECORDING){
            scheduledRecordingHelper.getScheduledRecordings(object :
            AsyncDataReceiver<List<ReferenceRecordingItem>>{
                override fun onReceive(data: List<ReferenceRecordingItem>) {
                    val list = GList<T>()
                    for (recItem in data) {
                        if (recItem != null) {
                            list.add(recItem as T)
                        }
                    }
                    callback.onReceive(list)
                }

                override fun onFailed(error: Error?) {
                    callback.onFailed(error)
                }
            })
        }*/
    }

    private fun <T : GenericEntity> loadEvents(
        tvChannel: ReferenceTvChannel,
        startDate: Long,
        endDate: Long,
        callback: AsyncDataReceiver<GList<T>>
    ) {
        val uri: Uri = TvContract.buildProgramsUriForChannel(
            tvChannel.id.toLong(),
            startDate,
            endDate
        )
        //Create query
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver
                .query(uri, null, null, null, null)
        } catch (e: Exception) {
            //For this query system permission is necessary
            //Throw security exception
            if (callback != null) {
                callback.onFailed(core_entities.Error(100, "No current event"))
            }
        }

        if (cursor == null) {
            callback.onFailed(core_entities.Error(100, "cursor is null"))
        }

        var retList = GList<ReferenceTvEvent>()

        if (cursor != null) {
            cursor.moveToFirst()
            var endTime = 0L
            var firstEvent = true
            while (!cursor.isAfterLast) {
                val cd = createTvEventFromCursor(tvChannel, cursor)
                //Remove overlapped items
                if (!firstEvent && cd.startDate.value.toLong() < endTime) {
                    firstEvent = false
                    cursor.moveToNext()
                } else {
                    firstEvent = false
                    if (cd.startDate.value.toLong() >= startDate && cd.endDate.value.toLong() <= endDate) {
                        retList.add(cd)
                    }
                    endTime = cd.endDate.value.toLong()
                    cursor.moveToNext()
                }
            }
            cursor.close()
            if (callback != null) {
                callback.onReceive(retList as GList<T>)
                if (retList.value.isNotEmpty()) {
                    retList.value.sortBy {
                        (it as ReferenceTvEvent).startDate.value.toLong()
                    }
                    var eventsList = ArrayList<ReferenceTvEvent>()
                    retList.value.forEach {
                        eventsList.add(it as ReferenceTvEvent)
                    }
                }
            }
        }
    }

    override fun <T : GenericEntity> addDataItemAsync(
        dataType: Int,
        dataItem: T,
        callback: AsyncReceiver,
        vararg param: Any?
    ) {
        if (dataType == DataType.FAVORITE) {
            val favoriteItem = dataItem as ReferenceFavoriteItem
            favoriteHelper.addToFavorites(favoriteItem, object : AsyncReceiver {
                override fun onSuccess() {
                    if (favoriteItem.tvChannel != null) {
                        favoriteItem.tvChannel.favListIds.clear()
                        favoriteItem.tvChannel.favListIds.addAll(favoriteItem.favListIds)
                    }
                    callback.onSuccess()
                }

                override fun onFailed(error: Error?) {
                    callback.onFailed(error)
                }
            })
        }
    }

    override fun <T : GenericEntity> removeDataItemAsync(
        dataType: Int,
        dataItem: T,
        callback: AsyncReceiver,
        vararg param: Any?
    ) {
        if (dataType == DataType.FAVORITE) {
            val favoriteItem = dataItem as ReferenceFavoriteItem
            favoriteHelper.removeFromFavorites(favoriteItem, object : AsyncReceiver {
                override fun onSuccess() {
                    favoriteItem.tvChannel.favListIds.clear()
                    callback.onSuccess()
                }

                override fun onFailed(error: Error?) {
                    callback.onFailed(error)
                }
            })
        }
    }

    /**
     * Remove favorite category
     *
     * @param favoriteCategory category name
     * @param receiver callback
     */
    fun removeFavoriteCategory(favoriteCategory: String, receiver: AsyncReceiver) {
        favoriteHelper.removeFavoriteCategory(favoriteCategory, receiver)
    }

    /**
     * Rename favorite category
     *
     * @param newName   new category name
     * @param oldName   old category name
     * @param receiver callback
     */
    fun renameFavoriteCategory(newName: String, oldName: String, receiver: AsyncReceiver) {
        favoriteHelper.renameFavoriteCategory(newName, oldName, receiver)
    }

    @SuppressLint("Range")
    private fun createChannelFromCursor(cursor: Cursor): ReferenceTvChannel {
        var id = -1
        var inputId = ""
        var displayNumber = ""
        var displayName = ""
        var logoImagePath = ""
        var isRadioChannel = false
        var isSkipped = false
        var isLocked = false;
        var tunerType = -1
        var ordinalNumber = 0
        var tsId = 0
        var onId = 0
        var serviceId = 0
        var internalId = -1L
        var isBrowsable = true
        var appLinkText = ""
        var appLinkIntentUri = ""
        var appLinkIconUri = ""
        var appLinkPosterUri = ""
        var packageName = ""
        var typeFullName = ""

        var referenceVideoQuality = GList<Int>()
        if (cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.ORIG_ID_COLUMN)) != null) {
            id = cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.ORIG_ID_COLUMN))
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.INPUT_ID_COLUMN)) != null) {
            inputId =
                cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.INPUT_ID_COLUMN))
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.DISPLAY_NUMBER_COLUMN)) != null) {
            displayNumber =
                cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.DISPLAY_NUMBER_COLUMN))
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.NAME_COLUMN)) != null) {
            displayName =
                (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.NAME_COLUMN))).trim()
        }
        //Check is channel renamed
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.REFERENCE_NAME_COLUMN)) != null) {
            var referenceName =
                (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.REFERENCE_NAME_COLUMN))).trim()
            if (referenceName != null && referenceName.isNotEmpty()) {
                displayName = referenceName
            }
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.SERVICE_ID_COLUMN)) != null) {
            serviceId =
                cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.SERVICE_ID_COLUMN))
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.TRANSPORT_STREAM_ID_COLUMN)) != null) {
            tsId =
                cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.TRANSPORT_STREAM_ID_COLUMN))
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.ORIGINAL_NETWORK_ID_COLUMN)) != null) {
            onId =
                cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.ORIGINAL_NETWORK_ID_COLUMN))
        }

        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.SERVICE_TYPE_COLUMN)) != null) {
            isRadioChannel =
                cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.SERVICE_TYPE_COLUMN)) == TvContract.Channels.SERVICE_TYPE_AUDIO
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.VIDEO_FORMAT_COLUMN)) != null) {
            var videoFormat =
                cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.VIDEO_FORMAT_COLUMN))
            var videoResolution = TvContract.Channels.getVideoResolution(videoFormat)
            when (videoResolution) {
                TvContract.Channels.VIDEO_RESOLUTION_ED -> {
                    referenceVideoQuality.add(ReferenceTvChannel.VIDEO_RESOLUTION_ED)
                }
                TvContract.Channels.VIDEO_RESOLUTION_FHD -> {
                    referenceVideoQuality.add(ReferenceTvChannel.VIDEO_RESOLUTION_FHD)
                }
                TvContract.Channels.VIDEO_RESOLUTION_HD -> {
                    referenceVideoQuality.add(ReferenceTvChannel.VIDEO_RESOLUTION_HD)
                }
                TvContract.Channels.VIDEO_RESOLUTION_SD -> {
                    referenceVideoQuality.add(ReferenceTvChannel.VIDEO_RESOLUTION_SD)
                }
                TvContract.Channels.VIDEO_RESOLUTION_UHD -> {
                    referenceVideoQuality.add(ReferenceTvChannel.VIDEO_RESOLUTION_UHD)
                }
            }
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.TYPE_COLUMN)) != null) {
            // TODO Add support for other standard not only for DVB
            val type =
                cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.TYPE_COLUMN))
            if (type == TvContract.Channels.TYPE_DVB_T || type == TvContract.Channels.TYPE_DVB_T2 ||
                type == TvContract.Channels.TYPE_ATSC_T || type == TvContract.Channels.TYPE_ATSC3_T) {
                tunerType = ReferenceTvChannel.TERRESTRIAL_TUNER_TYPE
            }
            if (type == TvContract.Channels.TYPE_DVB_C || type == TvContract.Channels.TYPE_DVB_C2 ||
                type == TvContract.Channels.TYPE_ATSC_C) {
                tunerType = ReferenceTvChannel.CABLE_TUNER_TYPE
            }
            if (type == TvContract.Channels.TYPE_DVB_S || type == TvContract.Channels.TYPE_DVB_S2) {
                tunerType = ReferenceTvChannel.SATELLITE_TUNER_TYPE
            }
            typeFullName = type
        }
        if (cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.SKIP_COLUMN)) != null) {
            isSkipped =
                cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.SKIP_COLUMN)) == 1
        }
        if (cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.BROWSABLE_COLUMN)) != null) {
            isBrowsable =
                cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.BROWSABLE_COLUMN)) == 1
        }
        if (cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.LOCKED_COLUMN)) != null) {
            isLocked =
                cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.LOCKED_COLUMN)) == 1
        }
        if (cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)) != null) {
            internalId =
                cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))
        }
        if (cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.ORDINAL_NUMBER_COLUMN)) != null) {
            ordinalNumber =
                cursor.getInt(cursor.getColumnIndex(ReferenceContract.Channels.ORDINAL_NUMBER_COLUMN))
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.APP_LINK_ICON_URI_COLUMN)) != null) {
            appLinkIconUri =
                cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.APP_LINK_ICON_URI_COLUMN))
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.APP_LINK_POSTER_ART_URI_COLUMN)) != null) {
            appLinkPosterUri =
                cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.APP_LINK_POSTER_ART_URI_COLUMN))
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.APP_LINK_TEXT_COLUMN)) != null) {
            appLinkText =
                cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.APP_LINK_TEXT_COLUMN))
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.APP_LINK_INTENT_URI_COLUMN)) != null) {
            appLinkIntentUri =
                cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.APP_LINK_INTENT_URI_COLUMN))
        }
        if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.PACKAGE_NAME_COLUMN)) != null) {
            packageName =
                cursor.getString(cursor.getColumnIndex(ReferenceContract.Channels.PACKAGE_NAME_COLUMN))
        }

        if (inputId.contains("iwedia")) {
            logoImagePath = appLinkIconUri
        }else{
            logoImagePath = checkForImage(onId, tsId, serviceId)
        }

        var index = if (displayNumber.isDigitsOnly()) displayNumber.toInt() else ordinalNumber
        var referenceTvChannel =
            ReferenceTvChannel(
                id,
                index,
                displayName,
                logoImagePath,
                videoQuality = referenceVideoQuality
            )
        referenceTvChannel.internalId = internalId
        referenceTvChannel.inputId = inputId;
        referenceTvChannel.displayNumber = displayNumber
        referenceTvChannel.lcn = if (displayNumber.isDigitsOnly()) displayNumber.toInt() else ordinalNumber
        referenceTvChannel.isRadioChannel = isRadioChannel
        referenceTvChannel.channelId = id.toLong()
        referenceTvChannel.tunerType = tunerType
        referenceTvChannel.isSkipped = isSkipped
        referenceTvChannel.isLocked = isLocked
        referenceTvChannel.ordinalNumber = ordinalNumber
        referenceTvChannel.serviceId = serviceId
        referenceTvChannel.tsId = tsId
        referenceTvChannel.onId = onId
        referenceTvChannel.isBrowsable = isBrowsable
        referenceTvChannel.appLinkIconUri = appLinkIconUri
        referenceTvChannel.appLinkPosterUri = appLinkPosterUri
        referenceTvChannel.appLinkText = appLinkText
        referenceTvChannel.appLinkIntentUri = appLinkIntentUri
        referenceTvChannel.packageName = packageName
        referenceTvChannel.type = typeFullName
        return referenceTvChannel
    }


    @SuppressLint("SdCardPath")
    fun checkForImage(onid: Int, tsid: Int, sid: Int): String {
        val filePath = "/data/data/com.iwedia.cltv/files/images"
        val directory: File = File(filePath)
        val files = directory.listFiles()
        try{
            files.forEach { it ->
                val split = it.name.split("_")
                if(split[0].toInt() == onid && split[1].toInt() == tsid && split[2].toInt() == sid){
                    return "/data/data/com.iwedia.cltv/files/images/${it.name}"
                }
            }
        }catch (E: Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkForImage: $E")
        }
        return ""
    }

    @SuppressLint("Range")
    private fun createTvEventFromCursor(
        tvChannel: ReferenceTvChannel,
        cursor: Cursor
    ): ReferenceTvEvent {
        var id = -1
        var name = ""
        var shortDescription = ""
        var longDescription = ""
        var imagePath = ""
        var eventStart: Long? = null
        var eventEnd: Long? = null
        var parentalRating: String? = null
        var providerFlag: Int? = null


        if (cursor.getLong(cursor.getColumnIndex(TvContract.Programs._ID)) != null) {
            id = cursor.getLong(cursor.getColumnIndex(TvContract.Programs._ID)).toInt()
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_TITLE)) != null) {
            name = cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_TITLE))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_SHORT_DESCRIPTION)) != null) {
            shortDescription =
                cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_SHORT_DESCRIPTION))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_LONG_DESCRIPTION)) != null) {
            longDescription =
                cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_LONG_DESCRIPTION))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_POSTER_ART_URI)) != null) {
            imagePath =
                cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_POSTER_ART_URI))
        }
        if (cursor.getLong(cursor.getColumnIndex(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS)) != null) {
            eventStart =
                cursor.getLong(cursor.getColumnIndex(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS))
        }
        if (cursor.getLong(cursor.getColumnIndex(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS)) != null) {
            eventEnd =
                cursor.getLong(cursor.getColumnIndex(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_CONTENT_RATING)) != null) {

            parentalRating =
                cursor.getString(cursor.getColumnIndex(TvContract.Programs.COLUMN_CONTENT_RATING))
        }
        if (parentalRating == null) parentalRating = ""
        if (cursor.getLong(cursor.getColumnIndex(TvContract.Programs.COLUMN_INTERNAL_PROVIDER_FLAG1)) != null) {
            providerFlag =
                cursor.getInt(cursor.getColumnIndex(TvContract.Programs.COLUMN_INTERNAL_PROVIDER_FLAG1))
        }

        shortDescription = shortDescription.trim().replace("\\s+", " ")
        longDescription = longDescription.trim().replace("\\s+", " ")
        return ReferenceTvEvent(
            id,
            tvChannel,
            name,
            shortDescription,
            longDescription,
            imagePath,
            GLong(eventStart.toString()),
            GLong(eventEnd.toString()),
            parentalRating = parentalRating,
            providerFlag = providerFlag
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun clearAllChannels(callback: AsyncReceiver) {
        CoroutineHelper.runCoroutine({
            var inputList = getInputIds()
            if (inputList!!.isNotEmpty()) {
                for (input in inputList) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ clear channels for input id $input")
                    clearChannelsForTvInput(input)
                }
            }
            callback.onSuccess()
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun clearChannelsForTvInput(inputId: String) {
        val contentResolver: ContentResolver = context.contentResolver
        contentResolver.delete(TvContract.buildChannelsUriForInput(inputId), null)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun deleteChannel(tvChannel: ReferenceTvChannel): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ delete channel ${tvChannel.internalId}")

        val contentValues = ContentValues()
        val uri = ReferenceContract.buildChannelsUri(tvChannel.internalId)
        contentValues.put(ReferenceContract.Channels.BROWSABLE_COLUMN, 0)
        contentValues.put(ReferenceContract.Channels.DELETED_COLUMN, 1)

        return try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
            tvChannel.isBrowsable = false
            var removedChannel: ReferenceTvChannel? = null
            channels.value.forEach {
                if (ReferenceTvChannel.compare(it, tvChannel)) {
                    removedChannel = it
                }

            }
            if (removedChannel != null) {
                channels.removeElement(removedChannel!!)
                ReferenceSdk.tvHandler!!.getChannelList().removeElement(removedChannel!!)
            }

            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    fun lockChannel(tvChannel: ReferenceTvChannel, lock: Boolean): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ lock channel ${tvChannel.internalId}")
        // TODO temporary solution for the manifest parental control permissions issue
        /*channels.value.forEach { channel ->
            if (channel.channelId == tvChannelId) {
                channel.isLocked = lock
                return@forEach
            }
        }
        return true*/

        val contentValues = ContentValues()
        val uri = ReferenceContract.buildChannelsUri(tvChannel.internalId)
        var locked = if (lock) 1 else 0
        contentValues.put(ReferenceContract.Channels.LOCKED_COLUMN, locked)

        return try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    @SuppressLint("Range")
    fun skipChannel(tvChannel: ReferenceTvChannel, skip: Boolean): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ skip channel ${tvChannel.internalId}")
        val contentValues = ContentValues()
        var isSkipped = if (skip) 1 else 0
        contentValues.put(ReferenceContract.Channels.SKIP_COLUMN, isSkipped)

        var uri = ReferenceContract.buildChannelsUri(tvChannel.internalId)

        return try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }


    //TODO only for testing purposes
    @SuppressLint("Range")
    fun rearrangeChannels() {
        val contentValues = ContentValues()
        //RTS 1
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 0)
        var uri = TvContract.buildChannelUri(24L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // RTS 2
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 1)
        uri = TvContract.buildChannelUri(32L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // RTS 3
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 2)
        uri = TvContract.buildChannelUri(31L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // PINK
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 3)
        uri = TvContract.buildChannelUri(30L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // PRVA
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 4)
        uri = TvContract.buildChannelUri(27L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // B92
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 5)
        uri = TvContract.buildChannelUri(28L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // HAPPY
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 6)
        uri = TvContract.buildChannelUri(26L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // RTV1
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 7)
        uri = TvContract.buildChannelUri(25L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // TV MOST
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 8)
        uri = TvContract.buildChannelUri(23L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // TV Sremska
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 9)
        uri = TvContract.buildChannelUri(22L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // RADIO BG
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 10)
        uri = TvContract.buildChannelUri(21L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // VTV
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 11)
        uri = TvContract.buildChannelUri(20L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // RADIO BG 2
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 12)
        uri = TvContract.buildChannelUri(17L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // TV Kanal 2
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 13)
        uri = TvContract.buildChannelUri(16L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // TV Delta
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 14)
        uri = TvContract.buildChannelUri(15L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // RTV 2
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 15)
        uri = TvContract.buildChannelUri(14L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // TV K 9
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 16)
        uri = TvContract.buildChannelUri(13L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // TV Bap
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 17)
        uri = TvContract.buildChannelUri(12L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // TV Dunav
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 18)
        uri = TvContract.buildChannelUri(11L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // TV INFO
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 19)
        uri = TvContract.buildChannelUri(29L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // NOVOSADSKA
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 20)
        uri = TvContract.buildChannelUri(18L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // RADIO BG 202
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 21)
        uri = TvContract.buildChannelUri(17L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // RADIO BG 2
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 22)
        uri = TvContract.buildChannelUri(19L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // My Headlines
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 23)
        uri = TvContract.buildChannelUri(1L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // Current Events
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 24)
        uri = TvContract.buildChannelUri(2L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // Science and Technology
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 25)
        uri = TvContract.buildChannelUri(3L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // Business News
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 26)
        uri = TvContract.buildChannelUri(4L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // Entertainment News
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 27)
        uri = TvContract.buildChannelUri(5L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // International News
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 28)
        uri = TvContract.buildChannelUri(6L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // Gaming News
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 29)
        uri = TvContract.buildChannelUri(7L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // Politics
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, 30)
        uri = TvContract.buildChannelUri(8L)

        try {
            var ret =
                context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("Range")
    fun clearChannels() {
        val contentValues = ContentValues()

        channels.value.forEach { channel ->
            contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, -1)
            var uri = TvContract.buildChannelUri(channel!!.channelId)

            try {
                var ret =
                    context.contentResolver.update(
                        uri,
                        contentValues,
                        null,
                        null
                    )
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Clear browsable column for all channels
     * After calling this method all channels will be browsable (not skipped)
     */
    fun clearBrowsable() {
        val contentValues = ContentValues()

        channels.value.forEach { channel ->
            contentValues.put(TvContract.Channels.COLUMN_BROWSABLE, 1)
            var uri = TvContract.buildChannelUri(channel!!.channelId)

            try {
                var ret =
                    context.contentResolver.update(
                        uri,
                        contentValues,
                        null,
                        null
                    )
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchChannelByTriplet(onId: Int, tsId: Int, serviceId: Int): ReferenceTvChannel? {
        val contentResolver: ContentResolver = context.contentResolver
        var selection =
            ReferenceContract.Channels.ORIGINAL_NETWORK_ID_COLUMN + " = ? and " + ReferenceContract.Channels.TRANSPORT_STREAM_ID_COLUMN + " = ? and " + ReferenceContract.Channels.SERVICE_ID_COLUMN + " = ?"
        var cursor = contentResolver.query(
            ReferenceContentProvider.CHANNELS_URI,
            null,
            selection,
            arrayOf(onId.toString(), tsId.toString(), serviceId.toString()),
            null
        )
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            return createChannelFromCursor(cursor)
        }
        return null
    }

    fun updateFavList() {
        var favString =
            ReferenceSdk.prefsHandler?.getValue(ReferenceFavoriteHandler.FAVORITE_STRING_TAG, "")!!
        if (favString != "" && favString != ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource()) {
            favoriteHelper.updateChannelFavList(favString.toString(), channels)
            ReferenceSdk.tvHandler?.setup()
        }
        channels.value.forEach { channel ->
            favoriteHelper.updateFavChannelData(channel)
        }
    }

    fun loadCurrentEpgEventsData(
        startTime: Long,
        endTime: Long,
        callback: AsyncDataReceiver<HashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>>
    ) {
        var retMap = HashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAllCurrentEvent date startTime ${Date(startTime)}: endTime ${Date(endTime)}")
        channels.value.forEach { channel ->
            loadEvents(
                channel,
                startTime,
                endTime,
                object : AsyncDataReceiver<GList<ReferenceTvEvent>> {
                    override fun onReceive(data: GList<ReferenceTvEvent>) {
                        retMap.put(channel, data)
                    }

                    override fun onFailed(error: Error?) {
                        retMap.put(channel, GList())
                    }
                })
        }
        callback.onReceive(retMap)
    }

    /**
     * loads the data for the provided channel list
     */
    fun loadEpgDataForChannelList(
        channelList: MutableList<ReferenceTvChannel>,
        startTime: Long,
        endTime: Long,
        callback: AsyncDataReceiver<LinkedHashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>>
    ) {
        var retMap = LinkedHashMap<ReferenceTvChannel, GList<ReferenceTvEvent>>()

        channelList.forEach {
                channel ->
                loadEvents(
                    channel,
                    startTime,
                    endTime,
                    object : AsyncDataReceiver<GList<ReferenceTvEvent>> {
                        override fun onReceive(data: GList<ReferenceTvEvent>) {
                            retMap.put(channel, data)
                        }

                        override fun onFailed(error: Error?) {
                            retMap.put(channel, GList())
                        }
                    })

            }
        callback.onReceive(retMap)

        }


}