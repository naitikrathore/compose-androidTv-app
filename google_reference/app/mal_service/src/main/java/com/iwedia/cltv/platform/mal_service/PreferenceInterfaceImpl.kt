package com.iwedia.cltv.platform.mal_service

import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.PreferenceInterface
import com.iwedia.cltv.platform.model.PrefMenu
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.PrefType

class PreferenceInterfaceImpl(private val serviceImpl: IServiceAPI) : PreferenceInterface {
    override fun getPreferenceTypes(): List<PrefType> {
        var result = arrayListOf<PrefType>()
        serviceImpl.preferenceTypes.forEach { prefType ->
            result.add(fromServicePrefType(prefType))
        }
        return result
    }

    override fun getPreferenceMenus(type: PrefType): List<PrefMenu> {
        var result = arrayListOf<PrefMenu>()
        val servicePrefType = com.cltv.mal.model.prefs.PrefType.values()[type.ordinal]
        serviceImpl.getPreferenceMenus(servicePrefType).forEach { prefMenu ->
            result.add(fromServicePrefMenu(prefMenu))
        }
        return result
    }

    override fun getPreferenceSubMenus(prefMenu: PrefMenu, type: PrefType): List<PrefSubMenu> {
        var result = arrayListOf<PrefSubMenu>()
        val servicePrefType = com.cltv.mal.model.prefs.PrefType.values()[type.ordinal]
        val servicePrefMenu = com.cltv.mal.model.prefs.PrefMenu.values()[prefMenu.ordinal]
        serviceImpl.getPreferenceSubMenus(servicePrefMenu, servicePrefType)
            .forEach { prefSubMenu ->
                result.add(fromServicePrefSubMenu(prefSubMenu))
            }
        return result
    }

    override fun initializeEas() {
        serviceImpl.initializeEas()
    }

    override fun disposeEas() {
        serviceImpl.disposeEas()
    }

    override fun getEasChannel(): String {
        return ""
    }

    override fun isTuneToDetailsChannel(): Boolean {
        return false
    }
}