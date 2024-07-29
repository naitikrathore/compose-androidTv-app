package com.example.hilt.Demo

import android.util.Log
import javax.inject.Inject

class Wheel {
    @Inject
    constructor()
    fun getWheel(){
        Log.e("nait","Engine")

    }

}