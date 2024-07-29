package com.iwedia.cltv.platform.t56

import android.content.Context
import android.media.AudioManager
import com.android.tv.settings.partnercustomizer.tvsettingservice.TVSettingConfig
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.mediatek.twoworlds.tv.MtkTvATSCCloseCaption
import com.mediatek.twoworlds.tv.MtkTvAnalogCloseCaption
import com.mediatek.twoworlds.tv.MtkTvBanner
import com.mediatek.twoworlds.tv.MtkTvChannelListBase
import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.common.MtkTvConfigType

class ClosedCaptionInterfaceImpl(context: Context, val utilsModule: UtilsInterface, playerInterface: PlayerInterface) :
    com.iwedia.cltv.platform.base.ClosedCaptionInterfaceBaseImpl(context, utilsModule, playerInterface) {

    //DONT TOUCH THIS CODE IF YOU DONT RELLY HAVE TO - THE ENTIRE ClosedCaptionInterfaceImpl NEEDS REWORK ALONGSIDE MTK MW REWORK

    private var tvSettingConfig: TVSettingConfig? = TVSettingConfig.getInstance(context)
    private var closedCaptionCallback: ClosedCaptionCallback? = null

    //these two ints track position of clicked cc and current cc
    private var ccPosition: Int = 0
    private var globalCCset = 1

    //some times the mw returns null as cc string so we show temp string while the mw starts back up
    private var tempString = ""

    //since our code is bad we call getCC multiple times, and since mw code dose not work we need to recalibrate cc and until that is done we need to stop getCC code from being called multiple times in a row
    private var logicStarted = false

    //on composite, hdmi inputs mw returns tv channel signal type and not signal type of other inputs we need a global variable to check if we are tuned to external input
    private var wasNotOnTvInput = false

    init {
        //get mtk mw db cc value and set it as current position
        val retVal = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_ANALOG_CAPTION) ?:  0
        globalCCset = getDefaultCCValues("caption_services") ?: 0
        tempString = MtkTvBanner.getInstance().caption ?: ""
        ccPosition = retVal

        val enabled = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_CAPTION_DISPLAY)
        if (enabled != null) {
            if(enabled == 2 || enabled == 1) {
                saveUserSelectedCCOptions("caption_services", globalCCset, false)
            }else if(enabled == 0){
                saveUserSelectedCCOptions("caption_services", 0, false)
            }
        }else{
            saveUserSelectedCCOptions("caption_services", 0, false)
        }
        setCCInfo()
    }

    //get mtk mw db cc values - works fine
    override fun getDefaultCCValues(ccOptions: String): Int? {
        when (ccOptions) {
            "display_cc" ->
            {
                return tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_CAPTION_DISPLAY)
            }
            "caption_services" ->
            {
                var value = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_ANALOG_CAPTION)
                if (value != null && value != 0) {
                    value -= 1
                }
                return value
            }
            "advanced_selection" ->
            {
                var value = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_DIGITAL_CAPTION)
                if (value != null && value != 0) {
                    value -= 1
                }
                return value
            }
            "text_size" ->
            {
                var value = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_FONT_SIZE)
                if (value != null) {
                    value = if (value == 255) 0 else value + 1
                }
                return value
            }
            "font_family" ->
            {
                var value = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_FONT_STYLE)
                if (value != null) {
                    value = if (value == 255) 0 else value + 1
                }
                return value
            }
            "text_color" ->
            {
                var value = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_FONT_COLOR)
                if (value != null) {
                    value = if (value == 255) 0 else value + 1
                }
                return value
            }
            "text_opacity" ->
            {
                var value = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_FONT_OPACITY)
                if (value != null) {
                    value = if (value == 255) 0 else value + 1
                }
                return value
            }
            "edge_type" ->
            {
                var value = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_EDGE_TYPE)
                if (value != null) {
                    value = if (value == 255) 0 else value + 1
                }
                return value
            }
            "edge_color" ->
            {
                var value = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_EDGE_COLOR)
                if (value != null) {
                    value = if (value == 255) 0 else value + 1
                }
                return value
            }
            "background_color" ->
            {
                var value = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_BACKGROUND_COLOR)
                if (value != null) {
                    value = if (value == 255) 0 else value + 1
                }
                return value
            }
            "background_opacity" ->
            {
                var value = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_BACKGROUND_OPACITY)
                if (value != null) {
                    value = if (value == 255) 0 else value + 1
                }
                return value
            }
        }
        return 0
    }

    //get mtk mw db cc value of mute - works fine
    override fun getDefaultMuteValues(): Boolean {
        val setupCaptionValue = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_CAPTION_DISPLAY)
        if(setupCaptionValue == 2){
            return true
        }
        return false
    }

    //set mtk mw db cc value of mute - settings caption service mtw mw doesnt set anything in mw just in db so we have to set it manually
    override fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int, isOtherInput: Boolean) {
        when (ccOptions) {
            "display_cc" -> {
                val oldValue = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_CAPTION_DISPLAY)
                tvSettingConfig?.setConifg(
                    TVSettingConfig.SETUP_CAPTION_DISPLAY,
                    newValue
                )
                if((oldValue == 0) && (newValue == 1)) {
                    val currentCC = getDefaultCCValues("caption_services")
                    if(currentCC != null) {
                        saveUserSelectedCCOptions("caption_services",currentCC,isOtherInput)
                        setCCInfo()
                    }
                }
            }
            "caption_services" -> {
                ccPosition = newValue + 1
                var endCCSet = ccPosition

                //this is done like this since mtk mw is strange
                //in other words if we are not tuned to a channel normal analog/atsc channel the mw does not work
                //in that case just set value to mw since it doesnt matter whats shown on current channel
                if(MtkTvChannelListBase.getCurrentChannel() != null) {
                    tvSettingConfig?.setConifg(
                        TVSettingConfig.SETUP_ANALOG_CAPTION,
                        ccPosition
                    )
                    try {
                        //since we can only rotate the next cc that is shown and we have position - we need to reset the circuital buffer so we can start calling set next cc until we get to desired cc

                        //this code bellow resets the cc to start
                        MtkTvATSCCloseCaption.getInstance().atscCCEnable(false)
                        MtkTvAnalogCloseCaption.getInstance().analogCCEnable(false)
                        var isDisplaying = getDefaultCCValues("display_cc")
                        if ((isDisplaying != null) && (isDisplaying == 1)) {
                            MtkTvATSCCloseCaption.getInstance().atscCCEnable(true)
                            MtkTvAnalogCloseCaption.getInstance().analogCCEnable(true)
                        }
                        MtkTvATSCCloseCaption.getInstance().atscCCDemoSet(0, 0)
                        //mtk mw is strange and it will reset the cc to custom text1,text2.. and not 0 or cc1 we need to after resenting cc manually set it to position 0
                    }catch (E: Exception){
                        //this try catch is here just in case - mtk mw is garbage so placing try catch is always a good idea since mtk mw can just stop working when it fells like it
                        println(E)
                    }
                    //next problem with mtk mw is that the mw calls to change cc is different for analog channel, atsc channel, radio channel(this part we dont have and we have not tested in any interface)
                    //also we can only check if the channel is analog we cannot check if the channel is atsc
                    //also we have the problem with other inputs where the current channel is the channel thats on tv input and not on current input - so there is that
                    if (!MtkTvChannelListBase.getCurrentChannel().isAnalogService && !isOtherInput) {
                        //again we have to reset the circuital buffer to 0 since we only know the position of the clicked cc in our preference
                        //this reset is done since the one above is only partial for atsc channel
                        MtkTvATSCCloseCaption.getInstance().atscCCEnable(false)
                        tvSettingConfig?.setConifg(
                            TVSettingConfig.SETUP_ANALOG_CAPTION,
                            0
                        )
                        MtkTvATSCCloseCaption.getInstance().atscCCEnable(true)
                        MtkTvATSCCloseCaption.getInstance().atscCCEnable(true)
                        MtkTvATSCCloseCaption.getInstance().atscCCDemoSet(0, 0)

                        //now that the cc is reset to some custom text cc we need to reset it to 0 position
                        var stepCounter = 0
                        var safetyCounter = 0
                        var start = false
                        while (true){
                            //go to 0 position
                            if ( checkIfNull() && !start){
                                tvSettingConfig?.setConifg(
                                    TVSettingConfig.SETUP_ANALOG_CAPTION,
                                    0
                                )
                                start = true
                            }
                            else if(start){
                                //ok now go to position that was set in preference
                                stepCounter++
                                if(checkIfNull()){
                                    //in case the current channel does not have that cc go to cc1 - this is in case we set have 6 cc options for atsc channel and user selects cc 7 in preference
                                    //the better solution would be to check for current channel how many cc it has and only mark them in preference but since we dont have time to refactor entire cc
                                    //before release this is the second way to do it
                                    MtkTvATSCCloseCaption.getInstance().atscCCNextStream()
                                    endCCSet = 1
                                    break
                                }else if(stepCounter >= ccPosition){
                                    //if we found the desired cc set it as current
                                    endCCSet = newValue + 1
                                    break
                                }
                            }
                            MtkTvATSCCloseCaption.getInstance().atscCCNextStream()

                            //this counter is here in case mtk mw is dumb and stops working
                            safetyCounter++
                            if(safetyCounter > 30){
                                break
                            }
                        }
                    }

                }

                //set current cc in mtk mw db and in global variable also set temp cc
                tvSettingConfig?.setConifg(
                    TVSettingConfig.SETUP_ANALOG_CAPTION,
                    endCCSet
                )
                globalCCset = endCCSet
                tempString = MtkTvBanner.getInstance().caption ?: ""
            }
            "advanced_selection" -> tvSettingConfig?.setConifg(
                TVSettingConfig.SETUP_DIGITAL_CAPTION,
                newValue + 1
            )
            "text_size" -> MtkTvATSCCloseCaption.getInstance().atscCCDemoSet(1, newValue)
            "font_family" -> MtkTvATSCCloseCaption.getInstance().atscCCDemoSet(2, newValue)
            "text_color" -> MtkTvATSCCloseCaption.getInstance().atscCCDemoSet(3, newValue)
            "text_opacity" -> MtkTvATSCCloseCaption.getInstance().atscCCDemoSet(4, newValue)
            "edge_type" -> MtkTvATSCCloseCaption.getInstance().atscCCDemoSet(9, newValue)
            "edge_color" -> MtkTvATSCCloseCaption.getInstance().atscCCDemoSet(10, newValue)
            "background_color" -> MtkTvATSCCloseCaption.getInstance().atscCCDemoSet(5, newValue)
            "background_opacity" -> MtkTvATSCCloseCaption.getInstance().atscCCDemoSet(6, newValue)
        }
    }

    //set mtk mw db cc value of mute - works fine
    override fun setCCWithMuteInfo() {
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_CC_DCS, 2)
        MtkTvATSCCloseCaption.getInstance().atscCCSetCcVisible(true)
    }

    //reset mtk mw cc value - this does not work as intended but since we are resting cc and we are going to current this does not matter
    override fun resetCC() {
        MtkTvATSCCloseCaption.getInstance().atscCCEnable(true)
        MtkTvAnalogCloseCaption.getInstance().analogCCEnable(true)
    }

    //this function is hot garbage - its used to show cc and we are calling it always even if mtw mw for cc is not set
    override fun setCCInfo() {
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_CC_DCS, 1)

        //since again analog calls are working differently than atsc we need to separate them
        if(MtkTvChannelListBase.getCurrentChannel() != null) {
            if (MtkTvChannelListBase.getCurrentChannel().isAnalogService) {
                //if its analog just set it to visible
                MtkTvAnalogCloseCaption.getInstance().analogCCSetCcVisible(true)
            } else {
                //if its atsc - did we switch with other input, if we did reset cc since getCurrentChannel does not work correctly with hybrid signals so keeping track of position of cc is a mess in that case
                if(wasNotOnTvInput){
                    globalCCset = 0
                }
                //if global position is 0/off/null just reset the cc until we get that MtkTvBanner.getInstance().caption is null
                //since MtkTvBanner.getInstance().caption - tells us what cc is set and not tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_ANALOG_CAPTION)
                if(globalCCset == 0){
                    var checkIfNull = checkIfNull()
                    if(globalCCset == 0 && !checkIfNull){
                        do {
                            MtkTvATSCCloseCaption.getInstance().atscCCNextStream()
                            checkIfNull = !checkIfNull()
                        }while(checkIfNull)
                        tvSettingConfig?.setConifg(
                            TVSettingConfig.SETUP_ANALOG_CAPTION,
                            0
                        )
                        ccPosition = globalCCset
                    }
                }
                else{
                    //some times the entire composite and analog swapping messes with the mw check do this check for first 4 cc values since they always have cc(value) in them
                    //in case the global is not the same as current just reset to global
                    if(globalCCset <= 4){
                        var checkIfNull = checkIfNull()
                        var safetyCounter = 0
                        if(checkIfNull){
                            while (true){
                                MtkTvATSCCloseCaption.getInstance().atscCCNextStream()
                                checkIfNull = checkIfNull()
                                if(!checkIfNull){
                                    if(MtkTvBanner.getInstance()?.caption?.lowercase()?.contains("cc$globalCCset") == true){
                                        break
                                    }
                                }

                                safetyCounter++
                                if(safetyCounter > 30 && checkIfNull){
                                    break
                                }
                            }
                            tvSettingConfig?.setConifg(
                                TVSettingConfig.SETUP_ANALOG_CAPTION,
                                globalCCset
                            )
                            ccPosition = globalCCset
                        }
                    }
                    else{
                        //if its correct for first 4 its working fine - mtk mystery set global to current
                        ccPosition = tvSettingConfig?.getConfigValueInt(
                                TVSettingConfig.SETUP_ANALOG_CAPTION
                            ) ?: 0
                        globalCCset = ccPosition
                    }
                }
                MtkTvATSCCloseCaption.getInstance().atscCCSetCcVisible(true)
            }
        } else {
            MtkTvATSCCloseCaption.getInstance().atscCCSetCcVisible(true)
        }
    }

    private fun checkIfNull(): Boolean{
        var checkIfNull = true
        try {
            checkIfNull = (MtkTvBanner.getInstance().caption == null || MtkTvBanner.getInstance().caption.equals("null"))
        }catch (E: Exception){
            println(E)
        }
        return checkIfNull
    }

    //disable cc via mtk mw call - works fine
    override fun disableCCInfo() {
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_CC_DCS, 0)
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_CC_ANALOG_CC, 0)
        MtkTvATSCCloseCaption.getInstance().atscCCSetCcVisible(false)
    }

    //mute cc via mtk mw call - works fine
    override fun setCCWithMute(isEnable: Boolean, audioManager: AudioManager) {
        if (isEnable) {
            MtkTvConfig.getInstance().setConfigValue(TVSettingConfig.SETUP_CAPTION_DISPLAY, 2)
            if (audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) setCCInfo()
            else disableCCInfo()
        } else {
            MtkTvConfig.getInstance().setConfigValue(TVSettingConfig.SETUP_CAPTION_DISPLAY, 1)
            if (audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) disableCCInfo()
            else setCCInfo()
        }
    }

    //check if cc  is enabled via mtk mw call - works fine
    override fun isClosedCaptionEnabled(): Boolean {
        return MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_CC_CC_ENABLED) != 0
    }

    //setClosedCaption function is a mess - we call this function when we click on cc on remote and when we click the cc button in info banner
    override fun setClosedCaption(isOtherInput: Boolean): Int {
        //this function is a headache since mtk mw does not work - it just does not work
        //since it does not work this was hacked like this to work
        if(MtkTvChannelListBase.getCurrentChannel() != null) {
            //the main idea is to do it like in saveUserSelectedCCOptions so most of the same things from there work in the same way
            //the only difference is now we dont have position but we can only go +1 from last one and that is a problem since MtkTvBanner.getInstance().caption can reset by it self at random
            //so we need to check with the globalCCset and ccPosition if that happend
            if (MtkTvChannelListBase.getCurrentChannel().isAnalogService || isOtherInput) {
                if(ccPosition == 1 && globalCCset != 1){
                    ccPosition = globalCCset
                }
                ccPosition++
                if (ccPosition >= 9) {
                    ccPosition = 0
                }
                globalCCset = ccPosition
                tvSettingConfig?.setConifg(
                    TVSettingConfig.SETUP_ANALOG_CAPTION,
                    ccPosition
                )

                MtkTvATSCCloseCaption.getInstance().atscCCEnable(false)
                MtkTvAnalogCloseCaption.getInstance().analogCCEnable(false)
                var isDisplaying = getDefaultCCValues("display_cc")
                if ((isDisplaying != null) && (isDisplaying == 1)) {
                    MtkTvATSCCloseCaption.getInstance().atscCCEnable(true)
                    MtkTvAnalogCloseCaption.getInstance().analogCCEnable(true)
                }
                MtkTvATSCCloseCaption.getInstance().atscCCDemoSet(0, 0)

                var retVal =
                    tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_ANALOG_CAPTION)
                if (retVal == null) {
                    retVal = 0
                }
                return retVal
            }
            else {
                val retVal = MtkTvATSCCloseCaption.getInstance().atscCCNextStream()
                //since we can get null(or off/0) as correct value in this case we can also end the process of setting cc with this
                if(checkIfNull()){
                    globalCCset = 0
                }else{
                    globalCCset++
                }
                tempString = MtkTvBanner.getInstance().caption ?: ""
                return retVal
            }
        } else {
            val retVal = MtkTvATSCCloseCaption.getInstance().atscCCNextStream()
            if(checkIfNull()){
                globalCCset = 0
            }else{
                globalCCset++
            }
            tempString = MtkTvBanner.getInstance().caption ?: ""
            return retVal
        }
    }

    //getClosedCaption function is a mess - we call this function when we need to get what value is of cc on current channel | one problem mtk can reset value of cc at any time
    override fun getClosedCaption(isOtherInput: Boolean): String? {
        //since mtk can reset cc value when we tune to channel or at random, check if the global value is the same as return value if not set the cc again
        var retVal = tvSettingConfig?.getConfigValueInt(TVSettingConfig.SETUP_ANALOG_CAPTION) ?: 1
        var startSinceMWIssueNext = retVal != globalCCset
        //this is a reset like in setCCInfo (from analog composite to atsc channel) - this can maybe be deleted but dont know still needs testing
        if(wasNotOnTvInput){
            ccPosition = 0
            globalCCset = 0

            var resetCC = checkIfNull()
            while (!resetCC) {
                MtkTvATSCCloseCaption.getInstance().atscCCNextStream()
            }
            tvSettingConfig?.setConifg(
                TVSettingConfig.SETUP_ANALOG_CAPTION,
                0
            )
            wasNotOnTvInput = false
            return ""
        }
        else if(MtkTvChannelListBase.getCurrentChannel() != null && !isOtherInput) {
            //since we call getClosedCaption multiple times in a row(since our code is bad) logicStarted prevents that by giving temp cc until we set cc to correct one
            if(!logicStarted) {
                logicStarted = true
                if (!MtkTvChannelListBase.getCurrentChannel().isAnalogService) {
                    //since global can be set to off just make sure that MtkTvBanner.getInstance().caption is also set to null or off/0
                    if(globalCCset == 0){
                        var checkIfNull = checkIfNull()
                        if(globalCCset == 0){
                            do {
                                MtkTvATSCCloseCaption.getInstance().atscCCNextStream()
                                checkIfNull = !checkIfNull()
                            }while(checkIfNull)
                            tvSettingConfig?.setConifg(
                                TVSettingConfig.SETUP_ANALOG_CAPTION,
                                0
                            )
                        }
                    }
                    else if ((checkIfNull() && globalCCset != 0) || startSinceMWIssueNext) {
                        //the standard reset done again since mtk mw
                        MtkTvATSCCloseCaption.getInstance().atscCCEnable(false)
                        tvSettingConfig?.setConifg(
                            TVSettingConfig.SETUP_ANALOG_CAPTION,
                            0
                        )
                        MtkTvATSCCloseCaption.getInstance().atscCCEnable(true)
                        MtkTvATSCCloseCaption.getInstance().atscCCEnable(true)
                        MtkTvATSCCloseCaption.getInstance().atscCCDemoSet(0, 0)

                        var stepCounter = 0
                        var safetyCounter = 0
                        var endCCSet = 1
                        var start = false
                        var wasNull = false
                        while (globalCCset != 0) {
                            if ((checkIfNull()) && !start) {
                                tvSettingConfig?.setConifg(
                                    TVSettingConfig.SETUP_ANALOG_CAPTION,
                                    0
                                )
                                start = true
                            } else if (start) {
                                stepCounter++
                                if(((checkIfNull()) && !wasNull) && stepCounter != 1){
                                    MtkTvATSCCloseCaption.getInstance().atscCCNextStream()
                                    endCCSet = 1
                                    break
                                }else if (stepCounter >= globalCCset) {
                                    endCCSet = globalCCset
                                    break
                                }
                            }
                            wasNull = checkIfNull()
                            MtkTvATSCCloseCaption.getInstance().atscCCNextStream()
                            safetyCounter++
                            if (safetyCounter > 30) {
                                break
                            }
                        }
                        if(globalCCset != 0) {
                            tvSettingConfig?.setConifg(
                                TVSettingConfig.SETUP_ANALOG_CAPTION,
                                endCCSet
                            )
                            globalCCset = endCCSet
                        }
                    }
                }
                logicStarted = false
                return MtkTvBanner.getInstance().caption
            }else{
                return tempString
            }
        }
        else{
            if(isOtherInput){
                if (checkIfNull()){
                    tempString = ""
                }
            }
            return MtkTvBanner.getInstance().caption ?: tempString
        }
    }

    override fun getSubtitlesState(): Boolean {
        return MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ENABLED_EX) != 0
    }

    override fun isCCTrackAvailable(): Boolean {
        return MtkTvBanner.getInstance().isDisplayCaptionIcon
    }

    override fun initializeClosedCaption() {
        val closedCaptionCallback = ClosedCaptionCallback(this)
        this.closedCaptionCallback = closedCaptionCallback
    }

    override fun disposeClosedCaption() {
        closedCaptionCallback?.removeCallback()
    }
}