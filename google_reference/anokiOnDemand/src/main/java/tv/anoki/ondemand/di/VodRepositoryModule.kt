package tv.anoki.ondemand.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tv.anoki.ondemand.domain.repository.VodNetworkRepository
import tv.anoki.ondemand.domain.repository.VodNetworkRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VodRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNetworkRepository(
        baseNetworkRepositoryImpl: VodNetworkRepositoryImpl
    ): VodNetworkRepository
}
