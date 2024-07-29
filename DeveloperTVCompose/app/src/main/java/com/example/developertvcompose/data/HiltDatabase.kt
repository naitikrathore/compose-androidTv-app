package com.example.developertvcompose.data

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class HiltDatabase {
    @Singleton
    @Provides
    fun provideDB(@ApplicationContext context:Context):DatabaseDB{
        return DatabaseDB(context)
    }
}