package com.iwedia.cltv.anoki_fast.epg

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.FastZapBannerDataProvider
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.scene.home_scene.HomeSceneData
import world.SceneManager

class BackFromPlayback {
    companion object {
        /**
         * [isAnyKeyPressed] maintains the state of whether the user has pressed any button during the LiveScene.
         * If the user presses any button, excluding Back or Escape, while in LiveScene, this flag is set to true.
         * This enables handling Back in LiveScene similar to pressing Ok in the LiveScene.
         */
        private var isAnyKeyPressed = false
        private var liveSceneEnteredFromLiveHome = false
        private lateinit var regionSupportedListener: ()->Boolean

        /**
         * This flag indicates that zap is done from home or search scene
         */
        var zapFromHomeOrSearch = false

        fun resetKeyPressedState() {
            isAnyKeyPressed = false
        }

        fun setKeyPressedState() {
            isAnyKeyPressed = true
        }

        fun setRegionSupportedListener(listener: ()->Boolean) {
           regionSupportedListener = listener
        }

        fun setLiveSceneFromLiveHomeState(state: Boolean){
            liveSceneEnteredFromLiveHome = state
        }

        fun getLiveSceneFromLiveHomeState(): Boolean{
            return liveSceneEnteredFromLiveHome
        }


        @RequiresApi(Build.VERSION_CODES.R)
        fun onOkPressed(onBackPressed: Boolean = false) {
            val position = if (regionSupportedListener()) {
                if ((ReferenceApplication.worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.DEFAULT.ordinal) {
                    // if internet is not there it would open homescene broadcast and there is not 2nd position in homescenebroadcast
                    //so initial position should be 1 in case of home scene broadcast otherwise blank screen will come.
                    if (ReferenceApplication.isRegionSupported) 2
                    else 1
                }else 1
            } else 1

            // 1 - focus should be on second item in Top Menu (Live)
            // 2 - focus should be on third item in Top Menu (Broadcast)
            ReferenceApplication.worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            var scene = ReferenceApplication.worldHandler?.active
            scene?.let {
                val sceneData = HomeSceneData(it.id, it.instanceId)
                sceneData.focusToCurrentEvent = onBackPressed
                sceneData.initialFilterPosition = position

                //need to hide fast zap banner if it is visible.
                val intent = Intent(FastZapBannerDataProvider.FAST_HIDE_ZAP_BANNER_INTENT)
                ReferenceApplication.applicationContext().sendBroadcast(intent)
                ReferenceApplication.worldHandler?.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.HOME_SCENE,
                    SceneManager.Action.SHOW_OVERLAY,
                    sceneData
                )
            }
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun backKeyPressed() {
            if (isAnyKeyPressed || liveSceneEnteredFromLiveHome) {
                onOkPressed(false) // focus TopMenu in HomeScene - Live if active channel is from Live or Broadcast otherwise.
                liveSceneEnteredFromLiveHome = false
            } else if (isSearchSceneHidden()) {
                ReferenceApplication.worldHandler?.triggerAction(
                    ReferenceWorldHandler.SceneId.SEARCH, SceneManager.Action.SHOW
                )
                zapFromHomeOrSearch = false
            } else if (isDetailsSceneHidden()) {
                ReferenceApplication.worldHandler?.triggerAction(
                    ReferenceWorldHandler.SceneId.DETAILS_SCENE, SceneManager.Action.SHOW
                )
                zapFromHomeOrSearch = false
            } else {
                if (ReferenceApplication.isHiddenFromSetings) {
                    ReferenceApplication.isHiddenFromSetings = false
                    return
                }
                ReferenceApplication.worldHandler?.triggerAction(
                    ReferenceWorldHandler.SceneId.HOME_SCENE, SceneManager.Action.SHOW
                )
                zapFromHomeOrSearch = false
            }
        }

        private fun isSearchSceneHidden(): Boolean {
            ReferenceApplication.worldHandler!!.getHiddens().value.forEach { sceneManager ->
                    if (sceneManager.id == ReferenceWorldHandler.SceneId.SEARCH) {
                        return true
                    }
                }
            return false
        }

        private fun isDetailsSceneHidden(): Boolean {
            ReferenceApplication.worldHandler!!.getHiddens().value.forEach { sceneManager ->
                if (sceneManager.id == ReferenceWorldHandler.SceneId.DETAILS_SCENE) {
                    return true
                }
            }
            return false
        }
    }
}