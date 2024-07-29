package com.iwedia.cltv.platform.refplus5.provider

import android.annotation.SuppressLint
import android.content.*
import android.database.ContentObserver
import android.database.Cursor
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.content_provider.getInputIds
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.refplus5.UtilsInterfaceImpl
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * Refplus channel data provider implementation
 */
@RequiresApi(Build.VERSION_CODES.S)
class ChannelDataProvider(val context: Context, val utilsInterface: UtilsInterface) : ChannelDataProviderInterface {

    private val TAG = javaClass.simpleName
    private var channelList = arrayListOf<TvChannel>()
    private val CHANNEL_UPDATE_TIMEOUT = 3000L
    private var channelUpdateTimer: CountDownTimer? = null
    private lateinit var channelListObserver: ContentObserver
    //used to prevent loading of channel list again when only channel locked status changes in db
    private var channelLockStatusUpdated = false
    val COLUMN_DIRECT_TUNE_NUMBER = "direct_tune_number"
    val COLUMN_BANDWIDTH = "bandwidth"
    val COLUMN_RF_NUMBER = "rf_number"
    val COLUMN_FREQUENCY = "frequency"
    val COLUMN_CENTRAL_FREQUENCY = "frequency_central";
    val COLUMN_MODULATION = "modulation"
    var COLUMN_INTERNAL_PROVIDER_ID = "internal_provider_id"
    private val MTK_CHANNEL_RATING_COLUMN = "mtk_channel_rating"

    val REF5_SCAN_COMPLETED_INTENT_ACTION = "com.mediatek.tv.scan.end"
    val REF5_SCAN_STARTED_INTENT_ACTION = "com.mediatek.tv.scan.start"
    companion object {
        val CFG_MISC_CH_LST_TYPE = "CFG_MISC_CH_LST_TYPE"
        const val NO_BROWSABLE_VALUE = 1 shl 0
        var camServicesAvailable = false
    }

    val GLOBAL_PROVIDER_ID = "global_value"
    val AUTHORITY = "com.mediatek.tv.internal.data";

    val GLOBAL_PROVIDER_URI_URI = Uri.parse("content://" + AUTHORITY + "/" + GLOBAL_PROVIDER_ID)

    var currentListType = 0

