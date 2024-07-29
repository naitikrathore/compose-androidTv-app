package tv.anoki.ondemand.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import tv.anoki.framework.data.Result
import tv.anoki.ondemand.MockkUnitTest
import tv.anoki.ondemand.domain.model.series.Season
import tv.anoki.ondemand.domain.use_case.GetSeriesWithIdUseCase
import tv.anoki.ondemand.presentation.seasons_and_episode_selection.SeasonsEpisodesScreenEvent
import tv.anoki.ondemand.presentation.seasons_and_episode_selection.SeasonsAndEpisodeViewModel

class SeasonsAndEpisodeViewModelTest : MockkUnitTest() {

    @SpyK
    @InjectMockKs
    lateinit var viewModel: SeasonsAndEpisodeViewModel

    @RelaxedMockK
    lateinit var getSeriesWithIdUseCase: GetSeriesWithIdUseCase

    @Test
    fun getSeriesTestLoading() = runTest {

        coEvery { (getSeriesWithIdUseCase.invoke("")) } returns flow { emit(Result.Loading) }

        viewModel.onEvent(SeasonsEpisodesScreenEvent.FetchSeries(""))

        // Assert (Then)
        viewModel.uiState.test {
            awaitItem().apply {
                Truth.assertThat(this.isLoading).isTrue()
            }
        }
    }


    @Test
    fun getSeriesTestReady() = runTest {

        val mockSeasonList = listOf<Season>()

        coEvery { (getSeriesWithIdUseCase.invoke("")) } returns flow {
            emit(
                Result.Success(
                    mockSeasonList
                )
            )
        }

        viewModel.onEvent(SeasonsEpisodesScreenEvent.FetchSeries(""))

        viewModel.uiState.test {
            awaitItem().apply {
                Truth.assertThat(this.seasonList).isNotNull()
            }
        }
    }


    @Test
    fun getSeriesTestError() = runTest {

        coEvery { (getSeriesWithIdUseCase.invoke("")) } returns flow { emit(Result.Error()) }

        viewModel.onEvent(SeasonsEpisodesScreenEvent.FetchSeries(""))

        viewModel.uiState.test {
            awaitItem().apply {
                Truth.assertThat(this.errorMessage).isNotNull()
            }
        }
    }
}