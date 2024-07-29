package tv.anoki.ondemand.domain.use_case

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tv.anoki.framework.data.Result
import tv.anoki.ondemand.domain.model.single_work.SingleWork
import tv.anoki.ondemand.domain.repository.VodNetworkRepository
import tv.anoki.ondemand.helper.buildOptionsMap
import javax.inject.Inject

/**
 * The function to invoke api call to get the single work details data
 */
class GetSingleWorkWithIdUseCase @Inject constructor(
    private val networkRepository: VodNetworkRepository,
    private val sharedPreferences: SharedPreferences
) {
    operator fun invoke(seriesId: String): Flow<Result<SingleWork>> = flow {
        emit(Result.Loading)

        val options: HashMap<String, String> = buildOptionsMap(sharedPreferences)
        options["contentId"] = seriesId
        try {
            val data = networkRepository.getVideoOnDemandsSingleWork(options)
            emit(Result.Success(data))
        } catch (e: Exception) {
            emit(Result.Error())
        }
    }
}