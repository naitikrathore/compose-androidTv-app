package com.iwedia.cltv.scene.fti.scanFinished

import world.SceneListener

/**
 * Fti finish scan scene listener
 *
 * @author Aleksandar Lazic
 */
interface FtiFinishScanSceneListener : SceneListener {
    fun onProceedClicked()
}