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
import com.iwedia.cltv.entities.PreferencesAdsTargetingInformation
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.utils.Utils
import world.widget.GWidget
import world.widget.GWidgetListener


class PreferencesAdsTargetingWidget :
    GWidget<ConstraintLayout, PreferencesAdsTargetingWidget.PreferenceAdsTargetingListener> {

    var context: Context? = null
    private val categories = mutableListOf<PreferenceCategoryItem>()

    private var preferencesLeftOptionsGridView: VerticalGridView? = null
    private var preferencesLeftOptionsAdapter: PreferenceSubMenuAdapter? = null

    var adsTargetingListener: PreferenceAdsTargetingListener? = null

    var zerothColumn = mutableListOf<PrefItem<Any>>()

    var information : PreferencesAdsTargetingInformation?= null

    constructor(
        context: Context,
        listener: PreferenceAdsTargetingListener
    ) : super(
        ReferenceWorldHandler.WidgetId.PREFERENCES_ADS_TARGETING,
        ReferenceWorldHandler.WidgetId.PREFERENCES_ADS_TARGETING,
        listener
    ) {
        this.context = context

        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.preferences_parental_layout, null) as ConstraintLayout

        preferencesLeftOptionsGridView = view!!.findViewById(R.id.preferences_parental_left_options)
        preferencesLeftOptionsGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferencesLeftOptionsGridView!!.windowAlignmentOffset = Utils.getDimensInPixelSize(R.dimen.custom_dim_25)
        preferencesLeftOptionsGridView!!.setNumColumns(1)
        adsTargetingListener = listener

    }

    val componentListener = object : PrefItemListener {
        override fun onAction(action: Action, id: Pref): Boolean {
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

            when (id) {
                Pref.ADS_TARGETING_SWITCH->{
                    listener.onAdsTargetingChange(isSelected)
                }
                else -> {

                }
            }
        }
    }

    override fun refresh(data: Any) {
        if (data is PreferencesAdsTargetingInformation) {
            information = data

            zerothColumn.clear()

            if (data.subCategories!!.isNotEmpty()) {
                categories.clear()
                val switchStatus = information!!.isAdsTargetingEnabled
                data.subCategories!!.forEach { item ->
                    val itemTitle = item.name

                    if (item.id == PrefSubMenu.ADS_TARGETING) {
                        val status = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")

                        val compoundItem = CompoundItem(status, switchStatus, 0,prefCompoundListener)

                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.ADS_TARGETING,
                                RootItem(
                                    itemTitle, status, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.ADS_TARGETING_SWITCH,
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

                    if(!information!!.isInternetConnectionAvailable){
                        zerothColumn.forEach {
                            (it.data as RootItem).isEnabled = false
                        }
                    }

                }

                preferencesLeftOptionsAdapter = PreferenceSubMenuAdapter(
                    zerothColumn,
                    view!!,
                    object : PreferenceSubMenuAdapter.Listener {
                        override fun onBack(action: Action): Boolean {
                            if(action == Action.LEFT) return false
                            adsTargetingListener?.onPrefsCategoriesRequestFocus()
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
                            listener.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                        }
                    })
                preferencesLeftOptionsGridView!!.adapter = preferencesLeftOptionsAdapter
            }
        }
    }

    fun setFocusToGrid() {
        preferencesLeftOptionsGridView!!.requestFocus()
    }

    interface PreferenceAdsTargetingListener : GWidgetListener, TTSSetterInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun onPrefsCategoriesRequestFocus()
        fun onAdsTargetingChange(enable: Boolean)
    }
}