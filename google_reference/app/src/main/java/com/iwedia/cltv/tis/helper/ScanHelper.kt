package com.iwedia.cltv.tis.helper

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.ChannelListModel
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.ProgramListModel
import com.iwedia.cltv.platform.model.content_provider.PromoProvider
import com.iwedia.cltv.platform.model.fast_backend_utils.AdvertisingIdHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.FastAnokiUidHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.FastRetrofitHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.FastUrlHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.IpAddressHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.LocaleHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.SystemPropertyHelper
import com.iwedia.cltv.tis.model.ChannelDescriptor
import com.iwedia.cltv.tis.ui.SetupActivity
import com.iwedia.cltv.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.time.Instant
import java.util.Date
import java.util.concurrent.TimeUnit


/**
 * Helper class to fetch data from server and update data into Tv.db.
 *
 * @author Abhilash M R
 */

object ScanHelper {
    private val TAG: String = "ScanHelper"
    private const val ANOKI_SCAN_TAG = Constants.SharedPrefsConstants.ANOKI_SCAN_TAG
    private const val ANOKI_EPG_TAG = Constants.SharedPrefsConstants.ANOKI_EPG_TAG
    const val PREFS_KEY_CURRENT_COUNTRY_ALPHA3 = Constants.SharedPrefsConstants.PREFS_KEY_CURRENT_COUNTRY_ALPHA3
    const val PREFS_KEY_AUID = FastAnokiUidHelper.ANOKI_UID_TAG
    const val COLUMN_SYSTEM_CHANNEL_KEY = "system_channel_key"
    var country:String = ""
    private val VIDEO_RESOLUTION_TO_FORMAT_MAP = mutableMapOf<String, String>()
    private var isScanReceiverRegistered = false
    private var channelListRefreshTimer: CountDownTimer?= null
    private var epgRefreshTimer: CountDownTimer?= null
    private val mutex = Mutex()
    private var versionName = ""

    init {
        VIDEO_RESOLUTION_TO_FORMAT_MAP["SD"] = TvContract.Channels.VIDEO_FORMAT_480P
        VIDEO_RESOLUTION_TO_FORMAT_MAP["HD"] = TvContract.Channels.VIDEO_FORMAT_720P
        VIDEO_RESOLUTION_TO_FORMAT_MAP["FHD"] = TvContract.Channels.VIDEO_FORMAT_1080P
        VIDEO_RESOLUTION_TO_FORMAT_MAP["UHD"] = TvContract.Channels.VIDEO_FORMAT_2160P
    }

    const val PERIODIC_SCAN_TIME = 60 * 60 * 1000L //3600000L  1 hour in ms
    const val EPG_REFRESH_TIME = 12 * 60 * 60 * 1000L //43200000L 12hrs in ms

    /**
     * Number of channels to load before triggering each intermittent EPG updates
     */
    private const val EPG_UPDATE_THRESHOLD = 20

