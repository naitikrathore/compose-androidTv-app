package com.example.developertvcompose.data

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp  //ready to provide DI throught the App
class BaseHilt:Application(){
    override fun onCreate() {
        super.onCreate()
    }
}