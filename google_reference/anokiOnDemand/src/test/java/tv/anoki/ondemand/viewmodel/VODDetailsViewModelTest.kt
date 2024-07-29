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
import tv.anoki.ondemand.domain.use_case.GetSeriesMetadataWithIdUseCase
import tv.anoki.ondemand.domain.use_case.GetSingleWorkWithIdUseCase
import tv.anoki.ondemand.presentation.details.UiState
import tv.anoki.ondemand.presentation.details.single_work.SingleWorkDetailsViewModel

class VODDetailsViewModelTest : MockkUnitTest() {

    @SpyK
    @InjectMockKs
    lateinit var viewModel: SingleWorkDetailsViewModel

    @RelaxedMockK
    lateinit var mockGetSingleWorkWithIdUseCase: GetSingleWorkWithIdUseCase

    @RelaxedMockK
    lateinit var getSeriesMetadataWithIdUseCase: GetSeriesMetadataWithIdUseCase

    @Test
    fun getVideoOnDemandsSingleWorkTest() = runTest {

        val contentId = ""

        coEvery { mockGetSingleWorkWithIdUseCase.invoke(contentId) } returns flow {
            emit(Result.Loading)
        }

        viewModel.getSingleWorkData(contentId)

        // Assert (Then)
        viewModel.uiState.test {
            awaitItem().apply {
                Truth.assertThat(this).isNotNull()
                Truth.assertThat(this).isInstanceOf(UiState::class.java)
            }
        }

    }
}