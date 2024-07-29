package com.example.hilt.Demo

import android.util.Log
import javax.inject.Inject

class Engine {
    @Inject
    constructor()
    fun getEngine(){
        Log.e("nait","Engine")
    }

}