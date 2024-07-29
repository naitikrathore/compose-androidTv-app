package com.iwedia.cltv.components

import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.*
import com.iwedia.cltv.entities.PreferencesCamInfoInformation
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.utils.Utils
import world.widget.GWidget
import world.widget.GWidgetListener

/**
 * Preferences Cam Info Widget
 *
 * @author Dejan Nadj
 */
class PreferencesCamInfoWidget :
    GWidget<ConstraintLayout, PreferencesCamInfoWidget.PreferencesCamInfoWidgetListener> {

    val TAG = javaClass.simpleName

    var adapter = PreferencesSubcategoryAdapter()
    var list = mutableListOf<PreferenceCategoryItem>()

    private val categories = mutableListOf<PreferenceCategoryItem>()
    private var preferencesLeftOptionsGridView: VerticalGridView? = null
    private var preferencesLeftOptionsAdapter: PreferenceSubMenuAdapter? = null
    var camListener : PreferencesCamInfoWidgetListener? = null

    private val MENU_CI_USER_PREFERENCE = "menu_ci_user_preference"
    private val MENU_CI_USER_PREFERENCE_DEFAULT_ID = "menu_ci_user_preference_default_id"
    private val MENU_CI_USER_PREFERENCE_AMMI_ID = "menu_ci_user_preference_ammi_id"
    private val MENU_CI_USER_PREFERENCE_BROADCAST_ID = "menu_ci_user_preference_broadcast_id"

    private val MENU_CAM_TYPE_PREFERENCE = "menu_cam_type_preference"
    private val MENU_CAM_TYPE_PREFERENCE_PCMCIA_ID = "menu_cam_type_preference_pcmcia_id"
    private val MENU_CAM_TYPE_PREFERENCE_USB_ID = "menu_cam_type_preference_usb_id"

    constructor(
        listener: PreferencesCamInfoWidgetListener
    ) : super(
        ReferenceWorldHandler.WidgetId.PREFERENCES_CAM_INFO,
        ReferenceWorldHandler.WidgetId.PREFERENCES_CAM_INFO,
        listener
    ) {
        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.layout_preferences_cam_info, null) as ConstraintLayout
        preferencesLeftOptionsGridView = view!!.findViewById(R.id.preferences_left_options)

        //setup adapters
        preferencesLeftOptionsGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferencesLeftOptionsGridView!!.windowAlignmentOffset = Utils.getDimensInPixelSize(R.dimen.custom_dim_25)
        preferencesLeftOptionsGridView!!.setNumColumns(1)
        camListener = listener
    }

    fun setFocusToGrid() {
        preferencesLeftOptionsGridView!!.requestFocus()
    }

    override fun refresh(data: Any) {
        super.refresh(data)
        if(data is PreferencesCamInfoInformation){
            val listener = object : PrefItemListener {
                override fun onAction(action: Action, id: Pref): Boolean {
                    if (action== Action.CLICK){
                        if(id == Pref.CAM_SUB_MENU){
                            camListener?.enterMMI()
                            return true
                        }
                        else if(id == Pref.CAM_PIN){
                            camListener?.changeCamPin()
                            return true
                        }
                        else if(id == Pref.CAM_SCAN){
                            camListener?.startCamScan()
                            return true
                        }
                         if(id == Pref.CAM_OPERATOR){
                             data.camOperatorNameStrings?.get(0).let {
                                 data.camOperatorNameStrings?.get(0)?.let { name ->
                                     camListener?.deleteProfile(
                                         name
                                     )
                                 }
                             }
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
                    if(id == Pref.CAM_MENU) {
                        //listener.getCiPlusInterface().selectMenuItem(position)
                    }else if(id == Pref.USER_PREFERENCE_RADIO){
                        when(index){
                            0->{
                                camListener?.storePrefsValue(MENU_CI_USER_PREFERENCE,MENU_CI_USER_PREFERENCE_DEFAULT_ID)
                            }
                            1->{
                                camListener?.storePrefsValue(MENU_CI_USER_PREFERENCE,MENU_CI_USER_PREFERENCE_AMMI_ID)
                            }
                            2->{
                                camListener?.storePrefsValue(MENU_CI_USER_PREFERENCE,MENU_CI_USER_PREFERENCE_BROADCAST_ID)
                            }
                        }
                    }
                    else if(id == Pref.CAM_TYPE_PREFERENCE_RADIO){
                        when(index){
                            0->{
                                camListener?.storePrefsValue(MENU_CAM_TYPE_PREFERENCE,MENU_CAM_TYPE_PREFERENCE_PCMCIA_ID)
                                camListener?.doCAMReconfiguration(CamTypePreference.PCMCIA)
                            }
                            1->{
                                camListener?.storePrefsValue(MENU_CAM_TYPE_PREFERENCE,MENU_CAM_TYPE_PREFERENCE_USB_ID)
                                camListener?.doCAMReconfiguration(CamTypePreference.USB)
                            }
                        }
                    }
                }
            }


            var zerothColumn = mutableListOf<PrefItem<Any>>()

            if (data.subCategories!!.isNotEmpty()) {
                categories.clear()
                data.subCategories!!.forEach { categoryOption ->
                    val itemTitle = categoryOption.name

                    when (categoryOption.id) {
                        PrefSubMenu.CAM_MENU -> {
                            var camMenuPreferenceItems =mutableListOf<PrefItem<Any>>()
                            data.camMenuPreferenceStrings!!.forEachIndexed { index, it ->
                                camMenuPreferenceItems.add(PrefItem (
                                    ViewHolderType.VT_MENU,
                                    Pref.CAM_SUB_MENU,
                                    RootItem(
                                        it, null, false
                                    ),
                                    listener))
                            }
                            if(data.camMenuPreferenceStrings!!.isNotEmpty()){
                                zerothColumn.add(
                                    PrefItem(
                                        ViewHolderType.VT_MENU,
                                        Pref.CAM_MENU,
                                        RootItem(itemTitle,
                                            null,
                                            true,
                                            camMenuPreferenceItems,
                                            null
                                        ),
                                        listener)
                                )
                            }else{

                                zerothColumn.add(
                                    PrefItem(
                                        ViewHolderType.VT_MENU,
                                        Pref.CAM_MENU,
                                        RootItem(itemTitle,
                                            null,
                                            false
                                        ),
                                        listener)
                                )
                            }
                        }
                        PrefSubMenu.USER_PREFERENCE -> {

                            var userPreferenceItems =mutableListOf<PrefItem<Any>>()
                            data.userPreferenceStrings!!.forEachIndexed { index, it ->
                                userPreferenceItems.add(PrefItem (
                                    ViewHolderType.VT_RADIO,
                                    Pref.USER_PREFERENCE_RADIO,
                                    CompoundItem(it, index==data.userPreferenceSelected, index, prefCompoundListener),
                                    listener))
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.USER_PREFERENCE,
                                    RootItem(
                                        itemTitle,
                                        data.userPreferenceStrings!![data.userPreferenceSelected],
                                        true,
                                        userPreferenceItems,
                                        object : InfoListener{
                                            override fun getInfo(): String? {
                                                userPreferenceItems.forEach {
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
                        PrefSubMenu.CAM_TYPE_PREFERENCE -> {
                            var camTypePreferenceItems =mutableListOf<PrefItem<Any>>()
                            data.camTypePreferenceStrings!!.forEachIndexed { index, it ->
                                camTypePreferenceItems.add(PrefItem (
                                    ViewHolderType.VT_RADIO,
                                    Pref.CAM_TYPE_PREFERENCE_RADIO,
                                    CompoundItem(it, index==data.camTypePreferenceSelected, index, prefCompoundListener),
                                    listener))
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.CAM_TYPE_PREFERENCE,
                                    RootItem(
                                        itemTitle,
                                        data.camTypePreferenceStrings!![data.camTypePreferenceSelected],
                                        true,
                                        camTypePreferenceItems,
                                        object : InfoListener{
                                            override fun getInfo(): String? {
                                                camTypePreferenceItems.forEach {
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
                        PrefSubMenu.CAM_PIN -> {
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.CAM_PIN,
                                    RootItem(
                                        itemTitle, null, false
                                    ),
                                    listener
                                )
                            )
                        }
                        PrefSubMenu.CAM_SCAN -> {
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.CAM_SCAN,
                                    RootItem(
                                        itemTitle, null, false
                                    ),
                                    listener
                                )
                            )
                        }
                        PrefSubMenu.CAM_OPERATOR -> {
                            var camOperatorPreferenceItems =mutableListOf<PrefItem<Any>>()
                            data.camOperatorNameStrings!!.forEachIndexed { index, it ->
                                data.camOperatorNameStrings!!.forEachIndexed { index, it ->
                                    camOperatorPreferenceItems.add(PrefItem (
                                        ViewHolderType.VT_MENU,
                                        Pref.CAM_OPERATOR,
                                        RootItem(
                                            it, null, false
                                        ),
                                        listener))
                                }
                            }
                            if(data.camOperatorNameStrings!!.isNotEmpty()){
                                zerothColumn.add(
                                    PrefItem(
                                        ViewHolderType.VT_MENU,
                                        Pref.CAM_OPERATOR_MENU,
                                        RootItem(itemTitle,
                                            null,
                                            true,
                                            camOperatorPreferenceItems,
                                            null
                                        ),
                                        listener)
                                )
                            }else{
                                zerothColumn.add(
                                    PrefItem(
                                        ViewHolderType.VT_MENU,
                                        Pref.CAM_OPERATOR,
                                        RootItem(itemTitle,
                                            null,
                                            false
                                        ),
                                        listener)
                                )
                            }
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
                            camListener?.onPrefsCategoriesRequestFocus(3)
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
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            camListener!!.setSpeechText(text = text, importance = importance)
                        }

                        override fun showToast(
                            text: String,
                            duration: UtilsInterface.ToastDuration
                        ) {
                            camListener!!.showToast(text, duration)
                        }

                        override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                            camListener!!.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                        }
                    }
                )
                preferencesLeftOptionsGridView!!.adapter = preferencesLeftOptionsAdapter
            }
        }


    }


    fun requestFocus() {
        //this might be needed
//        if (leftOptionsGridView!!.getChildAt(leftOptionsAdapter.selectedItem) != null) {
//            leftOptionsAdapter.requestFocus(leftOptionsAdapter.selectedItem)
//            leftOptionsGridView!!.getChildAt(leftOptionsAdapter.selectedItem).requestFocus()
//        }
    }

    interface PreferencesCamInfoWidgetListener : GWidgetListener, TTSSetterInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun getModuleInfoData()
        fun requestFocusOnPreferencesMenu()
        fun onSoftwareDownloadPressed()
        fun onSubscriptionStatusPressed()
        fun onEventStatusPressed()
        fun onTokenStatusPressed()
        fun onChangeCaPinPressed()
        fun onConaxCaMessagesPressed()
        fun onAboutConaxCaPressed()
        fun getMaturityRating(): String
        fun activatePopUpMessage(activate: Boolean)
        fun getSettingsLanguageList()
        fun onSettingsLanguageSelected(position: Int)
        fun isPopUpMessagesActivated(): Boolean
        fun onPrefsCategoriesRequestFocus(position: Int)
        fun getCiPlusInterface(): CiPlusInterface
        fun storePrefsValue(tag: String, defValue: String)
        fun getPrefsValue(tag: String, defValue: String): String
        fun startCamScan()
        fun changeCamPin()
        fun selectMenuItem(position: Int)
        fun enterMMI()
        fun deleteProfile(profileName: String)
        fun doCAMReconfiguration(camTypePreference: CamTypePreference)
    }
}