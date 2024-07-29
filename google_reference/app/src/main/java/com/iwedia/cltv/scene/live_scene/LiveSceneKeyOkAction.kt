package com.iwedia.cltv.scene.live_scene

import android.view.KeyEvent
import com.iwedia.cltv.utils.ConfigurableKey

class LiveSceneKeyOkAction(var sceneListener: LiveSceneListener, param: Int, type: Int) :
    ConfigurableKey(type) {

    var action = param

    companion object {
        const val NONE = -1
        val HOME = 0
        val INFO_BANNER = 1
        val CHANNEL_LIST = 2
        val TIMESHIFT_PLAYER = 3
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
            HOME -> {
                sceneListener.showHome(LiveSceneListener.ShowHomeType.SET_FOCUS_TO_LIVE_OR_BROADCAST_IN_TOP_MENU)
                return true
            }

            INFO_BANNER -> {
                sceneListener.showInfoBanner()
                return true
            }

            CHANNEL_LIST -> {
                sceneListener.showChannelList()
                return true
            }

            TIMESHIFT_PLAYER -> {
                if(sceneListener.getConfigInfo("timeshift")) {
                    sceneListener.showPlayer(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                }
                return true
            }
        }

        return false
    }

}