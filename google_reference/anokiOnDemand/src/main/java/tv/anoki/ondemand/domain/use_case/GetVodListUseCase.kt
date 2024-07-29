package tv.anoki.ondemand.domain.use_case

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tv.anoki.framework.data.Result
import tv.anoki.ondemand.domain.model.VODItems
import tv.anoki.ondemand.domain.repository.VodNetworkRepository
import tv.anoki.ondemand.helper.buildOptionsMap
import javax.inject.Inject

/**
 * The function to invoke api call to get the VOD lists data
 */
class GetVodListUseCase @Inject constructor(
    private val networkRepository: VodNetworkRepository,
    private val sharedPreferences: SharedPreferences
) {
    operator fun invoke(): Flow<Result<List<VODItems>>> = flow {
        emit(Result.Loading)

        val options: HashMap<String, String> = buildOptionsMap(sharedPreferences)
        try {
            val data = networkRepository.getVideoOnDemands(options)
            emit(Result.Success(data))
        } catch (e: Exception) {
            emit(Result.Error())
        }
    }
}