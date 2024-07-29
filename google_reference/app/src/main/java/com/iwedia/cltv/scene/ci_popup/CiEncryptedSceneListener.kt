package com.iwedia.cltv.scene.ci_popup

import com.iwedia.cltv.scene.ReferenceSceneListener

interface CiEncryptedSceneListener : ReferenceSceneListener {
    fun enquiryAnswer(abort: Boolean, answer: String)

    fun onNextChannel()

    fun onPreviousChannel()

}