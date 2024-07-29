package com.iwedia.cltv.scene.favourite

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.scene.ReferenceSceneListener

interface FavoriteSceneListener : ReferenceSceneListener, TTSSetterInterface {

    /**
     * Get available favorites categories
     */
    fun getFavoritesCategories(callback: IAsyncDataCallback<ArrayList<String>>)
    fun getFavoriteItemList(tvChannel: TvChannel): ArrayList<String>

    /**
     * On favorite button pressed
     *
     * @param tvChannel tv channel
     * @param favListIds favorites list ids that tv channel should be added to
     */
    fun onFavoriteButtonPressed(tvChannel: TvChannel, favListIds: ArrayList<String>)
}