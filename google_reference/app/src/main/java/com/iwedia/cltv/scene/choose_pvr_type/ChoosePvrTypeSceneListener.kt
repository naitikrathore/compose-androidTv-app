package com.iwedia.cltv.scene.choose_pvr_type

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import world.SceneListener

interface ChoosePvrTypeSceneListener: SceneListener, TTSSetterInterface {
        /**
         * Start recording for the active channel
         */
        fun startRecording()
        fun scheduleRecordingForNextEvent()
}