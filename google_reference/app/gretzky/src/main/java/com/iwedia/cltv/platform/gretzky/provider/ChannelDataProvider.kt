package com.iwedia.cltv.platform.gretzky.provider

import android.annotation.SuppressLint
import android.content.*
import android.database.ContentObserver
import android.database.Cursor
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.base.content_provider.TifChannelDataProvider
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import org.json.JSONObject
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

/**
 * Gretzky channel data provider implementation
 *
 * @author Dejan Nadj
 */
@RequiresApi(Build.VERSION_CODES.R)
class ChannelDataProvider(context: Context) : TifChannelDataProvider(context) {

    val THIRD_PARTY_CHANNEL_START_POSITON = 10001
    val LCN_COLUMN_URI: Uri =
        Uri.parse("content://${ContentProvider.AUTHORITY}/${"config"}/1/${"lcn"}")
    var display_number_third_party = THIRD_PARTY_CHANNEL_START_POSITON
    val SCRAMBLED_MASK = 0x8
    var numInserted: Int = 0
    val SCAN_COMPLETED_INTENT_ACTION = "scan_completed_sync_databases"
    var scanPerformed = false
    var channelListUpdateTimer: CountDownTimer? = null
    private var lcnConfigObserver: ContentObserver
    val TAG = javaClass.simpleName

