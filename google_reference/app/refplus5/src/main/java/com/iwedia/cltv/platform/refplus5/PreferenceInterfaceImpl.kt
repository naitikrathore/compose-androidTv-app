package com.iwedia.cltv.platform.refplus5

import com.iwedia.cltv.platform.base.PreferenceInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.GeneralConfigInterface
import com.iwedia.cltv.platform.`interface`.PreferenceInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.PrefMenu
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.parental.Region

class PreferenceInterfaceImpl(val utilsInterface: UtilsInterface,val generalConfigInterface: GeneralConfigInterface): PreferenceInterfaceBaseImpl(utilsInterface, generalConfigInterface) {

    override fun getPreferenceTypes(): List<PrefType> {
        val typeList = mutableListOf<PrefType>()

        if (PreferenceInterface.ENABLE_HYBRID) {
            typeList.add(PrefType.BASE)
        }
        typeList.add(PrefType.PLATFORM)
        return typeList
    }

    override fun getPreferenceMenus(type: PrefType): List<PrefMenu> {
        if (type == PrefType.BASE) return super.getPreferenceMenus(type)

        val menuList = mutableListOf(
            PrefMenu.SETUP,
            PrefMenu.PARENTAL_CONTROL,
            PrefMenu.AUDIO,
            PrefMenu.CLOSED_CAPTIONS,
            PrefMenu.SUBTITLE,
            PrefMenu.TELETEXT,
            PrefMenu.HBBTV,
            PrefMenu.PVR_TIMESHIFT,
            PrefMenu.CAMINFO,
            PrefMenu.SYSTEMINFO,
            PrefMenu.OPEN_SOURCE_LICENSES
        )

        for (option in menuList.toList()){
            if (utilsInterface.getRegion() == Region.US){
                when (option){
                    PrefMenu.SUBTITLE,
                    PrefMenu.TELETEXT,
                    PrefMenu.HBBTV,
                    PrefMenu.CAMINFO,
                    PrefMenu.PVR_TIMESHIFT,
                    PrefMenu.SYSTEMINFO -> menuList.remove(option)
                    else -> {}
                }

            }else if (utilsInterface.getRegion() ==  Region.EU){
                when (option){
                    PrefMenu.CLOSED_CAPTIONS-> menuList.remove(option)
                    else -> {}
                }
            } else if(utilsInterface.getRegion() ==  Region.PA) {
                when (option){
                    PrefMenu.CLOSED_CAPTIONS-> menuList.remove(option)
                    else -> {}
                }
            }
        }
        return menuList
    }

