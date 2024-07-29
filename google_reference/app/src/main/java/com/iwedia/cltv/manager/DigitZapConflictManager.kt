package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.scene.digit_zap_conflict.DigitZapConflictScene
import com.iwedia.cltv.scene.digit_zap_conflict.DigitZapConflictSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneData

class DigitZapConflictManager (context: MainActivity, worldHandler: ReferenceWorldHandler,var tvModule: TvInterface): GAndroidSceneManager(context, worldHandler, ReferenceWorldHandler.SceneId.CHANNEL_SCENE), DigitZapConflictSceneListener {

    override fun createScene() {
        scene = DigitZapConflictScene(context!!, this)

    }

    override fun onChannelClicked(tvChannel: TvChannel) {
        if (tvChannel != null) {
            tvModule.changeChannel(tvChannel, object : IAsyncCallback {
                override fun onFailed(error: Error) {
                }

                override fun onSuccess() {
                    InformationBus.submitEvent(Event(Events.CHANNEL_CHANGED, tvChannel))
                    //Reset the next/prev zap list to ALL channels
                    tvModule.activeCategoryId = FilterItemType.ALL_ID
                }
            })
        }

        ReferenceApplication.runOnUiThread {
            worldHandler!!.triggerAction(id, Action.DESTROY)
        }
    }

    override fun getChannelsInConflict(): MutableList<TvChannel> {
        return data!!.getData() as MutableList<TvChannel>
    }

    override fun onSceneInitialized() {
    }

    override fun triggerActionWithData(action: Int, data: SceneData?) {
        super.triggerActionWithData(action, data)
    }

    override fun onBackPressed(): Boolean {
        ReferenceApplication.runOnUiThread {
            worldHandler!!.triggerAction(id, Action.DESTROY)
        }
        return true
    }
}
