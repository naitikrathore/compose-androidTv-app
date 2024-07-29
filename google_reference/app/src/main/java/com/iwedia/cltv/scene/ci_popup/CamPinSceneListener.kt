package com.iwedia.cltv.scene.ci_popup

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.scene.ReferenceSceneListener

interface CamPinSceneListener : ReferenceSceneListener, TTSSetterInterface {
    fun setCamPin(pin : String)
}