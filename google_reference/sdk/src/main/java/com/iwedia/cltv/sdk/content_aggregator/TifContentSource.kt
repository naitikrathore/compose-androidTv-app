package com.iwedia.cltv.sdk.content_aggregator

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri
import android.util.Log
import androidx.core.text.isDigitsOnly
import com.google.gson.Gson
import com.iwedia.cltv.sdk.ReferenceEvents
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import com.iwedia.cltv.sdk.entities.ReferenceTvEvent
import com.iwedia.cltv.sdk.handlers.CoroutineHelper
import core_entities.ContentEntity
import core_entities.ContentSource
import data_type.GList
import data_type.GLong
import listeners.AsyncDataReceiver
import utils.information_bus.Event
import utils.information_bus.EventListener
import utils.information_bus.InformationBus

/**
 * Content source
 */
class TifContentSource constructor(var context: Context?) :
    ContentSource<ContentEntity>(0, "TifContentSource", "", -1, "", null) {

    val TAG = javaClass.simpleName
    val SIMILARITY = 0.5

    var loadedChannels = mutableListOf<ReferenceTvChannel>()
    var loadedEvents = mutableListOf<ReferenceTvEvent>()

    private val INTENT_TIF_CONTENT_READY =  "tif_content_source_ready"

    val PLACEHOLDER_IMAGE_URL =
        "https://firebasestorage.googleapis.com/v0/b/admin-panel-8eb67.appspot.com/o/Company%20Details%2Fbanner.png?alt=media&token=fe10e38c-9de5-4daf-9149-a175d6cc2434"

    init {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: ####### ############ GANDROID SEARCH INIT TIF CONTENT CLASS")
        CoroutineHelper.runCoroutine({
            refreshServicesAndEvents();
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: ####### ############ GANDROID SEARCH SENDING TIF READY")
            val intent = Intent(INTENT_TIF_CONTENT_READY)
            ReferenceSdk.context.sendBroadcast(intent)
        })

        InformationBus.registerEventListener(EventListener())
    }

    inner class EventListener : utils.information_bus.EventListener {

        constructor() {
            addType(ReferenceEvents.CHANNEL_LIST_UPDATED)
            addType(ReferenceEvents.EPG_DATA_UPDATED)

        }

        override fun callback(event: Event?) {
            if (event!!.type == ReferenceEvents.CHANNEL_LIST_UPDATED || event!!.type == ReferenceEvents.EPG_DATA_UPDATED) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "callback: ####### REFRESH EVENT IN EVENT LISTENER REFRESHING")
                loadedChannels.clear()
                loadedEvents.clear()
                refreshServicesAndEvents()
            }
        }
    }

    private fun refreshServicesAndEvents() {
        val contentResolver: ContentResolver = context!!.contentResolver
        var inputList = getInputIds()
        var iterator=inputList?.iterator()
        if (iterator!=null && iterator.next()!=null) {
            while (iterator.hasNext()) {
                var cursor = contentResolver.query(
                    TvContract.buildChannelsUriForInput(iterator.next()),
                    null,
                    null,
                    null,
                    null
                )

                if (cursor!!.count > 0) {
                    cursor.moveToFirst()
                    do {
                        val tvChannel = createChannelFromCursor(cursor)
                        tvChannel.index = loadedChannels.size;
                        loadedChannels.add(tvChannel)
                    } while (cursor.moveToNext())

                }
                cursor!!.close()
            }
        }
        loadedChannels.forEach { item ->
            var startTime = System.currentTimeMillis() - (48 * 60 * 60 * 1000)
            var endTime = System.currentTimeMillis() + (48 * 60 * 60 * 1000)
            loadEvents(item!!, startTime, endTime)
        }
    }

    override fun getContent(): GList<ContentEntity>? {
        return super.getContent()
    }

    override fun scan(receiver: AsyncDataReceiver<GList<ContentEntity>>) {
        super.scan(receiver)
    }

    override fun search(query: String?): GList<ContentEntity>? {
        return super.search(query)
    }

    /**
     * Get list of the available input ids
     *
     * @return available input ids
     */
    private fun getInputIds(): ArrayList<String>? {
        val retList = ArrayList<String>()
        //Get all TV inputs
        for (input in (context!!.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager).tvInputList) {
            try {
                val inputId = input.id
                retList.add(inputId)
            }catch (E: Exception){
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getInputIds: ${E.printStackTrace()}")
                break
            }
        }
        return retList
    }


    @SuppressLint("Range")
    private fun createChannelFromCursor(cursor: Cursor): ReferenceTvChannel {
        var id = -1
        var inputId = ""
        var displayNumber = ""
        var displayName = ""
        var logoImagePath = ""
        if (cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID)) != null) {
            id = cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID)).toInt()
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INPUT_ID)) != null) {
            inputId = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INPUT_ID))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER)) != null) {
            displayNumber =
                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER))
        }
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME)) != null) {
            displayName =
                cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME))
        }
        val channelLogoUri: Uri = TvContract.buildChannelLogoUri(id.toLong())
        if (channelLogoUri != null) {
            logoImagePath = channelLogoUri.toString()
        }
        var index = if (displayNumber.isDigitsOnly()) displayNumber.toInt() else id
        var referenceTvChannel =
            ReferenceTvChannel(id, index, displayName, logoImagePath)
        referenceTvChannel.inputId = inputId;
        referenceTvChannel.displayNumber = displayNumber
        return referenceTvChannel
    }


    override fun searchAsync(query: String?, receiver: AsyncDataReceiver<GList<ContentEntity>>) {

        var gson = Gson()

        var convertedQuery = StringConverter.convertToLatin(query!!)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "searchAsync: ############ ############ GANDROID SEARCH SEARCH ASYNC $query " +
                "$convertedQuery  CHANNEL SIZE ${loadedChannels.size} EVENT SIZE ${loadedEvents.size}")
        var tempResults: MutableList<ContentEntity> = mutableListOf()

        var result: GList<ContentEntity> = GList()
        loadedChannels.forEach { channel ->

            var similarity = similarity(channel.name, convertedQuery)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "searchAsync: ########## LOOP $similarity ${channel.name} $convertedQuery")
            if (similarity > SIMILARITY || channel.name.toLowerCase()
                    .contains(convertedQuery.toLowerCase())
            ) {
                tempResults.add(
                    ContentEntity(
                        channel.id,
                        channel.name,
                        PLACEHOLDER_IMAGE_URL,
                        channel.displayNumber.toString(),
                        1,
                        gson.toJson(channel)
                    )
                )
            }
        }

        loadedEvents.forEach { event ->
            var similarity = similarity(event.name, convertedQuery)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "searchAsync: ########## LOOP EVENT $similarity ${event.name} $convertedQuery")
            if (similarity > SIMILARITY || event.name.toLowerCase()
                    .contains(convertedQuery.toLowerCase())
            ) {
                tempResults.add(
                    ContentEntity(
                        event.id,
                        event.name,
                        PLACEHOLDER_IMAGE_URL,
                        event.tvChannel.name + " | " + event.shortDescription!!,
                        2,
                        gson.toJson(event)
                    )
                )
            }
        }

        tempResults.sortBy { myObject -> myObject.title }

        tempResults.forEach { item -> result.add(item) }


        receiver.onReceive(result)
    }

    private fun loadEvents(
        tvChannel: ReferenceTvChannel,
        startDate: Long,
        endDate: Long,
    ) {
        val uri: Uri = TvContract.buildProgramsUriForChannel(
            tvChannel.id.toLong(),
            startDate,
            endDate
        )

        //Create query
        var cursor: Cursor? = null
        try {
            cursor = ReferenceSdk.context.contentResolver
                .query(uri, null, null, null, null)
        } catch (e: Exception) {
            //For this query system permission is necessary
            //Throw security exception
            e.printStackTrace()
        }

        if (cursor != null) {
            if (cursor.count > 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val cd = createTvEventFromCursor(tvChannel, cursor)
                    loadedEvents.add(cd)
                    cursor.moveToNext()
                }
            }
            cursor.close()
        }
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
            providerFlag = providerFlag
        )
    }


    fun similarity(s1: String, s2: String): Double {
        var longer = s1.lowercase()
        var shorter = s2.lowercase()
        if (s1.length < s2.length) {
            longer = s2
            shorter = s1
        }
        val longerLength = longer.length
        return if (longerLength == 0) {
            1.0 /* both strings have zero length */
        } else (longerLength - getLevenshteinDistance(longer, shorter)) / longerLength.toDouble()
    }

    /**
     * LevenshteinDistance
     * copied from https://commons.apache.org/proper/commons-lang/javadocs/api-2.5/src-html/org/apache/commons/lang/StringUtils.html#line.6162
     */
    fun getLevenshteinDistance(s: String?, t: String?): Int {
        var s = s
        var t = t
        require(!(s == null || t == null)) { "Strings must not be null" }
        var n = s.length // length of s
        var m = t.length // length of t
        if (n == 0) {
            return m
        } else if (m == 0) {
            return n
        }
        if (n > m) {
            // swap the input strings to consume less memory
            val tmp: String = s
            s = t
            t = tmp
            n = m
            m = t.length
        }
        var p = IntArray(n + 1) //'previous' cost array, horizontally
        var d = IntArray(n + 1) // cost array, horizontally
        var _d: IntArray //placeholder to assist in swapping p and d

        // indexes into strings s and t
        var i: Int // iterates through s
        var j: Int // iterates through t
        var t_j: Char // jth character of t
        var cost: Int // cost
        i = 0
        while (i <= n) {
            p[i] = i
            i++
        }
        j = 1
        while (j <= m) {
            t_j = t[j - 1]
            d[0] = j
            i = 1
            while (i <= n) {
                cost = if (s[i - 1] == t_j) 0 else 1
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost)
                i++
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p
            p = d
            d = _d
            j++
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n]
    }
}