package com.iwedia.cltv.manager

import android.util.Log
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.scene.favouritesListModification.FavouritesListModificationScene
import com.iwedia.cltv.scene.favouritesListModification.FavouritesListModificationSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import listeners.AsyncDataReceiver

/**
 * Favourite list modification scene manager
 *
 * @author Aleksandar Lazic
 */
class FavouritesListModificationSceneManager : GAndroidSceneManager, FavouritesListModificationSceneListener {

    constructor(context: MainActivity, worldHandler: ReferenceWorldHandler) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.FAVOURITES_MODIFICATION_SCENE
    )

    var isAddListSceneType : Boolean? = null
    var isInitialized = false

    override fun createScene() {
        isInitialized = false
        scene = FavouritesListModificationScene(context!!, this)
    }

    override fun onEnterPressed(text: String) {
        (data!!.getDataByIndex(0) as AsyncDataReceiver<String>).onReceive(text)

        //destroy scene
        super.onBackPressed()
    }

    override fun onSceneInitialized() {
        isInitialized = true
        if (data!!.getDataByIndex(1) as Boolean) {
            //add fav list
            scene!!.refresh(true)
        } else {
            //edit name
            scene!!.refresh(false)

            //send current fav name
            scene!!.refresh(data!!.getDataByIndex(2))
        }
    }

    override fun onBackPressed(): Boolean {
        if (!isInitialized) return true
        (data!!.getDataByIndex(0) as AsyncDataReceiver<String>).onFailed(null)

        //destroy scene
        return super.onBackPressed()
    }
}