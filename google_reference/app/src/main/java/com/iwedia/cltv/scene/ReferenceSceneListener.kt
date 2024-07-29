package com.iwedia.cltv.scene

import world.SceneListener

interface ReferenceSceneListener : SceneListener {

    fun resolveConfigurableKey(keyCode: Int, action: Int) : Boolean

}