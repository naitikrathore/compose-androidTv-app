package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.*
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.PreferenceSetupInformation
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.postal_code.PostalCodeSceneData
import com.iwedia.cltv.utils.Utils
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneManager
import world.widget.GWidget
import world.widget.GWidgetListener

/**
 * Preferences setup widget
 *
 * @author Gaurav Jain
 */
class PreferencesSetupWidget :
    GWidget<ConstraintLayout, PreferencesSetupWidget.PreferencesSetupListener> {

    private val TAG = javaClass.simpleName
    var context: Context? = null
    private var preferencesLeftOptionsGridView: VerticalGridView? = null
    private var preferencesLeftOptionsAdapter: PreferenceSubMenuAdapter? = null

    //To handle scene focus for the License
    var isLicenseClicked = false

    var scrollListener: RecyclerView.OnScrollListener? = null
    var setUpListener : PreferencesSetupListener? = null

    var zerothColumn = mutableListOf<PrefItem<Any>>()
    val moveChannelItems = mutableListOf<PrefItem<Any>>()
    val swapChannelsItems = mutableListOf<PrefItem<Any>>()
    val skipChannelCheckBoxItems = mutableListOf<PrefItem<Any>>()
    private val deleteChannelCheckBoxItems = mutableListOf<PrefItem<Any>>()

    private var noSignalAutoPowerOffItemList = mutableListOf<PrefItem<Any>>()
    private lateinit var preferencePostalCode : PrefItem<Any>

    @SuppressLint("ResourceType")
    constructor(
        context: Context,
        listener: PreferencesSetupListener
    ) : super(
        ReferenceWorldHandler.WidgetId.PREFERENCES_SETUP,
        ReferenceWorldHandler.WidgetId.PREFERENCES_SETUP,
        listener
    ) {
        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_widget_preferences_setup, null) as ConstraintLayout

        preferencesLeftOptionsGridView = view!!.findViewById(R.id.preferences_left_options)

        //setup adapters
        preferencesLeftOptionsGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferencesLeftOptionsGridView!!.windowAlignmentOffset = Utils.getDimensInPixelSize(R.dimen.custom_dim_25)
        preferencesLeftOptionsGridView!!.setNumColumns(1)
        setUpListener = listener

    }

    override fun refresh(data: Any) {

        if (data is PreferenceSetupInformation) {
            if (listener.getPreferenceDeepLink()!= null){
               val subMenuItem = data.subCategories.filter {
                    it.id == PrefSubMenu.CHANNEL_EDIT_MENUS
                }
                data.subCategories.clear()
                data.subCategories.addAll(subMenuItem)
            }
            //Used for channel edit, Api calls can be handle here.
            val editChannelListener = object : EditChannelListener{
                override fun onMove(moveItems: ArrayList<TvChannel>, previousPosition:Int, newPosition:Int, channelMap: HashMap<Int, String>, callback: IAsyncCallback) {

                    //Refreshing other lists in Channel Edit
                    var swapTempList: MutableList<PrefItem<Any>> = mutableListOf()
                    var skipTempList: MutableList<PrefItem<Any>> = mutableListOf()
                    var deleteTempList: MutableList<PrefItem<Any>> = mutableListOf()

                    moveItems.forEach { item ->

                        for (prefItem in swapChannelsItems) {
                            if (item.channelId == (prefItem.data as ChannelItem).tvChannel.channelId) {
                                swapTempList.add(prefItem)
                                break
                            }
                        }

                        for (prefItem in skipChannelCheckBoxItems) {
                            if (item.channelId == (prefItem.data as ChannelItem).tvChannel.channelId) {
                                skipTempList.add(prefItem)
                                break
                            }
                        }

                        for (prefItem in deleteChannelCheckBoxItems) {
                            if (item.channelId == (prefItem.data as ChannelItem).tvChannel.channelId) {
                                deleteTempList.add(prefItem)
                                break
                            }
                        }
                    }

                    swapChannelsItems.removeAll(swapTempList)
                    skipChannelCheckBoxItems.removeAll(skipTempList)
                    deleteChannelCheckBoxItems.removeAll(deleteTempList)

                    if (newPosition < swapChannelsItems.size) {
                        swapChannelsItems.addAll(newPosition, swapTempList)
                    } else {
                        swapChannelsItems.addAll(swapTempList)
                    }

                    if (newPosition < skipChannelCheckBoxItems.size) {
                        skipChannelCheckBoxItems.addAll(newPosition, skipTempList)
                    } else {
                        skipChannelCheckBoxItems.addAll(skipTempList)
                    }

                    if (newPosition < deleteChannelCheckBoxItems.size) {
                        deleteChannelCheckBoxItems.addAll(newPosition, deleteTempList)
                    } else {
                        deleteChannelCheckBoxItems.addAll(deleteTempList)
                    }

                    setUpListener?.onMoveChannelSelected(moveItems, previousPosition, newPosition, channelMap)
                    callback.onSuccess()
                }
                override fun onSwap(
                    firstChannel: TvChannel,
                    secondChannel: TvChannel,
                    firstIndex: Int,
                    secondIndex: Int,
                    callback: IAsyncCallback
                ) {
                    //Refreshing other items
                    var temp = moveChannelItems[firstIndex]
                    moveChannelItems[firstIndex]=moveChannelItems[secondIndex]
                    moveChannelItems[secondIndex]=temp

                    temp = deleteChannelCheckBoxItems[firstIndex]
                    deleteChannelCheckBoxItems[firstIndex]=deleteChannelCheckBoxItems[secondIndex]
                    deleteChannelCheckBoxItems[secondIndex]=temp

                    temp = skipChannelCheckBoxItems[firstIndex]
                    skipChannelCheckBoxItems[firstIndex]=skipChannelCheckBoxItems[secondIndex]
                    skipChannelCheckBoxItems[secondIndex]=temp

                    setUpListener?.onSwapChannelSelected(firstChannel, secondChannel, firstIndex, secondIndex)
                    callback.onSuccess()
                }

                override fun onDelete(
                    tvChannel: TvChannel,
                    deletedIndex:Int,
                    callback: IAsyncCallback
                ) {
                    InformationBus.submitEvent(
                        Event(
                            Events.PREFERENCE_DELETE_CHANNEL,
                            object :IAsyncCallback{
                                override fun onFailed(error: Error) {}

                                override fun onSuccess() {
                                    //updating other options on deleting
                                    moveChannelItems.removeAt(deletedIndex)
                                    swapChannelsItems.removeAt(deletedIndex)
                                    skipChannelCheckBoxItems.removeAt(deletedIndex)

                                    setUpListener?.onDeleteChannelSelected(tvChannel, deletedIndex)
                                    callback.onSuccess()
                                }

                            }
                        )
                    )

                }

                override fun onSkip(
                    channel: TvChannel,
                    callback: IAsyncCallback
                ) {
                    callback.onSuccess()
                }

            }
            //Used mainly for click listener for each item, api call can be done here.
            val itemListener = object : PrefItemListener {
                override fun onAction(action: Action,id:Pref): Boolean {
                    if (action==Action.CLICK){
                        when (id) {
                            Pref.CHANNEL_SCAN -> {
                                //TODO checkIsRecordingStarted support
                                /*if (ReferenceSdk.checkIsRecordingStarted()) {
                                                    Toast.makeText(
                                                        ReferenceSdk.context,
                                                        ConfigStringsManager.getStringById("recording_scan_conflict_toast"),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {*/
                                setUpListener?.onChannelScanClicked()
                                //}
                            }
                            Pref.POSTAL_CODE -> {
                                val sceneData = PostalCodeSceneData(
                                    ReferenceApplication.worldHandler!!.active!!.id,
                                    ReferenceApplication.worldHandler!!.active!!.instanceId
                                )
                                sceneData.sceneType =
                                    PostalCodeSceneData.DEFAULT_POSTAL_CODE_SCENE_TYPE
                                sceneData.submitListener = object :
                                    PostalCodeSceneData.SubmitListener {
                                    override fun onSubmit(postalCode: String) {
                                        (preferencePostalCode.data as RootItem).info = postalCode
                                        preferencesLeftOptionsAdapter!!.notifyPreferenceUpdated()
                                    }
                                }

                                ReferenceApplication.worldHandler!!.triggerActionWithData(
                                    ReferenceWorldHandler.SceneId.POSTAL_SCENE,
                                    SceneManager.Action.SHOW_OVERLAY, sceneData
                                )
                                return true
                            }
                            Pref.CHANNELS_SETTING -> {
                                /*if (ReferenceSdk.checkIsRecordingStarted()) {
                                                    Toast.makeText(
                                                        ReferenceSdk.context,
                                                        ConfigStringsManager.getStringById("recording_scan_conflict_toast"),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {*/
                                setUpListener?.onChannelsSettingsClicked()
                                //}
                            }
                            Pref.PICTURE -> {
                                ReferenceApplication.isOkClickedOnSetUp = true
                                ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                                setUpListener?.onPictureSettingsClicked()
                                return true
                            }
                            Pref.SOUND -> {
                                ReferenceApplication.isOkClickedOnSetUp = true
                                ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                                setUpListener?.onSoundSettingsClicked()
                                return true
                            }
                            Pref.SCREEN -> {
                                ReferenceApplication.isOkClickedOnSetUp = true
                                ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                                setUpListener?.onScreenSettingsClicked()
                                return true
                            }
                            Pref.CHANNEL_EDIT -> {
                                setUpListener?.onChannelEditClicked()
                            }
                            Pref.PARENTAL_CONTROL -> {
                                setUpListener?.onParentalControlClicked()
                            }
                            Pref.CLEAR_CHANNELS -> {
                                InformationBus.submitEvent(Event(
                                    Events.PREFERENCE_CLEAR_ALL_CHANNELS,object :IAsyncCallback{
                                        override fun onFailed(error: Error) {
                                        }

                                        override fun onSuccess() {
                                            setUpListener?.onClearChannelSelected()
                                        }

                                    }
                                ))
                            }
                            Pref.CHANNELS -> {
                                setUpListener?.onChannelClicked()
                            }
                            Pref.POWER -> {
                                setUpListener?.onPowerClicked()
                            }
                            Pref.OAD_UPDATE -> {
                                setUpListener?.startOadScan()
                                return true
                            }

                            else -> {}
                        }
                    }

                    return false
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    setUpListener?.setSpeechText(text = text, importance = importance)
                }

                override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                    setUpListener?.showToast(text, duration)
                }
            }

            val prefCompoundListener = object : PrefCompoundListener {
                override fun onChange(
                    isSelected: Boolean,
                    index: Int,
                    id: Pref,
                    callback: IAsyncDataCallback<Boolean>
                ) {
                    if (id== Pref.DEFAULT_CHANNEL_SWITCH){
                        setUpListener!!.setPrefsValue(
                            ReferenceApplication.DEFAULT_CHANNEL_ENABLE,
                            isSelected
                        )
                    }
                    else if(id == Pref.DEFAULT_CHANNEL_RADIO){
                        setUpListener?.onDefaultChannelSelected(data.channels!![index])
                    }
                    else if(id == Pref.LCN_SWITCH){
                        listener.enableLcn(isSelected)
                    }
                    else if(id == Pref.ANTENNA_SWITCH){
                        listener.getUtilsModule().setAntennaPower(isSelected)
                    }
                    else if(id == Pref.DISPLAY_MODE_RADIO){
                        setUpListener?.onDisplayModeSelected(index)
                    }
                    else if(id == Pref.INTERACTION_CHANNEL_SWITCH){
                        setUpListener?.setInteractionChannel(isSelected)
                    }
                    else if(id == Pref.SKIP_CHANNEL_CHECKBOX){
                        setUpListener?.onSkipChannelSelected(data.channels?.get(index)!!, isSelected)
                    }
                    else if (id== Pref.BLUE_MUTE_SWITCH){
                        setUpListener!!.onBlueMuteChanged(isSelected)
                    }
                    else if (id== Pref.NO_SIGNAL_AUTO_POWER_OFF_SWITCH){
                        setUpListener!!.noSignalPowerOffChanged(isSelected)
                        if(isSelected){
                            if(noSignalAutoPowerOffItemList.size > 0){
                                noSignalAutoPowerOffItemList.forEachIndexed { index, prefItem ->
                                    if( (prefItem.data as CompoundItem).isChecked ){
                                        setUpListener!!.setPowerOffTime(index, data.noSignalPowerOff!![index])
                                        setUpListener!!.noSignalPowerOffTimeChanged()
                                    }
                                }
                            }
                        }
                    }
                    else if (id== Pref.NO_SIGNAL_AUTO_POWER_OFF_RADIO){
                        setUpListener!!.setPowerOffTime(index, data.noSignalPowerOff!![index])
                        listener.getUtilsModule().setPrefsValue(
                            "no_signal_auto_power_off",
                            data.noSignalPowerOff!![index])
                        setUpListener!!.noSignalPowerOffTimeChanged()
                    }
                    else if (id == Pref.ASPECT_RATIO_RADIO) {
                        setUpListener?.onAspectRatioClicked(index)
                    }
                    callback.onReceive(isSelected)
                }
            }

            if (data.subCategories.isNotEmpty()) {
                for (categoryOption in data.subCategories) {
                    var itemTitle = categoryOption.name
                    when (categoryOption.id) {
                        PrefSubMenu.CHANNEL_SCAN -> {
                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.CHANNEL_SCAN,
                                RootItem(
                                    itemTitle, null, false),
                                itemListener
                            ))
                        }
                        PrefSubMenu.POSTAL_CODE -> {
                            preferencePostalCode = PrefItem(
                                ViewHolderType.VT_MENU, Pref.POSTAL_CODE,
                                RootItem(
                                    itemTitle, listener.getUtilsModule().getEWSPostalCode(), false
                                ), itemListener
                            )
                            zerothColumn.add(preferencePostalCode)
                        }
                        PrefSubMenu.CHANNELS_SETTING -> {
                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.CHANNELS_SETTING,
                                RootItem(
                                    itemTitle, null, false),
                                itemListener
                            ))
                        }
                        PrefSubMenu.PICTURE -> {
                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.PICTURE,
                                RootItem(
                                    itemTitle, null, false),
                                itemListener
                            ))
                        }
                        PrefSubMenu.SCREEN -> {
                            var rootItem = RootItem(
                                itemTitle, null, false
                            )
                            if(!listener.getSignalAvailable()) {
                                rootItem.isEnabled = false
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.SCREEN,
                                    rootItem,
                                    itemListener
                                )
                            )
                        }
                        PrefSubMenu.ASPECT_RATIO -> {
                            var aspectRatioList = mutableListOf<PrefItem<Any>>()
                            data.aspectRatioOptions.forEachIndexed { index, it ->
                                aspectRatioList.add(PrefItem (
                                    ViewHolderType.VT_RADIO,
                                    Pref.ASPECT_RATIO_RADIO,
                                    CompoundItem(it, index==data.defaultAspectRatioOption, index, prefCompoundListener),
                                    itemListener))
                            }

                            val rootItem = RootItem(
                                itemTitle,
                                data.aspectRatioOptions[data.defaultAspectRatioOption],
                                true,
                                aspectRatioList,
                                object : InfoListener{
                                    override fun getInfo(): String? {
                                        aspectRatioList.forEach {
                                            val compoundItem = it.data as CompoundItem
                                            if(compoundItem.isChecked) return compoundItem.title
                                        }
                                        return null
                                    }
                                }
                            )

                            if(!listener.getSignalAvailable()) {
                                rootItem.isEnabled = false
                            }

                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.ASPECT_RATIO,
                                    rootItem,
                                    itemListener)
                            )
                        }
                        PrefSubMenu.SOUND -> {
                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.SOUND,
                                RootItem(
                                    itemTitle, null, false),
                                itemListener
                            ))
                        }
                        PrefSubMenu.POWER -> {
                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.POWER,
                                RootItem(
                                    itemTitle, null, false),
                                itemListener
                            ))
                        }
                        PrefSubMenu.CHANNELS -> {
                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.CHANNELS,
                                RootItem(
                                    itemTitle, null, false),
                                itemListener
                            ))
                        }
                        PrefSubMenu.CHANNEL_EDIT -> {
                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.CHANNEL_EDIT,
                                RootItem(
                                    itemTitle, null, false),
                                itemListener
                            ))
                        }
                        PrefSubMenu.DEFAULT_CHANNEL -> {
                            val switchStatus = setUpListener!!.getPrefsValue(
                                ReferenceApplication.DEFAULT_CHANNEL_ENABLE,
                                false
                            ) as Boolean

                            val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                            var defaultChannelItems = mutableListOf<PrefItem<Any>>()
                            val switchItem = PrefItem(
                                ViewHolderType.VT_SWITCH,
                                Pref.DEFAULT_CHANNEL_SWITCH,
                                CompoundItem(status, switchStatus, 0,prefCompoundListener),
                                itemListener
                            )

                            defaultChannelItems.add(switchItem as PrefItem<Any>)

                            data.channels?.forEachIndexed { index, tvChannel ->

                                val radioItem = PrefItem (
                                    ViewHolderType.VT_RADIO,
                                    Pref.DEFAULT_CHANNEL_RADIO,
                                    CompoundItem(tvChannel.name, tvChannel==data.defaultChannel, index, prefCompoundListener),
                                    itemListener
                                )as PrefItem<Any>

                                if(switchStatus){
                                    defaultChannelItems.add(radioItem)
                                }else{
                                    switchItem.data.hiddenOptions.add(radioItem)
                                }
                            }
                            var itemSubtitle = if (switchStatus) data.defaultChannel?.name else status

                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.DEFAULT_CHANNEL,
                                RootItem(
                                    itemTitle, itemSubtitle, true,
                                    defaultChannelItems,object :InfoListener{
                                        override fun getInfo(): String? {
                                            val item = defaultChannelItems[0].data as CompoundItem
                                            if(!item.isChecked) return ConfigStringsManager.getStringById(  "off")
                                            defaultChannelItems.forEach {
                                                val item = it.data as CompoundItem
                                                if(item.isChecked && it.viewHolderType == ViewHolderType.VT_RADIO)
                                                    return it.data.title
                                            }
                                            return ConfigStringsManager.getStringById(  "on")
                                        }
                                    }
                                ),
                                itemListener
                            ))
                        }
                        PrefSubMenu.LCN -> {
                            val switchStatus = listener.isLcnEnabled()
                            val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")

                            val compoundItem = CompoundItem(status, switchStatus, 0,prefCompoundListener)

                            zerothColumn.add(
                                PrefItem(ViewHolderType.VT_MENU,
                                    Pref.LCN,
                                    RootItem(
                                        itemTitle, status, true,
                                        mutableListOf(PrefItem(
                                            ViewHolderType.VT_SWITCH,
                                            Pref.LCN_SWITCH,
                                            compoundItem,
                                            itemListener
                                        )),object : InfoListener{
                                            override fun getInfo(): String? {
                                                return ConfigStringsManager.getStringById(if(compoundItem.isChecked) "on" else "off")
                                            }
                                        }
                                    ),
                                    itemListener
                                )

                            )
                        }
                        PrefSubMenu.AUTO_SERVICE_UPDATE -> {
                            val switchStatus = true
                            val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")

                            val compoundItem = CompoundItem(status, switchStatus, 0,prefCompoundListener)

                            zerothColumn.add(
                                PrefItem(ViewHolderType.VT_MENU,
                                    Pref.LCN,
                                    RootItem(
                                        itemTitle, status, true,
                                        mutableListOf(PrefItem(
                                            ViewHolderType.VT_SWITCH,
                                            Pref.AUTO_SERVICE_UPDATE_SWITCH,
                                            compoundItem,
                                            itemListener
                                        )),object : InfoListener{
                                            override fun getInfo(): String? {
                                                return ConfigStringsManager.getStringById(if(compoundItem.isChecked) "on" else "off")
                                            }
                                        }
                                    ),
                                    itemListener
                                )

                            )
                        }
                        PrefSubMenu.ANTENNA -> {
                            val switchStatus = listener.getUtilsModule().isAntennaPowerEnabled()

                            val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")

                            val compoundItem = CompoundItem(status, switchStatus, 0,prefCompoundListener)

                            zerothColumn.add(
                                PrefItem(ViewHolderType.VT_MENU,
                                    Pref.ANTENNA,
                                    RootItem(
                                        itemTitle, status, true,
                                        mutableListOf(PrefItem(
                                            ViewHolderType.VT_SWITCH,
                                            Pref.ANTENNA_SWITCH,
                                            compoundItem,
                                            itemListener
                                        )),object : InfoListener{
                                            override fun getInfo(): String? {
                                                return ConfigStringsManager.getStringById(if(compoundItem.isChecked) "on" else "off")
                                            }
                                        }
                                    ),
                                    itemListener
                                )
                            )
                        }
                        PrefSubMenu.DISPLAY_MODE -> {
                            var displayModeOptions =mutableListOf<PrefItem<Any>>()
                            data.displayMode!!.forEach {
                                displayModeOptions.add(PrefItem (
                                    ViewHolderType.VT_RADIO,
                                    Pref.DISPLAY_MODE_RADIO,
                                    CompoundItem(it.value, it.key==data.defaultDisplayMode, it.key, prefCompoundListener),
                                    itemListener))
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.DISPLAY_MODE,
                                    RootItem(
                                        itemTitle,
                                        data.displayMode!![data.defaultDisplayMode],
                                        true,
                                        displayModeOptions,
                                        object : InfoListener{
                                            override fun getInfo(): String? {
                                                displayModeOptions.forEach {
                                                    val compoundItem = it.data as CompoundItem
                                                    if(compoundItem.isChecked) return compoundItem.title
                                                }
                                                return null
                                            }
                                        }
                                    ),
                                    itemListener)
                            )
                        }
                        PrefSubMenu.PARENTAL_CONTROL -> {
                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.PARENTAL_CONTROL,
                                RootItem(
                                    itemTitle, null, false,
                                ),
                                itemListener
                            ))
                        }
                        PrefSubMenu.INTERACTION_CHANNEL -> {
                            val switchStatus =  data.isInteractionChannelEnabled

                            val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                            zerothColumn.add(
                                PrefItem(ViewHolderType.VT_MENU,
                                    Pref.INTERACTION_CHANNEL,
                                    RootItem(
                                        itemTitle, status, true,
                                        mutableListOf(PrefItem(
                                            ViewHolderType.VT_SWITCH,
                                            Pref.INTERACTION_CHANNEL_SWITCH,
                                            CompoundItem(status, switchStatus, 0,prefCompoundListener),
                                            itemListener
                                        ))
                                    ),
                                    itemListener
                                )
                            )
                        }
                        PrefSubMenu.PREFERRED_EPG_LANGUAGE -> {
                            var preferredEpgLanguageOptions = mutableListOf<PrefItem<Any>>()
                            data.availableAudioTracks?.forEachIndexed { index, language ->
                                val radioItem = PrefItem (
                                    ViewHolderType.VT_RADIO,
                                    Pref.PREFERRED_EPG_LANGUAGE_RADIO,
                                    CompoundItem(language.englishName, language==data.epgLanguage, index, prefCompoundListener),
                                    itemListener
                                )as PrefItem<Any>
                                preferredEpgLanguageOptions.add(radioItem)
                            }
                            var itemSubtitle =data.epgLanguage?.englishName

                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.PREFERRED_EPG_LANGUAGE,
                                RootItem(
                                    itemTitle,itemSubtitle , true,
                                    preferredEpgLanguageOptions,

                                    object :InfoListener{
                                        override fun getInfo(): String? {
                                            preferredEpgLanguageOptions.forEach {
                                                val compoundItem = it.data as CompoundItem
                                                if(compoundItem.isChecked) return compoundItem.title
                                            }
                                            return null
                                        }
                                    }
                                ),
                                itemListener
                            ))
                        }
                        PrefSubMenu.BLUE_MUTE -> {
                            val switchStatus = data.isBlueMuteEnabled
                            val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                            val compoundItem = CompoundItem(status, switchStatus, 0,prefCompoundListener)
                            zerothColumn.add(
                                PrefItem(ViewHolderType.VT_MENU,
                                    Pref.BLUE_MUTE,
                                    RootItem(
                                        itemTitle, status, true,
                                        mutableListOf(PrefItem(
                                            ViewHolderType.VT_SWITCH,
                                            Pref.BLUE_MUTE_SWITCH,
                                            compoundItem,
                                            itemListener
                                        )),
                                        object :InfoListener{
                                            override fun getInfo(): String? {
                                                return ConfigStringsManager.getStringById(if(compoundItem.isChecked) "on" else "off")
                                            }
                                        }
                                    ),
                                    itemListener
                                )
                            )
                        }
                        PrefSubMenu.OAD_UPDATE -> {
                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.OAD_UPDATE,
                                RootItem(
                                    itemTitle, null, false),
                                itemListener
                            ))
                        }

                        PrefSubMenu.NO_SIGNAL_AUTO_POWER_OFF -> {
                            val switchStatus = data.noSignalPowerOffEnabled

                            var status = if (switchStatus) ConfigStringsManager.getStringById(data.defaultNoSignalPowerOff!!) else ConfigStringsManager.getStringById("off")

                            var noSignalAutoPowerOffItems = mutableListOf<PrefItem<Any>>()

                            val switchItem = PrefItem(
                                ViewHolderType.VT_SWITCH,
                                Pref.NO_SIGNAL_AUTO_POWER_OFF_SWITCH,
                                CompoundItem(ConfigStringsManager.getStringById(if (switchStatus)"on" else "off")!!, switchStatus, 0,prefCompoundListener) ,
                                itemListener
                            )

                            noSignalAutoPowerOffItemList.clear()
                            data.noSignalPowerOff!!.onEachIndexed { index, timeString ->
                                val isSelected = timeString == data.defaultNoSignalPowerOff
                                val radioItem = PrefItem (
                                    ViewHolderType.VT_RADIO,
                                    Pref.NO_SIGNAL_AUTO_POWER_OFF_RADIO,
                                    CompoundItem(ConfigStringsManager.getStringById(timeString), isSelected, index, prefCompoundListener),
                                    itemListener
                                )as PrefItem<Any>
                                noSignalAutoPowerOffItemList.add(radioItem)

                                if(switchStatus){
                                    noSignalAutoPowerOffItems.add(radioItem)
                                }else{
                                    switchItem.data.hiddenOptions.add(radioItem)
                                }
                            }
                            noSignalAutoPowerOffItems.add(0,switchItem as PrefItem<Any>)

                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.NO_SIGNAL_AUTO_POWER_OFF,
                                RootItem(
                                    itemTitle, status, true,
                                    noSignalAutoPowerOffItems,
                                    object :InfoListener{
                                        override fun getInfo(): String? {
                                            val item = noSignalAutoPowerOffItems[0].data as CompoundItem
                                            if(!item.isChecked) {
                                                return ConfigStringsManager.getStringById(
                                                    "off"
                                                )
                                            }
                                            else{
                                                noSignalAutoPowerOffItems.forEach {
                                                    val item = it.data as CompoundItem
                                                    if(item.isChecked && it.viewHolderType == ViewHolderType.VT_RADIO) {
                                                        return it.data.title
                                                    }
                                                }
                                            }
                                            return ConfigStringsManager.getStringById(  "off")
                                        }
                                    }

                                ),
                                itemListener
                            ))
                        }
                        PrefSubMenu.CHANNEL_EDIT_MENUS -> {
                            //skip channel
                            val channelEditItems = mutableListOf<PrefItem<Any>>()
                            skipChannelCheckBoxItems.clear()

                            data.channels?.forEachIndexed { index, tvChannel ->
                                if (tvChannel.isBrowsable) {
                                    skipChannelCheckBoxItems.add(
                                        PrefItem(
                                            ViewHolderType.VT_EDIT_CHANNEL,
                                            Pref.SKIP_CHANNEL_CHECKBOX,
                                            ChannelItem(
                                                tvChannel.name, tvChannel.isSkipped, index, prefCompoundListener,
                                                tvChannel,
                                                EditChannel.CHANNEL_SKIP, editChannelListener
                                            ),
                                            itemListener
                                        )
                                    )
                                }
                            }

                            var skipChannelTitle = ConfigStringsManager.getStringById("skip_channel")
                            var skipChannelItem = PrefItem<Any>(ViewHolderType.VT_MENU,
                                Pref.SKIP_CHANNEL,
                                RootItem(
                                    skipChannelTitle , null, true,
                                    skipChannelCheckBoxItems
                                ),   itemListener
                            )
                            channelEditItems.add(skipChannelItem)
                            //swap channel
                            swapChannelsItems.clear()
                            data.channels?.forEachIndexed { index, tvChannel ->
                                if (tvChannel.isBrowsable) {
                                    swapChannelsItems.add(
                                        PrefItem(
                                            ViewHolderType.VT_EDIT_CHANNEL,
                                            Pref.SWAP_CHANNELS_ITEM,
                                            ChannelItem(
                                                tvChannel.name,
                                                false,
                                                index,
                                                prefCompoundListener,
                                                tvChannel,
                                                EditChannel.CHANNEL_SWAP,
                                                editChannelListener
                                            ),
                                            itemListener
                                        )
                                    )
                                }
                            }

                            var swapChannelTitle = ConfigStringsManager.getStringById("swap_channel")
                            var swapChannelItem = PrefItem<Any>(ViewHolderType.VT_MENU,
                                Pref.SWAP_CHANNEL,
                                RootItem(
                                    swapChannelTitle , null, true,
                                    swapChannelsItems
                                ),   itemListener
                            )
                            channelEditItems.add(swapChannelItem)

                            //Move channel
                            moveChannelItems.clear()
                            data.channels?.forEachIndexed { index, tvChannel ->
                                if (tvChannel.isBrowsable) {
                                    moveChannelItems.add(
                                        PrefItem(
                                            ViewHolderType.VT_EDIT_CHANNEL,
                                            Pref.MOVE_CHANNEL_ITEM,
                                            ChannelItem(
                                                tvChannel.name, false, index, prefCompoundListener,
                                                tvChannel,
                                                EditChannel.CHANNEL_MOVE, editChannelListener
                                            ),
                                            itemListener
                                        )
                                    )
                                }
                            }
                            var moveChannelTitle = ConfigStringsManager.getStringById("move_channel")
                            var moveItem = PrefItem<Any>(ViewHolderType.VT_MENU,
                                Pref.MOVE_CHANNEL,
                                RootItem(
                                    moveChannelTitle , null, true,
                                    moveChannelItems
                                ),   itemListener
                            )
                            channelEditItems.add(moveItem)

                            //delete channel
                            deleteChannelCheckBoxItems.clear()

                            data.channels?.forEachIndexed { index, tvChannel ->
                                if (tvChannel.isBrowsable) {
                                    deleteChannelCheckBoxItems.add(
                                        PrefItem(
                                            ViewHolderType.VT_EDIT_CHANNEL,
                                            Pref.DELETE_CHANNEL_CHECKBOX,
                                            ChannelItem(
                                                tvChannel.name, false, index, prefCompoundListener,
                                                tvChannel,
                                                EditChannel.CHANNEL_DELETE, editChannelListener
                                            ),
                                            itemListener
                                        )
                                    )
                                }
                            }
                            val deleteChannelTitle = ConfigStringsManager.getStringById("delete_channel")
                            var deleteChannelItem = PrefItem<Any>(ViewHolderType.VT_MENU,
                                Pref.DELETE_CHANNEL,
                                RootItem(
                                    deleteChannelTitle , null, true,
                                    deleteChannelCheckBoxItems
                                ),
                                itemListener
                            )
                            channelEditItems.add(deleteChannelItem)

                            //lcn
                            val lcnTitle = ConfigStringsManager.getStringById("lcn")
                            val switchStatus = listener.isLcnEnabled()
                            val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                            val compoundItem = CompoundItem(status, switchStatus, 0,prefCompoundListener)
                            var lcnItem =PrefItem<Any>(ViewHolderType.VT_MENU,
                                Pref.LCN,
                                RootItem(
                                    lcnTitle, status, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.LCN_SWITCH,
                                        compoundItem,
                                        itemListener
                                    )),object : InfoListener{
                                        override fun getInfo(): String? {
                                            return ConfigStringsManager.getStringById(if(compoundItem.isChecked) "on" else "off")
                                        }
                                    }
                                ),
                                itemListener
                            )
                            /*as per agreement, we removed this button
                            channelEditItems.add(lcnItem)*/

                            //clear channel
                            val clearChannelTitle = ConfigStringsManager.getStringById("clear_all_channels")
                            val clearChannelButton = PrefItem<Any>(ViewHolderType.VT_MENU,
                                Pref.CLEAR_CHANNELS,
                                RootItem(
                                    clearChannelTitle, null, false),
                                itemListener
                            )
                            channelEditItems.add(clearChannelButton)
                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.CHANNEL_EDIT_KONKA,
                                RootItem(itemTitle, null, true, channelEditItems),
                                itemListener
                            ))
                        }
                        else -> {

                        }
                    }

                }

                preferencesLeftOptionsAdapter = PreferenceSubMenuAdapter(
                    zerothColumn,
                    view!!,
                    object : PreferenceSubMenuAdapter.Listener {
                        override fun onBack(action: Action): Boolean {

                            if(action == Action.LEFT) return false
                            if (isLicenseClicked) {
                                //do not post listener, for other item like channelScan activity is launched
                                isLicenseClicked = false
                            } else {
                                setUpListener?.onPrefsCategoriesRequestFocus()
                            }
                            return true
                        }

                        override fun updateInfo() {
                            //ignore
                        }

                        override fun isAccessibilityEnabled(): Boolean {
                            return false
                        }

                        override fun updateHintText(status: Boolean, type: Int) {
                        }

                        override fun getPreferenceDeepLink(): ReferenceWidgetPreferences.DeepLink? {
                            return setUpListener?.getPreferenceDeepLink()
                        }

                        override fun onExit() {
                            setUpListener?.onExit()
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            setUpListener?.setSpeechText(text = text, importance = importance)
                        }

                        override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                            setUpListener?.showToast(text, duration)
                        }

                        override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                            setUpListener?.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                        }
                    }
                )
                preferencesLeftOptionsGridView!!.adapter = preferencesLeftOptionsAdapter
            }
        }
        super.refresh(data)
    }

    fun setFocusToGrid() {
        preferencesLeftOptionsGridView!!.requestFocus()
    }


    interface PreferencesSetupListener : GWidgetListener, TTSSetterInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun onPrefsCategoriesRequestFocus()
        fun onChannelScanClicked()
        fun onChannelsSettingsClicked()
        fun onPictureSettingsClicked()
        fun onScreenSettingsClicked()
        fun onSoundSettingsClicked()
        fun onChannelEditClicked()
        fun onDefaultChannelSelected(tvChannel: TvChannel)
        fun onDisplayModeSelected(selectedMode: Int)
        fun onParentalControlClicked()
        fun onEvaluationLicenseClicked()
        fun onAspectRatioClicked(position: Int)
        fun setInteractionChannel(selected: Boolean)
        fun onEpgLanguageSelected(language: String)
        fun getPrefsValue(key: String, value: Any?): Any?
        fun setPrefsValue(key: String, defValue: Any?)
        fun onSkipChannelSelected(tvChannel: TvChannel, status: Boolean)
        fun onSwapChannelSelected(firstChannel: TvChannel, secondChannel: TvChannel, previousPosition:Int,newPosition:Int)
        fun onMoveChannelSelected(moveChannelList: ArrayList<TvChannel>, previousPosition:Int,newPosition:Int, channelMap: HashMap<Int, String>)
        fun onDeleteChannelSelected(tvChannel: TvChannel, index: Int)
        fun onClearChannelSelected()
        fun onChannelClicked()
        fun onBlueMuteChanged(enable: Boolean)
        fun noSignalPowerOffChanged(isEnabled: Boolean)
        fun noSignalPowerOffTimeChanged()
        fun getUtilsModule(): UtilsInterface
        fun isLcnEnabled(): Boolean
        fun enableLcn(enable: Boolean)
        fun getSignalAvailable() : Boolean
        fun setPowerOffTime(value: Int, time: String)
        fun onPowerClicked()
        fun onExit()
        fun getPreferenceDeepLink() : ReferenceWidgetPreferences.DeepLink?
        fun startOadScan()
    }
}