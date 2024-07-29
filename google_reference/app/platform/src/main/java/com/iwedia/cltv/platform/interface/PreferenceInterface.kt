package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.PrefMenu
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.PrefType

interface PreferenceInterface {

    companion object {
        /*
        * To enable hybrid preference
        * */
        var ENABLE_HYBRID = true
    }

    /**
     *returns the list of preference types
     */
    fun getPreferenceTypes(): List<PrefType>

    /**
     *returns the list of preference Menus for particular type
     */
    fun getPreferenceMenus(type: PrefType = PrefType.PLATFORM): List<PrefMenu>

    /**
    returns the list of subOptions for particular menu
     */
    fun getPreferenceSubMenus(prefMenu: PrefMenu, type: PrefType = PrefType.PLATFORM):List<PrefSubMenu>

    /**
     It will create EAS object
     */
    fun initializeEas()

    /**
    It will remove EAS callback
     */
    fun disposeEas()

    /**
     * It will zap to the Eas relevant channel
     */
    fun getEasChannel() : String

    /**
     * It will check if zap to the Eas relevant channel is allowed
     */
    fun isTuneToDetailsChannel() : Boolean
}