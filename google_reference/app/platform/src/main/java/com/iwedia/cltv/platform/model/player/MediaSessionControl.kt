package com.iwedia.cltv.platform.model.player

import android.annotation.SuppressLint
import android.app.Activity

@SuppressLint("StaticFieldLeak")
object MediaSessionControl {
    private const val TAG = "MediaSessionControl"
    private var mPlaybackPositionSeconds = 0L
    private lateinit var mActivity: Activity

    private val SPEED_FF_2X = 2
    private val SPEED_FF_4X = 4
    private val SPEED_FF_8X = 8
    private val SPEED_FR_2X = -2
    private val SPEED_FR_4X = -4
    private val SPEED_FR_8X = -8
    val SPEED_FF_1X = 1

    val changeSpeedArr = listOf(SPEED_FR_8X, SPEED_FR_4X , SPEED_FR_2X , SPEED_FF_1X, SPEED_FF_2X , SPEED_FF_4X, SPEED_FF_8X)
    var currentSpeedValue = SPEED_FF_1X
    private var REGULAR_SPEED = SPEED_FF_1X.toFloat()


    fun updatePlaybackSpeed(speed: Float){
       REGULAR_SPEED = speed
    }

    fun getPlaybackSpeed() : Int{
        return REGULAR_SPEED.toInt()
    }

    fun checkRequiredLimit(endPosition: Long, currPosition: Int): Long {
        var needPosition: Long = 1
        var speed = REGULAR_SPEED.toInt()
        if(speed <1){
            var reqPosition =  when (speed){
                SPEED_FF_1X -> SPEED_FF_1X + 1
                SPEED_FF_2X -> SPEED_FF_2X + 2
                SPEED_FF_4X -> SPEED_FF_4X + 8
                SPEED_FF_8X -> SPEED_FF_8X +  40
                else -> {SPEED_FF_1X}
            }
            needPosition = (currPosition + reqPosition).toLong()
        }else{
            var reqPosition =  when (speed){
                SPEED_FF_1X -> SPEED_FF_1X + 1
                SPEED_FF_2X -> SPEED_FF_2X + 2
                SPEED_FF_4X -> SPEED_FF_4X + 8
                SPEED_FF_8X -> SPEED_FF_8X +  40
                else -> {SPEED_FF_1X}
            }
            needPosition = (endPosition - currPosition) + reqPosition
        }
        return needPosition
    }

    val SPEED_FACTORS_MAPPING = mapOf(
        SPEED_FR_8X to 48,
        SPEED_FR_4X to 12 ,
        SPEED_FR_2X to 4,
        SPEED_FF_1X to 2,
        SPEED_FF_2X to 4,
        SPEED_FF_4X to 12,
        SPEED_FF_8X to 48)
}