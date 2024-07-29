package tv.anoki.ondemand.domain.use_case

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tv.anoki.framework.data.Result
import tv.anoki.ondemand.domain.model.series.Season
import tv.anoki.ondemand.domain.repository.VodNetworkRepository
import tv.anoki.ondemand.helper.buildOptionsMap
import javax.inject.Inject

/**
 * The function to invoke api call to get the series data
 */
class GetSeriesWithIdUseCase @Inject constructor(
    private val networkRepository: VodNetworkRepository,
    private val sharedPreferences: SharedPreferences
) {

    operator fun invoke(seriesId: String): Flow<Result<List<Season>>> = flow {
        emit(Result.Loading)

        val options: HashMap<String, String> = buildOptionsMap(sharedPreferences)
        options["contentId"] = seriesId
        try {
            val data = networkRepository.getSeasonsForSeries(options)
            emit(Result.Success(data))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }
}
