package com.example.lazymultiselectcompose

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Tweetsy :Application(){
    override fun onCreate() {
        super.onCreate()
    }
}