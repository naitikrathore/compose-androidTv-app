package com.iwedia.cltv.scene.zap_banner_scene

import android.view.KeyEvent
import com.iwedia.cltv.utils.ConfigurableKey

class ZapBannerKeyDpadLeftAction(var sceneListener: ZapBannerSceneListener, param: Int, type: Int) :
    ConfigurableKey(type) {

    var action = param
    companion object {
        const val NONE = -1
        val CHANNEL_LIST = 0
        val INFO_BANNER = 1
        val CHANNEL_UP = 2
        val CHANNEL_DOWN = 3

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
            INFO_BANNER -> {
                sceneListener.showInfoBanner()
                return true
            }

            CHANNEL_LIST -> {
                sceneListener.showChannelList()
                return true
            }

            CHANNEL_UP -> {
                sceneListener.channelUp()
                return true
            }

            CHANNEL_DOWN -> {
                sceneListener.channelDown()
                return true
            }
        }

        return false
    }

}