    override fun getPreferenceSubMenus(prefMenu: PrefMenu, type: PrefType): List<PrefSubMenu>{
        if (type == PrefType.BASE) return super.getPreferenceSubMenus(prefMenu, type)

        when(prefMenu){
            PrefMenu.SETUP-> {
                val setupList = mutableListOf<PrefSubMenu>()

                when(utilsInterface.getRegion()){
                    Region.US->{
                        if(!utilsInterface.kidsModeEnabled()){
                            setupList.add(PrefSubMenu.CHANNELS_SETTING)
                            setupList.add(PrefSubMenu.PICTURE)
                            setupList.add(PrefSubMenu.SCREEN)
                            setupList.add(PrefSubMenu.SOUND)
                        }
                        if(generalConfigInterface.getGeneralSettingsInfo("blue_mute")) {
                            setupList.add(PrefSubMenu.BLUE_MUTE)
                        }
                        setupList.add(PrefSubMenu.NO_SIGNAL_AUTO_POWER_OFF)
                    }
                    Region.EU->{
                        setupList.add(PrefSubMenu.CHANNEL_SCAN)
                        setupList.add(PrefSubMenu.CHANNEL_EDIT_MENUS)
                        setupList.add(PrefSubMenu.NO_SIGNAL_AUTO_POWER_OFF)
                        if(!utilsInterface.kidsModeEnabled()){
                            setupList.add(PrefSubMenu.PICTURE)
                            setupList.add(PrefSubMenu.SCREEN)
                            setupList.add(PrefSubMenu.SOUND)
                        }
                        setupList.add(PrefSubMenu.POWER)
                        if(generalConfigInterface.getGeneralSettingsInfo("blue_mute")) {
                            setupList.add(PrefSubMenu.BLUE_MUTE)
                        }
                    }
                    Region.PA->{
                        setupList.add(PrefSubMenu.CHANNEL_SCAN)
                        setupList.add(PrefSubMenu.CHANNEL_EDIT_MENUS)
                        setupList.add(PrefSubMenu.NO_SIGNAL_AUTO_POWER_OFF)
                        if(!utilsInterface.kidsModeEnabled()){
                            setupList.add(PrefSubMenu.PICTURE)
                            setupList.add(PrefSubMenu.SCREEN)
                            setupList.add(PrefSubMenu.SOUND)
                        }
                        setupList.add(PrefSubMenu.POWER)
                        if(generalConfigInterface.getGeneralSettingsInfo("blue_mute")) {
                            setupList.add(PrefSubMenu.BLUE_MUTE)
                        }
                        if(generalConfigInterface.getGeneralSettingsInfo("oad")) {
                            setupList.add(PrefSubMenu.OAD_UPDATE)
                        }
                        if (utilsInterface.getCountryCode() == "ID")
                            setupList.add(PrefSubMenu.POSTAL_CODE)
                    }
                    else -> {}
                }

                return setupList
            }

            PrefMenu.PARENTAL_CONTROL ->{
                val parentalControlList = mutableListOf(
                    PrefSubMenu.PARENTAL_CONTROL,
                    PrefSubMenu.CHANNEL_BLOCK,
                    PrefSubMenu.RATING_SYSTEMS,
                    PrefSubMenu.RATING_LOCK,
                    PrefSubMenu.BLOCK_UNRATED_PROGRAMS,
                    PrefSubMenu.RRT5_LOCK,
                    PrefSubMenu.CHANGE_PIN
                )
                //block unrated programs is only supported in US and SA region.
                val selectedRegion = utilsInterface.getRegion()
                if(selectedRegion != Region.US && selectedRegion != Region.SA){
                    parentalControlList.remove(PrefSubMenu.BLOCK_UNRATED_PROGRAMS)
                }
                if (selectedRegion == Region.EU || selectedRegion == Region.PA) {
                    parentalControlList.remove(PrefSubMenu.RRT5_LOCK)
                }
                return parentalControlList
            }

            PrefMenu.AUDIO-> {
                val region = utilsInterface.getRegion()
                val audioList = mutableListOf<PrefSubMenu>()
                if (region == Region.EU) audioList.add(PrefSubMenu.AUDIO_TYPE)
                audioList.add(PrefSubMenu.FIRST_LANGUAGE)
                if (region == Region.EU || region == Region.PA) audioList.add(PrefSubMenu.SECOND_LANGUAGE)
                if (region == Region.US || region == Region.PA) audioList.add(PrefSubMenu.AUDIO_DESCRIPTION)
                if (region == Region.US) audioList.add(PrefSubMenu.HEARING_IMPAIRED)
                if (region == Region.EU || region == Region.US || region == Region.PA) audioList.add(PrefSubMenu.VISUALLY_IMPAIRED)

                return audioList
            }

            PrefMenu.CLOSED_CAPTIONS-> return mutableListOf(
                PrefSubMenu.DISPLAY_CC,
                PrefSubMenu.CAPTION_SERVICES,
                PrefSubMenu.ADVANCED_SELECTION,
                PrefSubMenu.TEXT_SIZE,
                PrefSubMenu.FONT_FAMILY,
                PrefSubMenu.TEXT_COLOR,
                PrefSubMenu.TEXT_OPACITY,
                PrefSubMenu.EDGE_TYPE,
                PrefSubMenu.EDGE_COLOR,
                PrefSubMenu.BACKGROUND_COLOR,
                PrefSubMenu.BACKGROUND_OPACITY,
            )

            PrefMenu.SUBTITLE -> {
                val subtitleList = mutableListOf<PrefSubMenu>()
                if (utilsInterface.getRegion() == Region.US) {
                    subtitleList.add(PrefSubMenu.GENERAL)
                    subtitleList.add(PrefSubMenu.FIRST_LANGUAGE)
                    subtitleList.add(PrefSubMenu.SECOND_LANGUAGE)
                }
                if (utilsInterface.getRegion() == Region.EU) {
                    subtitleList.add(PrefSubMenu.ANALOG_SUBTITLE)
                    subtitleList.add(PrefSubMenu.DIGITAL_SUBTITLE)
                    subtitleList.add(PrefSubMenu.FIRST_LANGUAGE)
                    subtitleList.add(PrefSubMenu.SECOND_LANGUAGE)
                }

                if (utilsInterface.getRegion() == Region.PA) {
                    subtitleList.add(PrefSubMenu.ANALOG_SUBTITLE)
                    subtitleList.add(PrefSubMenu.DIGITAL_SUBTITLE)
                    subtitleList.add(PrefSubMenu.FIRST_LANGUAGE)
                    subtitleList.add(PrefSubMenu.SECOND_LANGUAGE)
                }

                return subtitleList
            }

            PrefMenu.TELETEXT ->return mutableListOf(
                PrefSubMenu.DIGITAL_LANGUAGE,
                PrefSubMenu.DECODING_PAGE_LANGUAGE
            )

            PrefMenu.HBBTV->return mutableListOf(
                PrefSubMenu.HBBTV_SUPPORT,
                PrefSubMenu.TRACK,
                PrefSubMenu.COOKIE_SETTING,
                PrefSubMenu.PERSISTENT_STORAGE,
                PrefSubMenu.BLOCK_TRACKING_SITES,
                PrefSubMenu.DEVICE_ID,
                PrefSubMenu.RESET_DEVICE_ID,
            )

            PrefMenu.CAMINFO-> {
                var camPinEnabled = false
                var camScanEnabled = false

                if(CiPlusInterfaceImpl.getInstance() != null) {
                    camPinEnabled = CiPlusInterfaceImpl.getInstance()?.isCamPinEnabled() == true
                    camScanEnabled = CiPlusInterfaceImpl.getInstance()?.isCamScanEnabled() == true
                }



                var ciPrefList = mutableListOf(
                    PrefSubMenu.CAM_MENU,
                    PrefSubMenu.USER_PREFERENCE,
                    PrefSubMenu.CAM_TYPE_PREFERENCE)

                if(camPinEnabled) {
                    ciPrefList.add(PrefSubMenu.CAM_PIN)
                }

                if(camScanEnabled) {
                    ciPrefList.add(PrefSubMenu.CAM_SCAN)
                }

                return ciPrefList
                
            }

            PrefMenu.PVR_TIMESHIFT-> return mutableListOf(
                PrefSubMenu.DEVICE_INFO,
                PrefSubMenu.TIMESHIFT_MODE
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