package com.example.hilt

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.FragmentComponent

//@InstallIn(FragmentComponent::class)//error
@Module
@InstallIn(ActivityComponent::class)
class UserModule {
    @Provides
    fun provideUserRepo(): UserRepo{
        return FirebaseRepo()
    }
}
//@Module
//abstract class UserModule{
//    @Binds
//    abstract fun bindsUserRepoInterfaceWithObject(sqlRepo: SQLRepo):UserRepo
//    //wheneve there is need od UserRepo interface object hilt will retutn sql repo object
//}