    init {
        val intentFilter = IntentFilter(SCAN_COMPLETED_INTENT_ACTION)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (SCAN_COMPLETED_INTENT_ACTION == intent!!.action) {
                    scanPerformed = true
                    startUpdateTimer()
                }
            }
        }, intentFilter)

        var cursor = context.contentResolver.query(
            ContentProvider.CHANNELS_URI,
            null,
            null,
            null,
            null
        )

        Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "init cursor size ${cursor?.count}")
        if (cursor!!.count == 0) {
            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "init channels database is empty")
            initReferenceDatabase()
        }

        var channelListObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                startUpdateTimer()
            }
        }

        context.contentResolver.registerContentObserver(
            TvContract.Channels.CONTENT_URI,
            true,
            channelListObserver
        )

        lcnConfigObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                if (isLcnEnabled()) {
                    loadChannels()
                } else {
                    //Change display number if lcn is not enabled
                    getChannelList().forEach { tvChannel ->
                        tvChannel.displayNumber = tvChannel.ordinalNumber.toString()
                    }
                    var activeChannelIndex =
                        context.getSharedPreferences("OnlinePrefsHandler", Context.MODE_PRIVATE)
                            .getInt("CurrentActiveChannel", 0)
                    var activeChannel =
                        if (getChannelList().isNotEmpty() && activeChannelIndex < getChannelList().size)
                            getChannelList().get(activeChannelIndex)
                        else null
                    sortChannelList(activeChannel)
                }
            }
        }
        context.contentResolver.registerContentObserver(
            LCN_COLUMN_URI,
            true,
            lcnConfigObserver
        )

        loadChannels()
    }

    /**
     * Stop channel list udpate timer if it is already started
     */
    @Synchronized
    private fun stopUpdateTimer() {
        if (channelListUpdateTimer != null) {
            channelListUpdateTimer!!.cancel()
            channelListUpdateTimer = null
        }
    }

    /**
     * Start channel list update timer
     */
    @Synchronized
    private fun startUpdateTimer() {
        //Cancel timer if it's already started
        stopUpdateTimer()

        //Start new count down timer
        channelListUpdateTimer = object :
            CountDownTimer(
                2000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "update channel list")
                initReferenceDatabase()
            }
        }
        channelListUpdateTimer!!.start()
    }

    @SuppressLint("Range")
    private fun getFrequency(cursor: Cursor, fromReferenceDb: Boolean): Int {
        var blob: ByteArray?
        var frequency = 0
        if (fromReferenceDb) {
            blob =
                cursor.getBlob(cursor.getColumnIndex(Contract.Channels.INTERNAL_PROVIDER_DATA_COLUMN))
        } else {
            blob =
                cursor.getBlob(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA))
        }
        try {
            if (blob != null && blob.isNotEmpty()) {
                var providerData = String(blob, Charsets.UTF_8)
                val obj = JSONObject(providerData)
                if (obj != null && obj.has("transport")) {
                    val jsonTransportObject = obj.getJSONObject("transport")
                    if (jsonTransportObject.has("frequency"))
                        frequency = jsonTransportObject.getInt("frequency")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "getFrequency ${e.printStackTrace()}")
            return frequency
        }
        return frequency
    }

    @SuppressLint("Range")
    @RequiresApi(Build.VERSION_CODES.R)
    @Synchronized
    private fun clearReferenceDatabase() {
        Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "clearDatabase")
        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(ContentProvider.CHANNELS_URI, null, null, null)
        var channelsToDelete = arrayListOf<Int>()
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            var serviceId = 0
            var tsId = 0
            var onId = 0
            do {
                if (cursor.getString(cursor.getColumnIndex(Contract.Channels.SERVICE_ID_COLUMN)) != null) {
                    serviceId =
                        cursor.getInt(cursor.getColumnIndex(Contract.Channels.SERVICE_ID_COLUMN))
                }
                if (cursor.getString(cursor.getColumnIndex(Contract.Channels.TRANSPORT_STREAM_ID_COLUMN)) != null) {
                    tsId =
                        cursor.getInt(cursor.getColumnIndex(Contract.Channels.TRANSPORT_STREAM_ID_COLUMN))
                }
                if (cursor.getString(cursor.getColumnIndex(Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN)) != null) {
                    onId =
                        cursor.getInt(cursor.getColumnIndex(Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN))
                }
                var selection =
                    TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID + " = ? and " + TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID + " = ? and " + TvContract.Channels.COLUMN_SERVICE_ID + " = ?"
                var c = contentResolver.query(
                    TvContract.Channels.CONTENT_URI,
                    null,
                    selection,
                    arrayOf(onId.toString(), tsId.toString(), serviceId.toString()),
                    null,
                    null,
                )
                if (c == null || c.count == 0) {
                    var id = cursor.getInt(cursor.getColumnIndex(TvContract.BaseTvColumns._ID))
                    channelsToDelete.add(id)
                } else {
                    var delete = true
                    c.let {
                        it.moveToFirst()
                        do {
                            var frequencyTv = getFrequency(c, false)
                            var frequencyDb = getFrequency(cursor, true)
                            if (frequencyTv == frequencyDb && frequencyTv != 0) {
                                delete = false
                            }
                        } while (it.moveToNext())
                        c.close()
                    }
                    if (delete) {
                        var id = cursor.getInt(cursor.getColumnIndex(TvContract.BaseTvColumns._ID))
                        channelsToDelete.add(id)
                    }
                }
            } while (cursor.moveToNext())
            cursor.close()
        }
        channelsToDelete.forEach { id ->
            contentResolver.delete(Contract.buildChannelsUri(id.toLong()), null)
        }
        Log.d(Constants.LogTag.CLTV_TAG +
            "GlobalAppReceiver [clearDatabase]: ",
            "number of deleted channels ${channelsToDelete.size}"
        )
    }

    @SuppressLint("Range")
    fun isThirdPartyInputEnabled(): Boolean {
        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(
            ContentProvider.OEM_CUSTOMIZATION_URI,
            null,
            null,
            null,
            null
        )
        if (cursor!!.count > 0) {
            cursor.moveToFirst()
            if (cursor.getInt(cursor.getColumnIndex(Contract.OemCustomization.THIRD_PARTY_INPUT_ENABLED_COLUMN)) == 0) {
                return false
            }
        }
        return true
    }

    private fun getInputIds(): ArrayList<String>? {
        val retList = ArrayList<String>()
        //Get all TV inputs
        for (input in (context.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager).tvInputList) {
            val inputId = input.id
            retList.add(inputId)
        }
        return retList
    }

    inner class DisplayNumberChangeInfo(
        val displayNumber: String,
        val ordinalNumber: Int,
        val onid: Int,
        val tsid: Int,
        val serviceId: Int,
        val frequency: Int,
    )

    @SuppressLint("Range")
    private fun setDisplayNumber(
        cursor: Cursor,
        input: String,
        value: ContentValues,
        displayNumberList: ArrayList<String>
    ) {
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER)) != null) {
            var displayNumber =
                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER))
            //3rd party channel position should start from 10001
            if (input.contains("com.google.android.tv.dtvinput") || input.contains("com.mediatek.tvinput")) {
                //If normal channel's display Number is 10000+, make sure no conflict
                try {
                    var int_display_number = displayNumber.toInt()
                    if (int_display_number >= THIRD_PARTY_CHANNEL_START_POSITON) {
                        if (int_display_number <= display_number_third_party) {
                            int_display_number = display_number_third_party++
                        }
                        while (displayNumberList.contains(int_display_number.toString())) {
                            int_display_number = display_number_third_party++
                        }
                        displayNumber = int_display_number.toString()
                    }
                } catch (e: NumberFormatException) {
                }
            } else {
                //3rd party channels - position should not be below 10000.
                var updateDisplayNumber = false
                try {
                    if (displayNumber.toInt() < THIRD_PARTY_CHANNEL_START_POSITON) {
                        updateDisplayNumber = true
                    }
                } catch (e: NumberFormatException) {
                    // 3rd party channel is not having proper LCN, update it.
                    updateDisplayNumber = true
                }
                if (updateDisplayNumber) {
                    while (displayNumberList.contains(display_number_third_party.toString())) {
                        display_number_third_party++
                    }
                    displayNumber = display_number_third_party.toString()
                    display_number_third_party++
                }
            }
            value.put(Contract.Channels.DISPLAY_NUMBER_COLUMN, displayNumber)
            displayNumberList.add(displayNumber)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("Range")
    @Synchronized
    private fun initReferenceDatabase() {
        Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "initDatabase")
        var context = context
        val contentResolver: ContentResolver = context.contentResolver
        var inputList = getInputIds()
        // Clear previous database content
        clearReferenceDatabase()
        display_number_third_party = THIRD_PARTY_CHANNEL_START_POSITON
        //Get the display number for all swapped channels
        var displayNumberChangeList = ArrayList<DisplayNumberChangeInfo>()
        //Get the display number for all the existing channels
        var displayNumberList = ArrayList<String>()
        var temp_cursor = contentResolver.query(
            ContentProvider.CHANNELS_URI,
            null, null,
            null, null
        )
        if (temp_cursor!!.count > 0) {
            temp_cursor.moveToFirst()
            do {
                if (temp_cursor.getString(temp_cursor.getColumnIndex(Contract.Channels.DISPLAY_NUMBER_COLUMN)) != null) {
                    if (temp_cursor.getInt(temp_cursor.getColumnIndex(Contract.Channels.DISPLAY_NUMBER_CHANGED_COLUMN)) == 1) {
                        displayNumberChangeList.add(
                            DisplayNumberChangeInfo(
                                temp_cursor.getString(temp_cursor.getColumnIndex(Contract.Channels.DISPLAY_NUMBER_COLUMN)),
                                temp_cursor.getInt(temp_cursor.getColumnIndex(Contract.Channels.ORDINAL_NUMBER_COLUMN)),
                                temp_cursor.getInt(temp_cursor.getColumnIndex(Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN)),
                                temp_cursor.getInt(temp_cursor.getColumnIndex(Contract.Channels.TRANSPORT_STREAM_ID_COLUMN)),
                                temp_cursor.getInt(temp_cursor.getColumnIndex(Contract.Channels.SERVICE_ID_COLUMN)),
                                getFrequency(temp_cursor, true)
                            )
                        )
                    } else {
                        displayNumberList.add(
                            temp_cursor.getString(
                                temp_cursor.getColumnIndex(
                                    Contract.Channels.DISPLAY_NUMBER_COLUMN
                                )
                            )
                        )
                    }
                }
            } while (temp_cursor.moveToNext())
        }
        temp_cursor?.close()

        var ordinalNumber = 1
        if (inputList!!.isNotEmpty()) {
            for (input in inputList) {
                //If third party input customization value is disabled, don't include the third party channel to database.
                if ((!isThirdPartyInputEnabled()) && !(input.contains("com.google.android.tv.dtvinput"))) {
                    Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "Skipping 3rd party content.. $input")
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
                    val contentValues = ArrayList<ContentValues>(cursor.count)
                    cursor.moveToFirst()
                    do {
                        var value = ContentValues()
                        if (cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID)) != null) {
                            var id = cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID))
                                .toInt()
                            value.put(Contract.Channels.ORIG_ID_COLUMN, id)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_PACKAGE_NAME)) != null) {
                            var packageName =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_PACKAGE_NAME))
                            value.put(Contract.Channels.PACKAGE_NAME_COLUMN, packageName)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INPUT_ID)) != null) {
                            var inputId =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INPUT_ID))
                            value.put(Contract.Channels.INPUT_ID_COLUMN, inputId)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_TYPE)) != null) {
                            var type =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_TYPE))
                            value.put(Contract.Channels.TYPE_COLUMN, type)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_TYPE)) != null) {
                            var serviceType =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_TYPE))
                            value.put(Contract.Channels.SERVICE_TYPE_COLUMN, serviceType)
                        }
                        var onid = 0
                        var tsid = 0
                        var serviceId = 0
                        var name = ""
                        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID)) != null) {
                            onid =
                                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID))
                            value.put(Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN, onid)
                        }
                        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID)) != null) {
                            tsid =
                                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID))
                            value.put(Contract.Channels.TRANSPORT_STREAM_ID_COLUMN, tsid)
                        }
                        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID)) != null) {
                            serviceId =
                                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID))
                            value.put(Contract.Channels.SERVICE_ID_COLUMN, serviceId)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_NETWORK_AFFILIATION)) != null) {
                            var networkAffiliation = cursor.getString(
                                cursor.getColumnIndex(
                                    TvContract.Channels.COLUMN_NETWORK_AFFILIATION
                                )
                            )
                            value.put(
                                Contract.Channels.NETWORK_AFFILIATION_COLUMN,
                                networkAffiliation
                            )
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DESCRIPTION)) != null) {
                            var description =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DESCRIPTION))
                            value.put(Contract.Channels.DESCRIPTION_COLUMN, description)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_VIDEO_FORMAT)) != null) {
                            var videoFormat =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_VIDEO_FORMAT))
                            value.put(Contract.Channels.VIDEO_FORMAT_COLUMN, videoFormat)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME)) != null) {
                            name =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME))
                            value.put(Contract.Channels.NAME_COLUMN, name)
                        }

                        var browsable = 1
                        var oemCursor: Cursor? = contentResolver.query(
                            ContentProvider.OEM_CUSTOMIZATION_URI,
                            null,
                            null,
                            null,
                            null
                        )
                        if ((oemCursor != null) && (oemCursor!!.count > 0)) {
                            var scrambled = false
                            oemCursor.moveToFirst()
                            val scanTypeIndex =
                                oemCursor.getColumnIndex(Contract.OemCustomization.SCAN_TYPE)
                            if (scanTypeIndex != -1) {
                                val scanType =
                                    oemCursor.getString(oemCursor.getColumnIndex(Contract.OemCustomization.SCAN_TYPE))
                                if (scanType.equals("free")) {
                                    var mInternalProviderFlag1 =
                                        cursor.getInt(cursor.getColumnIndex(Contract.Channels.INTERNAL_PROVIDER_FLAG1_COLUMN))
                                    if ((mInternalProviderFlag1 and SCRAMBLED_MASK) !== 0) {
                                        scrambled = true
                                    }
                                }
                                if (scrambled) {
                                    browsable = 0
                                }
                            }
                            oemCursor?.close()
                        }
                        value.put(Contract.Channels.BROWSABLE_COLUMN, browsable)

                        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SEARCHABLE)) != null) {
                            var searchable =
                                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SEARCHABLE))
                            value.put(Contract.Channels.SEARCHABLE_COLUMN, searchable)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_ICON_URI)) != null) {
                            var iconUri =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_ICON_URI))
                            value.put(Contract.Channels.APP_LINK_ICON_URI_COLUMN, iconUri)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI)) != null) {
                            var posterArtUri =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI))
                            value.put(
                                Contract.Channels.APP_LINK_POSTER_ART_URI_COLUMN,
                                posterArtUri
                            )
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI)) != null) {
                            var posterArtUri =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI))
                            value.put(
                                Contract.Channels.APP_LINK_POSTER_ART_URI_COLUMN,
                                posterArtUri
                            )
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_TEXT)) != null) {
                            var appLinkText =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_TEXT))
                            value.put(Contract.Channels.APP_LINK_TEXT_COLUMN, appLinkText)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_COLOR)) != null) {
                            var appLinkColor =
                                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_COLOR))
                            value.put(
                                Contract.Channels.APP_LINK_COLOR_COLUMN,
                                appLinkColor
                            )
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_INTENT_URI)) != null) {
                            var appLinkIntent =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_INTENT_URI))
                            value.put(
                                Contract.Channels.APP_LINK_INTENT_URI_COLUMN,
                                appLinkIntent
                            )
                        }
                        if (cursor.getBlob(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA)) != null) {
                            var blob =
                                cursor.getBlob(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA))
                            value.put(
                                Contract.Channels.INTERNAL_PROVIDER_DATA_COLUMN,
                                blob
                            )
                        }

                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1)) != null) {
                            var flag1 =
                                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1))
                            value.put(
                                Contract.Channels.INTERNAL_PROVIDER_FLAG1_COLUMN,
                                flag1
                            )
                            if (((flag1 and 0x1) == 0)) {
                                browsable = 0
                                value.put(Contract.Channels.BROWSABLE_COLUMN, browsable)
                            }
                        }

                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_VERSION_NUMBER)) != null) {
                            var versionNumber =
                                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_VERSION_NUMBER))
                            value.put(
                                Contract.Channels.VERSION_NUMBER_COLUMN,
                                versionNumber
                            )
                        }
                        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSIENT)) != null) {
                            var transient =
                                cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSIENT))
                            value.put(Contract.Channels.TRANSIENT_COLUMN, transient)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID)) != null) {
                            var transient =
                                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID))
                            value.put(Contract.Channels.TRANSIENT_COLUMN, transient)
                        }
                        value.put(
                            Contract.Channels.ORDINAL_NUMBER_COLUMN,
                            ordinalNumber++
                        )

                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER)) != null) {
                            var displayNumberSet = false
                            var frequency = getFrequency(cursor, false)
                            displayNumberChangeList.forEach {
                                if ((it.onid == onid) && (it.tsid == tsid) && (it.serviceId == serviceId) && (it.frequency == frequency)) {
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        "GlobalAppReceiver ",
                                        "Keeping channel swap ${it.displayNumber} for $name onid: $onid tsid:  $tsid sid: $serviceId freq: $frequency"
                                    )
                                    value.put(
                                        Contract.Channels.DISPLAY_NUMBER_COLUMN,
                                        it.displayNumber
                                    )
                                    value.put(
                                        Contract.Channels.ORDINAL_NUMBER_COLUMN,
                                        it.ordinalNumber
                                    )
                                    displayNumberSet = true
                                }
                            }
                            if (!displayNumberSet) {
                                setDisplayNumber(cursor, input, value, displayNumberList)
                            }
                        }

                        //Update existing channel in db
                        var selection =
                            Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN + " = ? and " + Contract.Channels.TRANSPORT_STREAM_ID_COLUMN + " = ? and " + Contract.Channels.SERVICE_ID_COLUMN + " = ?"
                        var c = contentResolver.query(
                            ContentProvider.CHANNELS_URI,
                            null,
                            selection,
                            arrayOf(onid.toString(), tsid.toString(), serviceId.toString()),
                            null
                        )
                        var updateId = -1
                        var isDeleted = 0
                        Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "check update  $name")
                        if (c != null && c.count > 0) {
                            c.moveToFirst()
                            do {
                                var frequencyTv = getFrequency(cursor, false)
                                var frequencyRef = getFrequency(c, true)
                                if (frequencyTv == frequencyRef && frequencyTv != 0) {
                                    updateId = c.getInt(c.getColumnIndex(TvContract.Channels._ID))
                                    isDeleted =
                                        c.getInt(c.getColumnIndex(Contract.Channels.DELETED_COLUMN))
                                }
                            } while (c.moveToNext())
                            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "check update  $name $updateId")
                            if (updateId != -1) {

                                if (isDeleted == 1 && !scanPerformed) {
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        "GlobalAppReceiver ",
                                        "Channel was manually deleted, do not update its browsable"
                                    )
                                    browsable = 0
                                    value.put(
                                        Contract.Channels.BROWSABLE_COLUMN,
                                        browsable
                                    )
                                }
                                contentResolver.update(
                                    Contract.buildChannelsUri(updateId.toLong()),
                                    value,
                                    null
                                )
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    "GlobalAppReceiver ",
                                    "updated channel $updateId $onid $tsid $serviceId"
                                )
                            } else {
                                if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_LOCKED)) != null) {
                                    var locked =
                                        cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_LOCKED))
                                    value.put(Contract.Channels.LOCKED_COLUMN, locked)
                                }
                                value.put(Contract.Channels.SKIP_COLUMN, 0.toInt())
                                contentValues.add(value)
                            }
                        } else {
                            if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_LOCKED)) != null) {
                                var locked =
                                    cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_LOCKED))
                                value.put(Contract.Channels.LOCKED_COLUMN, locked)
                            }
                            value.put(Contract.Channels.SKIP_COLUMN, 0.toInt())
                            Log.d(Constants.LogTag.CLTV_TAG +
                                "GlobalAppReceiver ",
                                "added channel $name $onid $tsid $serviceId"
                            )
                            contentValues.add(value)
                        }
                        c!!.close()
                    } while (cursor.moveToNext())
                    var cv = arrayOfNulls<ContentValues>(contentValues.size)
                    for (qwe in 0..contentValues.size - 1) {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "initReferenceDatabase: contentValues [$qwe] = ${
                                contentValues.get(qwe)
                            }"
                        )
                    }
                    cv = contentValues.toArray(cv)
                    //todo notification is sent before bulkInsert is finished - this might cause a problem latter on
                    numInserted =
                        contentResolver.bulkInsert(ContentProvider.CHANNELS_URI, cv)
                    Log.d(Constants.LogTag.CLTV_TAG + "ChannelLog", "init database number of inserted channels $numInserted")
                }
                cursor!!.close()
            }
        }
        loadChannels()
    }


    @Synchronized
    override fun loadChannels() {
        CoroutineHelper.runCoroutine({
            var activeChannelIndex =
                context.getSharedPreferences("OnlinePrefsHandler", Context.MODE_PRIVATE)
                    .getInt("CurrentActiveChannel", 0)
            var activeChannel =
                if (getChannelList().isNotEmpty() && activeChannelIndex < getChannelList().size)
                    getChannelList().get(activeChannelIndex)
                else null
            super.clearChannelList()
            val contentResolver: ContentResolver = context.contentResolver
            var cursor = contentResolver.query(
                ContentProvider.CHANNELS_URI,
                null,
                null,
                null,
                null
            )
            var isLcnEnabled = isLcnEnabled()
            if (cursor!!.count > 0) {
                cursor?.moveToFirst()
                do {
                    val channel = (createChannelFromCursor(cursor))
                    if (!isLcnEnabled) {
                        channel.displayNumber = channel.ordinalNumber.toString()
                    }
                    if (channel.isBrowsable) {
                        super.addChannel(channel)
                    }
                } while (cursor.moveToNext())
            }
            cursor?.close()

            sortChannelList(activeChannel)
            InformationBus.informationBusEventListener?.submitEvent(Events.CHANNELS_LOADED)
        })
    }

    @Synchronized
    private fun sortChannelList(activeChannel: TvChannel?) {
        //Sort channels
        Collections.sort(getChannelList(),
            Comparator { channel1, channel2 ->
                if (channel1.displayNumber > channel2.displayNumber) {
                    return@Comparator 1
                } else if (channel1.displayNumber < channel2.displayNumber) {
                    return@Comparator -1
                }
                0
            })

        //Set channel indexes
        var index = 0
        getChannelList().forEach { tvChannel ->
            tvChannel.index = index
            index++
        }
        activeChannel?.let {
            updateActiveChannel(it)
        }
    }

    private fun updateActiveChannel(activeChannel: TvChannel) {
        var newActiveChannelIndex = 0

        run exitForEach@{
            getChannelList().forEach { tvChannel ->
                if (activeChannel.channelId == tvChannel.channelId) {
                    newActiveChannelIndex = tvChannel.index
                    return@exitForEach
                }
            }
        }
        context.getSharedPreferences("OnlinePrefsHandler", Context.MODE_PRIVATE).edit()
            .putInt("CurrentActiveChannel", newActiveChannelIndex)
            .apply()
    }

    override fun deleteChannel(tvChannel: TvChannel): Boolean {
        val contentResolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues()
        val uri = Contract.buildChannelsUri(tvChannel.internalId)
        contentValues.put(Contract.Channels.BROWSABLE_COLUMN, 0)
        contentValues.put(Contract.Channels.DELETED_COLUMN, 1)

        return try {
            var ret =
                contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
            tvChannel.isBrowsable = false

            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun lockUnlockChannel(tvChannel: TvChannel, lock: Boolean): Boolean {
        val contentResolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues()
        val uri = Contract.buildChannelsUri(tvChannel.internalId)
        var locked = if (lock) 1 else 0
        contentValues.put(Contract.Channels.LOCKED_COLUMN, locked)

        return try {
            var ret =
                contentResolver.update(
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

    override fun skipUnskipChannel(tvChannel: TvChannel, skip: Boolean): Boolean {
        val contentResolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues()
        var isSkipped = if (skip) 1 else 0
        contentValues.put(Contract.Channels.SKIP_COLUMN, isSkipped)

        val uri = Contract.buildChannelsUri(tvChannel.internalId)

        return try {
            var ret =
                contentResolver.update(
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

    override fun enableLcn(enableLcn: Boolean) {
        var contentValues = ContentValues()
        var lcn = if (enableLcn) "true" else "false"
        contentValues.put(Contract.Config.LCN_COLUMN, lcn)
        var uri = ContentProvider.CONFIG_URI

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
    override fun isLcnEnabled(): Boolean {
        var isLcnEnabled = false
        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(
            ContentProvider.CONFIG_URI,
            null,
            null,
            null,
            null
        )
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            if (cursor.getString(cursor.getColumnIndex(Contract.Config.LCN_COLUMN)) != null) {
                val lcn =
                    cursor.getString(cursor.getColumnIndex(Contract.Config.LCN_COLUMN))
                        .toString()
                isLcnEnabled = lcn == "true"
            }
        }
        return isLcnEnabled
    }
}