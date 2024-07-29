package com.iwedia.cltv.sdk

import ReferenceCiPlusHandler
import TvConfigurationHelper
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider
import com.iwedia.cltv.sdk.content_provider.ReferenceContract
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import com.iwedia.cltv.sdk.entities.ReferenceTvEvent
import com.iwedia.cltv.sdk.handlers.*
import com.iwedia.guide.android.tools.GAndroidPlatformHandler
import com.iwedia.guide.android.tools.GAndroidPrefsHandler
import core_entities.*
import default_sdk.entities.*
import handlers.ChannelsHandler
import handlers.DataProvider
import handlers.FavoritesHandler
import handlers.PvrHandler
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import utils.BaseSdk
import utils.information_bus.Event
import utils.information_bus.EventListener
import utils.information_bus.InformationBus
import utils.information_bus.events.Events


@SuppressLint("StaticFieldLeak")
object ReferenceSdk : BaseSdk<
        ReferenceTvChannel,
        ReferenceTvEvent,
        UserProfile<DefaultSocialNetwork, PromoPackage<ReferenceTvChannel, DefaultVod, DefaultVodSeries>, SubscriptionPackage<ReferenceTvChannel, DefaultVod, DefaultVodSeries>>,
        DefaultSocialNetwork,
        PromoPackage<ReferenceTvChannel, DefaultVod, DefaultVodSeries>,
        SubscriptionPackage<ReferenceTvChannel, DefaultVod, DefaultVodSeries>,
        DefaultVod,
        DefaultVodSeries,
        DefaultVodSeason,
        DefaultVodEpisode,
        DefaultFavoriteItem,
        Recording<ReferenceTvChannel, ReferenceTvEvent, String>,
        ScheduledRecording<ReferenceTvChannel, ReferenceTvEvent>,
        ScheduledReminder<ReferenceTvChannel, ReferenceTvEvent>,
        App<Any>,
        DefaultRecommended>() {

    var IS_CATCH_UP_SUPPORTED = false
    var IS_TIME_SHIFT_SUPPORTED = true
    val TV_INPUT_SETUP_ACTIVITY_REQUEST_CODE = 999
    val CUSTOM_SCAN_MIDDLEWARE_CODE = 1000

    val TAG = "ReferenceSdk"

    lateinit var context: Context
    lateinit var activity: Activity

    var sdkListener: ReferenceSdkListener? = null

    var tvInputHandler: ReferenceTvInputHandler? = null
    var recentlyHandler: ReferenceRecentlyHandler? = null
    var referenceSearchHandler: ReferenceSearchHandler? = null
    var recordingsHandler: ReferenceRecordingsHandler? = null
    var watchlistHandler: ReferenceWatchlistHandler? = null

    var pvrSchedulerHandler: ReferencePvrSchedulerHandler? = null
    var networkHandler: ReferenceNetworkHandler? = null
    var ciPlusHandler: ReferenceCiPlusHandler? = null

    var isLcnEnabled: Boolean? = null
    private var isRecordingStarted: Boolean? = null
    private lateinit var lcnConfigObserver: ContentObserver
    private lateinit var countryConfigObserver: ContentObserver

    val CONFIG_TABLE = "config"
    val LCN_COLUMN_ID = "lcn"
    val COUNTRY_COLUMN_ID = "current_country"
    val SCAN_TYPE_ID = "scan_type"

    val LCN_COLUMN_URI: Uri =
        Uri.parse("content://${ReferenceContentProvider.AUTHORITY}/${CONFIG_TABLE}/1/${LCN_COLUMN_ID}")
    val COUNTRY_COLUMN_URI: Uri =
        Uri.parse("content://${ReferenceContentProvider.AUTHORITY}/${CONFIG_TABLE}/1/${COUNTRY_COLUMN_ID}")
    val SCAN_TYPE_URI: Uri =
        Uri.parse("content://${ReferenceContentProvider.AUTHORITY}/${CONFIG_TABLE}/1/${SCAN_TYPE_ID}")

    override fun preInitDataProvider() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "PreInitDataProvider called")
        super.preInitDataProvider()

        prefsHandler = GAndroidPrefsHandler(context)
        handlersList.add(prefsHandler!!)

        if (networkHandler == null) {
            networkHandler = ReferenceNetworkHandler(context)
            networkHandler!!.setup()
        }
        handlersList.add(networkHandler!!)

        checkLcn()
        lcnConfigObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Lcn changed")
                //checkLcn()
            }
        }
        context.contentResolver.registerContentObserver(
            LCN_COLUMN_URI,
            true,
            lcnConfigObserver
        )


        countryConfigObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Country changed")
                TvConfigurationHelper.setDeafaultLanguages()
            }
        }
        context.contentResolver.registerContentObserver(
            COUNTRY_COLUMN_URI,
            true,
            countryConfigObserver
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun postInitDataProvider() {
        super.postInitDataProvider()

        if (tvHandler == null) {
            tvHandler = ReferenceTvHandler(dataProvider!!)
            tvHandler!!.setup()
        }
        handlersList.add(tvHandler!!)

        if (channelsHandler == null) {
            channelsHandler = ChannelsHandler(dataProvider!!)
            //channelsHandler!!.setup()
        }

        handlersList.add(channelsHandler!!)

        if (pvrSchedulerHandler == null) {
            pvrSchedulerHandler = ReferencePvrSchedulerHandler(dataProvider!!)
            pvrSchedulerHandler!!.setup()
        }
        handlersList.add(pvrSchedulerHandler!!)

        if (tvInputHandler == null) {
            tvInputHandler = ReferenceTvInputHandler(context)
            sdkListener!!.runOnUiThread(Runnable {
                tvInputHandler!!.setup()
            })
        }
        handlersList.add(tvInputHandler!!)

        if (epgHandler == null) {
            epgHandler = ReferenceEpgHandler(dataProvider!!)
//            epgHandler = ReferenceEpgHandler(dataProvider!!) as EpgHandler<ReferenceTvChannel, TvEvent<ReferenceTvChannel>>
            //epgHandler!!.setup()
        }
        handlersList.add(epgHandler!!)

        if (platformHandler == null) {
            platformHandler = GAndroidPlatformHandler(activity)
            platformHandler!!.setup()
        }
        handlersList.add(platformHandler!!)

        if (recentlyHandler == null) {
            recentlyHandler = ReferenceRecentlyHandler()
            recentlyHandler!!.setup()
        }
        handlersList.add(recentlyHandler!!)

        if (referenceSearchHandler == null) {
            referenceSearchHandler = ReferenceSearchHandler(dataProvider!!)
        }
        handlersList.add(referenceSearchHandler!!)

        if (categoryHandler == null) {
            categoryHandler = ReferenceCategoryHandler(dataProvider!!)
            categoryHandler!!.init(object : AsyncReceiver {
                override fun onFailed(error: Error?) {
                    TODO("Not yet implemented")
                }

                override fun onSuccess() {
//                    InformationBus.submitEvent(AppInitializedEvent())
                }

            })
        }
        handlersList.add(categoryHandler!!)

        if (favoriteHandler == null) {
            favoriteHandler =
                ReferenceFavoriteHandler(dataProvider!!) as FavoritesHandler<DefaultFavoriteItem>
        }
        handlersList.add(favoriteHandler!!)

        if (recordingsHandler == null) {
            recordingsHandler = ReferenceRecordingsHandler(dataProvider!!)
        }
        handlersList.add(recordingsHandler!!)

        if (pvrHandler == null) {
            pvrHandler =
                ReferencePVRHandler(dataProvider!!) as PvrHandler<ReferenceTvChannel, ReferenceTvEvent, Recording<ReferenceTvChannel, ReferenceTvEvent, String>>
            pvrHandler!!.setup()
        }
        handlersList.add(pvrHandler!!)

        if (watchlistHandler == null) {
            watchlistHandler = ReferenceWatchlistHandler()
            watchlistHandler!!.setup()
        }
        handlersList.add(watchlistHandler!!)


        if (ciPlusHandler == null) {
            ciPlusHandler = ReferenceCiPlusHandler()
            ciPlusHandler!!.setup()
        }
        handlersList.add(ciPlusHandler!!)

        registeredHandlers = handlersList

        /**
         * Update favorite list data with delay
         */
        CoroutineHelper.runCoroutineWithDelay( {
            (dataProvider as TifDataProvider).updateFavList()
        }, 5000)

        InformationBus.submitEvent(Event(ReferenceEvents.SDK_POST_INIT_FINISHED))
    }

    override fun dispose() {
        context.contentResolver.unregisterContentObserver(lcnConfigObserver)
        context.contentResolver.unregisterContentObserver(countryConfigObserver)
        (dataProvider as TifDataProvider).dispose()
    }

    fun refreshProvider(callback: AsyncReceiver?) {
        dataProvider!!.init(object : AsyncDataReceiver<Int> {
            override fun onFailed(error: Error?) {
                callback?.onFailed(error)
            }

            override fun onReceive(data: Int) {
                if (data == DataProvider.DataEvent.EPG_LOADED) {
                    tvHandler!!.setup()
                    // Wait for tv handler setup
                    var eventListener = ChannelsLoadedEventListener(callback)
                    InformationBus.registerEventListener(eventListener)
                }
            }
        })

    }

    @SuppressLint("Range")
    private fun checkLcn() {
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        var cursor = contentResolver.query(
            ReferenceContentProvider.CONFIG_URI,
            null,
            null,
            null,
            null
        )
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Config.LCN_COLUMN)) != null) {
                val lcn =
                    cursor.getString(cursor.getColumnIndex(ReferenceContract.Config.LCN_COLUMN))
                        .toString()
                if (isLcnEnabled == null) {
                    isLcnEnabled = lcn == "true"
                } else {
                    isLcnEnabled = lcn == "true"
                    (tvHandler as ReferenceTvHandler).enableLcn(isLcnEnabled!!)
                }
            }
        }
    }

    fun setLcn(enableLcn: Boolean) {
        var contentValues = ContentValues()
        var lcn = if (enableLcn) "true" else "false"
        contentValues.put(ReferenceContract.Config.LCN_COLUMN, lcn)
        var uri = ReferenceContentProvider.CONFIG_URI

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

    fun setIsRecordingStarted(isRecordingStarted: Boolean) {
        this.isRecordingStarted = isRecordingStarted
        var contentValues = ContentValues()
        var isRecordingStarted = if (isRecordingStarted) "true" else "false"
        contentValues.put(ReferenceContract.Config.IS_RECORDING_STARTED, isRecordingStarted)
        var uri = ReferenceContentProvider.CONFIG_URI

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

    fun checkIsRecordingStarted() : Boolean {
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        var cursor = contentResolver.query(
            ReferenceContentProvider.CONFIG_URI,
            null,
            null,
            null,
            null
        )
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Config.IS_RECORDING_STARTED)) != null) {
                val isStarted = cursor.getString(cursor.getColumnIndex(ReferenceContract.Config.IS_RECORDING_STARTED)).toString()
                if (isRecordingStarted == null) {
                    isRecordingStarted = false
                } else {
                    isRecordingStarted = isStarted == "true"
                }
            }
        }

        return isRecordingStarted!!
    }

    public fun isLcnEnabled(): Boolean {
        return isLcnEnabled ?: false
    }

    class ChannelsLoadedEventListener(var callback: AsyncReceiver?) : EventListener() {

        init {
            addType(Events.CHANNELS_LOADED)
        }

        override fun callback(event: Event?) {
            super.callback(event)
            callback?.onSuccess()
            InformationBus.unregisterEventListener(this)
        }
    }
}