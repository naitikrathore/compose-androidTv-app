package com.iwedia.cltv.scene.device_option

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import world.SceneListener

interface DeviceOptionListener: SceneListener, TTSSetterInterface, ToastInterface {

    override fun onBackPressed(): Boolean

    override fun onSceneInitialized() {
    }

    fun onSelctTimeshift()

    fun onSelectPvr()

    fun onSelectFormat()

    fun onSelectSpeedTest()
}