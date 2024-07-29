package com.iwedia.cltv.platform.base.content_provider

import android.annotation.SuppressLint
import android.content.Context
import android.media.tv.TvContract
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import com.iwedia.cltv.platform.`interface`.FastDataProviderInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.FastFavoriteItem
import com.iwedia.cltv.platform.model.FastRatingItem
import com.iwedia.cltv.platform.model.FastRatingListItem
import com.iwedia.cltv.platform.model.FastUserSettingsItem
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PromotionItem
import com.iwedia.cltv.platform.model.RecommendationRow
import com.iwedia.cltv.platform.model.UserSettings
import com.iwedia.cltv.platform.model.fast_backend_utils.AdvertisingIdHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.FastAnokiUidHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.FastRetrofitHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.FastTosOptInHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.FastUrlHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.IpAddressHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.LocaleHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.SystemPropertyHelper
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

open class FastDataProvider(var context: Context): FastDataProviderInterface {
    private val TAG = "FastDataProvider"
    var fastPromotionList: ArrayList<PromotionItem> = arrayListOf()
    var fastRecommendationRows: ArrayList<RecommendationRow> = arrayListOf()
    var genres = arrayListOf<String>()
    var ratingList = arrayListOf<FastRatingListItem>()

    private var anokiUID : String = ""
    private var versionName = ""
    private var tosOptIn: Int = 0
    private var userSettings : UserSettings? = null
    //list of all channel Ids added to Fav
    var favoriteList: ArrayList<String> = arrayListOf()
    private var counter = AtomicInteger(0)
    val ANOKI_RECOMMENDATION_TAG = Constants.SharedPrefsConstants.ANOKI_RECOMMENDATION_TAG
    val ANOKI_GENRE_TAG = Constants.SharedPrefsConstants.ANOKI_GENRE_TAG
    val ANOKI_PROMOTION_TAG = Constants.SharedPrefsConstants.ANOKI_PROMOTION_TAG
    private var promotionsRefreshTimer: CountDownTimer?= null
    private var recommendationRefreshTimer: CountDownTimer?= null
    private val PERIODIC_PROMOTION_TIME = 15 * 60 * 1000L //900000L  15 minutes in ms
    private val PERIODIC_RECOMMENDATIONS_AND_GENRE_TIME = 60 * 60 * 1000L //3600000L  1 hour in ms
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    override fun getPromotionList(): ArrayList<PromotionItem> {
        var retList = ArrayList<PromotionItem>()
        retList.addAll(fastPromotionList)
        return retList
    }

    override fun getRecommendationRows(): ArrayList<RecommendationRow> {
        var retList = ArrayList<RecommendationRow>()
        retList.addAll(fastRecommendationRows)
        return retList
    }

    override fun getGenreList(): ArrayList<String> {
        var retList = ArrayList<String>()
        retList.addAll(genres)
        return retList
    }
    override fun getFastFavoriteList(): ArrayList<String> {
        var retList = ArrayList<String>()
        retList.addAll(favoriteList)
        return retList
    }

    override fun getAnokiUID(): String {
        return anokiUID
    }

    override fun updateDNT(enableDNT: Boolean, callback: IAsyncCallback) {
        updateDntInServer(enableDNT, callback)
    }

    override fun getDNT(): Int {
        return userSettings!!.dnt
    }

    override fun getTosOptIn(): Int {
        return tosOptIn
    }

    override fun updateTosOptIn(value: Int) {
        updateTosOptInServer(value)
    }

    override fun getFastRatingList(): ArrayList<FastRatingListItem> {
        var retList = ArrayList<FastRatingListItem>()
        retList.addAll(ratingList)
        return retList
    }

    override fun updateRating(rating: String) {
        updateRatingInServer(rating)
    }

