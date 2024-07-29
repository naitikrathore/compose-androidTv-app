package com.iwedia.cltv.components

import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.entities.PreferencesHbbTVInfromation
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.utils.Utils
import world.SceneManager
import world.widget.GWidget
import world.widget.GWidgetListener

/**
 * Preferences hbb tv widget
 *
 * @author Gaurav Jain
 */

class PreferencesHbbTvWidget :
    GWidget<ConstraintLayout, PreferencesHbbTvWidget.PreferencesHbbListener> {

    var context: Context? = null
    private val categories = mutableListOf<PreferenceCategoryItem>()

    private var preferencesLeftOptionsGridView: VerticalGridView? = null
    private var preferencesLeftOptionsAdapter: PreferenceSubMenuAdapter? = null

    var hbbtvListener : PreferencesHbbListener? = null

    constructor(
        context: Context,
        listener: PreferencesHbbListener
    ) : super(
        ReferenceWorldHandler.WidgetId.PREFERENCES_HBBTV_SETTINGS,
        ReferenceWorldHandler.WidgetId.PREFERENCES_HBBTV_SETTINGS,
        listener
    ) {
        this.context = context

        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.layout_widget_preferences_hbbtv, null) as ConstraintLayout
        preferencesLeftOptionsGridView = view!!.findViewById(R.id.preferences_left_options)

        //setup adapters
        preferencesLeftOptionsGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferencesLeftOptionsGridView!!.windowAlignmentOffset = Utils.getDimensInPixelSize(R.dimen.custom_dim_25)
        preferencesLeftOptionsGridView!!.setNumColumns(1)
        hbbtvListener = listener

    }

    override fun refresh(data: Any) {

        if (data is PreferencesHbbTVInfromation) {

            val listener = object : PrefItemListener {
                override fun onAction(action: Action,id:Pref): Boolean {
                    if (action==Action.CLICK){
                        if(id == Pref.RESET_DEVICE_ID){
                            showResetDeviceIdConfirmationDialog()
                            return true
                        }
                    }
                    return false
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

                override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                    listener.showToast(text, duration)
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
                    if(id == Pref.HBBTV_SUPPORT_SWITCH){
                        hbbtvListener?.onHbbSupportSwitch(isSelected)
                    }
                    else if(id == Pref.TRACK_SWITCH){
                        hbbtvListener?.onHbbTrackSwitch(isSelected)
                    }
                    else if(id == Pref.COOKIE_SETTING_RADIO){
                        hbbtvListener?.onHbbTvCookieOptionSelected(index)
                    }
                    else if(id == Pref.PERSISTENT_STORAGE_SWITCH){
                        hbbtvListener?.onHbbPersistentStorageSwitch(isSelected)
                    }
                    else if(id == Pref.BLOCK_TRACKING_SITES_SWITCH){
                        hbbtvListener?.onHbbBlockTracking(isSelected)
                    }
                    else if(id == Pref.DEVICE_ID_SWITCH){
                        hbbtvListener?.onHbbTvDeviceId(isSelected)
                    }

                }
            }

            var zerothColumn = mutableListOf<PrefItem<Any>>()

            if (data.subCategories!!.isNotEmpty()) {
                categories.clear()
                data.subCategories!!.forEach { item ->
                    val itemTitle = item.name
                    val id = item.id
                    if( id == PrefSubMenu.HBBTV_SUPPORT){
                        val switchStatus = data.isHbbTvSupport
                        val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                        val compoundItem = CompoundItem(status, switchStatus, 0,prefCompoundListener)
                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.HBBTV_SUPPORT,
                                RootItem(
                                    itemTitle, status, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.HBBTV_SUPPORT_SWITCH,
                                        compoundItem,
                                        listener
                                    )),object : InfoListener{
                                        override fun getInfo(): String? {
                                            return ConfigStringsManager.getStringById(if(compoundItem.isChecked) "on" else "off")
                                        }
                                    }
                                ),
                                listener
                            )

                        )
                    }
                    else if( id == PrefSubMenu.TRACK){
                        val switchStatus = data.isTrack
                        val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                        val compoundItem = CompoundItem(status, switchStatus, 0,prefCompoundListener)
                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.TRACK,
                                RootItem(
                                    itemTitle, status, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.TRACK_SWITCH,
                                        compoundItem,
                                        listener
                                    )),object : InfoListener{
                                        override fun getInfo(): String? {
                                            return ConfigStringsManager.getStringById(if(compoundItem.isChecked) "on" else "off")
                                        }
                                    }
                                ),
                                listener
                            )

                        )
                    }
                    else if( id == PrefSubMenu.COOKIE_SETTING){
                        var cookieSettingItems =mutableListOf<PrefItem<Any>>()
                        data.cookieSettingsStrings!!.forEachIndexed { index, it ->
                            cookieSettingItems.add(PrefItem (
                                ViewHolderType.VT_RADIO,
                                Pref.COOKIE_SETTING_RADIO,
                                CompoundItem(it, index==data.cookieSettingsSelected, index, prefCompoundListener),
                                listener))
                        }

                        zerothColumn.add(
                            PrefItem(
                                ViewHolderType.VT_MENU,
                                Pref.COOKIE_SETTING,
                                RootItem(
                                    itemTitle,
                                    data.cookieSettingsStrings!![data.cookieSettingsSelected],
                                    true,
                                    cookieSettingItems,
                                    object : InfoListener{
                                        override fun getInfo(): String? {
                                            cookieSettingItems.forEach {
                                                val compoundItem = it.data as CompoundItem
                                                if(compoundItem.isChecked) return compoundItem.title
                                            }
                                            return null
                                        }
                                    }
                                ),
                                listener)
                        )
                    }
                    else if( id == PrefSubMenu.PERSISTENT_STORAGE){
                        val switchStatus = data.isPresistentStorage
                        val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                        val compoundItem = CompoundItem(status, switchStatus, 0,prefCompoundListener)
                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.PERSISTENT_STORAGE,
                                RootItem(
                                    itemTitle, status, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.PERSISTENT_STORAGE_SWITCH,
                                        compoundItem,
                                        listener
                                    )),object : InfoListener{
                                        override fun getInfo(): String? {
                                            return ConfigStringsManager.getStringById(if(compoundItem.isChecked) "on" else "off")
                                        }
                                    }
                                ),
                                listener
                            )

                        )
                    }
                    else if( id == PrefSubMenu.BLOCK_TRACKING_SITES){
                        val switchStatus = data.isBlockTrackingSites
                        val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                        val compoundItem = CompoundItem(status, switchStatus, 0,prefCompoundListener)
                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.BLOCK_TRACKING_SITES,
                                RootItem(
                                    itemTitle, status, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.BLOCK_TRACKING_SITES_SWITCH,
                                        compoundItem,
                                        listener
                                    )),object : InfoListener{
                                        override fun getInfo(): String? {
                                            return ConfigStringsManager.getStringById(if(compoundItem.isChecked) "on" else "off")
                                        }
                                    }
                                ),
                                listener
                            )

                        )
                    }
                    else if( id == PrefSubMenu.DEVICE_ID){
                        val switchStatus = data.isDeviceId
                        val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                        val compoundItem = CompoundItem(status, switchStatus, 0,prefCompoundListener)
                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.DEVICE_ID,
                                RootItem(
                                    itemTitle, status, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.DEVICE_ID_SWITCH,
                                        compoundItem,
                                        listener
                                    )),object : InfoListener{
                                        override fun getInfo(): String? {
                                            return ConfigStringsManager.getStringById(if(compoundItem.isChecked) "on" else "off")
                                        }
                                    }
                                ),
                                listener
                            )
                        )
                    }
                    else if( id == PrefSubMenu.RESET_DEVICE_ID){
                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.RESET_DEVICE_ID,
                                RootItem(
                                    itemTitle, null, false
                                ),
                                listener
                            )
                        )
                    }
                    preferencesLeftOptionsAdapter = PreferenceSubMenuAdapter(
                        zerothColumn,
                        view!!,
                        object : PreferenceSubMenuAdapter.Listener {
                            override fun onBack(action: Action): Boolean {

                                if(action == Action.LEFT) return false
                                hbbtvListener?.onPrefsCategoriesRequestFocus()
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
                                return null
                            }

                            override fun onExit() {
                                TODO("Not yet implemented")
                            }

                            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                                listener.setSpeechText(text = text, importance = importance)
                            }

                            override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                                listener.showToast(text, duration)
                            }

                            override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                                hbbtvListener!!.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                            }
                        })
                    preferencesLeftOptionsGridView!!.adapter = preferencesLeftOptionsAdapter
                }
            }

        }
    }

    fun setFocusToGrid() {
        preferencesLeftOptionsGridView!!.requestFocus()
    }
    private fun showResetDeviceIdConfirmationDialog(){
        ReferenceApplication.runOnUiThread {
            var currentActiveScene = ReferenceApplication.worldHandler!!.active
            var sceneData = DialogSceneData(currentActiveScene!!.id, currentActiveScene.instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("reset_device_id_confirmation")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("yes")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("no")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                    ReferenceApplication.worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        SceneManager.Action.DESTROY
                    )
                }

                override fun onPositiveButtonClicked() {
                    hbbtvListener?.onHbbTvResetDeviceId()
                    ReferenceApplication.worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        SceneManager.Action.DESTROY
                    )
                }
            }
            ReferenceApplication.worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                SceneManager.Action.SHOW, sceneData
            )
        }
    }

    interface PreferencesHbbListener : GWidgetListener, TTSSetterInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun onPrefsCategoriesRequestFocus()
        fun onHbbSupportSwitch(isEnabled: Boolean)
        fun onHbbTrackSwitch(isEnabled: Boolean)
        fun onHbbPersistentStorageSwitch(isEnabled: Boolean)
        fun onHbbBlockTracking(isEnabled: Boolean)
        fun onHbbTvDeviceId(isEnabled: Boolean)
        fun onHbbTvCookieOptionSelected(position: Int)
        fun onHbbTvResetDeviceId()
    }
}