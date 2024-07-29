package com.example.hilt.Demo

import android.util.Log
import javax.inject.Inject

class Car @Inject constructor(private val engine: Engine,private val wheel: Wheel){

    fun getCar(){
        Log.e("nait","car drive")
        engine.getEngine()
        wheel.getWheel(  )
    }



}