    override fun deleteAllFastData(inputId: String) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "deleteAllFastData: ")
        fastPromotionList.clear()
        fastPromotionList.clear()
        fastRecommendationRows.clear()
        favoriteList.clear()
        val listOfPrefs = listOf(Constants.SharedPrefsConstants.ANOKI_SCAN_TAG, Constants.SharedPrefsConstants.ANOKI_EPG_TAG, FastAnokiUidHelper.ANOKI_UID_TAG,
            Constants.SharedPrefsConstants.PREFS_KEY_CURRENT_COUNTRY_ALPHA3, ANOKI_RECOMMENDATION_TAG, ANOKI_GENRE_TAG, ANOKI_PROMOTION_TAG)
        listOfPrefs.forEach { tag ->
            context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit().remove(tag).apply()
        }
        context.contentResolver.delete(TvContract.buildChannelsUriForInput(inputId),null,null)
        InformationBus.informationBusEventListener.submitEvent(Events.FAST_DATA_UPDATED)
    }

    override fun updateFavoriteList(
        channelId: String,
        addToFavorite: Boolean,
        callback: IAsyncCallback
    ) {
        updateChannelToFavorites(channelId,addToFavorite, callback)
    }

    private fun checkTos(callback: IAsyncDataCallback<Boolean>){
        FastTosOptInHelper.fetchTosOptInFromServer(context){
            tosOptIn = it
            if(it == -1){// internet is not connected, wait until it connected
                var eventReceiver: Any ?= null
                InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.ANOKI_SERVER_REACHABLE, Events.ETHERNET_EVENT), callback = {eventReceiver = it}, onEventReceived = {
                    InformationBus.informationBusEventListener.unregisterEventListener(eventReceiver!!)
                    CoroutineHelper.runCoroutine({
                        checkTos(callback)
                    })
                })
            }else{
                CoroutineHelper.runCoroutine({
                    callback.onReceive(it == 1)
                })
            }
        }
    }

    open fun init() {
        versionName = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString("version_name", "")!!
        checkTos(object : IAsyncDataCallback<Boolean>{
            override fun onFailed(error: Error) {}

            override fun onReceive(data: Boolean) {
                if(!data){
                    //Skip data fetching if terms of services not accepted
                    var eventReceiver: Any ?= null
                    InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.TOS_ACCEPTED), {
                        eventReceiver = it
                    }, {
                        val advertisingId = AdvertisingIdHelper.fetchAdvertisingId(context)
                        anokiUID = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString(FastAnokiUidHelper.ANOKI_UID_TAG, "")!!
                        if(anokiUID.isNotEmpty()){
                            doInitialSetup()
                        }else{
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: anoki uid is empty, feching uid")
                            fetchAnokiUID(advertisingId){
                                doInitialSetup()
                            }
                        }
                        InformationBus.informationBusEventListener.unregisterEventListener(eventReceiver!!)
                    })
                } else {
                    val advertisingId = AdvertisingIdHelper.fetchAdvertisingId(context)
                    anokiUID = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString(FastAnokiUidHelper.ANOKI_UID_TAG, "")!!
                    if(anokiUID.isNotEmpty()){
                        doInitialSetup()
                    }else{
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: anoki uid is empty, feching uid")
                        fetchAnokiUID(advertisingId){
                            doInitialSetup()
                        }
                    }

                    InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.ETHERNET_EVENT,
                        Events.NO_ETHERNET_EVENT,
                        Events.ANOKI_SERVER_REACHABLE,
                        Events.ANOKI_SERVER_NOT_REACHABLE,
                        Events.ANOKI_RATING_LEVEL_CHANGED), callback = {
                    }, onEventReceived = {
                        if (it == Events.NO_ETHERNET_EVENT || it == Events.ANOKI_SERVER_NOT_REACHABLE) {
                            fastPromotionList.clear()
                            fastRecommendationRows.clear()
                            genres.clear()
                            favoriteList.clear()
                            android.os.Handler().postDelayed(Runnable {
                                InformationBus.informationBusEventListener.submitEvent(Events.FAST_DATA_UPDATED)
                            },100)
                        } else {
                            CoroutineHelper.runCoroutine({
                                counter = AtomicInteger(0)
                                anokiUID = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString(FastAnokiUidHelper.ANOKI_UID_TAG, "")!!
                                if(anokiUID.isNotEmpty()){
                                    fetchData()
                                }else{
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: anoki uid is empty, feching uid")
                                    val advertisingId = AdvertisingIdHelper.fetchAdvertisingId(context)
                                    fetchAnokiUID(advertisingId){
                                        fetchData()
                                    }
                                }

                            }, Dispatchers.IO)
                        }
                    })
                }
            }
        })
    }

    private fun doInitialSetup() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "doInitialSetup:")
        fetchData(initialSetup = true)
        startPeriodicPromotionUpdate()
        startPeriodicRecommendationAndGenreUpdate()
    }

    private fun fetchData(initialSetup: Boolean = false) {
        fetchPromotions()
        fetchRecommendations()
        fetchGenreList()
        fetchFavoriteList()
        fetchRatingList(showModeToast = initialSetup)
        fetchUserSettings()
    }

    private fun fetchAnokiUID(deviceId: String, callback: (auid: String)->Unit){
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Fetch anoki UID")
        FastAnokiUidHelper.fetchAnokiUID(context, deviceId){
            anokiUID = it
            callback.invoke(anokiUID)
        }
    }

    private fun fetchTosOptIn(){
        FastTosOptInHelper.fetchTosOptInFromServer(context){
            tosOptIn = it
        }
    }

    private fun updateTosOptInServer(value: Int){
        FastTosOptInHelper.putTosOptInServer(context, value){
            if (it){
                tosOptIn = value
            }
        }
    }

    private fun fetchRatingList(showModeToast: Boolean = false) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "fetchRatingList: ")
        try {
            val call = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.BASE_URL).getRatingList(anokiUID,LocaleHelper.getCurrentLocale(),SystemPropertyHelper.getPropertiesAsString(),versionName)
            call.enqueue(object : Callback<ArrayList<FastRatingListItem>>{
                override fun onResponse(
                    call: Call<ArrayList<FastRatingListItem>>,
                    response: Response<ArrayList<FastRatingListItem>>
                ) {
                    if(response.isSuccessful && response.body() != null){
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: fetchRatingList successful")
                        ratingList.clear()
                        ratingList = response.body()!!
                        for(i in ratingList){
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: Rating list $i")
                        }
                        if (showModeToast) {
                            InformationBus.informationBusEventListener.submitEvent(Events.SHOW_ANOKI_MODE_TOAST)
                        }
                    }
                }

                override fun onFailure(call: Call<ArrayList<FastRatingListItem>>, t: Throwable) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailure: failed to fetch rating list ${t.message}")
                }
            })
        } catch (e: Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "fetchRatingList: failed to fetch rating list ${e.message}")
        }
    }

    private fun updateRatingInServer(rating: String){
        try {
            val call = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.BASE_URL).updateRating(
                FastRatingItem(anokiUID,SystemPropertyHelper.getPropertiesAsString(), rating)
            )
            call.enqueue(object : Callback<ResponseBody>{
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: update rating in server ${response.message()}")
                    InformationBus.informationBusEventListener.submitEvent(Events.ANOKI_RATING_LEVEL_CHANGED)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailure: Failed to update rating in server ${t.message}")
                }

            })
        } catch (e: Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateRatingInServer: Failed to update rating in server ${e.message}")
        }
    }

    fun fetchPromotions(informUi: Boolean = false) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Fetch promotions")
        //using getIpAddress since fetching of promotions will be called every 10 minutes
        try {
            val call = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.BASE_URL).getPromotionList(
                anokiUID,
                IpAddressHelper.getIpAddress(),
                "USA",
                AdvertisingIdHelper.getAdvertisingId(context),
                LocaleHelper.getCurrentLocale(),
                SystemPropertyHelper.getPropertiesAsString(),
                versionName
            )
            call.enqueue(object : Callback<ArrayList<PromotionItem>> {
                override fun onResponse(
                    call: Call<ArrayList<PromotionItem>>,
                    response: Response<ArrayList<PromotionItem>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Fetch promotions response successful")
                        fastPromotionList.clear()
                        fastPromotionList = response.body()!!

                        for (pi in fastPromotionList) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Promotions $pi")
                        }
                        checkIfReadyForRefresh()
                        if(informUi){
                            InformationBus.informationBusEventListener.submitEvent(Events.FAST_DATA_UPDATED)
                        }
                        //Save last promotion update timestamp
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: Save last promotion update timestamp")
                        context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                            .putLong(ANOKI_PROMOTION_TAG, System.currentTimeMillis()).apply()
                    }
                }

                override fun onFailure(call: Call<ArrayList<PromotionItem>>, t: Throwable) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to fetch promotions")
                    checkIfReadyForRefresh()
                }
            })
        } catch(e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to fetch promotions ${e.message}")
        }
    }

    fun fetchRecommendations(informUi: Boolean = false) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Fetch recommendations")
        try {
            val call = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.BASE_URL).getRecommendationRows(
                anokiUID,
                IpAddressHelper.getIpAddress(),
                "USA",
                AdvertisingIdHelper.getAdvertisingId(context),
                LocaleHelper.getCurrentLocale(),
                SystemPropertyHelper.getPropertiesAsString(),
                versionName
            )
            call.enqueue(object : Callback<ArrayList<RecommendationRow>> {
                override fun onResponse(
                    call: Call<ArrayList<RecommendationRow>>,
                    response: Response<ArrayList<RecommendationRow>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Fetch recommendations response successful")
                        fastRecommendationRows.clear()
                        fastRecommendationRows = response.body()!!

                        for (rows in fastRecommendationRows) {
                            var elements = rows.items
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Recommendation row - ${rows.name}")
                            for (temp in elements) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Recommendation $temp")
                            }
                        }
                        checkIfReadyForRefresh()
                        if(informUi){
                            InformationBus.informationBusEventListener.submitEvent(Events.FAST_DATA_UPDATED)
                        }
                        //Save last recommendations update timestamp
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: Save last recommendations update timestamp")
                        context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                            .putLong(ANOKI_RECOMMENDATION_TAG, System.currentTimeMillis()).apply()
                    }
                }

                override fun onFailure(call: Call<ArrayList<RecommendationRow>>, t: Throwable) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to fetch recommendations")
                    checkIfReadyForRefresh()
                }
            })
        } catch(e: Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Failed to fetch recommendations ${e.message}")
        }
    }

    fun fetchGenreList() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Fetch genre list")
        try {
            val call = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.BASE_URL).getGenreList(
                anokiUID,
                IpAddressHelper.getIpAddress(),
                "USA",
                AdvertisingIdHelper.getAdvertisingId(context),
                LocaleHelper.getCurrentLocale(),
                SystemPropertyHelper.getPropertiesAsString(),
                versionName
            )
            call.enqueue(object : Callback<ArrayList<String>> {
                override fun onResponse(
                    call: Call<ArrayList<String>>,
                    response: Response<ArrayList<String>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Fetch genre list response successful")
                        genres.clear()
                        genres = response.body()!!

                        for (genre in genres) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Genre item - ${genre}")
                        }
                    }
                    checkIfReadyForRefresh()
                    //Save last genre update timestamp
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: Save last genre update timestamp")
                    context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                        .putLong(ANOKI_GENRE_TAG, System.currentTimeMillis()).apply()
                }

                override fun onFailure(call: Call<ArrayList<String>>, t: Throwable) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to fetch genre list")
                    checkIfReadyForRefresh()
                }
            })
        } catch(e: Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Failed to fetch genre list ${e.message}")
        }
    }

    private fun fetchFavoriteList(){
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "fetchFavoriteList: fetch favorites for ${AdvertisingIdHelper.getAdvertisingId(context)}")
        if(anokiUID.isEmpty()){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "fetchFavoriteList: anoki uid is empty, can't fetch fav list")
            return
        }
        try {
            val call = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.BASE_URL).getFavoriteList(anokiUID,AdvertisingIdHelper.getAdvertisingId(context),SystemPropertyHelper.getPropertiesAsString(),versionName)
            call.enqueue(object : Callback<ArrayList<String>> {
                override fun onResponse(
                    call: Call<ArrayList<String>>,
                    response: Response<ArrayList<String>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        favoriteList = response.body()!!
                        InformationBus.informationBusEventListener.submitEvent(Events.FAST_FAVORITES_COLLECTED)
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "fetchFavoriteList onResponse: $favoriteList")
                    }
                    checkIfReadyForRefresh()
                }

                override fun onFailure(call: Call<ArrayList<String>>, t: Throwable) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "failed to fetch favorite items")
                    checkIfReadyForRefresh()
                }

            })
        } catch (e: Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Failed to fetch favorite items ${e.message}")
        }
    }
    private fun updateChannelToFavorites(channelId: String, addToFavorite: Boolean, callback: IAsyncCallback){
        //shouldAdd -> 1 to add item and 0 to remove
        var shouldAdd = if (addToFavorite) 1 else 0
        //TODO: instead of abc have to use the profile Id
        try {
            val call = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.BASE_URL).updateFavoriteList(
                FastFavoriteItem(
                    anokiUID,
                    SystemPropertyHelper.getPropertiesAsString(),
                    AdvertisingIdHelper.getAdvertisingId(context),
                    channelId,
                    shouldAdd
                )
            )
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: ${response.message()}")
                    callback.onSuccess()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailure: ${t.message}")
                    callback.onFailed(Error("Failed to update Favorites"))
                }
            })
        } catch(e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to update Favorites: ${e.message}")
        }
    }

    private fun updateDntInServer(value: Boolean, callback: IAsyncCallback){
        val dnt = if (value) 1 else 0
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateUserSettings: dnt === $dnt")
        try {
            val call = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.BASE_URL).putUserSettings(FastUserSettingsItem(
                anokiUID,SystemPropertyHelper.getPropertiesAsString(), dnt))
            call.enqueue(object : Callback<ResponseBody>{
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: ${response.message()}")
                    userSettings?.dnt = dnt
                    callback.onSuccess()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailure: ${t.message}")
                    userSettings?.dnt = 0
                    callback.onFailed(Error("Failed to update DNT"))
                }

            })
        } catch (e:Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateUserSettings: Failed to update DNT: ${e.message}")
        }
    }

    private fun fetchUserSettings(){
        if(anokiUID.isEmpty()){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "fetchUserSettings: anoki uid is empty, can't fetch user settings")
            return
        }
        try{
            val call = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.BASE_URL).getUserSettings(anokiUID, AdvertisingIdHelper.getAdvertisingId(context),SystemPropertyHelper.getPropertiesAsString(),versionName)
            call.enqueue(object : Callback<UserSettings> {
                override fun onResponse(
                    call: Call<UserSettings>,
                    response: Response<UserSettings>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        userSettings = response.body()
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: fetchUserSettings userSettings ==== ${response.body()}")
                    }
                }

                override fun onFailure(call: Call<UserSettings>, t: Throwable) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to fetch user settings $t")
                }
            })

        }catch (e:Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getDntFromServer: Failed to get user settings from server")
        }
    }

    private fun checkIfReadyForRefresh() {
        if (counter?.incrementAndGet() == 4) {
            InformationBus.informationBusEventListener.submitEvent(Events.FAST_DATA_UPDATED)
            counter = AtomicInteger(0)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun startPeriodicRecommendationAndGenreUpdate(){
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicRecommendationAndGenreUpdate: ")
        if (recommendationRefreshTimer != null) {
            recommendationRefreshTimer?.cancel()
            recommendationRefreshTimer = null
        }
        Handler(Looper.getMainLooper()).post {
            recommendationRefreshTimer = object: CountDownTimer(15 * 60 * 1000L, 60000L) {
                override fun onFinish() {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicRecommendationAndGenreUpdate finish")

                    if(isRecommendationUpdateReady()){
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicRecommendationAndGenreUpdate: fetchRecommendations")
                        fetchRecommendations(true)
                    }
                    if(isGenreUpdateReady()){
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicRecommendationAndGenreUpdate: fetchGenreList")
                        fetchGenreList()
                    }
                    startPeriodicRecommendationAndGenreUpdate()
                }

                override fun onTick(millisUntilFinished: Long) {}
            }.start()
        }
    }

    private fun isRecommendationUpdateReady(): Boolean{
        val currentTime = System.currentTimeMillis()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isRecommendationUpdateReady: current time ${Date(currentTime)}")
        val timeStampRecommendation =  context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getLong(FastDataProvider(context).ANOKI_RECOMMENDATION_TAG, 0L)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isRecommendationUpdateReady: last recommendation update time ${Date(timeStampRecommendation)}")
        val diffTimeRecommendations = currentTime - timeStampRecommendation
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isRecommendationUpdateReady: diff between current time and last recommendation update time is ${TimeUnit.MILLISECONDS.toMinutes(diffTimeRecommendations)} minutes")
        if(timeStampRecommendation != 0L && diffTimeRecommendations>= PERIODIC_RECOMMENDATIONS_AND_GENRE_TIME){
            return true
        }
        return false
    }

    private fun isGenreUpdateReady(): Boolean{
        val currentTime = System.currentTimeMillis()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isGenreUpdateReady: current time ${Date(currentTime)}")
        val timeStampGenre = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getLong(FastDataProvider(context).ANOKI_GENRE_TAG, 0L)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isGenreUpdateReady: last genre update time ${Date(timeStampGenre)}")
        val diffTimeGenre = currentTime - timeStampGenre
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isGenreUpdateReady: diff between current time and last genre update time is ${TimeUnit.MILLISECONDS.toMinutes(diffTimeGenre)} minutes")
        if(timeStampGenre != 0L && diffTimeGenre >= PERIODIC_RECOMMENDATIONS_AND_GENRE_TIME){
            return true
        }
        return false
    }

    private fun startPeriodicPromotionUpdate() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicPromotionUpdate: ")

        if (promotionsRefreshTimer != null) {
            promotionsRefreshTimer?.cancel()
            promotionsRefreshTimer = null
        }
        Handler(Looper.getMainLooper()).post {
            promotionsRefreshTimer = object: CountDownTimer(15 * 60 * 1000L, 60000L) {
                override fun onFinish() {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicPromotionUpdate finish")
                    if(isPromotionUpdateReady()){
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startPeriodicPromotionUpdate fetchPromotions called")
                        fetchPromotions(true)
                    }
                    startPeriodicPromotionUpdate()
                }

                override fun onTick(millisUntilFinished: Long) {}
            }.start()
        }
    }

    private fun isPromotionUpdateReady(): Boolean{
        val currentTime = System.currentTimeMillis()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isPromotionUpdateReady: current time ${Date(currentTime)}")
        val timeStampPromotions = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getLong(FastDataProvider(context).ANOKI_PROMOTION_TAG, 0L)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isPromotionUpdateReady: last promotion update time ${Date(timeStampPromotions)}")
        val diffTimePromotions = currentTime - timeStampPromotions
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isPromotionUpdateReady: diff between current time and last promotion update time is ${TimeUnit.MILLISECONDS.toMinutes(diffTimePromotions)} minutes")
        if(timeStampPromotions != 0L && diffTimePromotions >= PERIODIC_PROMOTION_TIME){
            return true
        }
        return false
    }
}