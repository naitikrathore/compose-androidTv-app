package com.iwedia.cltv.components

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.applicationContext
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider.Companion.getTypeFace
import com.iwedia.cltv.config.ConfigColorManager.Companion.getColor
import com.iwedia.cltv.config.ConfigFontManager.Companion.getFont
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.PreferencesTermsOfServiceInformation
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.utils.Utils
import world.widget.GWidget
import world.widget.GWidgetListener


class PreferencesTermsOfServiceWidget :
    GWidget<ConstraintLayout, PreferencesTermsOfServiceWidget.PreferenceTermsOfServiceListener> {

    var context: Context? = null
    private val categories = mutableListOf<PreferenceCategoryItem>()

    private var preferencesLeftOptionsGridView: VerticalGridView? = null
    private var preferencesLeftOptionsAdapter: PreferenceSubMenuAdapter? = null

    var termsOfServiceListener: PreferenceTermsOfServiceListener? = null

    var zerothColumn = mutableListOf<PrefItem<Any>>()

    var information : PreferencesTermsOfServiceInformation?= null

    constructor(
        context: Context,
        listener: PreferenceTermsOfServiceListener
    ) : super(
        ReferenceWorldHandler.WidgetId.TERMS_OF_SERVICE,
        ReferenceWorldHandler.WidgetId.TERMS_OF_SERVICE,
        listener
    ) {
        this.context = context

        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.preferences_terms_of_service_layout, null) as ConstraintLayout

        val descriptionTextView : TextView = view!!.findViewById(R.id.descriptionTextView)
        descriptionTextView.text = ConfigStringsManager.getStringById("terms_of_service_description")
        descriptionTextView.setTypeface(getTypeFace(applicationContext(), getFont("font_medium")))
        descriptionTextView.setTextColor(Color.parseColor(getColor("color_main_text")))

        preferencesLeftOptionsGridView = view!!.findViewById(R.id.preferences_terms_of_service_left_options)
        preferencesLeftOptionsGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferencesLeftOptionsGridView!!.windowAlignmentOffset = Utils.getDimensInPixelSize(R.dimen.custom_dim_25)
        preferencesLeftOptionsGridView!!.setNumColumns(1)
        termsOfServiceListener = listener

    }

    private val itemListener = object : PrefItemListener {
        override fun onAction(action: Action, id: Pref): Boolean {
            if (action==Action.CLICK){
                when (id) {
                    Pref.PRIVACY_POLICY -> {
                        listener.onClickPrivacyPolicy()
                        return true
                    }
                    Pref.TERMS_OF_SERVICE -> {
                        listener.onClickTermsOfService()
                        return true
                    }
                    else -> {}
                }
            }
            return false
        }

        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
            listener.setSpeechText(text = text,importance = importance)
        }

        override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
            listener.showToast(text, duration)
        }
    }

    override fun refresh(data: Any) {
        if (data is PreferencesTermsOfServiceInformation) {
            information = data

            zerothColumn.clear()

            if (data.subCategories.isNotEmpty()) {
                categories.clear()
                data.subCategories.forEach { item ->
                    val itemTitle = item.name

                    if (item.id == PrefSubMenu.PRIVACY_POLICY) {
                        zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                            Pref.PRIVACY_POLICY,
                            RootItem(itemTitle, null, false),
                            itemListener
                        ))
                    }
                    if (item.id == PrefSubMenu.TERMS_OF_SERVICE) {
                        zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                            Pref.TERMS_OF_SERVICE,
                            RootItem(itemTitle, null, false),
                            itemListener
                        ))
                    }
                }

                preferencesLeftOptionsAdapter = PreferenceSubMenuAdapter(
                    zerothColumn,
                    view!!,
                    object : PreferenceSubMenuAdapter.Listener {
                        override fun onBack(action: Action): Boolean {
                            if(action == Action.LEFT) return false
                            termsOfServiceListener?.onPrefsCategoriesRequestFocus()
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
                            listener.setSpeechText(text = text,importance = importance)
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

    interface PreferenceTermsOfServiceListener : GWidgetListener, TTSSetterInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun onPrefsCategoriesRequestFocus()
        fun onClickTermsOfService()
        fun onClickPrivacyPolicy()
    }
}