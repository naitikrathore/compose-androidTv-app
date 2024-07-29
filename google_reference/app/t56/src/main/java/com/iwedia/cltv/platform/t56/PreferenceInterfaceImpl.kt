package com.iwedia.cltv.platform.t56

import com.iwedia.cltv.platform.base.PreferenceInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.GeneralConfigInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.PrefMenu
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.parental.Region
import com.mediatek.twoworlds.tv.MtkTvChannelListBase
import com.mediatek.twoworlds.tv.MtkTvEASBase
import com.mediatek.twoworlds.tv.model.MtkTvEasEventInfoBase

class PreferenceInterfaceImpl(val utilsInterface: UtilsInterface, generalConfigInterface: GeneralConfigInterface) : PreferenceInterfaceBaseImpl(utilsInterface, generalConfigInterface) {

    var easControl: EasControl? = null
    private var mEas: MtkTvEASBase? = null
    var eventInfo: MtkTvEasEventInfoBase? = null

    override fun getPreferenceTypes(): List<PrefType> {
        return mutableListOf(
            PrefType.BASE
        )
    }

    override fun getPreferenceMenus(type: PrefType): List<PrefMenu> {
        if (type == PrefType.BASE) return super.getPreferenceMenus(type)

        val menuList = mutableListOf(
            PrefMenu.SETUP,
            PrefMenu.PARENTAL_CONTROL,
            PrefMenu.AUDIO,
            PrefMenu.CLOSED_CAPTIONS,
            PrefMenu.SUBTITLE,
            PrefMenu.CAMINFO,
            PrefMenu.HBBTV,
            PrefMenu.PVR_TIMESHIFT,
            PrefMenu.TELETEXT,
            PrefMenu.SYSTEMINFO,
            PrefMenu.OPEN_SOURCE_LICENSES
        )

        for (option in menuList.toList()){
            if (utilsInterface.getRegion() == Region.US){
                when (option){
                    PrefMenu.SUBTITLE,
                    PrefMenu.CAMINFO,
                    PrefMenu.HBBTV,
                    PrefMenu.TELETEXT,
                    PrefMenu.SYSTEMINFO -> menuList.remove(option)
                    else -> {}
                }

            }else if (utilsInterface.getRegion() ==  Region.EU){
                when (option){
                    PrefMenu.CLOSED_CAPTIONS,
                    PrefMenu.PVR_TIMESHIFT-> menuList.remove(option)
                    else -> {}
                }
            }
        }
        return menuList
    }

    override fun getPreferenceSubMenus(prefMenu: PrefMenu, type: PrefType): List<PrefSubMenu> {
        if (type == PrefType.BASE) return super.getPreferenceSubMenus(prefMenu,type)

        when(prefMenu){
            PrefMenu.SETUP-> {
                var setupList: MutableList<PrefSubMenu> = mutableListOf()
                if(!utilsInterface.kidsModeEnabled()){
                    setupList.add(PrefSubMenu.CHANNELS_SETTING)
                    setupList.add(PrefSubMenu.PICTURE)
                    setupList.add(PrefSubMenu.SCREEN)
                    setupList.add(PrefSubMenu.SOUND)
                }
                setupList.add(PrefSubMenu.CHANNEL_EDIT)
                setupList.add(PrefSubMenu.LCN)
                setupList.add(PrefSubMenu.PARENTAL_CONTROL)
                setupList.add(PrefSubMenu.PREFERRED_EPG_LANGUAGE)
                setupList.add(PrefSubMenu.BLUE_MUTE)
                setupList.add(PrefSubMenu.NO_SIGNAL_AUTO_POWER_OFF)

                for (option in setupList.toList()){
                    if (utilsInterface.getRegion() ==  Region.US){
                        when (option){
                            PrefSubMenu.CHANNEL_EDIT,
                            PrefSubMenu.LCN,
                            PrefSubMenu.PARENTAL_CONTROL,
                            PrefSubMenu.PREFERRED_EPG_LANGUAGE -> setupList.remove(option)

                            else -> {}
                        }

                    }else if (utilsInterface.getRegion() ==  Region.EU){
                        when (option){
                            PrefSubMenu.BLUE_MUTE,
                            PrefSubMenu.NO_SIGNAL_AUTO_POWER_OFF,
                                ->setupList.remove(option)
                            else
                                -> {}
                        }
                    }
                }

                return setupList
            }
            PrefMenu.PARENTAL_CONTROL -> return mutableListOf(
                    PrefSubMenu.PARENTAL_CONTROL,
                    PrefSubMenu.CHANNEL_BLOCK,
                    PrefSubMenu.INPUT_BLOCK,
                    PrefSubMenu.RATING_SYSTEMS,
                    PrefSubMenu.RATING_LOCK,
                    PrefSubMenu.BLOCK_UNRATED_PROGRAMS,
                    PrefSubMenu.RRT5_LOCK,
                    PrefSubMenu.CHANGE_PIN
                )

            PrefMenu.AUDIO-> {
                var audioList = mutableListOf(
                    PrefSubMenu.FIRST_LANGUAGE,
                    PrefSubMenu.SECOND_LANGUAGE,
                    PrefSubMenu.AUDIO_DESCRIPTION,
                    PrefSubMenu.HEARING_IMPAIRED,
                    PrefSubMenu.VISUALLY_IMPAIRED,
                )
                if(utilsInterface.getRegion() == Region.US){
                    audioList.remove(PrefSubMenu.SECOND_LANGUAGE)
                }
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

            PrefMenu.CAMINFO->return mutableListOf(
                PrefSubMenu.CAM_MENU,
                PrefSubMenu.USER_PREFERENCE,
                PrefSubMenu.CAM_TYPE_PREFERENCE,
            )

            PrefMenu.SUBTITLE->return mutableListOf(
                PrefSubMenu.GENERAL,
                PrefSubMenu.FIRST_LANGUAGE,
                PrefSubMenu.SECOND_LANGUAGE,
            )

            PrefMenu.PVR_TIMESHIFT-> return mutableListOf(
                PrefSubMenu.DEVICE_INFO,
                PrefSubMenu.TIMESHIFT_MODE
            )

            PrefMenu.SYSTEMINFO-> return mutableListOf(
            )
            else -> {

            }
        }
        return mutableListOf()
    }

    override fun initializeEas() {
        mEas = MtkTvEASBase()
        eventInfo = mEas!!.easEventDataInfo
        val easControl = EasControl(eventInfo)
        this.easControl= easControl
        mEas!!.EASSetAndroidLaunchStatus(false)
    }

    override fun disposeEas() {
        easControl?.removeCallback()
    }

    override fun getEasChannel(): String {
        return MtkTvChannelListBase.getCurrentChannel().svlRecId.toString()
    }
}