package com.iwedia.cltv.manager

import android.content.Context
import android.util.Log
import androidx.core.text.isDigitsOnly
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.entities.FilterItem
import com.iwedia.cltv.platform.`interface`.CategoryInterface
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TimeshiftInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.zap_digit.DigitZapItem
import com.iwedia.cltv.scene.zap_digit.ZapDigitScene
import com.iwedia.cltv.scene.zap_digit.ZapDigitSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import world.SceneData
import kotlin.Boolean
import kotlin.Error
import kotlin.Int
import kotlin.collections.ArrayList

/**
 * Zap digit manager
 *
 * @author Veljko Ilkic
 */
class ZapDigitManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    val tvModule: TvInterface,
    val timeshiftModule: TimeshiftInterface,
    val categoryModule: CategoryInterface,
    private val utilsModule: UtilsInterface,
    private val textToSpeechModule: TTSInterface
) : GAndroidSceneManager(
    context,
    worldHandler, ReferenceWorldHandler.SceneId.DIGIT_ZAP
),
    ZapDigitSceneListener {

    /**
     * Digit array
     */
    var digitArray = mutableListOf<kotlin.String>()

    var addedDash = false

    var channelList = mutableListOf<TvChannel>()

    var isNotDigitsOnly = false
    var filter : FilterItemType? = null
    var filterMetadata : String? = null

    class ZapDigitMaxNumber {
        var value = -1

        constructor(maxNumber: Int) {
            this.value = maxNumber
        }
    }

    var maxDigitNumber: ZapDigitMaxNumber? = null
    private val SPECIAL_DIRECT_TUNE = -2
    init {

        //Detect max number of digits
        var channels = tvModule.getChannelList()
        var maxDisplayNumber = -1
        channels.forEach { item ->
            if (item.displayNumber.isDigitsOnly() && item.displayNumber.toInt() > maxDisplayNumber) {
                maxDisplayNumber = item.displayNumber.toInt()
            }
            if(!item.displayNumber.isDigitsOnly()){
                isNotDigitsOnly = true
                val displayNumber = item.getDisplayNumberDigits().toInt()
                if(displayNumber > maxDisplayNumber){
                    maxDisplayNumber = displayNumber
                }

            }
        }

        if(isNotDigitsOnly){
            maxDigitNumber = if (maxDisplayNumber != -1) ZapDigitMaxNumber((maxDisplayNumber).toString().length+1)
            else ZapDigitMaxNumber((channels.size).toString().length)
        }
        else {
            maxDigitNumber = if (maxDisplayNumber != -1) ZapDigitMaxNumber((maxDisplayNumber).toString().length)
            else ZapDigitMaxNumber((channels.size).toString().length)
        }



    }

    override fun createScene() {
        scene = ZapDigitScene(context!!, this)
    }

    override fun onDigitPressed(digit: Int) {
        addDigit(digit.toString())
        refreshScene()
    }

    override fun onPeriodPressed() {
        if (digitArray.isNotEmpty()) {
            if (BuildConfig.FLAVOR == "rtk") {
                addDigit(".")
            } else {
                addDigit("-")
            }
            addedDash = true
        }
        refreshScene()
    }

    override fun onBackspacePressed() {
        if (digitArray.isNotEmpty()) {
            digitArray.removeAt(digitArray.lastIndex)
        }
        refreshScene()
    }

    var channelsInConflict = mutableListOf<TvChannel>()

    override fun zapOnDigit(itemId: Int) {
        worldHandler!!.triggerAction(id, Action.DESTROY)
        tvModule.getSelectedChannelList(
            object : IAsyncDataCallback<ArrayList<TvChannel>> {
                override fun onFailed(error: Error) {

                }
                override fun onReceive(data: ArrayList<TvChannel>) {
                    if(itemId == SPECIAL_DIRECT_TUNE){
                            val itemdata = channelList.filter { it.id == SPECIAL_DIRECT_TUNE }.firstOrNull()
                        itemdata?.let {
                            tvModule.changeChannel(it, object : IAsyncCallback {
                                override fun onFailed(error: Error) {}
                                override fun onSuccess() {}
                            })
                        }
                    }else {
                        data.forEach { item ->
                            if (item.id == itemId) {
                                if (timeshiftModule.isTimeShiftActive) {
                                    showTimeShiftExitDialog(item)
                                } else {
                                    //if guide is active on home scene then no need to do actual zap
                                    if (ReferenceApplication.worldHandler!!.isVisible(
                                            ReferenceWorldHandler.SceneId.HOME_SCENE
                                        )
                                    ) {
                                        InformationBus.informationBusEventListener.submitEvent(
                                            Events.ZAP_ON_GUIDE_ONLY,
                                            arrayListOf(item)
                                        )
                                    } else {
                                        tvModule.changeChannel(item, object : IAsyncCallback {
                                            override fun onFailed(error: Error) {}
                                            override fun onSuccess() {}
                                        })
                                    }
                                }
                            }
                        }
                    }

//                    else {
//                        checkInOtherFilter(displayNumber.toInt())
//                    }
                    ReferenceApplication.runOnUiThread {
                        worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.DETAILS_SCENE,
                            Action.DESTROY
                        )
                        worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.INFO_BANNER,
                            Action.DESTROY
                        )
                        worldHandler!!.triggerAction(id, Action.DESTROY)
                    }
                    ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    InformationBus.informationBusEventListener.submitEvent(Events.CHANNEL_CHANGED, arrayListOf(data))
                }
            },filter = filter, filterMetadata = filterMetadata)
    }

    override fun onTimerEnd(){
        if (worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.RCU_SCENE)) {
            worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.RCU_SCENE, Action.DESTROY)
        }
        if(channelList.isEmpty()){
            ReferenceApplication.runOnUiThread {
                showToast(
                    "No channel associated with that number found in the current channel list"
                )
            }
        }
        ReferenceApplication.runOnUiThread {
            worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.DIGIT_ZAP, Action.DESTROY)

        }
    }

    override fun onTimerEndZap(itemId: Int){
        zapOnDigit(itemId)
    }

    override fun getChannelSourceType(tvChannel: TvChannel) : String {
        return tvModule.getChannelSourceType(tvChannel)
    }

    private fun checkInOtherFilter(digit: Int) {
        categoryModule.getActiveCategoryChannelList(FilterItem.ALL_ID,
            object : IAsyncDataCallback<ArrayList<TvChannel>> {
                override fun onFailed(error: Error) {

                }

                override fun onReceive(data: ArrayList<TvChannel>) {
                    val channels = mutableListOf<TvChannel>()
                    data.forEach { item ->
                        if (item.displayNumber == digit.toString()) {
                            channels.add(item)
                        }
                    }

                    channels.forEach { item ->
                        if (item.displayNumber == digit.toString()) {
                            channelsInConflict.add(item)
                        }
                    }
                    if (channels.size > 1) {
                        ReferenceApplication.runOnUiThread {
                            worldHandler!!.triggerAction(id, Action.DESTROY)
                        }
                        val sceneData = SceneData(id, instanceId, channels)
                        worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.DIGIT_ZAP_CONFLICT,
                            Action.SHOW_OVERLAY, sceneData
                        )
                    } else if (channels.size == 1) {
                        if (timeshiftModule.isTimeShiftActive) {
                            showTimeShiftExitDialog(channels[0])
                        } else {
                            tvModule.changeChannel(channels[0], object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                }

                                override fun onSuccess() {
                                    categoryModule.setActiveCategory(FilterItem.ALL_ID.toString())
                                }
                            })
                        }
                    } else {
                        ReferenceApplication.runOnUiThread {
                            showToast(ConfigStringsManager.getStringById("no_channel_associated"))
                        }
                    }
                }
            })

    }

    fun requestFocus(){
        if(channelList.isNotEmpty()){
            (scene as ZapDigitScene).requestFocus()
        }
    }

    override fun onBackPressed(): Boolean {
        //check if rcu scene is visible
        if(ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.RCU_SCENE)){
            //check which scene has focus and should handle back key
            if ((scene as ZapDigitScene).hasFocus() || channelList.isEmpty()) {
                //if zap digit scene has focus then both scenes should be destroyed
                worldHandler!!.triggerAction(id, Action.DESTROY)
                worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.RCU_SCENE, Action.DESTROY)
                return true
            }
            //if rcu scene has focus then it should handle back key
            else return false
        }
        //if rcu scene isn't visible then just destroy zap digit scene
        else {
            worldHandler!!.triggerAction(id, Action.DESTROY)
            return true
        }
    }

    override fun onSceneInitialized() {
        scene!!.refresh(maxDigitNumber)

//        initDigitArray()

        // Check zap digit timer timeout
        if (data!!.getDataByIndex(1) != null && data!!.getDataByIndex(1)  is Long ) {
            scene?.refresh(data!!.getDataByIndex(1))
        }

        var digitItem = data!!.getData() as DigitZapItem
        addDigit(digitItem.digit.toString())
        /**
         * this is useful when we zap from epg using number keys.
         * when we are trying to zap from the epg we are externally passing our filter and some extra info about the filter
         * [filter] denotes the active filter
         * [filterMetadata] denotes the extra info like if it is fav filter then which favorite category like favorite 1 or favorite 2
         * similarly we are passing extra data for the tif category and genres if they are active in epg
         *
         * mostly or should be only in case of epg
         */
        if (data!!.getDataByIndex(1)!=null && data!!.getDataByIndex(1) is FilterItemType) {
            filter = data!!.getDataByIndex(1) as FilterItemType
            if (data!!.getDataByIndex(2) != null && data!!.getDataByIndex(2) is String) {
                filterMetadata = data!!.getDataByIndex(2) as String
            }
        }
        refreshScene()
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    private fun refreshScene() {

        var channelIndex = getChannelIndex()

        scene!!.refresh(channelIndex)

        channelList.clear()

        //TODO non browsable channels
        tvModule.getSelectedChannelList(
            object : IAsyncDataCallback<ArrayList<TvChannel>> {
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + "ZapDigitManager", "onFailed: Only single channel or no data found")
                    scene?.refresh(Error("no data found or only single channel in list"))
                    Log.d(Constants.LogTag.CLTV_TAG + "ZapDigitManager", "failed")
                }

                override fun onReceive(data: ArrayList<TvChannel>) {
                    val directTuneChannelObj = tvModule.addDirectTuneChannel(channelIndex, context as Context)
                    directTuneChannelObj?.let {
                        channelList.add(it)
                    }
                    filterChannels(data, channelIndex)
                    scene!!.refresh(channelList)
                }
            }, filter = filter, filterMetadata = filterMetadata
        )
    }
    private fun filterChannels(data: ArrayList<TvChannel>, channelIndexTemp:String){
        var channelIndex = channelIndexTemp
        val newChannelList = mutableListOf<TvChannel>()
        data.forEach { item ->
            var displayNumberChannel = item.displayNumber

            //remove zeros from the beginning of channel display number
            if (displayNumberChannel.startsWith("0")) {
                do {
                    displayNumberChannel = displayNumberChannel.drop(1)
                } while (displayNumberChannel.startsWith("0"))
            }

            if(utilsModule.getCountryPreferences(UtilsInterface.CountryPreference.DISABLE_ZERO_DIAL, false) == true) {
                //Checking whether channelIndex contains digit other than 0
                if (channelIndex.contains("[1-9]".toRegex())) {
                    // Remove zeros from the channelIndex beginning
                    if (channelIndex.startsWith("0")) {
                        do {
                            channelIndex = channelIndex.drop(1)
                        } while (channelIndex.startsWith("0"))
                    }
                }
            } else {
                if (channelIndex.startsWith("0")) {
                    do {
                        channelIndex = channelIndex.drop(1)
                    } while (channelIndex.startsWith("0"))
                }
            }

            if (displayNumberChannel.startsWith(channelIndex) &&
                tvModule.isChannelSelectable(item)) {
                newChannelList.add(item)
            }
        }
        newChannelList.sortBy { it.displayNumber }
        channelList.clear()
        channelList.addAll(newChannelList)
    }
    /**
     * Add digit
     *
     * @param digit Digit
     */
    private fun addDigit(digit: kotlin.String) {
        //disable repeated "-" input
        if(digitArray.contains("-") && digit == "-") {
            return
        }

        if(digitArray.size > maxDigitNumber!!.value-1){
            digitArray.clear()
        }

        digitArray.add(digit)
    }

    /**
     * Get channel number
     *
     * @return Channel number string
     */
    private fun getChannelIndex(): kotlin.String {
        var result = ""
        digitArray.forEach {
            result += it
        }
        return result
    }

    /**
     * Init digit array
     */
    private fun initDigitArray() {
        for (i in 0 until maxDigitNumber!!.value) {
            digitArray.add("0")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        initDigitArray()
    }

    private fun showTimeShiftExitDialog(tvChannel: TvChannel) {

        context!!.runOnUiThread {
            var sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("timeshift_channel_change_msg")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")

            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW, sceneData
            )

            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                    ReferenceApplication.runOnUiThread {
                        worldHandler!!.triggerAction(id, Action.DESTROY)
                    }
                }

                override fun onPositiveButtonClicked() {
                    timeshiftModule.timeShiftStop(object : IAsyncCallback{
                        override fun onSuccess() {
                        }

                        override fun onFailed(error: Error) {
                        }
                    })

                    ReferenceApplication.worldHandler!!.playbackState =
                        ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE

                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        Action.DESTROY
                    )
                    worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    timeshiftModule.setTimeShiftIndication(
                        false
                    )

                    tvModule.changeChannel(tvChannel!!, object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                            Log.i("channel play failed", "onFailed: ")
                        }

                        override fun onSuccess() {
                        }
                    })
                    ReferenceApplication.runOnUiThread {
                        worldHandler!!.triggerAction(id, Action.DESTROY)
                    }
                }
            }
        }
    }

    //restart Inactivity timer when virtual RCU keyboard is open
    fun restartTimer(){
        (scene as ZapDigitScene).restartTimer()
    }
}
