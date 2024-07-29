package com.iwedia.cltv.scene.evaluation_scene

import world.SceneListener

/**
 * EvaluationScene listener
 */
interface EvaluationSceneListener : SceneListener {
    fun onAccepted()
    fun isFirstRun(): Boolean
}