package com.iwedia.cltv.scene.oad

import world.SceneListener

interface OadSceneListener : SceneListener {
    fun cancelOadScan()
    fun acceptOadDownload()
    fun cancelOadDownload()
    fun installOadUpdate()
    fun restartOadScan()
}