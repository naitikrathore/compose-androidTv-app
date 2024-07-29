package tv.anoki.ondemand.domain.use_case

import android.content.SharedPreferences
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import tv.anoki.framework.data.Result
import tv.anoki.ondemand.MockkUnitTest
import tv.anoki.ondemand.domain.model.series.Season
import tv.anoki.ondemand.domain.repository.VodNetworkRepository
import tv.anoki.ondemand.helper.buildOptionsMap
import tv.anoki.ondemand.test_helpers.readFileFromResources

class GetSeriesWithIdUseCaseTest : MockkUnitTest() {

    @SpyK
    @InjectMockKs
    lateinit var getSeriesWithIdUseCase: GetSeriesWithIdUseCase

    @RelaxedMockK
    lateinit var sharedPreferences: SharedPreferences

    @RelaxedMockK
    lateinit var networkRepository: VodNetworkRepository

    private fun getOptionsMap(seriesId: String): HashMap<String, String> {
        val options = buildOptionsMap(sharedPreferences)
        options["contentId"] = seriesId
        return options
    }

    @Test
    fun invokeSuccessTest() = runTest {
        val seriesId = "testId"

        // Load local data from json file
        val seriesData: List<Season> = readFileFromResources(
            coroutineContext,
            "/series_data.json",
            List::class.java
        ) as List<Season>

        // Mock the api call
        coEvery {
            networkRepository.getSeasonsForSeries(getOptionsMap(seriesId))
        } returns seriesData

        // Collect all emits
        var list: List<Result<Any>> = emptyList()
        getSeriesWithIdUseCase(seriesId).collect { result ->
            list = list.plus(result)
        }

        // Verify the responses
        Assert.assertTrue(
            "Responses is of type Loading",
            list[0] is Result.Loading
        )
        Assert.assertTrue(
            "Responses is of type Success",
            list[1] is Result.Success
        )
        Assert.assertEquals(
            "Responses should content success data",
            Result.Success(seriesData),
            list[1]
        )
    }

    @Test
    fun invokeFailureTest() = runTest {
        val seriesId = "testId"

        // Mock the api call
        coEvery {
            networkRepository.getSeasonsForSeries(getOptionsMap(seriesId))
        } throws Exception("Error")

        // Collect all emits
        var list: List<Result<Any>> = emptyList()
        getSeriesWithIdUseCase(seriesId).collect { result ->
            list = list.plus(result)
        }

        // Verify the responses
        Assert.assertTrue(
            "Responses is of type loading",
            list[0] is Result.Loading
        )
        Assert.assertTrue(
            "Responses is of type error",
            list[1] is Result.Error
        )
    }

}