package com.iwedia.cltv.scene.ci_popup

import com.iwedia.cltv.scene.ReferenceSceneListener

interface MmiMenuSceneListener: ReferenceSceneListener {
    fun onSelectMenuItem(position: Int)
    fun onCancelCurrentMenu()
}