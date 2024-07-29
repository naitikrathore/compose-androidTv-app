package tv.anoki.ondemand.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tv.anoki.ondemand.domain.repository.VodNetworkRepository
import tv.anoki.ondemand.domain.use_case.GetSeriesWithIdUseCase
import tv.anoki.ondemand.domain.use_case.GetSingleWorkWithIdUseCase
import tv.anoki.ondemand.domain.use_case.GetVodListUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class VodUseCaseModule {

    @Singleton
    @Provides
    fun provideGetSeriesWithIdUseCase(
        networkRepository: VodNetworkRepository,
        sharedPreference: SharedPreferences
    ): GetSeriesWithIdUseCase {
        return GetSeriesWithIdUseCase(networkRepository, sharedPreference)
    }

    @Singleton
    @Provides
    fun provideGetSingleWorkWithIdUseCase(
        networkRepository: VodNetworkRepository,
        sharedPreference: SharedPreferences
    ): GetSingleWorkWithIdUseCase {
        return GetSingleWorkWithIdUseCase(networkRepository, sharedPreference)
    }


    @Singleton
    @Provides
    fun provideGetVodListUseCase(
        networkRepository: VodNetworkRepository,
        sharedPreference: SharedPreferences
    ): GetVodListUseCase {
        return GetVodListUseCase(networkRepository, sharedPreference)
    }
    //TODO Vasilisa: remove app context once service integration is done


}