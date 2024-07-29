package com.example.hilt

import android.app.Application
import android.util.Log
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.internal.managers.ApplicationComponentManager
import javax.inject.Inject
import javax.inject.Singleton

//we cnat create DI of interface direclty we need to use @binds or @provides
interface One{
    fun getName()
}
//Dinjection of this class is cretaaed
class ImplementOne @Inject constructor():One{
    override fun getName() {
       Log.e("nai","test")
    }
}

//Dinjection of this class is cretaaed
class Main @Inject constructor(private val one: One){
    fun getName(){
        one.getName()
    }
}

@Module  //container
@InstallIn(ApplicationComponentManager::class)   //module can be accesed thoughout app
abstract class AppModule{
    @Binds // to provide dependcy of interface
    @Singleton   //can be used anywhere in app(global scope)
    abstract fun binding(
        implementOne: ImplementOne
    ):One
}


//@Module
//@InstallIn(Application::class)
//class AppModule{
//    @Provides
//    @Singleton
//    fun binding():One = ImplementOne()
//}