package com.example.videoplayer.Hilt

import android.content.Context
import com.example.videoplayer.data.DatabaseDB
import com.example.videoplayer.data.Repository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) //where to install the module
class DatabaseModule {
    @Singleton   //only one instance
    @Provides
    fun provideDB(@ApplicationContext context: Context):DatabaseDB{
        return DatabaseDB(context)
    }

//    @Singleton   //only one instance
//    @Provides
//    fun provideRepository (databaseDB: DatabaseDB ):Repository{
//        return Repository(databaseDB)
//    }
}