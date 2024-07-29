package com.iwedia.cltv.scene.favouritesListModification

import world.SceneListener

/**
 * Favourite list modification scene listener
 *
 * @author Aleksandar Lazic
 */
interface FavouritesListModificationSceneListener : SceneListener {
    fun onEnterPressed(text : String)
}