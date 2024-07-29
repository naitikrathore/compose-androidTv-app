package com.iwedia.cltv.scene.postal_code

import world.SceneListener

interface PostalCodeSceneListener : SceneListener {
    fun isAccessibilityEnabled(): Boolean
    fun getPostalCode(): String?
    fun onPostalConfirmed(postalCode: String)

}