    /**
     * Scan broadcast receiver
     */
    private val receiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Received scan intent ")
            if (intent != null && intent.action == ReferenceApplication.FAST_SCAN_START) {

                CoroutineScope(Dispatchers.IO).launch {
                    scanChannels(ReferenceApplication.applicationContext()) {
                        triggerFastScanResultIntent(context, it)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun registerScanReceiver(context: Context) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "ScanHelper register receiver $isScanReceiverRegistered")
        if (!isScanReceiverRegistered) {
            // Receiver is not registered
            FastUrlHelper.setSelectedUrl(FastUrlHelper.getSelectedUrlIndex(context))
            startPeriodicChannelListUpdateTask(context)
            startPeriodicEpgRefreshTask(context)
            context.registerReceiver(receiver, IntentFilter(ReferenceApplication.FAST_SCAN_START))
        }
        isScanReceiverRegistered = true
    }

    private fun triggerFastScanResultIntent(context: Context, channelsCount: Int){
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Scan intent channel count $channelsCount")
        val fastScanResultIntent = Intent()
        fastScanResultIntent.action = ReferenceApplication.FAST_SCAN_RESULT
        fastScanResultIntent.putExtra(ReferenceApplication.FAST_SCAN_RESULT, channelsCount)
        context.sendBroadcast(fastScanResultIntent)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun scanChannels(context: Context, callback: (numberOfChannels: Int) -> Unit) {
        if(ChannelListHelper.scanningInProgress){
            Log.d(Constants.LogTag.CLTV_TAG + TAG,"Scan is in progress, do not start new one")
            return
        }
        ChannelListHelper.scanningInProgress = true
        context.contentResolver.delete(TvContract.buildChannelsUriForInput(SetupActivity.INPUT_ID),null,null)
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"INPUT_ID = ${SetupActivity.INPUT_ID}")
        var cursorChannels =
            context.contentResolver.query(
                TvContract.buildChannelsUriForInput(
                    SetupActivity.INPUT_ID
                ),
                null,
                null,
                null
            )
        insertPromotions(context)
        var prefs : SharedPreferences = context.getSharedPreferences("shared_prefs",0)
        /*if (!BuildConfig.FLAVOR.contains("base") && prefs.getInt("scan_on_reboot", 0) == 0) {

            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Scan wasn't done before or delete was performed, do not scan")
            if (cursorChannels!=null) {
                cursorChannels.close()
            }
            return
        }*/

        if (cursorChannels!!.count > 0) {
            callback.invoke(cursorChannels.count)
            ChannelListHelper.scanningInProgress = false
            return
        }
        //target country for channel list.
        country = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString(PREFS_KEY_CURRENT_COUNTRY_ALPHA3, "")
            .toString()
        var auid = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString(PREFS_KEY_AUID, "")
            .toString()
        versionName = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString("version_name", "")
            .toString()
        Log.d("[CLTV] " + TAG, "scanChannels: countryCode $country $versionName")

        // try-catch block to handle the API call and the exception
        try {
            if(auid.isEmpty()){
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scanChannels: auid is empty, fetching auid")
                auid = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.ANOKI_UID_URL).getAnokiUidFromServer(AdvertisingIdHelper.getAdvertisingId(context),
                    SystemPropertyHelper.getPropertiesAsString(),versionName).body()!!
                context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit().putString(PREFS_KEY_AUID, auid).apply()
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "scanChannels: auid === $auid, deviceId === ${AdvertisingIdHelper.getAdvertisingId(context)}")
            val channelList = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.BASE_URL).getChannelList(country, IpAddressHelper.fetchPublicIpAddress(context), auid, AdvertisingIdHelper.getAdvertisingId(context),LocaleHelper.getCurrentLocale(),SystemPropertyHelper.getPropertiesAsString(),versionName)
            // Log the number of channels
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "scanChannels: Number of Channels = ${channelList.body()?.size}")
            // Insert the channels into the database
            insertChannels(context, channelList, callback)
            ChannelListHelper.scanningInProgress = false
        } catch (e: Exception) {
            // Log the error details
            ChannelListHelper.scanningInProgress = false
            callback.invoke(0)
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Error message: ${e.printStackTrace()}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun insertChannels(
        context: Context,
        channelList: Response<ArrayList<ChannelListModel>>,
        callback: (numberOfChannels: Int) -> Unit
    ) {
        mutex.withLock {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "insertChannels channel list size ${channelList.body()?.size}")
            var cursor =
                context.contentResolver.query(
                    TvContract.buildChannelsUriForInput(SetupActivity.INPUT_ID),
                    ChannelDescriptor.getProjection(),
                    null,
                    null
                )
            if(!isChannelListRefreshReady(context) && (cursor != null && cursor.count > 0)) {
                return
            }
            cursor?.moveToFirst()
            // delete existing channels to add dummy data
            if (cursor != null && cursor.count > 0) {
                context.contentResolver.delete(
                    TvContract.buildChannelsUriForInput(SetupActivity.INPUT_ID),
                    null,
                    null
                )
            }
            cursor?.close()

            var index = 1
            channelList.body()?.forEach { item ->
                insertChannelInDb(item, index)
                index++
            }

            var cursorChannels =
                context.contentResolver.query(
                    TvContract.buildChannelsUriForInput(
                        SetupActivity.INPUT_ID
                    ),
                    null,
                    null,
                    null
                )

            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Total Channels found  " + cursorChannels!!.count)
            callback.invoke(cursorChannels.count)
            cursorChannels.close()

            saveScanTimeStamp()

            //fetch epg data for all channels
            getEpgDataForAllChannels()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun insertChannelInDb(item: ChannelListModel, index: Int) {
        val values = ContentValues().apply {
            put(TvContract.Channels.COLUMN_INPUT_ID, SetupActivity.INPUT_ID)
            put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID, 2000 + index)
            put(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID, 2000 + index)
            put(TvContract.Channels.COLUMN_SERVICE_ID, 2000 + index)
            put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4, index)
            put(TvContract.Channels.COLUMN_DISPLAY_NAME, item.name)
            put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, item.channelId)
            put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, item.playbackUrl)
            put(TvContract.Channels.COLUMN_APP_LINK_ICON_URI, item.logo)
            put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG3, item.rating.toInt() - 1)
            if(!item.licenseServerUrl.isNullOrEmpty()) put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID, item.licenseServerUrl)
            //put(TvContract.Channels.COLUMN_BROWSABLE, 1)
            var genres = ""
            if(item.genre != null && item.genre.isNotEmpty()){
                item.genre.forEach {
                    genres += "$it,"
                }
                put(TvContract.Channels.COLUMN_DESCRIPTION, genres)
            }
            if(item.resolution.isNotEmpty()){
                val resolution = item.resolution[0]
                put(TvContract.Channels.COLUMN_VIDEO_FORMAT,
                    VIDEO_RESOLUTION_TO_FORMAT_MAP[resolution]
                )
            }
        }
        val uri = ReferenceApplication.applicationContext().contentResolver.insert(
            TvContract.Channels.CONTENT_URI,
            values
        )
        val value = ContentValues().apply {
            put(TvContract.Channels.COLUMN_APP_LINK_INTENT_URI, uri.toString())

        }
        ReferenceApplication.applicationContext().contentResolver.update(uri!!, value,null)
        // Necessary to maintain channel details inside app.
        val chDesc = ChannelDescriptor(
            mInput = SetupActivity.INPUT_ID,
            mChLogo = item.logo,
            mChName = item.name,
            mChPlaybackUrl = item.playbackUrl,
            mOrdinalNumber = index,
            mLicenseServerUrl = if(!item.licenseServerUrl.isNullOrEmpty()) item.licenseServerUrl else ""
        )
        val temp = uri.toString().split("/")
        val id = temp[temp.size - 1]
        //this is needed for referencing from TIS, check onTune in TvInputService to understand.
        chDesc.mChId = id.toLong()
        chDesc.mChannelUri = uri!!
        chDesc.mExternalId = item.channelId
        ChannelListHelper.idMap[item.channelId.toInt()] = id.toInt()
        ChannelListHelper.channels.add(chDesc)
        //storeChannelImage(context, id.toLong(), item.logo)
    }

    @SuppressLint("CheckResult")
    private fun storeChannelImage(context: Context, channelId: Long, logoPath: String) {
        Glide
            .with(context)
            .asBitmap()
            .load(logoPath)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val channelLogoUri: Uri = TvContract.buildChannelLogoUri(channelId)
                    try {

                        val stream = ByteArrayOutputStream()
                        resource.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                        val byteArray: ByteArray = stream.toByteArray()

                        val fd: AssetFileDescriptor? =
                            context.contentResolver.openAssetFileDescriptor(channelLogoUri, "rw")
                        val os: OutputStream? = fd?.createOutputStream()
                        os?.write(byteArray)
                        os?.close()
                        fd?.close()
                    } catch (e: IOException) {
                        // Handle error cases.
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    private fun triggerChannelReorderingIntent(context: Context){
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "triggerChannelReorderingIntent: send channel reordering intent")
        val channelReorderingIntent = Intent()
        channelReorderingIntent.action = ReferenceApplication.FAST_CHANNEL_REORDER
        context.sendBroadcast(channelReorderingIntent)
    }

    /**
     * Triggers an intent to update the EPG events
     * @param isLoadingFinished `True` when completes loading events for all channels
     */
    private fun triggerEpgIntent(context: Context, isLoadingFinished: Boolean){
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "triggerEpgIntent: send epg intent")
        val epgIntent = Intent()
        epgIntent.putExtra("is_loading_finished", isLoadingFinished)
        epgIntent.action = ReferenceApplication.FAST_EPG_RESULT
        context.sendBroadcast(epgIntent)
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun getEpgData(context: Context, channels: List<ChannelDescriptor>) {
        if(channels.isEmpty()){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEpgData: channel list is empty")
            return
        }
        val auid = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString(PREFS_KEY_AUID, "")
            .toString()
        var channelIds = ""
        channels.toList().forEach { channel ->
            channelIds += channel.mExternalId + ","
        }
        try {
            if(channelIds.isNotEmpty()){
                val programList =
                    FastRetrofitHelper.getFastBackendApi(FastUrlHelper.BASE_URL)
                        .getProgramList(country, channelIds, Instant.now().epochSecond,auid,LocaleHelper.getCurrentLocale(),SystemPropertyHelper.getPropertiesAsString(),versionName)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEpgData: channelIds = $channelIds")
                updatePrograms(context, programList.body()!!)
            }
            ChannelListHelper.scanningInProgress = false
        } catch(e : Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Failed to get Epg data ${e.message}")
        }
        //update epg timestamp only when epg is fetched for all channels present in db
        if(channels.size == ChannelListHelper.channels.size){
            saveEpgTimeStamp()
        }
    }

    suspend fun getEpgDataForAllChannels(){
        getEpgData(ReferenceApplication.applicationContext(), ChannelListHelper.channels)
    }

    private fun updatePrograms(
        context: Context,
        programs: List<ProgramListModel>
    ) {
        val programCount = programs.size
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updatePrograms: programCount = $programCount")
        if (programCount == 0) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, " No Programs Found !!!")
            return
        }
        programs.forEachIndexed { index, items ->
            val valuesList = mutableListOf<ContentValues>()
            items.programList.forEach { program ->
                val values = ContentValues()
                values.put(TvContract.Programs.COLUMN_TITLE, program.title)
                values.put(TvContract.Programs.COLUMN_CHANNEL_ID, ChannelListHelper.idMap[items.channelId.toInt()])
                values.put(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS, program.startTimeEpoch * 1000)
                val endTime = program.startTimeEpoch + program.durationSec
                values.put(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS, endTime * 1000)
                values.put(TvContract.Programs.COLUMN_SHORT_DESCRIPTION, program.description)
                values.put(TvContract.Programs.COLUMN_THUMBNAIL_URI, program.thumbnail)
                values.put(TvContract.Programs.COLUMN_POSTER_ART_URI, program.image)
                values.put(TvContract.Programs.COLUMN_BROADCAST_GENRE, program.genre)
                values.put(TvContract.Programs.COLUMN_AUDIO_LANGUAGE, program.language)
                if (!program.rating.isNullOrBlank()) {
                    var ratingSystem = "US_TV"
                    var rating = program.rating.replace("TV", "")
                    rating = rating.replace("-", "")
                    if (rating == "R") {
                        ratingSystem = "US_MV"
                    } else if(rating.startsWith("PG") && rating.length > 2) {
                        ratingSystem = "US_MV"
                    }

                    val contentRating =  TvContentRating.createRating("com.android.tv", ratingSystem, ratingSystem + "_" + rating)
                    values.put(TvContract.Programs.COLUMN_CONTENT_RATING, contentRating.flattenToString())
                }
                valuesList.add(values)
            }
            val numberOfInsertedPrograms = context.contentResolver.bulkInsert(
                TvContract.Programs.CONTENT_URI,
                valuesList.toTypedArray()
            )
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updatePrograms: numberOfInsertedPrograms = $numberOfInsertedPrograms")
            if( index != 0 && index % EPG_UPDATE_THRESHOLD == 0){
                triggerEpgIntent(context, true)
            }
        }
        triggerEpgIntent(context, false)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun deleteChannels(context: Context) {
        if (ChannelListHelper.scanningInProgress) {
            Toast.makeText(context, "Scan is in progress. Please wait for scan to be finished", Toast.LENGTH_SHORT).show()
            return
        }
        ChannelListHelper.channels.apply {
            if (isNotEmpty()) clear()
        }
        val deletedRows = context.contentResolver.delete(TvContract.buildChannelsUriForInput(SetupActivity.INPUT_ID),null,null)
        if (deletedRows > 0) {
            Toast.makeText(context, "Deleted $deletedRows channels", Toast.LENGTH_LONG).show()
        }
    }

    fun insertPromotions(context: Context) {
        val cursor = context.contentResolver.query(PromoProvider.PROMO_URI,null,null,null,null)
        val result = JSONObject().apply {
            val promoBannerData = JSONArray().apply {
                val promoBanner = JSONObject().apply {
                    put("promo_banner_url", "https://dl.rt-rk.com/?t=c4afcb5f874323d4270d7328b4aea7b4")
                    put("countries", JSONArray().apply {
                        put("USA")
                        put("FRA")
                        put("DEU")
                        put("ITA")
                        put("ESP")
                        put("IND")
                        put("AUS")
                        put("IDN")
                        put("THA")
                        put("MYS")
                        put("MEX")
                        put("BRA")
                        put("KOR")
                    })
                }
                put(promoBanner)
            }
            put("promo_banner_data", promoBannerData)
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"Promotion insertation: ${result.toString()}")



        if(cursor!=null && cursor.count>0){
            Log.d(Constants.LogTag.CLTV_TAG + TAG,"Promotion exists, update it")
            val uri = Uri.withAppendedPath(PromoProvider.PROMO_URI, "/1")
            val contentValues = ContentValues()
            contentValues.put("supported_country", result.toString())
            val where = "_id" + " =?"
            val args = arrayOf(
                "1"
            )
            context.contentResolver.update(uri, contentValues, where, args)
            cursor.close()
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG,"Promotion doesn't exist, insert it")
            val contentValues = ContentValues()
            contentValues.put("supported_country", result.toString())
            context.contentResolver.insert(PromoProvider.PROMO_URI,contentValues)
        }
    }

    @SuppressLint("RestrictedApi")
    fun startPeriodicEpgRefreshTask(context: Context) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicEpgRefreshTask")
        //creates timer for periodic epg data refreshing for every 15 minutes
        if (epgRefreshTimer != null) {
            epgRefreshTimer?.cancel()
            epgRefreshTimer = null
        }
        if(isEpgRefreshReady(context)){
            CoroutineScope(Dispatchers.IO).launch {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicEpgRefreshTask getEpgDataForAllChannels called")
                getEpgDataForAllChannels()
            }
        }
        epgRefreshTimer = object: CountDownTimer(15 * 60 * 1000L, 60000L) {
            override fun onFinish() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicEpgRefreshTask finish")
                if(isEpgRefreshReady(context)){
                    CoroutineScope(Dispatchers.IO).launch {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicEpgRefreshTask getEpgDataForAllChannels called")
                        getEpgDataForAllChannels()
                    }
                }
                start()
            }

            override fun onTick(millisUntilFinished: Long) {}
        }.start()
    }

    private fun isEpgRefreshReady(context: Context): Boolean {
        val currentTime = System.currentTimeMillis()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isEpgRefreshReady: current time ${Date(currentTime)}")
        val timestampScan = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getLong(
            ANOKI_SCAN_TAG, 0L)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isEpgRefreshReady: last channel list update time ${Date(timestampScan)}")
        val timeStampEpg = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getLong(
            ANOKI_EPG_TAG, 0L)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isEpgRefreshReady: last epg refresh time ${Date(timeStampEpg)}")
        val diffTimeEpg = currentTime - timeStampEpg
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isEpgRefreshReady: diff between current time and last epg refresh time is ${TimeUnit.MILLISECONDS.toHours(diffTimeEpg)} hrs")
        return timestampScan != 0L && (timeStampEpg == 0L || diffTimeEpg > EPG_REFRESH_TIME)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("RestrictedApi")
    fun startPeriodicChannelListUpdateTask(context: Context) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicChannelListUpdateTask")

        if (channelListRefreshTimer != null) {
            channelListRefreshTimer?.cancel()
            channelListRefreshTimer = null
        }
        if(isChannelListRefreshReady(context)){
            CoroutineScope(Dispatchers.IO).launch {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicChannelListUpdateTask updateChannelList called")
                updateChannelList(context)
            }
        }
        channelListRefreshTimer = object: CountDownTimer(15 * 60 * 1000L, 60000L) {
            @RequiresApi(Build.VERSION_CODES.S)
            override fun onFinish() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicChannelListUpdateTask finish")
                if(isChannelListRefreshReady(context)){
                    CoroutineScope(Dispatchers.IO).launch {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicChannelListUpdateTask updateChannelList called")
                        updateChannelList(context)
                    }
                }
                start()
            }

            override fun onTick(millisUntilFinished: Long) {}
        }.start()
    }

    private fun isChannelListRefreshReady(context: Context) : Boolean{
        val currentTime = System.currentTimeMillis()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isChannelListRefreshReady: current time ${Date(currentTime)}")
        val timestampScan = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getLong(
            ANOKI_SCAN_TAG, 0L)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isChannelListRefreshReady: last channel list update time ${Date(timestampScan)}")
        val diffTime = currentTime - timestampScan
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isChannelListRefreshReady: diff between current time and last channel list update time is ${TimeUnit.MILLISECONDS.toMinutes(diffTime)} minutes")
        return diffTime > PERIODIC_SCAN_TIME
    }

    private fun saveScanTimeStamp(){
        //Save last scan timestamp
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "saveScanTimeStamp: save last scan timestamp")
        ReferenceApplication.applicationContext().getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
            .putLong(ANOKI_SCAN_TAG, System.currentTimeMillis()).apply()
    }


    private fun saveEpgTimeStamp(){
        //Save last refresh time of Epg
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "saveEpgTimeStamp: save last refresh time of epg")
        ReferenceApplication.applicationContext().getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
            .putLong(ANOKI_EPG_TAG, System.currentTimeMillis()).apply()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun updateChannelList(context: Context) {
        if(ChannelListHelper.scanningInProgress){
            return
        }
        val channelIdAndOrdinalNumberMap = mutableMapOf<String, Int>()
        val channelsToBeDeletedMap = mutableMapOf<String, Boolean>()
        val listOfNewChannels = mutableListOf<ChannelDescriptor>()
        val oldChannelList = ChannelListHelper.getChannelList()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateChannelList: oldChannelList size ${oldChannelList.size}")
        if(oldChannelList.isEmpty()){
            scanChannels(context){
                triggerFastScanResultIntent(context, it)
            }
            return
        }
        oldChannelList.forEach { channelDescriptor ->
            channelIdAndOrdinalNumberMap[channelDescriptor.mExternalId] = channelDescriptor.mOrdinalNumber
            channelsToBeDeletedMap[channelDescriptor.mExternalId] = true
        }
        country = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString(PREFS_KEY_CURRENT_COUNTRY_ALPHA3, "")
            .toString()
        var auid = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString(PREFS_KEY_AUID, "")
            .toString()
        try {
            ChannelListHelper.scanningInProgress = true
            val newChannelList = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.BASE_URL).getChannelList(
                country,
                IpAddressHelper.fetchPublicIpAddress(context),
                auid,
                AdvertisingIdHelper.getAdvertisingId(context),
                LocaleHelper.getCurrentLocale(),
                SystemPropertyHelper.getPropertiesAsString(),
                versionName
            )
            var index = 1
            var isNewChannelFound = false
            var isChannelOrderChanged = false
            newChannelList.body()?.forEach { item ->
                channelsToBeDeletedMap[item.channelId] = false
                if (channelIdAndOrdinalNumberMap.containsKey(item.channelId)) {
                    if (channelIdAndOrdinalNumberMap[item.channelId] != index) {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "updateChannelList: wrong order channelId === ${item.channelId} channelName === ${item.name} channelIndex === $index"
                        )
                        val values = ContentValues().apply {
                            put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4, index)
                            //update genre for reordered channels
                            if(item.genre.isNotEmpty()){
                                var genres = ""
                                item.genre.forEach {
                                    genres += "$it,"
                                }
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateChannelList: Genre for reordered channel channelName ==== ${item.name} channelGenres === $genres")
                                put(TvContract.Channels.COLUMN_DESCRIPTION, genres)
                            }
                            //reassign network, transport and service ID since order of channels are changed due to new channel
                            if (isNewChannelFound) {
                                put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID, 2000 + index)
                                put(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID, 2000 + index)
                                put(TvContract.Channels.COLUMN_SERVICE_ID, 2000 + index)
                            }
                        }
//                        ReferenceApplication.applicationContext().contentResolver.update(
//                            TvContract.Channels.CONTENT_URI, values,
//                            "${TvContract.Channels.COLUMN_DISPLAY_NUMBER}=${item.channelId}", null
//                        )

                        ReferenceApplication.applicationContext().contentResolver.update(
                            TvContract.buildChannelUri(ChannelListHelper.idMap[item.channelId.toInt()]!!.toLong()), values,
                            null, null
                        )
                        isChannelOrderChanged = true
                    } else {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "updateChannelList: correct order channelId === ${item.channelId} channelName === ${item.name} channelIndex === $index"
                        )
                    }
                } else {
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "updateChannelList: new channel from server, inserting in tv db, channelId === ${item.channelId} channelName === ${item.name} channelIndex === $index"
                    )
                    isNewChannelFound = true
                    //insert new channel in tv db
                    insertChannelInDb(item, index)
                    val chDesc = ChannelDescriptor()
                    chDesc.mExternalId = item.channelId
                    listOfNewChannels.add(chDesc)
                }
                index++
            }
            saveScanTimeStamp()
            //update epg data for new channel
            if(listOfNewChannels.isNotEmpty()){
                getEpgData(ReferenceApplication.applicationContext(), listOfNewChannels.toList())
            }

            //Delete channels that are no more present on server side
            channelsToBeDeletedMap.forEach { item ->
                if(item.value){
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateChannelList: deleting channel from tv db, channelId === ${item.key}")
                    val selection = TvContract.Channels.COLUMN_DISPLAY_NUMBER + "=" + item.key
                    ReferenceApplication.applicationContext().contentResolver.delete(
                        TvContract.Channels.CONTENT_URI, selection, null
                    )
                }
            }

            //update channel list
            ChannelListHelper.scanningInProgress = false
            ChannelListHelper.channels.clear()
            ChannelListHelper.initData(ReferenceApplication.applicationContext())
            if(isChannelOrderChanged){
                triggerChannelReorderingIntent(context)
            }
        } catch(e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateChannelList: failed to get channels list ${e.message}")
            ChannelListHelper.scanningInProgress = false
        }
    }

    fun deleteChannelsAndAllTimeStampsAndRestartApp(context: Context){
        val listOfPrefs = listOf(ANOKI_SCAN_TAG, ANOKI_EPG_TAG, PREFS_KEY_AUID, PREFS_KEY_CURRENT_COUNTRY_ALPHA3,
            Constants.SharedPrefsConstants.ANOKI_RECOMMENDATION_TAG, Constants.SharedPrefsConstants.ANOKI_GENRE_TAG, Constants.SharedPrefsConstants.ANOKI_PROMOTION_TAG)
        listOfPrefs.forEach { tag ->
            context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit().remove(tag).apply()
        }
        context.contentResolver.delete(TvContract.buildChannelsUriForInput(SetupActivity.INPUT_ID),null,null)
        Utils.restartApp()
    }
}