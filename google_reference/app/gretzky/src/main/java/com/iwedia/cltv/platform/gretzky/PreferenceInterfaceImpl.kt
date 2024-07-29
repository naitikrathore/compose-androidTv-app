package com.iwedia.cltv.platform.gretzky

import com.iwedia.cltv.platform.`interface`.PreferenceInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.base.PreferenceInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.GeneralConfigInterface
import com.iwedia.cltv.platform.model.PrefMenu
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.PrefType

class PreferenceInterfaceImpl(val utilsInterface: UtilsInterface, generalConfigInterface: GeneralConfigInterface) : PreferenceInterfaceBaseImpl(utilsInterface, generalConfigInterface) {

    override fun getPreferenceTypes(): List<PrefType> {
        return mutableListOf(
            PrefType.PLATFORM
        )
    }

    override fun getPreferenceMenus(type: PrefType): List<PrefMenu> {
        if (type == PrefType.BASE) return super.getPreferenceMenus(type)
        return mutableListOf(
            PrefMenu.SETUP,
            PrefMenu.AUDIO,
            PrefMenu.SUBTITLE,
            PrefMenu.SYSTEMINFO,
            PrefMenu.OPEN_SOURCE_LICENSES
        )
    }

    override fun getPreferenceSubMenus(prefMenu: PrefMenu, type: PrefType): List<PrefSubMenu>{
        if (type == PrefType.BASE) return super.getPreferenceSubMenus(prefMenu, type)

        when(prefMenu){
            PrefMenu.SETUP-> return mutableListOf(
                PrefSubMenu.CHANNEL_SCAN,
                PrefSubMenu.CHANNEL_EDIT,
                PrefSubMenu.DEFAULT_CHANNEL,
                PrefSubMenu.LCN,
                PrefSubMenu.ANTENNA,
                PrefSubMenu.PARENTAL_CONTROL,
                PrefSubMenu.DISPLAY_MODE
            )

            PrefMenu.AUDIO-> return mutableListOf(
                PrefSubMenu.FIRST_LANGUAGE,
                PrefSubMenu.SECOND_LANGUAGE,
                PrefSubMenu.AUDIO_DESCRIPTION
            )
            PrefMenu.SUBTITLE-> return mutableListOf(
                PrefSubMenu.GENERAL,
                PrefSubMenu.FIRST_LANGUAGE,
                PrefSubMenu.SECOND_LANGUAGE
            )
            PrefMenu.SYSTEMINFO-> return mutableListOf(
            )
            else -> {

            }
        }
        return mutableListOf()
    }

    override fun initializeEas() {
    }

    override fun disposeEas() {
    }

    override fun getEasChannel(): String {
        return ""
    }
}