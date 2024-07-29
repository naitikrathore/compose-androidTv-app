package com.iwedia.cltv.scene.input_scene

import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.parental.InputSourceData
import world.SceneListener


interface InputSceneListener : SceneListener {
    fun onClicked(inputData: InputItem, position: Int, blocked: Boolean)

    fun blockInput(inputData: InputSourceData, isBlock: Boolean)
}