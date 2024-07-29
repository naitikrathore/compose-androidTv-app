package com.iwedia.cltv.sdk.media

import android.app.SearchManager
import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.media.tv.TvContract
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ConditionVariable
import android.provider.BaseColumns
import android.util.Log
import com.google.gson.Gson
import content_aggregator.ContentAggregator
import core_entities.ContentEntity
import core_entities.ContentSource
import core_entities.Error
import data_type.GList
import listeners.AsyncDataReceiver
import utils.information_bus.InformationBus
import utils.information_bus.events.SearchForContentQueryEvent


class GoogleAssistantContentProvider<S : ContentSource<C>?, C : ContentEntity, T : ContentAggregator<S, C>?> :
    ContentProvider {

    private val ACTION_CHANNEL_TUNE = "2"
    private val URI_PARAM_ACTION = "action"
    val TAG = javaClass.simpleName

    private val TURN_TO_CHANNEL_COLUMNS = arrayOf(
        SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA,
        SearchManager.SUGGEST_COLUMN_CONTENT_TYPE
    )

    private val SEARCHABLE_COLUMNS = arrayOf<String>(
        SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA,

    )
    private val HDMI1InputId = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW4"
    private val HDMI2InputId = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW5";
    private val HDMI3InputId = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW6"

    // Required data for "Turn to channel" command
    private val INTENT_ACTION = "android.intent.action.VIEW"
    private val INTENT_DATA = "content://android.media.tv/channel/"
    private val CONTENT_TYPE = "vnd.android.cursor.item/program"
    private val INTENT_TIF_CONTENT_READY =  "tif_content_source_ready"

    private var tifInitialized = false

    constructor() : super()

    val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            when (intent?.action) {
                INTENT_TIF_CONTENT_READY-> {
                    tifInitialized = true
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: ############ GANDROID SEARCH PROVIDER TIF INITIALIZED RECEIVED")
                }
            }
        }
    }

    override fun onCreate(): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreate: ############ GANDROID SEARCH PROVIDER CREATED")
        val intentFilter = IntentFilter(INTENT_TIF_CONTENT_READY)
        context!!.registerReceiver(broadCastReceiver, intentFilter)
        return true
    }

    @Deprecated("")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    @Deprecated("")
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    @Deprecated("")
    /**
     * Stubed and overriden with  query(Uri, String[], Bundle, CancellationSignal)
     * https://developer.android.com/reference/android/content/ContentProvider.html#query(android.net.Uri,%20java.lang.String[],%20java.lang.String,%20java.lang.String[],%20java.lang.String)
     */
    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        // If received command is "Turn to channel", Uri contains parameter
        // URI_PARAM_ACTION ('action') which is equal to ACTION_CHANNEL_TUNE ('2').
        // Creating Cursor with required data for tuning to channel.
        // If received command is "Turn to channel", Uri contains parameter
        // URI_PARAM_ACTION ('action') which is equal to ACTION_CHANNEL_TUNE ('2').
        // Creating Cursor with required data for tuning to channel.
        val action = uri.getQueryParameter(URI_PARAM_ACTION)
        if (action != null && action == ACTION_CHANNEL_TUNE) {
            return createTurnToChannelCursor(selectionArgs!![0])
        }

        //Stub
        return null
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        return 0
    }


    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
        cancellationSignal: CancellationSignal?
    ): Cursor? {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "query: ############ GANDROID SEARCH PROVIDER ENTER uri = $uri")
        // If received command is "Turn to channel", Uri contains parameter
        // URI_PARAM_ACTION ('action') which is equal to ACTION_CHANNEL_TUNE ('2').
        // Creating Cursor with required data for tuning to channel.
        // If received command is "Turn to channel", Uri contains parameter
        // URI_PARAM_ACTION ('action') which is equal to ACTION_CHANNEL_TUNE ('2').
        // Creating Cursor with required data for tuning to channel.
        val action = uri.getQueryParameter(URI_PARAM_ACTION)
        if (action != null && action == ACTION_CHANNEL_TUNE) {
            return createTurnToChannelCursor(selectionArgs!![0])
        }

        return null
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        queryArgs: Bundle?,
        cancellationSignal: CancellationSignal?
    ): Cursor? {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "query: ############ GANDROID SEARCH PROVIDER ENTER uri =  $uri")
        var tifWaitRetry = 0
        //query gets called twice, first time before the search with "dummy" in queryArgs
        //second time after the search with search query

        //query gets called twice, first time before the search with "dummy" in queryArgs
        //second time after the search with search query
        val searchQueries = queryArgs!!.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS)

        if (searchQueries != null && searchQueries.size > 0) {
            for (searchQuery in searchQueries) {

                //backend expects lower case query
                val searchQueryTemp = searchQuery.toLowerCase()
                //Handle voice search to inputs
                if (searchQueryTemp.contains("hdmi")) {
                    var inputId = ""
                    var inputname = ""
                    if (searchQueryTemp == "hdmi 1") {
                        inputname ="HDMI 1"
                         inputId = HDMI1InputId
                    } else if (searchQueryTemp == "hdmi 2") {
                        inputname = "HDMI 2"
                        inputId = HDMI2InputId
                    } else if (searchQueryTemp == "hdmi 3") {
                        inputname = "HDMI 3"
                        inputId = HDMI3InputId
                    }
                    return  createInputsCursor(inputname,inputId)
                }

                val action = uri.getQueryParameter(URI_PARAM_ACTION)
                if (action != null && action == ACTION_CHANNEL_TUNE) {
                    return createTurnToChannelCursor(searchQueryTemp)
                }


                val conditionVariable = ConditionVariable()
                val content: GList<C> = GList()
                val gson = Gson()

                while((!tifInitialized) && (tifWaitRetry < 10)) {
                    Thread.sleep(1000)
                    tifWaitRetry++
                }
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "query: ############ GANDROID SEARCH PROVIDER FIRE EVENT searchQuery = $searchQueryTemp")
                // Submit search for content information bus event and wait for search result
                InformationBus.submitEvent(SearchForContentQueryEvent(searchQueryTemp, object :
                    AsyncDataReceiver<GList<ContentEntity>> {

                    override fun onReceive(data: GList<ContentEntity>) {
                        data.value.forEach { item ->
                            content.add(item as C)
                        }
                        conditionVariable.open()
                    }

                    override fun onFailed(error: Error?) {
                    }
                }))
                conditionVariable.block(10000)
                if (content.size() > 0) {
                    val cursor = MatrixCursor(
                        arrayOf(
                            BaseColumns._ID,
                            SearchManager.SUGGEST_COLUMN_TEXT_1,
                            SearchManager.SUGGEST_COLUMN_ICON_1,
                            SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
                            SearchManager.SUGGEST_COLUMN_DURATION,
                            SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                            SearchManager.SUGGEST_COLUMN_TEXT_2
                        ), 0
                    )
                    var i = 0
                    content.value.forEach { item ->
                        cursor.addRow(
                            arrayOf(
                                i.toString(),
                                item.title,
                                item.thumbnail, 0.toString(), 0.toString(),
                                gson.toJson(item),
                                item.description
                            )
                        )

                        i++
                    }
                    cursor.setNotificationUri(context!!.contentResolver, uri)
                    return cursor
                }
            }
        }
        return MatrixCursor(
            arrayOf(
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_ICON_1,
                SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
                SearchManager.SUGGEST_COLUMN_DURATION,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                SearchManager.SUGGEST_COLUMN_TEXT_2
            ), 0
        )
    }

    /**
     *Builds up a cursor for Turn to channel command
     *@return Cursor instance
     */
    private fun createTurnToChannelCursor(channelNumber: String): Cursor? {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "createTurnToChannelCursor: ############ GANDROID SEARCH PROVIDER CREATE TURN TO CHANNEL CURSORS $channelNumber")
        val cursor = MatrixCursor(TURN_TO_CHANNEL_COLUMNS, 1)
        val row: MutableList<String?> = ArrayList(TURN_TO_CHANNEL_COLUMNS.size)
        row.add(INTENT_ACTION)
        row.add(INTENT_DATA.toString() + channelNumber)
        row.add(CONTENT_TYPE)
        cursor.addRow(row)
        return cursor
    }

    private fun createInputsCursor(input: String, inputId : String) : Cursor {

        val cursor = MatrixCursor(SEARCHABLE_COLUMNS)
        val INPUT_INTENT_ACTION = "com.iwedia.cltv.inputs.assistant"
        val INPUT_INTENT_DATA = TvContract.buildChannelUriForPassthroughInput(inputId).toString()+input
        val row: MutableList<String?> = ArrayList(SEARCHABLE_COLUMNS.size)
        row.add(INPUT_INTENT_ACTION)
        row.add(INPUT_INTENT_DATA)
        cursor.addRow(row)
        return cursor
    }
}