package com.iwedia.cltv.scene.fti.selectInput

import android.media.tv.TvInputInfo
import world.SceneListener

/**
 * Fti select input scan scene interface
 *
 * @author Aleksandar Lazic
 */
interface FtiSelectInputScanSceneListener : SceneListener {
    fun onInputSelected(input : TvInputInfo)
    fun requestInputs()
}