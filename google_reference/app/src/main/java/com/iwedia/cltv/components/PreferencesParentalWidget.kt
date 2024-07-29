package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.content.Context
import android.media.tv.ContentRatingSystem
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.entities.PreferencesParentalControlInformation
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.parental_control.change_pin.ParentalPinSceneData
import com.iwedia.cltv.utils.PinHelper
import com.iwedia.cltv.utils.Utils
import world.SceneManager
import world.widget.GWidget
import world.widget.GWidgetListener


class PreferencesParentalWidget :
    GWidget<ConstraintLayout, PreferencesParentalWidget.PreferenceParentalListener> {
    private val TAG = javaClass.simpleName

    var context: Context? = null
    private val categories = mutableListOf<PreferenceCategoryItem>()

    private var preferencesLeftOptionsGridView: VerticalGridView? = null
    private var preferencesLeftOptionsAdapter: PreferenceSubMenuAdapter? = null

    var parentalListener: PreferenceParentalListener? = null

    var zerothColumn = mutableListOf<PrefItem<Any>>()

    val ratingItems = mutableListOf<PrefItem<Any>>()
    private var ratingLockItem : PrefItem<Any>? = null
    private val ratingLockItems = mutableListOf<PrefItem<Any>>()
    private val ratingLockCountryItems = mutableListOf<PrefItem<Any>>()
    private var ratingLockGlobalRestrictions : PrefItem<Any>? = null

    private var anokiRatingSystem : PrefItem<Any>?= null
    var anokiRatingSystemItems = mutableListOf<PrefItem<Any>>()


    var rrt5LockItem : PrefItem<Any>? = null
    val rrt5LockItems = mutableListOf<PrefItem<Any>>()
    val crsNameMap = HashMap<String,ContentRatingSystem>()
    val ratingNameMap = HashMap<String,ContentRatingSystem.Rating>()

    var information : PreferencesParentalControlInformation?= null
    var isRef5Flavour = false

    var parentalSwitchHandler = Handler() /*Introduced to handle multiple clicks on parental switch*/

    constructor(
        context: Context,
        listener: PreferenceParentalListener
    ) : super(
        ReferenceWorldHandler.WidgetId.PREFERENCES_PARENTAL_CONTROL,
        ReferenceWorldHandler.WidgetId.PREFERENCES_PARENTAL_CONTROL,
        listener
    ) {
        this.context = context
        isRef5Flavour = ((ReferenceApplication.worldHandler) as ReferenceWorldHandler).getPlatformName().contains("RefPlus")
        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.preferences_parental_layout, null) as ConstraintLayout

        if(listener.isAccessibilityEnabled()) {
            view!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            view!!.isFocusable = false
        }

        preferencesLeftOptionsGridView = view!!.findViewById(R.id.preferences_parental_left_options)
        preferencesLeftOptionsGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferencesLeftOptionsGridView!!.windowAlignmentOffset = Utils.getDimensInPixelSize(R.dimen.custom_dim_25)
        preferencesLeftOptionsGridView!!.setNumColumns(1)
        parentalListener = listener

        if(listener.isAccessibilityEnabled()) {
            preferencesLeftOptionsGridView!!.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            preferencesLeftOptionsGridView!!.isFocusable = false
        }

    }

    val componentListener = object : PrefItemListener {
        override fun onAction(action: Action, id: Pref): Boolean {
            if (action == Action.CLICK) {
                if(id == Pref.CHANGE_PIN){
                    if (PinHelper.USE_PIN_INSERT_ACTIVITY) {
                        listener.startChangePinActivity()
                    } else {
                        val sceneData = ParentalPinSceneData(
                            ReferenceApplication.worldHandler!!.active!!.id,
                            ReferenceApplication.worldHandler!!.active!!.instanceId
                        )
                        sceneData.sceneType = ParentalPinSceneData.DEFAULT_CHANGE_PIN_SCENE_TYPE
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.PARENTAL_PIN,
                            SceneManager.Action.SHOW_OVERLAY, sceneData
                        )
                    }

                    return true
                }
                if(id == Pref.RRT5_RESET){
                    showResetDialog()
                    return true
                }
            }
            return false
        }

        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
            parentalListener!!.setSpeechText(text = text, importance = importance)
        }

        override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
            parentalListener!!.showToast(text, duration)
        }
    }

    val prefCompoundListener = object : PrefCompoundListener {
        override fun onChange(
            isSelected: Boolean,
            index: Int,
            id: Pref,
            callback: IAsyncDataCallback<Boolean>
        ) {
            callback.onReceive(isSelected)

            when (id) {
                Pref.PARENTAL_CONTROL_SWITCH->{
                    preferencesLeftOptionsAdapter?.container?.adapter?.restrictMovement = true
                    parentalSwitchHandler.removeCallbacksAndMessages(null)
                    parentalSwitchHandler.postDelayed({
                        if (parentalListener!!.setParentalControlsEnabled(isSelected)){
                            if (isRef5Flavour) {
                                val channelIdsFromPrefs =
                                    parentalListener!!.getLockedChannelIdsFromPrefs()
                                handleParentalSwitch(isSelected, channelIdsFromPrefs)
                                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "channelIdsFromPrefs $channelIdsFromPrefs")
                            }

                            zerothColumn.forEach {
                                if(it.id != Pref.PARENTAL_CONTROL){
                                    (it.data as RootItem).isEnabled = isSelected
                                    preferencesLeftOptionsAdapter!!.notifyItemChanged(zerothColumn.indexOf(it))
                                }
                                if(it.id == Pref.ANOKI_RATING_SYSTEM_RADIO) {
                                    (it.data as RootItem).isEnabled =
                                        isSelected && it.data.itemList!!.isNotEmpty()
                                    preferencesLeftOptionsAdapter!!.notifyItemChanged(zerothColumn.indexOf(it))

                                }
                                if(it.id == Pref.CHANNEL_BLOCK) {
                                    if (isRef5Flavour) {
                                        (it.data as RootItem).info = blockedChannelCount(isSelected)
                                        preferencesLeftOptionsAdapter!!.notifyItemChanged(zerothColumn.indexOf(it))
                                    }
                                }

                            }
                        }
                        preferencesLeftOptionsAdapter?.container?.adapter?.restrictMovement = false
                    }, 500)
                }

                Pref.CHANNEL_BLOCK_CHECKBOX->{
                    var browsableChannels = ArrayList<TvChannel>()
                    information!!.channelList.forEach {
                        browsableChannels.add(it)
                    }

                    parentalListener!!.lockChannel(browsableChannels[index], isSelected)
                }

                Pref.INPUT_BLOCK_CHECKBOX->{
                    parentalListener!!.blockInput(information!!.blockedInputs[index], isSelected)
                }

                Pref.ANOKI_RATING_SYSTEM_RADIO-> {
                    if (parentalListener!!.checkNetworkConnection()) {
                        parentalListener!!.setAnokiRatingLevel(index)
                        zerothColumn.forEach {
                            if(it.id == Pref.ANOKI_RATING_SYSTEM_RADIO){
                                (it.data as RootItem).info = information!!.anokiRatingSystemArray[index]
                                preferencesLeftOptionsAdapter!!.notifyItemChanged(zerothColumn.indexOf(it))
                            }
                        }
                    }
                }

                Pref.RATING_SYSTEMS_CHECKBOX->{
                    val contentRatingSystem = information!!.contentRatingData[index]
                    parentalListener!!.setContentRatingSystemEnabled(contentRatingSystem, isSelected)
                    if(isSelected) information!!.contentRatingsEnabled.add(contentRatingSystem)
                    else information!!.contentRatingsEnabled.remove(contentRatingSystem)

                    val ratingSystemList = mutableListOf<ContentRatingSystem>()
                    information!!.contentRatingData.forEach {
                        if(information!!.contentRatingsEnabled.contains(it))
                            ratingSystemList.add(it)
                    }

                    information!!.contentRatingsEnabled.clear()
                    information!!.contentRatingsEnabled.addAll(ratingSystemList)

                    prepareRatingLockItems()
                }

                Pref.BLOCK_UNRATED_PROGRAMS_CHECKBOX->{
                    information!!.isBlockUnratedPrograms = isSelected
                    parentalListener!!.setBlockUnrated(isSelected)
                }

                Pref.GLOBAL_RESTRICTIONS_RADIO->{
                    parentalListener!!.setContentRatingLevel(index)
                    notifyParentalChanged()
                }
                else -> {

                }
            }
        }
    }

    private fun handleParentalSwitch(enabled: Boolean, channelIdsFromPrefs: MutableSet<String>){
        val channelList1 = information?.channelList
        if (enabled) {
            channelList1?.forEach {
                for (i in channelIdsFromPrefs) {
                    if (i == it.getUniqueIdentifier()) {
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"parental switch enabled locked shared pref channels ${it.name}")
                        parentalListener!!.lockChannel(it, true)
                        it.isLocked = true
                    }
                }
            }
        } else {
            channelList1?.forEach {
                if (it.isLocked) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"parental switch disabled Unlock all channels ${it.name} ")
                    parentalListener!!.lockChannel(it, false)
                    it.isLocked = false
                }
            }
        }
        information!!.channelList = channelList1!!
    }

    private fun blockedChannelCount(isSelected: Boolean) : String {
        if (isRef5Flavour) {
            val count: String = if (isSelected) {
                channelBlockCount().toString()
            } else {
                "0"
            }
            return count
        }
        return channelBlockCount().toString()
    }

    override fun refresh(data: Any) {
        if (data is PreferencesParentalControlInformation) {
            information = data

            zerothColumn.clear()

            if (data.subCategories!!.isNotEmpty()) {
                categories.clear()
                val isParentalControlEnabled = parentalListener!!.isParentalControlsEnabled()
                data.subCategories!!.forEach { item ->
                    val itemTitle = item.name

                    if (item.id == PrefSubMenu.PARENTAL_CONTROL) {
                        val status = ConfigStringsManager.getStringById(if(isParentalControlEnabled) "on" else "off")

                        val compoundItem = CompoundItem(status, isParentalControlEnabled, 0,prefCompoundListener)

                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.PARENTAL_CONTROL,
                                RootItem(
                                    itemTitle, status, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.PARENTAL_CONTROL_SWITCH,
                                        compoundItem,
                                        componentListener
                                    )),object : InfoListener{
                                        override fun getInfo(): String {
                                            return ConfigStringsManager.getStringById(if(compoundItem.isChecked) "on" else "off")
                                        }
                                    }
                                ),
                                componentListener
                            )
                        )
                    }
                    else if (item.id == PrefSubMenu.CHANNEL_BLOCK) {
                        val channelItems = mutableListOf<PrefItem<Any>>()
                        var index = 0
                        var title: String

                        data.channelList.forEach { tvChannel ->
                            // Checking if channel is visible in channel block list.
                                title = if (tvChannel.name.isNullOrEmpty()) {
                                    tvChannel.displayNumber
                                } else {
                                    "${tvChannel.displayNumber} ${tvChannel.name}"
                                }
                            if (isRef5Flavour) {
                                for (i in parentalListener!!.getLockedChannelIdsFromPrefs()) {
                                    if (i == tvChannel.getUniqueIdentifier()) {
                                        tvChannel.isLocked = true
                                    }
                                }
                            }
                                channelItems.add(
                                    PrefItem(
                                        ViewHolderType.VT_CHECKBOX,
                                        Pref.CHANNEL_BLOCK_CHECKBOX,
                                        CompoundItem(
                                            title,
                                            tvChannel.isLocked,
                                            index,
                                            prefCompoundListener,
                                            parentalListener!!.getChannelSourceType(tvChannel)
                                        ),
                                        componentListener
                                    )
                                )
                                index++
                            }

                        zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                            Pref.CHANNEL_BLOCK,
                            RootItem(itemTitle,  blockedChannelCount(isParentalControlEnabled), true, channelItems, object : InfoListener{
                                override fun getInfo(): String {
                                    var count =0
                                    channelItems.forEach {
                                        val item = it.data as CompoundItem
                                        if(item.isChecked && it.viewHolderType == ViewHolderType.VT_CHECKBOX)
                                            count++
                                    }
                                    return count.toString()
                                }
                            }),
                            componentListener
                        ))
                    }
                    else if (item.id == PrefSubMenu.INPUT_BLOCK) {
                        val inputBlockItems = mutableListOf<PrefItem<Any>>()

                        data.blockedInputs.forEachIndexed { index, input ->
                            inputBlockItems.add(PrefItem (
                                ViewHolderType.VT_CHECKBOX,
                                Pref.INPUT_BLOCK_CHECKBOX,
                                CompoundItem(input.inputSourceName, input.isBlocked, index, prefCompoundListener),
                                componentListener
                            ))
                        }

                        zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                            Pref.INPUT_BLOCK,
                            RootItem(itemTitle, data.blockedInputCount.toString(), true, inputBlockItems, object : InfoListener{
                                override fun getInfo(): String {
                                    var count =0
                                    inputBlockItems.forEach {
                                        val item = it.data as CompoundItem
                                        if(item.isChecked) count++
                                    }
                                    return count.toString()
                                }
                            }),
                            componentListener
                        ))
                    }
                    else if (item.id == PrefSubMenu.RATING_SYSTEMS) {
                        ratingItems.clear()
                        data.contentRatingData.forEachIndexed { index, it ->
                            ratingItems.add(PrefItem (
                                ViewHolderType.VT_CHECKBOX,
                                Pref.RATING_SYSTEMS_CHECKBOX,
                                CompoundItem(ratingSystemName(it), data.contentRatingsEnabled.contains(it), index, prefCompoundListener),
                                componentListener
                            ))
                        }

                        zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                            Pref.RATING_SYSTEMS,
                            RootItem(itemTitle,  data.contentRatingsEnabled.size.toString(), true, ratingItems, object : InfoListener{
                                override fun getInfo(): String {
                                    var count = 0
                                    ratingItems.forEach {
                                        val item = it.data as CompoundItem
                                        if(item.isChecked) count++
                                    }
                                    return count.toString()
                                }
                            }),
                            componentListener
                        ))
                    }
                    else if (item.id == PrefSubMenu.RATING_LOCK) {
                        ratingLockItem = PrefItem(ViewHolderType.VT_MENU,
                            Pref.RATING_LOCK,
                            RootItem(itemTitle, ConfigStringsManager.getStringById(information!!.globalRestrictionsArray[parentalListener!!.getContentRatingLevelIndex()]), true,
                                ratingLockItems),
                            componentListener
                        )

                        prepareRatingLockItems()
                        zerothColumn.add(ratingLockItem!!)
                    }
                    else if (item.id == PrefSubMenu.ANOKI_PARENTAL_RATING) {
                        var selected = try {
                            information!!.anokiRatingSystemArray[parentalListener!!.getAnokiRatingLevel()]
                        }catch (e:Exception){
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "refresh: exception = ${e.message}")
                            null
                        }
                        anokiRatingSystem = PrefItem(ViewHolderType.VT_MENU,
                            Pref.ANOKI_RATING_SYSTEM_RADIO,
                            RootItem(
                                ConfigStringsManager.getStringById("rating_systems"),
                                selected,
                                true,
                                anokiRatingSystemItems),
                            componentListener
                        )

                        prepareAnokiRatingSystem()
                        zerothColumn.add(anokiRatingSystem!!)
                    }
                    else if (item.id == PrefSubMenu.RRT5_LOCK) {
                        rrt5LockItem = PrefItem(ViewHolderType.VT_MENU,
                            Pref.RRT5_LOCK,
                            RootItem(itemTitle, null, true, rrt5LockItems),
                            componentListener
                        )
                        if (BuildConfig.FLAVOR == "rtk") {
                            prepareRRT5WithRegionSupportLockItems()
                        } else {
                            prepareRRT5LockItems()
                        }
                        zerothColumn.add(rrt5LockItem!!)
                    } else if (item.id == PrefSubMenu.CHANGE_PIN) {

                        zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                            Pref.CHANGE_PIN,
                            RootItem(itemTitle, null, false),
                            componentListener
                        ))
                    }

                }

                zerothColumn.forEach {
                    if(it.id != Pref.PARENTAL_CONTROL) (it.data as RootItem).isEnabled = isParentalControlEnabled
                    //if rating system list is empty then rating system will be disabled
                    if(it.id == Pref.ANOKI_RATING_SYSTEM_RADIO) {
                        (it.data as RootItem).isEnabled =
                            isParentalControlEnabled && it.data.itemList!!.isNotEmpty()
                    }
                }

                preferencesLeftOptionsAdapter = PreferenceSubMenuAdapter(
                    zerothColumn,
                    view!!,
                    object : PreferenceSubMenuAdapter.Listener {
                        override fun onBack(action: Action): Boolean {
                            if(action == Action.LEFT) return false
                            if(listener.isAccessibilityEnabled()) {
                                preferencesLeftOptionsAdapter!!.setFocusOfViewHolders(false)
                            }
                            parentalListener?.onPrefsCategoriesRequestFocus()
                            return true
                        }

                        override fun updateInfo() {
                            //ignore
                        }

                        override fun isAccessibilityEnabled(): Boolean {
                            return listener.isAccessibilityEnabled()
                        }

                        override fun updateHintText(status: Boolean, type: Int) {
                        }

                        override fun getPreferenceDeepLink(): ReferenceWidgetPreferences.DeepLink? {
                            return null
                        }

                        override fun onExit() {
                            TODO("Not yet implemented")
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            parentalListener!!.setSpeechText(text = text, importance = importance)
                        }

                        override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                            parentalListener!!.showToast(text, duration)
                        }

                        override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                            parentalListener!!.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                        }
                    }
                )
                preferencesLeftOptionsGridView!!.adapter = preferencesLeftOptionsAdapter
            }
        }
    }
    private fun prepareAnokiRatingSystem() {
        anokiRatingSystemItems.clear()
        information!!.anokiRatingSystemArray.forEachIndexed { index, item ->
            val radioItem : PrefItem<Any> = PrefItem(
                ViewHolderType.VT_RADIO,
                Pref.ANOKI_RATING_SYSTEM_RADIO,
                CompoundItem(
                    item,
                    parentalListener!!.getAnokiRatingLevel() == index,
                    index,
                    prefCompoundListener
                ),
                componentListener
            )
            anokiRatingSystemItems.add(radioItem)
        }
    }

    private fun prepareRatingLockItems() {
        ratingLockItems.clear()
        ratingLockCountryItems.clear()

        if(information!!.blockUnratedProgramsAvailability){
            ratingLockItems.add(PrefItem (
                ViewHolderType.VT_CHECKBOX,
                Pref.BLOCK_UNRATED_PROGRAMS_CHECKBOX,
                CompoundItem(ConfigStringsManager.getStringById("block_unrated_programs"), information!!.isBlockUnratedPrograms, 0, prefCompoundListener),
                componentListener
            ))
        }

        val globalRestrictionsItems = mutableListOf<PrefItem<Any>>()

        information!!.globalRestrictionsArray.forEachIndexed { index, it ->
            val radioItem : PrefItem<Any> = PrefItem(
                ViewHolderType.VT_RADIO,
                Pref.GLOBAL_RESTRICTIONS_RADIO,
                CompoundItem(
                    ConfigStringsManager.getStringById(it),
                    parentalListener!!.getContentRatingLevelIndex() == index,
                    index,
                    prefCompoundListener
                ),
                componentListener
            )
            globalRestrictionsItems.add(radioItem)
        }
        ratingLockGlobalRestrictions = PrefItem (
            ViewHolderType.VT_MENU,
            Pref.RATING_LOCK_GLOBAL_RESTRICTIONS,
            RootItem(ConfigStringsManager.getStringById("global_restrictions"),
                ConfigStringsManager.getStringById(information!!.globalRestrictionsArray[parentalListener!!.getContentRatingLevelIndex()]),
                true,
                globalRestrictionsItems),
            componentListener
        )
        ratingLockItems.add(ratingLockGlobalRestrictions!!)


        ratingItems.forEachIndexed { index, it->
            if((it.data as CompoundItem).isChecked){
                val contentRatingSystem = information!!.contentRatingData[index]
                val countryRatingItems = mutableListOf<PrefItem<Any>>()

                contentRatingSystem.ratings.forEach {

                    if( it.subRatings.isNotEmpty()){

                        val countrySubRatingItems = mutableListOf<PrefItem<Any>>()

                        val allRadioItem: PrefItem<Any> = PrefItem(
                            ViewHolderType.VT_CHECKBOX,
                            Pref.RATING_LOCK_GROUP_CHECKBOX_ALL,
                            CompoundItem(
                                it.title,
                                true,
                                index,
                                object : PrefCompoundListener{
                                    override fun onChange(
                                        data: Boolean,
                                        index: Int,
                                        id: Pref,
                                        callback: IAsyncDataCallback<Boolean>
                                    ) {

                                        if(parentalListener!!.setRatingBlocked(
                                                contentRatingSystem,
                                                it!!,
                                                data
                                            )){

                                            it.subRatings.forEach { subRating ->
                                                parentalListener!!.setSubRatingBlocked(
                                                    contentRatingSystem,
                                                    it, subRating, data
                                                )
                                            }
                                            parentalListener!!.setRelativeRatingsEnabled(
                                                contentRatingSystem, it, data
                                            )

                                            notifyParentalChanged()
                                        }
                                    }
                                }
                            ),
                            componentListener
                        )
                        countrySubRatingItems.add(allRadioItem)

                        it.subRatings.forEach { subRating->
                            val isEnabled = parentalListener!!.isSubRatingEnabled(contentRatingSystem, it,subRating )
                            if(!isEnabled) (allRadioItem.data as CompoundItem).isChecked = false
                            val radioItem: PrefItem<Any> = PrefItem(
                                ViewHolderType.VT_CHECKBOX,
                                Pref.RATING_LOCK_GROUP_CHECKBOX,
                                CompoundItem(
                                    ConfigStringsManager.getStringById(subRating.description),
                                    isEnabled,
                                    index,
                                    object : PrefCompoundListener{
                                        override fun onChange(
                                            data: Boolean,
                                            index: Int,
                                            id: Pref,
                                            callback: IAsyncDataCallback<Boolean>
                                        ) {
                                            parentalListener!!.setSubRatingBlocked(
                                                contentRatingSystem,
                                                it!!, subRating, data
                                            )

                                            parentalListener!!.setRelativeRating2SubRatingEnabled(
                                                contentRatingSystem,
                                                data, it, subRating
                                            )
                                            notifyParentalChanged()
                                        }
                                    }
                                ),
                                componentListener
                            )
                            countrySubRatingItems.add(radioItem)
                        }

                        countryRatingItems.add(PrefItem (
                            ViewHolderType.VT_MENU,
                            Pref.RATING_LOCK_GROUP,
                            RootItem(it.title,null,true,
                                countrySubRatingItems),
                            componentListener
                        ))
                    }else{
                        val radioItem : PrefItem<Any> = PrefItem(
                            ViewHolderType.VT_CHECKBOX,
                            Pref.RATING_LOCK_CHECKBOX,
                            CompoundItem(
                                ConfigStringsManager.getSystemLanguageString(it.title),
                                parentalListener!!.isRatingBlocked(contentRatingSystem,it),
                                index,
                                object : PrefCompoundListener{
                                    override fun onChange(
                                        data: Boolean,
                                        index: Int,
                                        id: Pref,
                                        callback: IAsyncDataCallback<Boolean>
                                    ) {
                                        if(parentalListener!!.setRatingBlocked(
                                                contentRatingSystem,
                                                it!!,
                                                data
                                            )){

                                            parentalListener!!.setRelativeRatingsEnabled(
                                                contentRatingSystem, it, data
                                            )

                                            notifyParentalChanged()
                                        }
                                    }
                                }
                            ),
                            componentListener
                        )
                        countryRatingItems.add(radioItem)
                    }
                }
                val ratingLockCountry = PrefItem (
                    ViewHolderType.VT_MENU,
                    Pref.RATING_LOCK_COUNTRY,
                    RootItem(ratingSystemName(contentRatingSystem),null,true,
                        countryRatingItems),
                    componentListener
                ) as PrefItem<Any>
                ratingLockCountryItems.add(ratingLockCountry)
            }
        }
        ratingLockItems.addAll(ratingLockCountryItems)

        prepareParentalInfo()
    }

    private fun notifyRRT5Changed() {
        prepareRRT5Info()
        preferencesLeftOptionsAdapter!!.notifyPreferenceUpdated()
    }

    private fun prepareRRT5LockItems() {
        rrt5LockItems.clear()

        parentalListener!!.getRRT5Regions().forEachIndexed { regionIndex, region->
            val dimItems = mutableListOf<PrefItem<Any>>()
            parentalListener!!.getRRT5DimInfo(regionIndex).forEachIndexed { dimIndex, dim ->

                val levelItems = mutableListOf<PrefItem<Any>>()

                parentalListener!!.getRRT5LevelInfo(regionIndex,dimIndex).forEachIndexed { levelIndex, level ->
                    val radioItem: PrefItem<Any> = PrefItem(
                        ViewHolderType.VT_CHECKBOX,
                        Pref.RRT5_LEVEL_CHECKBOX,
                        CompoundItem(
                            level,
                            false,
                            levelIndex,
                            object : PrefCompoundListener{
                                override fun onChange(
                                    data: Boolean,
                                    index: Int,
                                    id: Pref,
                                    callback: IAsyncDataCallback<Boolean>
                                ) {
                                    parentalListener!!.setSelectedItemsForRRT5Level(regionIndex,dimIndex,levelIndex)
                                    notifyRRT5Changed()
                                }
                            }
                        ), object : PrefItemListener{
                            override fun onAction(action: Action, id: Pref): Boolean {
                                if (action == Action.FOCUSED) {
                                    setDim(dimIndex)
                                    notifyRRT5Changed()
                                }
                                return false
                            }

                            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                                parentalListener!!.setSpeechText(text = text, importance = importance)
                            }

                            override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                                parentalListener!!.showToast(text, duration)
                            }
                        }
                    )
                    levelItems.add(radioItem)
                }

                dimItems.add(PrefItem (
                    ViewHolderType.VT_MENU,
                    Pref.RRT5_DIM,
                    RootItem(dim,null,true, levelItems),
                    object : PrefItemListener{
                        override fun onAction(action: Action, id: Pref): Boolean {
                            if (action == Action.FOCUSED) {
                                setRegion(regionIndex)
                                notifyRRT5Changed()
                            }
                            return false
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            parentalListener!!.setSpeechText(text = text, importance = importance)
                        }

                        override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                            parentalListener!!.showToast(text, duration)
                        }
                    }
                ))
            }
            val ratingLockCountry = PrefItem (
                ViewHolderType.VT_MENU,
                Pref.RRT5_REGION,
                RootItem(region,null,true, dimItems),
                object : PrefItemListener{
                    override fun onAction(action: Action, id: Pref): Boolean {
                        if (action == Action.FOCUSED) {
                            parentalListener!!.setPrefsValue("RRT5_REGION", regionIndex)
                            notifyRRT5Changed()
                        }
                        return false
                    }

                    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                        parentalListener!!.setSpeechText(text = text, importance = importance)
                    }

                    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                        parentalListener!!.showToast(text, duration)
                    }
                }
            ) as PrefItem<Any>
            rrt5LockItems.add(ratingLockCountry)
        }

        rrt5LockItems.add(PrefItem(ViewHolderType.VT_MENU,
            Pref.RRT5_RESET,
            RootItem(ConfigStringsManager.getStringById("rrt5_reset"), null, false),
            componentListener
        ))

        prepareRRT5Info()
    }

    private fun prepareRRT5WithRegionSupportLockItems() {
        rrt5LockItems.clear()
        crsNameMap.clear()
        ratingNameMap.clear()

        parentalListener!!.getRRT5Regions().forEachIndexed { regionIndex, region ->
            val dimItems = mutableListOf<PrefItem<Any>>()
            parentalListener!!.getRRT5CrsInfo(region)
                .forEachIndexed { dimIndex, contentRatingSystem ->
                    crsNameMap[contentRatingSystem.name] = contentRatingSystem
                    val levelItems = mutableListOf<PrefItem<Any>>()
                    val ratings = contentRatingSystem.ratings
                    ratings.forEachIndexed { levelIndex, level ->
                        if (!TextUtils.isEmpty(level.name.trim())) {
                            ratingNameMap[level.name] = level
                            val checkboxItem: PrefItem<Any> = PrefItem(
                                ViewHolderType.VT_CHECKBOX,
                                Pref.RRT5_LEVEL_CHECKBOX,
                                CompoundItem(
                                    level.name,
                                    parentalListener!!.isRatingBlocked(
                                        contentRatingSystem,
                                        level
                                    ),
                                    levelIndex,
                                    object : PrefCompoundListener {
                                        override fun onChange(
                                            data: Boolean,
                                            index: Int,
                                            id: Pref,
                                            callback: IAsyncDataCallback<Boolean>
                                        ) {
                                            parentalListener!!.setRatingBlocked(
                                                contentRatingSystem,
                                                level,
                                                data
                                            )
                                            parentalListener!!.setRelativeRatingsEnabled(
                                                contentRatingSystem, level, data
                                            )
                                            notifyRRT5WithRegionsChanged()
                                        }
                                    }
                                ), object : PrefItemListener {
                                    override fun onAction(action: Action, id: Pref): Boolean {
                                        if (action == Action.FOCUSED) {
                                            setDim(dimIndex)
                                            notifyRRT5WithRegionsChanged()
                                        }
                                        return false
                                    }

                                    override fun setSpeechText(
                                        vararg text: String,
                                        importance: SpeechText.Importance
                                    ) {
                                        parentalListener!!.setSpeechText(text = text, importance)
                                    }

                                    override fun showToast(
                                        text: String,
                                        duration: UtilsInterface.ToastDuration
                                    ) {
                                        parentalListener!!.showToast(text = text, duration)
                                    }
                                }
                            )
                            levelItems.add(checkboxItem)
                        }
                    }
                    dimItems.add(PrefItem(
                        ViewHolderType.VT_MENU,
                        Pref.RRT5_DIM,
                        RootItem(contentRatingSystem.name, null, true, levelItems),
                        object : PrefItemListener {
                            override fun onAction(action: Action, id: Pref): Boolean {
                                if (action == Action.FOCUSED) {
                                    setRegion(regionIndex)
                                    notifyRRT5WithRegionsChanged()
                                }
                                return false
                            }

                            override fun setSpeechText(
                                vararg text: String,
                                importance: SpeechText.Importance
                            ) {
                                parentalListener!!.setSpeechText(text = text, importance = importance)
                            }

                            override fun showToast(
                                text: String,
                                duration: UtilsInterface.ToastDuration
                            ) {
                                parentalListener!!.showToast(text, duration)
                            }
                        }
                    ))
                }
            val ratingLockCountry = PrefItem(
                ViewHolderType.VT_MENU,
                Pref.RRT5_REGION,
                RootItem(region, null, true, dimItems),
                object : PrefItemListener {
                    override fun onAction(action: Action, id: Pref): Boolean {
                        if (action == Action.FOCUSED) {
                            parentalListener!!.setPrefsValue("RRT5_REGION", regionIndex)
                            notifyRRT5WithRegionsChanged()
                        }
                        return false
                    }

                    override fun setSpeechText(
                        vararg text: String,
                        importance: SpeechText.Importance
                    ) {
                        parentalListener!!.setSpeechText(text = text, importance = importance)
                    }

                    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                        parentalListener!!.showToast(text, duration)
                    }
                }
            ) as PrefItem<Any>
            rrt5LockItems.add(ratingLockCountry)
        }
        rrt5LockItems.add(
            PrefItem(
                ViewHolderType.VT_MENU,
                Pref.RRT5_RESET,
                RootItem(ConfigStringsManager.getStringById("rrt5_reset"), null, false),
                componentListener
            )
        )
        prepareRRT5WithRegionsInfo()

    }

    private fun prepareRRT5WithRegionsInfo() {
        rrt5LockItems.forEachIndexed { regionIndex, regionItem ->
            val region = regionItem.data as RootItem
            if (region.itemList != null) {
                if (getRegin() == regionIndex || getRegin() == 0) (rrt5LockItem!!.data as RootItem).info =
                    region.name
                region.info = null
                region.itemList?.forEachIndexed { dimIndex, dimItem ->

                    val dim = dimItem.data as RootItem
                    if (getDim() == dimIndex) region.info = dim.name
                    dim.info = ""
                    var levelSeparator = ""
                    dim.itemList!!.forEachIndexed { levelIndex, levelItem ->
                        val level = levelItem.data as CompoundItem
                        level.isChecked = parentalListener!!.isRatingBlocked(
                            crsNameMap[dim.name] as ContentRatingSystem,
                            ratingNameMap[level.title]!!
                        )
                        if (level.isChecked) {
                            dim.info = dim.info + levelSeparator + level.title
                            levelSeparator = ", "
                        }

                    }
                    if (dim.info!!.isEmpty()) {
                        dim.info = ConfigStringsManager.getStringById("menu_arrays_None")
                    }

                }
            }

        }
    }

    private fun setDim(index:Int){
        parentalListener!!.setPrefsValue("RRT5_DIM", index)
    }

    private fun setRegion(index:Int){
        parentalListener!!.setPrefsValue("RRT5_REGION", index)
    }

    private fun getDim():Int{
        return parentalListener!!.getPrefsValue("RRT5_DIM", -1) as Int
    }

    private fun getRegin():Int{
        return parentalListener!!.getPrefsValue("RRT5_REGION", -1) as Int
    }

    private fun notifyParentalChanged() {
        prepareParentalInfo()
        preferencesLeftOptionsAdapter!!.notifyPreferenceUpdated()
    }

    private fun notifyRRT5WithRegionsChanged() {
        prepareRRT5WithRegionsInfo()
        preferencesLeftOptionsAdapter!!.notifyPreferenceUpdated()
    }

    private fun prepareParentalInfo() {
        val globalRatingItem = ratingLockGlobalRestrictions!!.data as RootItem
        globalRatingItem.itemList!!.forEachIndexed { index, prefItem ->
            val globalRestrictionOption = prefItem.data as CompoundItem

            globalRestrictionOption.isChecked = parentalListener!!.getContentRatingLevelIndex() ==index
            if(globalRestrictionOption.isChecked){
                (ratingLockItem!!.data as RootItem).info = globalRestrictionOption.title
                globalRatingItem.info = globalRestrictionOption.title
            }
        }

        information!!.contentRatingsEnabled.forEachIndexed { index, contentRatingSystem ->
            val countryItem = ratingLockCountryItems.get(index).data as RootItem
            countryItem.info = ""
            var countryItemSeparator = ""
            countryItem.itemList!!.forEachIndexed { index, it ->
                val rating  = contentRatingSystem.ratings[index]
                if(it.viewHolderType == ViewHolderType.VT_CHECKBOX){
                    val ratingItem = it.data as CompoundItem
                    ratingItem.isChecked = parentalListener!!.isRatingBlocked(contentRatingSystem,rating)

                    if(ratingItem.isChecked){
                        countryItem.info += ( countryItemSeparator +ratingItem.title)
                        countryItemSeparator = ", "
                    }
                }else{
                    val ratingGroupItem = it.data as RootItem

                    ratingGroupItem.info = ""
                    var ratingGroupSeparator = ""
                    val ratingItemAll = ratingGroupItem.itemList!![0].data as CompoundItem
                    ratingItemAll.isChecked = parentalListener!!.isRatingBlocked(contentRatingSystem,rating)
                    var partialChecked = false
                    ratingGroupItem.itemList.forEachIndexed { index, it  ->
                        if(index!=0){
                            val subRating  = rating.subRatings[index-1]
                            val subRatingItem = it.data as CompoundItem
                            subRatingItem.isChecked = parentalListener!!.isSubRatingEnabled(contentRatingSystem,rating,subRating)
                            if(subRatingItem.isChecked) {
                                ratingGroupItem.info += ( ratingGroupSeparator +subRatingItem.title)
                                ratingGroupSeparator = ", "
                                partialChecked = true
                            }
                        }
                    }
                    if(ratingItemAll.isChecked) {
                        ratingGroupItem.info = ratingItemAll.title + ratingGroupSeparator + ratingGroupItem.info
                    }else if(partialChecked){
                        ratingGroupItem.info = ConfigStringsManager.getStringById("rating_hint_partial") + ratingGroupSeparator + ratingGroupItem.info
                    }

                    if(ratingGroupItem.info!!.isEmpty()){
                        ratingGroupItem.info = ConfigStringsManager.getStringById("menu_arrays_None")
                    }

                    if(ratingItemAll.isChecked){
                        countryItem.info += (countryItemSeparator + ratingGroupItem.name)
                        countryItemSeparator = ", "
                    }
                }
            }
            if(countryItem.info!!.isEmpty()){
                countryItem.info = ConfigStringsManager.getStringById("menu_arrays_None")
            }
        }
    }

    private fun prepareRRT5Info() {

        rrt5LockItems.forEachIndexed { regionIndex, regionItem ->

            val region = regionItem.data as RootItem
            if(region.itemList!=null){

                if(getRegin() == regionIndex || getRegin() == 0) (rrt5LockItem!!.data as RootItem).info = region.name
                region.info = null
                region.itemList!!.forEachIndexed { dimIndex, dimItem ->

                    val dim = dimItem.data as RootItem
                    if(getDim() == dimIndex) region.info = dim.name

                    val selectedLevelMap = parentalListener!!.rrt5BlockedList(regionIndex,dimIndex)


                    dim.info = ""
                    var levelSeparator = ""

                    dim.itemList!!.forEachIndexed { levelIndex, levelItem ->

                        val level = levelItem.data as CompoundItem
                        level.isChecked = selectedLevelMap[levelIndex]==1

                        if(level.isChecked){
                            dim.info = dim.info + levelSeparator + level.title
                            levelSeparator = ", "
                        }

                    }

                    if(dim.info!!.isEmpty()){
                        dim.info = ConfigStringsManager.getStringById("menu_arrays_None")
                    }
                }
            }

        }
    }

    private fun showResetDialog() {
        ReferenceApplication.runOnUiThread(Runnable {
            val sceneData = DialogSceneData(ReferenceApplication.worldHandler!!.active!!.id, ReferenceApplication.worldHandler!!.active!!.instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("rrt5_reset")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {

                @SuppressLint("NotifyDataSetChanged")
                override fun onPositiveButtonClicked() {
                    parentalListener!!.resetRRT5()
                    listener.showToast(ConfigStringsManager.getStringById("rrt5_reset_message"))
                    ReferenceApplication.worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        SceneManager.Action.DESTROY
                    )
                    setDim(-1)
                    setRegion(-1)
                    // Refresh RRT5 Region List
                    if (BuildConfig.FLAVOR == "rtk") {
                        prepareRRT5WithRegionSupportLockItems()
                    } else {
                        prepareRRT5LockItems()
                    }
                    preferencesLeftOptionsAdapter!!.container!!.adapter!!.notifyDataSetChanged()
                }
                override fun onNegativeButtonClicked() {
                    ReferenceApplication.worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        SceneManager.Action.DESTROY
                    )
                }
            }
            ReferenceApplication.worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                SceneManager.Action.SHOW_OVERLAY, sceneData
            )
        })
    }

    private fun channelBlockCount(): Int {
        var count = 0
        information!!.channelList.forEach {
            if (it.isLocked) count++
        }
        return count
    }

    private fun ratingSystemName(it: ContentRatingSystem) : String{
        return it.displayName.replace("other_countries",ConfigStringsManager.getStringById("other_countries"))
            .replace("french",ConfigStringsManager.getStringById("french"))
            .replace("title_us_mv",ConfigStringsManager.getStringById("title_us_mv"))
            .replace("title_ca_tv_fr",ConfigStringsManager.getStringById("title_ca_tv_fr"))
    }

    fun setFocusToGrid() {
        if(listener.isAccessibilityEnabled()) {
            preferencesLeftOptionsAdapter!!.setFocusOfViewHolders(true)
        }
        preferencesLeftOptionsGridView!!.requestFocus()
    }

    interface PreferenceParentalListener : GWidgetListener, TTSSetterInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun isAccessibilityEnabled(): Boolean
        fun onPrefsCategoriesRequestFocus()
        fun getPrefsValue(key: String, value: Any?): Any?
        fun setPrefsValue(key: String, defValue: Any?)
        fun lockChannel(tvChannel: TvChannel, selected: Boolean)
        fun blockInput(inputSourceData: InputSourceData, blocked: Boolean)
        fun isParentalControlsEnabled(): Boolean
        fun setParentalControlsEnabled(enabled : Boolean): Boolean
        fun setContentRatingSystemEnabled(contentRatingSystem: ContentRatingSystem, enabled: Boolean)
        fun setContentRatingLevel(index: Int)
        fun getContentRatingLevelIndex(): Int
        fun setRatingBlocked(contentRatingSystem: ContentRatingSystem, it: ContentRatingSystem.Rating, data: Boolean): Boolean
        fun setRelativeRatingsEnabled(contentRatingSystem: ContentRatingSystem, it: ContentRatingSystem.Rating, data: Boolean)
        fun isRatingBlocked(contentRatingSystem: ContentRatingSystem,it: ContentRatingSystem.Rating): Boolean
        fun isSubRatingEnabled(contentRatingSystem: ContentRatingSystem, it: ContentRatingSystem.Rating, subRating: ContentRatingSystem.SubRating): Boolean
        fun setSubRatingBlocked(contentRatingSystem: ContentRatingSystem,rating: ContentRatingSystem.Rating,subRating: ContentRatingSystem.SubRating,data: Boolean)
        fun setRelativeRating2SubRatingEnabled(contentRatingSystem: ContentRatingSystem,data: Boolean,rating: ContentRatingSystem.Rating,subRating: ContentRatingSystem.SubRating)
        fun setBlockUnrated(blocked: Boolean)
        fun getRRT5DimInfo(index: Int): MutableList<String>
        fun getRRT5CrsInfo(regionName : String) : MutableList<ContentRatingSystem>
        fun getRRT5LevelInfo(countryIndex: Int, dimIndex: Int): MutableList<String>
        fun getSelectedItemsForRRT5Level(): HashMap<Int, Int>
        fun rrt5BlockedList(regionPosition :Int,position: Int): HashMap<Int, Int>
        fun setSelectedItemsForRRT5Level(regionIndex: Int, dimIndex: Int, levelIndex: Int)
        fun resetRRT5()
        fun getRRT5Regions(): MutableList<String>
        fun getChannelSourceType(tvChannel: TvChannel): String
        fun checkNetworkConnection(): Boolean
        fun getAnokiRatingLevel(): Int
        fun setAnokiRatingLevel(level: Int)
        fun startChangePinActivity()
        fun getLockedChannelIdsFromPrefs(): MutableSet<String>
    }
}