package com.iwedia.cltv.scene.live_scene

import android.view.KeyEvent
import com.iwedia.cltv.utils.ConfigurableKey

class LiveSceneKeyRedAction(var sceneListener: LiveSceneListener, param: Int, type: Int) :
    ConfigurableKey(type) {

    var action = param

    companion object {
        const val NONE = -1
        val TIMESHIFT_PLAYER = 1
        val RECORD = 2
    }

    override fun handleKey(actionType: Int): Boolean {

        if (actionType == KeyEvent.ACTION_UP) {
            if (type == Type.ACTION_DOWN) {
                return false
            }
        }

        if (actionType == KeyEvent.ACTION_DOWN) {
            if (type == Type.ACTION_UP) {
                return false
            }
        }

        when (action) {
        }

        return false
    }

}