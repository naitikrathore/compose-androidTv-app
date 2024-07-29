package com.iwedia.cltv.platform.rtk

import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.PreferenceInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.GeneralConfigInterface
import com.iwedia.cltv.platform.`interface`.PreferenceInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.PrefMenu
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.rtk.eas.EasControl
import com.iwedia.cltv.platform.rtk.provider.ChannelDataProvider
import com.realtek.system.RtkProjectConfigs

class PreferenceInterfaceImpl(val utilsInterface: UtilsInterface, val channelDataProvider: ChannelDataProvider, generalConfigInterface: GeneralConfigInterface): PreferenceInterfaceBaseImpl(utilsInterface, generalConfigInterface) {
    @RequiresApi(Build.VERSION_CODES.P)
    private var easControl: EasControl? = null

    override fun getPreferenceTypes(): List<PrefType> {
        val typeList = mutableListOf<PrefType>()

        if (PreferenceInterface.ENABLE_HYBRID) {
            typeList.add(PrefType.BASE)
        }
        typeList.add(PrefType.PLATFORM)
        return typeList
    }

    @RequiresApi(Build.VERSION_CODES.P)
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
                    PrefMenu.HBBTV -> {
                        var isHbbtvEnableConfig: Boolean = RtkProjectConfigs.getInstance().isSupportHbbtv
                        var tvSupportHbbtv = (utilsInterface as UtilsInterfaceImpl).getTvSetting().isHbbtvSupported

                        if (!isHbbtvEnableConfig || !tvSupportHbbtv) {
                            menuList.remove(option)
                        }
                    }
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
                            setupList.add(PrefSubMenu.ASPECT_RATIO)
                            setupList.add(PrefSubMenu.SOUND)
                        }
                        setupList.add(PrefSubMenu.NO_SIGNAL_AUTO_POWER_OFF)
                    }
                    Region.EU->{
                        setupList.add(PrefSubMenu.CHANNEL_SCAN)
                        setupList.add(PrefSubMenu.CHANNEL_EDIT_MENUS)
                        setupList.add(PrefSubMenu.NO_SIGNAL_AUTO_POWER_OFF)
                        if(!utilsInterface.kidsModeEnabled()){
                            setupList.add(PrefSubMenu.PICTURE)
                            setupList.add(PrefSubMenu.ASPECT_RATIO)
                            setupList.add(PrefSubMenu.SOUND)
                        }
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
                if(utilsInterface.getRegion() == Region.EU){
                    parentalControlList.remove(PrefSubMenu.RRT5_LOCK)
                }
                return parentalControlList
            }

            PrefMenu.AUDIO-> {
                val audioList = mutableListOf<PrefSubMenu>()
                if (utilsInterface.getRegion() == Region.EU) audioList.add(PrefSubMenu.AUDIO_TYPE)
                audioList.add(PrefSubMenu.FIRST_LANGUAGE)
                if (utilsInterface.getRegion() == Region.EU) audioList.add(PrefSubMenu.SECOND_LANGUAGE)
                if (utilsInterface.getRegion() == Region.US) audioList.add(PrefSubMenu.AUDIO_DESCRIPTION)
                audioList.add(PrefSubMenu.VISUALLY_IMPAIRED_MINIMAL)

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
                return subtitleList
            }

            PrefMenu.TELETEXT ->return mutableListOf(
                PrefSubMenu.DIGITAL_LANGUAGE,
                PrefSubMenu.DECODING_PAGE_LANGUAGE
            )

            PrefMenu.HBBTV->return mutableListOf(
                PrefSubMenu.HBBTV_SUPPORT,
                PrefSubMenu.TRACK
            )

            PrefMenu.PVR_TIMESHIFT-> return mutableListOf(
                PrefSubMenu.DEVICE_INFO,
                PrefSubMenu.TIMESHIFT_MODE
            )

            PrefMenu.CAMINFO->return mutableListOf(
                PrefSubMenu.CAM_MENU,
                PrefSubMenu.USER_PREFERENCE,
                PrefSubMenu.CAM_TYPE_PREFERENCE,
                PrefSubMenu.CAM_PIN,
                PrefSubMenu.CAM_SCAN,
            )

            else -> {

            }
        }
        return mutableListOf()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun initializeEas() {
        this.easControl = EasControl(utilsInterface, channelDataProvider)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun disposeEas() {
        easControl?.removeCallback()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getEasChannel(): String {
        return easControl?.getEasChannel()!!
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun isTuneToDetailsChannel(): Boolean {
        return (easControl?.isTuneToDetailsChannel() == true)
    }
}