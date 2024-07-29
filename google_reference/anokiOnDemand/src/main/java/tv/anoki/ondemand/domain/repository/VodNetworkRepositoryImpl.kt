package tv.anoki.ondemand.domain.repository

import tv.anoki.ondemand.data.mapper.toSeasons
import tv.anoki.ondemand.data.mapper.toSeriesMetadata
import tv.anoki.ondemand.data.mapper.toSingleWork
import tv.anoki.ondemand.data.mapper.toVodItems
import tv.anoki.ondemand.data.remote.VodNetworkApi
import tv.anoki.ondemand.domain.model.VODItems
import tv.anoki.ondemand.domain.model.series.Season
import tv.anoki.ondemand.domain.model.series.SeriesMetadata
import tv.anoki.ondemand.domain.model.single_work.SingleWork
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VodNetworkRepositoryImpl @Inject constructor(
    private val api: VodNetworkApi,
) : VodNetworkRepository {

    override suspend fun getVideoOnDemands(options: Map<String, String>): List<VODItems> {
        val result = api.getVodList(options)
        return result.map { it.toVodItems() }
    }

    override suspend fun getVideoOnDemandsSingleWork(options: Map<String, String>): SingleWork {
        val result = api.getVodSingleWork(options)
        return result.toSingleWork()
    }

    override suspend fun getSeriesMetadata(options: Map<String, String>): SeriesMetadata {
        val result = api.getSeriesMetadata(options)
        return result.toSeriesMetadata()
    }

    override suspend fun getSeasonsForSeries(options: Map<String, String>): List<Season> {
        val result = api.getSeasonsForSeries(options)
        return result.map { it.toSeasons() }
    }
}