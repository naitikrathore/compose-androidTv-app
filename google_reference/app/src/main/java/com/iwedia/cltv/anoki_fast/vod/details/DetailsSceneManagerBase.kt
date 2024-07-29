package com.iwedia.cltv.anoki_fast.vod.details

import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.reference_scene.ReferenceSceneManager
import com.iwedia.cltv.anoki_fast.vod.details.series.SeriesDetailsScene
import com.iwedia.cltv.anoki_fast.vod.details.single_work.SingleWorkDetailsScene
import com.iwedia.cltv.anoki_fast.vod.player.VodBannerSceneData
import com.iwedia.cltv.manager.HomeSceneManager
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface

abstract class DetailsSceneManagerBase(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    private val playerModule: PlayerInterface,
    protected val type: Type,
    networkModule: NetworkInterface
) : ReferenceSceneManager(
    context,
    worldHandler,
    type.id,
    networkModule
), DetailsSceneListener {

    private var detailsSceneData:DetailsSceneData? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBackPressed(): Boolean {
        ReferenceApplication.worldHandler!!.playbackState = ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
        if (data != null && data!!.previousSceneId == ReferenceWorldHandler.SceneId.SEARCH) {
            playerModule.unmute()
        }
        return super.onBackPressed()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun createScene() {
        HomeSceneManager.IS_VOD_ACTIVE = true
        playerModule.mute()
        scene = when (type) {
            Type.SERIES -> {
                SeriesDetailsScene(context!!, this)
            }

            Type.SINGLE_WORK -> {
                SingleWorkDetailsScene(context!!, this)
            }
        }

    }

    override fun onSceneInitialized() {
        detailsSceneData = data as DetailsSceneData
        scene!!.refresh(detailsSceneData)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onVodItemClicked(vodItem: Any) {
        detailsSceneData?.let {
            val sceneData = VodBannerSceneData(id, instanceId, vodItem)
            ReferenceApplication.worldHandler!!.playbackState = ReferenceWorldHandler.PlaybackState.VOD
            ReferenceApplication.worldHandler?.triggerActionWithData(
                ReferenceWorldHandler.SceneId.VOD_BANNER_SCENE, Action.SHOW_OVERLAY, sceneData
            )
            ReferenceApplication.worldHandler?.triggerAction(
                type.id, Action.HIDE
            )
            ReferenceApplication.worldHandler!!.playbackState =
                ReferenceWorldHandler.PlaybackState.VOD
        }
    }


    enum class Type(val id: Int) {
        SERIES(ReferenceWorldHandler.SceneId.VOD_SERIES_DETAILS_SCENE), SINGLE_WORK(
            ReferenceWorldHandler.SceneId.VOD_SINGLE_WORK_DETAILS_SCENE
        )
    }
}