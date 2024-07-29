import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.*
import com.iwedia.cltv.entities.PreferencesTeletextInformation
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

class PreferencesTeletextWidget :
    GWidget<ConstraintLayout, PreferencesTeletextWidget.PreferencesTeletextListener> {

    var context: Context? = null
    private val categories = mutableListOf<PreferenceCategoryItem>()

    private var preferencesLeftOptionsGridView: VerticalGridView? = null
    private var preferencesLeftOptionsAdapter: PreferenceSubMenuAdapter? = null

    var txtListener : PreferencesTeletextListener? = null

    constructor(
        context: Context,
        listener: PreferencesTeletextListener
    ) : super(
        ReferenceWorldHandler.WidgetId.PREFERENCES_TELETEXT,
        ReferenceWorldHandler.WidgetId.PREFERENCES_TELETEXT,
        listener
    ) {
        this.context = context

        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.layout_widget_preferences_txt, null) as ConstraintLayout
        preferencesLeftOptionsGridView = view!!.findViewById(R.id.preferences_left_options)

        //setup adapters
        preferencesLeftOptionsGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferencesLeftOptionsGridView!!.windowAlignmentOffset = Utils.getDimensInPixelSize(R.dimen.custom_dim_25)
        preferencesLeftOptionsGridView!!.setNumColumns(1)
        txtListener = listener

    }

    override fun refresh(data: Any) {

        if (data is PreferencesTeletextInformation) {

            val listener = object : PrefItemListener {
                override fun onAction(action: Action, id: Pref): Boolean {
                    if (action== Action.CLICK){

                    }
                    return false
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    (txtListener as PreferencesTeletextListener).setSpeechText(text = text, importance = importance)
                }

                override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                    (txtListener as PreferencesTeletextListener).showToast(text, duration)
                }
            }

            val prefCompoundListener = object : PrefCompoundListener {
                override fun onChange(
                    isSelected: Boolean,
                    index: Int,
                    id: Pref,
                    callback: IAsyncDataCallback<Boolean>
                ) {
                    if (id== Pref.DIGITAL_LANGUAGE_RADIO){
                        data.digitalLanguages?.get(index)?.let {
                            (txtListener as PreferencesTeletextListener).onTTXDigitalLanguageSelected(
                                index, it.languageCodeISO6392)
                        }
                    }
                    if (id== Pref.PREFERED_TELETEXT_LANGUAGE_RADIO){

                    }
                    if (id== Pref.DECODING_PAGE_LANGUAGE_RADIO){
                        (txtListener as PreferencesTeletextListener).onTTXDecodeLanguageSelected(index)

                    }
                    callback.onReceive(isSelected)
                }
            }

            var zerothColumn = mutableListOf<PrefItem<Any>>()

            if (data.subCategories!!.isNotEmpty()) {
                categories.clear()
                data.subCategories!!.forEach { categoryOption ->
                    val itemTitle = categoryOption.name

                    if (categoryOption.id == PrefSubMenu.DIGITAL_LANGUAGE) {
                        var digitalLangOptions = mutableListOf<PrefItem<Any>>()
                        var itemSubtitle = ""

                        data.digitalLanguages?.forEachIndexed { index, language ->
                            val radioItem = PrefItem (
                                ViewHolderType.VT_RADIO,
                            Pref.DIGITAL_LANGUAGE_RADIO,
                                CompoundItem(language.englishName, language.languageCodeISO6392==data.defaultDigitalLang, index, prefCompoundListener),
                                listener
                            )as PrefItem<Any>
                            digitalLangOptions.add(radioItem)
                            if (language.languageCodeISO6392==data.defaultDigitalLang) {
                                itemSubtitle = language.englishName
                            }
                        }

                        zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                            Pref.DIGITAL_LANGUAGE,
                            RootItem(
                                itemTitle,itemSubtitle , true,
                                digitalLangOptions,   object : InfoListener{
                                    override fun getInfo(): String? {
                                        digitalLangOptions.forEach {
                                            val compoundItem = it.data as CompoundItem
                                            if(compoundItem.isChecked) return compoundItem.title
                                        }
                                        return null
                                    }
                                }
                            ),
                            listener
                        ))
                    }

                    if (categoryOption.id == PrefSubMenu.DECODING_PAGE_LANGUAGE) {
                        var decodingLngOptions = mutableListOf<PrefItem<Any>>()
                        data.decodingPageLanguages?.forEachIndexed { index, language ->
                            val radioItem = PrefItem (
                                ViewHolderType.VT_RADIO,
                                Pref.DECODING_PAGE_LANGUAGE_RADIO,
                                CompoundItem(language, language== data.decodingPageLanguages!![data.defaultDecodingPageLang!!], index, prefCompoundListener),
                                listener
                            )as PrefItem<Any>
                            decodingLngOptions.add(radioItem)
                        }
                        var itemSubtitle = if (!data.decodingPageLanguages.isNullOrEmpty()) data.decodingPageLanguages!![data.defaultDecodingPageLang!!] else ""

                        zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                            Pref.DECODING_PAGE_LANGUAGE,
                            RootItem(
                                itemTitle,itemSubtitle , true,
                                decodingLngOptions,   object : InfoListener{
                                    override fun getInfo(): String? {
                                        decodingLngOptions.forEach {
                                            val compoundItem = it.data as CompoundItem
                                            if(compoundItem.isChecked) return compoundItem.title
                                        }
                                        return null
                                    }
                                }
                            ),
                            listener
                        ))
                    }

                    if (categoryOption.id == PrefSubMenu.PREFERED_TELETEXT_LANGUAGE) {
                        var preferredTeletextOptions = mutableListOf<PrefItem<Any>>()
                        data.digitalLanguages?.forEachIndexed { index, language ->
                            val radioItem = PrefItem (
                                ViewHolderType.VT_RADIO,
                                Pref.PREFERED_TELETEXT_LANGUAGE_RADIO,
                                CompoundItem(language.englishName, language.englishName==data.defaultPreferredLang, index, prefCompoundListener),
                                listener
                            )as PrefItem<Any>
                            preferredTeletextOptions.add(radioItem)
                        }
                        var itemSubtitle =data.defaultPreferredLang

                        zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                            Pref.PREFERED_TELETEXT_LANGUAGE,
                            RootItem(
                                itemTitle,itemSubtitle , true,
                                preferredTeletextOptions,   object : InfoListener{
                                    override fun getInfo(): String? {
                                        preferredTeletextOptions.forEach {
                                            val compoundItem = it.data as CompoundItem
                                            if(compoundItem.isChecked) return compoundItem.title
                                        }
                                        return null
                                    }
                                }
                            ),
                            listener
                        ))
                    }

                    preferencesLeftOptionsAdapter = PreferenceSubMenuAdapter(
                        zerothColumn,
                        view!!,
                        object : PreferenceSubMenuAdapter.Listener {
                            override fun onBack(action: Action): Boolean {

                                if(action == Action.LEFT) return false
                                txtListener?.onPrefsCategoriesRequestFocus()
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
                                (txtListener as PreferencesTeletextListener).setSpeechText(text = text, importance = importance)
                            }

                            override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                                (txtListener as PreferencesTeletextListener).showToast(text, duration)
                            }

                            override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                                (txtListener as PreferencesTeletextListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                            }
                        }
                    )
                    preferencesLeftOptionsGridView!!.adapter = preferencesLeftOptionsAdapter
                }
            }

        }
    }

    fun setFocusToGrid() {
        preferencesLeftOptionsGridView!!.requestFocus()
    }

    interface PreferencesTeletextListener : GWidgetListener, TTSSetterInterface, ToastInterface, TTSSetterForSelectableViewInterface {
        fun onPrefsCategoriesRequestFocus()
        fun onTTXDigitalLanguageSelected(position: Int, language: String)
        fun onTTXDecodeLanguageSelected(position: Int)
    }
}