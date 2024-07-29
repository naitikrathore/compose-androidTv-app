package tv.anoki.ondemand.data.remote

import retrofit2.http.GET
import retrofit2.http.QueryMap
import tv.anoki.ondemand.data.remote.dto.VodListDto
import tv.anoki.ondemand.data.remote.dto.series.SeasonDto
import tv.anoki.ondemand.data.remote.dto.series.SeriesMetadataDto
import tv.anoki.ondemand.data.remote.dto.single_work.SingleWorkDto

interface VodNetworkApi {

    @GET("v3/vodlist")
    suspend fun getVodList(@QueryMap(encoded = true) options: Map<String, String>): VodListDto

    @GET("/v3/vod/single-work")
    suspend fun getVodSingleWork(@QueryMap(encoded = true) options: Map<String, String>): SingleWorkDto

    @GET("/v3/vod/series-metadata")
    suspend fun getSeriesMetadata(@QueryMap(encoded = true) options: Map<String, String>): SeriesMetadataDto

    @GET("/v3/vod/series")
    suspend fun getSeasonsForSeries(@QueryMap(encoded = true) options: Map<String, String>): List<SeasonDto>

}