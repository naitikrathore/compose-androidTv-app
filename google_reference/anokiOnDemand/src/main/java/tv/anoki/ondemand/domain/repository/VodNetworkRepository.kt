package tv.anoki.ondemand.domain.repository

import tv.anoki.ondemand.domain.model.VODItems
import tv.anoki.ondemand.domain.model.series.Season
import tv.anoki.ondemand.domain.model.series.SeriesMetadata
import tv.anoki.ondemand.domain.model.single_work.SingleWork

interface VodNetworkRepository {

    suspend fun getVideoOnDemands(options: Map<String, String>): List<VODItems>

    suspend fun getVideoOnDemandsSingleWork(options: Map<String, String>): SingleWork

    suspend fun getSeriesMetadata(options: Map<String, String>): SeriesMetadata

    suspend fun getSeasonsForSeries(options: Map<String, String>): List<Season>
}