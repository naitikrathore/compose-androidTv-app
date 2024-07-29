
package com.iwedia.cltv.platform.base

import com.iwedia.cltv.platform.`interface`.GeneralConfigInterface
import com.iwedia.cltv.platform.`interface`.PreferenceInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants.AnokiParentalConstants.USE_ANOKI_RATING_SYSTEM
import com.iwedia.cltv.platform.model.PrefMenu
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.PrefType

open class PreferenceInterfaceBaseImpl(utilsInterface: UtilsInterface, generalConfigInterface: GeneralConfigInterface) : PreferenceInterface{

    override fun getPreferenceTypes(): List<PrefType> {
        return mutableListOf(
            PrefType.BASE
        )
    }

    override fun getPreferenceMenus(type: PrefType): List<PrefMenu> {
        return mutableListOf(
            PrefMenu.PARENTAL_CONTROL,
            PrefMenu.AUDIO,
            PrefMenu.SUBTITLE,
            PrefMenu.ADS_TARGETING,
            PrefMenu.FEEDBACK,
            PrefMenu.OPEN_SOURCE_LICENSES,
            PrefMenu.TERMS_OF_SERVICE
        )
    }

    override fun getPreferenceSubMenus(prefMenu: PrefMenu, type: PrefType): List<PrefSubMenu>{

        when(prefMenu){

            PrefMenu.AUDIO-> return mutableListOf(
                PrefSubMenu.FIRST_LANGUAGE,
                PrefSubMenu.SECOND_LANGUAGE
            )

            PrefMenu.SUBTITLE-> return mutableListOf(
                PrefSubMenu.GENERAL,
                PrefSubMenu.FIRST_LANGUAGE,
                PrefSubMenu.SECOND_LANGUAGE
            )

            PrefMenu.ADS_TARGETING-> return mutableListOf(
                PrefSubMenu.ADS_TARGETING
            )

            PrefMenu.PARENTAL_CONTROL-> if (USE_ANOKI_RATING_SYSTEM) return mutableListOf(
                PrefSubMenu.PARENTAL_CONTROL,
                PrefSubMenu.ANOKI_PARENTAL_RATING,
                PrefSubMenu.CHANGE_PIN
            )
            else return mutableListOf(
                PrefSubMenu.PARENTAL_CONTROL,
                PrefSubMenu.CHANNEL_BLOCK,
                PrefSubMenu.RATING_SYSTEMS,
                PrefSubMenu.RATING_LOCK,
                PrefSubMenu.CHANGE_PIN
            )
            PrefMenu.TERMS_OF_SERVICE -> return mutableListOf(
                PrefSubMenu.PRIVACY_POLICY,
                PrefSubMenu.TERMS_OF_SERVICE
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
        return  ""
    }

    override fun isTuneToDetailsChannel(): Boolean {
        return false
    }
}