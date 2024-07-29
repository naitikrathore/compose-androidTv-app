package tv.anoki.ondemand.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import tv.anoki.ondemand.data.remote.VodNetworkApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
open class VodNetworkModule {

    @Singleton
    @Provides
    fun provideVodNetworkApiService(retrofit: Retrofit): VodNetworkApi {
        return retrofit.create(VodNetworkApi::class.java)
    }
}