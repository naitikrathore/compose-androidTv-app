package com.iwedia.cltv.manager

import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.CategoryInterface
import com.iwedia.cltv.platform.`interface`.FavoritesInterface
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.favorite.FavoriteItem
import com.iwedia.cltv.platform.model.favorite.FavoriteItemType
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.favourite.FavoriteScene
import com.iwedia.cltv.scene.favourite.FavoriteSceneListener
import com.iwedia.cltv.scene.home_scene.guide.HorizontalGuideSceneWidget

class FavoriteSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    private var tvModule: TvInterface,
    private var categoryModule: CategoryInterface,
    private val favoriteModule: FavoritesInterface,
    private val textToSpeechModule: TTSInterface
) : ReferenceSceneManager(context, worldHandler, ReferenceWorldHandler.SceneId.FAVOURITE_SCENE),
    FavoriteSceneListener {

    override fun createScene() {
        scene = FavoriteScene(context!!, this)
    }

    override fun initConfigurableKeys() {}

    override fun onTimeChanged(currentTime: Long) {}

    override fun getFavoritesCategories(callback: IAsyncDataCallback<ArrayList<String>>) {
        favoriteModule.getAvailableCategories(callback)
    }

    override fun getFavoriteItemList(tvChannel: TvChannel): ArrayList<String> {
        tvModule.getChannelList().forEach {
            if (it.channelId == tvChannel.channelId) {
                return it.favListIds
            }
        }
        return arrayListOf()
    }

    override fun onFavoriteButtonPressed(tvChannel: TvChannel, favListIds: ArrayList<String>) {
        val callback = object : IAsyncCallback {
            override fun onSuccess() {
                (scene as FavoriteScene).refreshFavoriteButton()
                categoryModule.setActiveEpgFilter(HorizontalGuideSceneWidget.GUIDE_FILTER_ALL)
            }

            override fun onFailed(error: Error) {
            }
        }
        run exitForEach@{
            tvModule.getChannelList().forEach { channel ->
                if (tvChannel.channelId == channel.channelId) {
                    val favoriteItem = FavoriteItem(
                        channel.id,
                        FavoriteItemType.TV_CHANNEL,
                        getFavoriteItemList(channel),
                        channel,
                        favListIds
                    )
                    favoriteModule.updateFavoriteItem(
                        favoriteItem,
                        callback
                    )
                    return@exitForEach
                }
            }
        }
    }

    override fun resolveConfigurableKey(keyCode: Int, action: Int): Boolean {
        return false
    }

    override fun onSceneInitialized() {
        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {}

            override fun onReceive(data: TvChannel) {
                scene!!.refresh(data)
            }
        })
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule!!.setSpeechText(text = text, importance = importance)
    }
}