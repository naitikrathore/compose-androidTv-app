package com.iwedia.cltv.platform.model.fast_backend_utils

import com.iwedia.cltv.platform.model.ChannelListModel
import com.iwedia.cltv.platform.model.FastFavoriteItem
import com.iwedia.cltv.platform.model.FastInfoItem
import com.iwedia.cltv.platform.model.FastRatingItem
import com.iwedia.cltv.platform.model.FastRatingListItem
import com.iwedia.cltv.platform.model.FastTosOptInItem
import com.iwedia.cltv.platform.model.FastUserSettingsItem
import com.iwedia.cltv.platform.model.ProgramListModel
import com.iwedia.cltv.platform.model.PromotionItem
import com.iwedia.cltv.platform.model.RecommendationRow
import com.iwedia.cltv.platform.model.UserSettings
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface FastBackendAPI {

    @GET("v3/promotions")
    fun getPromotionList(
        @Query("auid") auid: String,
        @Query("ip") ip: String,
        @Query("country") country: String,
        @Query("deviceid") deviceId: String,
        @Query("locale") locale: String,
        @Query("platform-id") platformId: String,
        @Query("buildNumber") buildNumber: String,
    ): Call<ArrayList<PromotionItem>>

    @GET("v3/recommendations")
    fun getRecommendationRows(
        @Query("auid") auid: String,
        @Query("ip") ip: String,
        @Query("country") country: String,
        @Query("deviceid") deviceId: String,
        @Query("locale") locale: String,
        @Query("platform-id") platformId: String,
        @Query("buildNumber") buildNumber: String,
    ): Call<ArrayList<RecommendationRow>>

    @GET("v3/genrelist")
    fun getGenreList(
        @Query("auid") auid: String,
        @Query("ip") ip: String,
        @Query("country") country: String,
        @Query("deviceid") deviceId: String,
        @Query("locale") locale: String,
        @Query("platform-id") platformId: String,
        @Query("buildNumber") buildNumber: String,
    ): Call<ArrayList<String>>

    @GET("v3/favouritelist")
    fun getFavoriteList(
        @Query("auid") auid:String,
        @Query("deviceid") deviceid:String,
        @Query("platform-id") platformId: String,
        @Query("buildNumber") buildNumber: String,
    ): Call<ArrayList<String>>

    @PUT("/v3/updatefavourite")
    fun updateFavoriteList(@Body fastFavoriteItem: FastFavoriteItem): Call<ResponseBody>

    @PUT("/v3/updateusersettings")
    fun putUserSettings(@Body fastUserSettingsItem: FastUserSettingsItem) : Call<ResponseBody>

    @GET("/v3/usersettings")
    fun getUserSettings(@Query("auid") auid: String, @Query("deviceid") deviceId: String,@Query("platform-id") platformId: String,@Query("buildNumber") buildNumber: String,): Call<UserSettings>

    @GET("/v3/tos/opt-in")
    fun getTosOptIn(@Query("auid") auid: String): Call<ResponseBody>

    @GET("/v3/fast-info")
    fun getFastInfo(@Query("auid") auid: String, @Query("locale") locale: String, @Query("buildNumber") buildNumber: String,): Call<FastInfoItem>

    @PUT("/v3/tos/opt-in")
    fun putTosOptIn(@Body fastTosOptInItem: FastTosOptInItem) : Call<ResponseBody>

    @GET("/v3/ratinglist")
    fun getRatingList(@Query("auid") auid: String,@Query("locale") locale: String,@Query("platform-id") platformId: String,@Query("buildNumber") buildNumber: String,): Call<ArrayList<FastRatingListItem>>

    @GET("/v3/channellist")
    suspend fun getChannelList(@Query("country") country : String, @Query("ip") ip : String, @Query("auid") auid: String, @Query("deviceid") deviceid: String,@Query("locale") locale: String,@Query("platform-id") platformId: String,@Query("buildNumber") buildNumber: String,): Response<ArrayList<ChannelListModel>>

    @GET("/v3/programlist")
    suspend fun getProgramList(@Query("country") country : String, @Query("channelIds") channelIds : String, @Query("startEpoch") startEpoch : Long, @Query("auid") auid: String,@Query("locale") locale: String, @Query("platform-id") platformId: String,@Query("buildNumber") buildNumber: String,): Response<ArrayList<ProgramListModel>>

    @GET("/v3/auid")
    suspend fun getAnokiUidFromServer(@Query("deviceId") deviceId: String, @Query("platform-id") platformId: String,@Query("buildNumber") buildNumber: String,): Response<String>

    @GET("/v3/auid")
    fun getAnokiUID(@Query("deviceId") deviceId: String, @Query("platform-id") platformId: String,@Query("buildNumber") buildNumber: String,): Call<ResponseBody>

    @PUT("/v3/updaterating")
    fun updateRating(@Body fastRatingItem: FastRatingItem): Call<ResponseBody>
}