    init {
        /*val intentFilter = IntentFilter(REF5_SCAN_STARTED_INTENT_ACTION)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (REF5_SCAN_STARTED_INTENT_ACTION == intent!!.action) {
                    InformationBus.informationBusEventListener.submitEvent(Events.EXIT_APPLICATION_ON_SCAN)
                }
            }
        }, intentFilter)*/

        channelListObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " Channel data updated")
                if (channelLockStatusUpdated) {
                    channelLockStatusUpdated = false
                    return
                }
                startChannelUpdateTimer()
            }
        }

        var currentListTypeString = (utilsInterface as UtilsInterfaceImpl).readMtkInternalGlobalValue(context, CFG_MISC_CH_LST_TYPE)
        if((currentListTypeString != null) &&
            (currentListTypeString.toIntOrNull() != null)) {
            currentListType = currentListTypeString.toInt()
        }
        else {
            currentListType = 0
        }

        context.contentResolver.registerContentObserver(
            TvContract.Channels.CONTENT_URI,
            true,
            channelListObserver
        )

        context.contentResolver.registerContentObserver(
            GLOBAL_PROVIDER_URI_URI.buildUpon()
                .appendPath(CFG_MISC_CH_LST_TYPE)
                .build(),
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    thread {
                        loadChannels()
                    }
                }
            }
        )
    }

    override fun dispose() {
        stopChannelUpdateTimer()
        if (channelListObserver != null) {
            context.contentResolver.unregisterContentObserver(channelListObserver)
        }
    }

    override fun enableLcn(enableLcn: Boolean) {
        TODO("Not yet implemented")
    }

    override fun isLcnEnabled(): Boolean {
        return false
    }

    @SuppressLint("Range")
    override fun getPlatformData(cursor: Cursor): Any? {
        var channelListId = ""
        var directTuneNumber = -1
        var bandwidth = -1
        var rfNumber = -1
        var frequency = -1
        var modulation : String? = null
        var mtkChannelRating = ""
        var internalProviderId = ""

        val platformSpecificData = PlatformSpecificData()
        try {
            if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_CHANNEL_LIST_ID)) != null) {
                channelListId = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_CHANNEL_LIST_ID))
            }
            platformSpecificData.channelListId = channelListId
            val indexOfColumnDirectTuneNumber = cursor.getColumnIndex(COLUMN_DIRECT_TUNE_NUMBER)
            if (indexOfColumnDirectTuneNumber != -1 && cursor.getString(indexOfColumnDirectTuneNumber) != null) {
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
            if((frequency == 0) || (frequency == -1)) {
                val indexOfColumnCentralFrequency = cursor.getColumnIndex(COLUMN_CENTRAL_FREQUENCY)
                frequency = if(indexOfColumnCentralFrequency != -1) cursor.getInt(indexOfColumnCentralFrequency) else frequency
            }

            if((frequency == 0) || (frequency == -1)) {
                frequency = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1))
            }
            platformSpecificData.frequency = frequency

            val indexOfInternalProviderId = cursor.getColumnIndex(COLUMN_INTERNAL_PROVIDER_ID)
            if (indexOfInternalProviderId != -1 && cursor.getString(indexOfInternalProviderId) != null) {
                internalProviderId = cursor.getString(indexOfInternalProviderId)
            }

            platformSpecificData.internalProviderId = internalProviderId

            try {
                val indexOfColumnModulation = cursor.getColumnIndex(COLUMN_MODULATION)
                if (indexOfColumnModulation != -1 && cursor.getString(indexOfColumnModulation) != null) {
                    modulation = cursor.getString(indexOfColumnModulation)
                }
            }catch (E: Exception){
                modulation = ""
            }
            platformSpecificData.modulation = modulation

            val indexOfMtkChannelRating = cursor.getColumnIndex(MTK_CHANNEL_RATING_COLUMN)
            if(indexOfMtkChannelRating != -1 && cursor.getString(indexOfMtkChannelRating) != null){
                mtkChannelRating = cursor.getString(indexOfMtkChannelRating)
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "loadChannels: mtkChannelRating === $mtkChannelRating")
            platformSpecificData.mtkChannelRating  = mtkChannelRating
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to parse platform specific data : ${e.message}")
        }

        return platformSpecificData
    }

    fun isAnalogService(channel : TvChannel) : Boolean
    {
        if((channel.type == TvContract.Channels.TYPE_NTSC) ||
            (channel.type == TvContract.Channels.TYPE_PAL) ||
            (channel.type == TvContract.Channels.TYPE_SECAM)) {
            return true
        }
        return false
    }

    @SuppressLint("Range")
    @RequiresApi(Build.VERSION_CODES.S)
    @Synchronized
    fun loadChannelsSync() {

        var lChannelList = arrayListOf<TvChannel>()
        lChannelList.clear()
        camServicesAvailable = false

        val contentResolver: ContentResolver = context.contentResolver
        var inputList = getInputIds(context)
        if (inputList!!.isNotEmpty()) {
            for (input in inputList) {
                if (input.contains("com.google.android.videos")) {
                    continue
                }
                var cursor = contentResolver.query(
                    TvContract.buildChannelsUriForInput(input),
                    null,
                    null,
                    null,
                    null
                )

                if (cursor!!.count > 0) {
                    cursor.moveToFirst()
                    do {
                        try {
                            var tvChannel =
                                com.iwedia.cltv.platform.base.content_provider.createChannelFromCursor(
                                    context,
                                    cursor,
                                    lChannelList.size
                                )
                            if (tvChannel.inputId.contains("anoki", ignoreCase = true)) {
                                tvChannel.ordinalNumber =
                                    cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4))
                            } else {
                                try {
                                    tvChannel.platformSpecific = getPlatformData(cursor)
                                } catch (E: Exception) {
                                    println(E)
                                }

                                if((utilsInterface.getCountryPreferences(UtilsInterface.CountryPreference.USE_HIDDEN_SERVICE_FLAG,false) == true) &&
                                    !isAnalogService(tvChannel))
                                {
                                    tvChannel.isBrowsable = tvChannel.isBrowsable && (tvChannel.providerFlag4 == 0x3)
                                }
                                tvChannel.ordinalNumber = lChannelList.size
                            }

                            if (isCAMChannel(tvChannel)) {
                                camServicesAvailable = true
                            }

                            if((tvChannel.providerFlag4 != null) &&
                                (utilsInterface.getCountryPreferences(UtilsInterface.CountryPreference.USE_HIDDEN_SERVICE_FLAG,false) == true)
                                && !tvChannel.inputId.contains("anoki", ignoreCase = true) && !isAnalogService(tvChannel)) {
                                //if service is not selectable remove it from list
                                var flagValue : Int = tvChannel.providerFlag4!!
                                if (flagValue > 0) {
                                    lChannelList.add(tvChannel)
                                }
                            } else {
                                lChannelList.add(tvChannel)
                            }

                        } catch(e: Exception) {
                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "loadChannels: failed to load channel ${e.message}")
                        }
                    } while (cursor.moveToNext())
                }
                cursor!!.close()
            }
            updateChannelList(filterCamChannelList(lChannelList))
            //prevent deadlock on direct tune
            thread {
                InformationBus.informationBusEventListener.submitEvent(Events.CHANNELS_LOADED)
            }

            var listTypeString = (utilsInterface as UtilsInterfaceImpl).readMtkInternalGlobalValue(context, CFG_MISC_CH_LST_TYPE)
            if((listTypeString != null) &&
                (listTypeString.toIntOrNull() != null)) {
                var listType = listTypeString.toInt()
                if (currentListType != listType) {
                    thread {
                        //needs thread or there will be deadlocks
                        InformationBus.informationBusEventListener.submitEvent(Events.CHANNEL_LIST_SWITCHED)
                    }
                }
                currentListType = listType
            }
        }
    }

    @SuppressLint("Range")
    @RequiresApi(Build.VERSION_CODES.S)
    @Synchronized
    fun loadChannels() {
        CoroutineHelper.runCoroutine({
            loadChannelsSync()
        })
    }

    fun isCAMChannel(channel : TvChannel) : Boolean {
        if(channel.platformSpecific == null) {
            return false
        }

        var listId = (channel.platformSpecific as PlatformSpecificData).channelListId
        if (listId.startsWith("MEDIATEK_CHANNEL_LIST_TERRESTRIAL_CAM/") ||
                    listId.startsWith("MEDIATEK_CHANNEL_LIST_CABLE_CAM/") ||
                    listId.startsWith("MEDIATEK_CHANNEL_LIST_SATELLITE_CAM/") ||
                    listId.startsWith("MEDIATEK_CHANNEL_LIST_GENERAL_SATELLITE_CAM/") ||
                    listId.startsWith("MEDIATEK_CHANNEL_LIST_PREFERRED_SATELLITE_CAM/")
        ) {
            return true
        }
        return false
    }

    private fun filterCamChannelList(inputList : ArrayList<TvChannel>) : ArrayList<TvChannel> {
        var listTypeString = (utilsInterface as UtilsInterfaceImpl).readMtkInternalGlobalValue(context, CFG_MISC_CH_LST_TYPE)
        var listType = 0
        if((listTypeString != null) &&
            (listTypeString.toIntOrNull() != null)) {
            listType = listTypeString.toInt()
        }
        var outList = arrayListOf<TvChannel>()

        if(listType == 1) {
            var camServiceFound = false
            for (channel in inputList) {
                if(channel.platformSpecific != null) {
                    if (isCAMChannel(channel) && channel.isBrowsable) {
                        camServiceFound = true
                    }
                }
            }

            //There is no CAM services and we are on CAM service list, fallback to Broadcast service list
            if (!camServiceFound) {
                var zeroValue = 0
                utilsInterface.saveMtkInternalGlobalValue(context, CFG_MISC_CH_LST_TYPE, zeroValue.toString(),true)
            }
        }

        listTypeString = utilsInterface.readMtkInternalGlobalValue(context, CFG_MISC_CH_LST_TYPE)
        if((listTypeString != null) &&
            (listTypeString.toIntOrNull() != null)) {
            listType = listTypeString.toInt()
        } else {
            listType = 0
        }


        for(channel in inputList) {
            if(channel.platformSpecific != null) {
                var listId = (channel.platformSpecific as PlatformSpecificData).channelListId
                if (isProperList(listType, listId, channel.isFastChannel())) {
                    outList.add(channel)
                }
            } else {
                outList.add(channel)
            }
        }

        return outList
    }

    private fun isProperList(listType : Int?, listID : String, isAnoki : Boolean) : Boolean {
        if(!isAnoki) {
            //if on Broadcast service list skip CAM services
            if((listType == 0) && (listID.startsWith("MEDIATEK_CHANNEL_LIST_TERRESTRIAL_CAM/") ||
                        listID.startsWith("MEDIATEK_CHANNEL_LIST_CABLE_CAM/") ||
                        listID.startsWith("MEDIATEK_CHANNEL_LIST_SATELLITE_CAM/") ||
                        listID.startsWith("MEDIATEK_CHANNEL_LIST_GENERAL_SATELLITE_CAM/") ||
                        listID.startsWith("MEDIATEK_CHANNEL_LIST_PREFERRED_SATELLITE_CAM/"))) {
                return false
            }
            //if on CAM service list skip Broadcast services
            if((listType == 1) && (listID.startsWith("MEDIATEK_CHANNEL_LIST_TERRESTRIAL/")
                        || listID.startsWith("MEDIATEK_CHANNEL_LIST_CABLE/") ||
                        listID.startsWith("MEDIATEK_CHANNEL_LIST_GENERAL_SATELLITE/") ||
                        listID.startsWith("MEDIATEK_CHANNEL_LIST_PREFERRED_SATELLITE/") )) {
                return false
            }
        }
        return true
    }

    @Synchronized
    override fun getChannelList(): ArrayList<TvChannel> {
        var list = arrayListOf<TvChannel>()
        if (channelList.isNotEmpty()) {
            list.addAll(getSortedChannelList(channelList))
        }
        return list
    }

    @Synchronized
    private fun updateChannelList(updatedList : ArrayList<TvChannel>) {
        channelList = updatedList
    }

    override fun getSortedChannelList(oldChannelList: MutableList<TvChannel>): java.util.ArrayList<TvChannel> {
        val sortedChannelList = CopyOnWriteArrayList(oldChannelList)
        try {
            return utilsInterface.sortListByNumber(ArrayList(sortedChannelList))
        } catch (ex: Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onReceive: display number missing")
        }
        return ArrayList(sortedChannelList)
    }

    override fun deleteChannel(tvChannel: TvChannel): Boolean {
        val contentResolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues()
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4, 0)
        contentValues.put("channel_operation", NO_BROWSABLE_VALUE)

        var uri = TvContract.buildChannelUri(tvChannel.channelId)
        return try {
            var ret =
                contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null

                )
            if (ret > 0) {
                tvChannel.isBrowsable = false
                channelList.forEach { item ->
                    if (item.channelId == tvChannel.channelId) {
                        item.isBrowsable = false
                    }
                }
                true
            }  else false
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun lockUnlockChannel(tvChannel: TvChannel, lock: Boolean): Boolean {
        val contentResolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues()
        var uri = TvContract.buildChannelUri(tvChannel.channelId)
        var locked = if (lock) 1 else 0
        contentValues.put(TvContract.Channels.COLUMN_LOCKED, locked)

        return try {
            var ret =
                contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
            if (ret > 0) {
                channelLockStatusUpdated = true
                tvChannel.isLocked = lock
                true
            }  else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun isChannelLockAvailable(tvChannel: TvChannel): Boolean {
            return tvChannel.isBrowsable
    }

    override fun skipUnskipChannel(tvChannel: TvChannel, skip: Boolean): Boolean {
        return false
    }

    /**
     * Stop channel data update timer if it is already started
     */
    private fun stopChannelUpdateTimer() {
        if (channelUpdateTimer != null) {
            channelUpdateTimer!!.cancel()
            channelUpdateTimer = null
        }
    }

    /**
     * Start channel data update timer
     */
    private fun startChannelUpdateTimer() {
        //Cancel timer if it's already started
        stopChannelUpdateTimer()

        //Start new count down timer
        channelUpdateTimer = object :
            CountDownTimer(
                CHANNEL_UPDATE_TIMEOUT,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                // Wait for event loading
                var eventListener = ChannelLoadedEventListener(object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                    }

                    override fun onSuccess() {
                        //needs thread or there will be deadlocks
                        thread {
                            InformationBus.informationBusEventListener.submitEvent(Events.CHANNEL_LIST_UPDATED)
                        }
                    }
                })
                loadChannels()
            }
        }
        channelUpdateTimer!!.start()
    }

    inner class ChannelLoadedEventListener(var callback: IAsyncCallback?){

        private var eventListener: Any?= null
        init {
            InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.CHANNELS_LOADED), callback = {
                eventListener = it
            }, onEventReceived = {
                callback?.onSuccess()
                InformationBus.informationBusEventListener.unregisterEventListener(eventListener!!)
            })
        }
    }
}
