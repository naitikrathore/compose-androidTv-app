package com.example.hilt

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApplication :Application() {
    //The annotation genrates Hilts code that will first create a base class that will extemnds Android app application class
    //and it will generate container which will hold all the dependecies
    //and it ties the lifecycle of container to the life cycle of application

}