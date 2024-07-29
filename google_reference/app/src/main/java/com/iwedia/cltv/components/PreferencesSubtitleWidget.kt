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
import com.iwedia.cltv.entities.PreferenceSubtitleInformation
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.language.LanguageCode
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.utils.Utils
import world.widget.GWidget
import world.widget.GWidgetListener


class PreferencesSubtitleWidget :
    GWidget<ConstraintLayout, PreferencesSubtitleWidget.PreferencesSubtitleListener> {

    var context: Context? = null
    private val categories = mutableListOf<PreferenceCategoryItem>()
    var zerothColumn = mutableListOf<PrefItem<Any>>()

    private var preferencesLeftOptionsGridView: VerticalGridView? = null
    private var preferencesLeftOptionsAdapter: PreferenceSubMenuAdapter? = null

    var subtitleListener: PreferencesSubtitleListener? = null


    constructor(
        context: Context,
        listener: PreferencesSubtitleListener
    ) : super(
        ReferenceWorldHandler.WidgetId.PREFERENCES_SUBTITLES,
        ReferenceWorldHandler.WidgetId.PREFERENCES_SUBTITLES,
        listener
    ) {

        this.context = context

        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.preferences_audio_layout, null) as ConstraintLayout
        preferencesLeftOptionsGridView = view!!.findViewById(R.id.preferences_audio_left_options)

        preferencesLeftOptionsGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferencesLeftOptionsGridView!!.windowAlignmentOffset = Utils.getDimensInPixelSize(R.dimen.custom_dim_25)
        preferencesLeftOptionsGridView!!.setNumColumns(1)
        subtitleListener = listener

    }

    override fun refresh(data: Any) {
        if (data is PreferenceSubtitleInformation) {

            //Default values
            val isSubtitleEnabled = subtitleListener!!.isDigitalSubtitleEnabled()
            val firstLanguage = subtitleListener!!.getFirstSubtitleLanguage()
            val secondLanguage = subtitleListener!!.getSecondSubtitleLanguage()
            val isSubtitleAnalogEnabled = subtitleListener!!.isAnalogSubtitleEnabled()

            val listener = object : PrefItemListener {
                override fun onAction(action: Action, id: Pref): Boolean {
                    if (action == Action.CLICK) { }
                    return false
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    subtitleListener!!.setSpeechText(text = text, importance = importance)
                }

                override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                    subtitleListener!!.showToast(text, duration)
                }
            }

            val prefCompoundListener = object : PrefCompoundListener {
                override fun onChange(
                    isSelected: Boolean,
                    index: Int,
                    id: Pref,
                    callback: IAsyncDataCallback<Boolean>
                ) {
                    when (id) {
                        Pref.GENERAL_SWITCH -> {
                            subtitleListener?.onSubtitlesEnabledClicked(isSelected)
                        }
                        Pref.GENERAL_RADIO -> {
                            subtitleListener?.onSubtitlesTypeClicked(index)
                        }
                        Pref.ANALOG_SUBTITLE_SWITCH -> {
                            subtitleListener?.onSubtitlesAnalogTypeClicked(index)
                        }
                        Pref.DIGITAL_SUBTITLE_RADIO -> {
                            subtitleListener?.onSubtitlesTypeClicked(index)
                            zerothColumn.forEach {
                                if((it.id != Pref.DIGITAL_SUBTITLE) && (it.id != Pref.ANALOG_SUBTITLE)) {
                                    (it.data as RootItem).isEnabled = (index != 0)
                                    preferencesLeftOptionsAdapter!!.notifyItemChanged(zerothColumn.indexOf(it))
                                }
                            }
                        }
                        Pref.FIRST_LANGUAGE_RADIO -> {
                            subtitleListener?.onFirstSubtitleLanguageSelected(data.availableSubtitleLanguages!![index])
                        }
                        Pref.SECOND_LANGUAGE_RADIO -> {
                            subtitleListener?.onSecondSubtitleLanguageSelected(data.availableSubtitleLanguages!![index])
                        }
                        Pref.SUBTITLE_TYPE_RADIO ->{

                        }
                        else -> {

                        }
                    }

                    callback.onReceive(isSelected)
                }
            }

            if (data.subCategories!!.isNotEmpty()) {
                categories.clear()

                var isDigitalSubtitleMenuAvailable = false

                data.subCategories.forEach { item ->
                    val itemTitle = item.name


                    if (item.id == PrefSubMenu.ANALOG_SUBTITLE){
                        //TODO: PREFERENCE implement analog subtitle
                        val subTitle = ConfigStringsManager.getStringById(if(isSubtitleAnalogEnabled) "on" else "off")
                        val compoundItem = CompoundItem(subTitle, isSubtitleAnalogEnabled, 0,prefCompoundListener)
                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.ANALOG_SUBTITLE,
                                RootItem(
                                    itemTitle, subTitle, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.ANALOG_SUBTITLE_SWITCH,
                                        compoundItem,
                                        listener
                                    )),object : InfoListener{
                                        override fun getInfo(): String? {
                                            return ConfigStringsManager.getStringById(if(isSubtitleAnalogEnabled) "on" else "off")
                                        }
                                    }
                                ),
                                listener
                            )
                        )

                    }
                    if (item.id == PrefSubMenu.DIGITAL_SUBTITLE){

                        isDigitalSubtitleMenuAvailable = true

                        val digitalSubtitleMenus = mutableListOf<PrefItem<Any>>()

                        data.subtitleTypeStrings?.forEachIndexed { index, title ->
                            val radioItem : PrefItem<Any> = PrefItem(
                                ViewHolderType.VT_RADIO,
                                Pref.DIGITAL_SUBTITLE_RADIO,
                                CompoundItem(
                                    title,
                                    title==data.subtitleTypeStrings!![data.subtitleType],
                                    index,
                                    prefCompoundListener
                                ),
                                listener
                            )
                            digitalSubtitleMenus.add(radioItem)
                        }

                        var itemSubtitle = data.subtitleTypeStrings!![data.subtitleType]
                        zerothColumn.add(
                            PrefItem(
                                ViewHolderType.VT_MENU,
                                Pref.DIGITAL_SUBTITLE,
                                RootItem(
                                    itemTitle, itemSubtitle, true,
                                    digitalSubtitleMenus,
                                    object :InfoListener{
                                        override fun getInfo(): String? {
                                            digitalSubtitleMenus.forEach {
                                                val compoundItem = it.data as CompoundItem
                                                if(compoundItem.isChecked) return compoundItem.title
                                            }
                                            return null
                                        }
                                    }
                                ),
                                listener
                            )
                        )

                    }

                    if (item.id == PrefSubMenu.ENABLE_SUBTITLES){
                        var switchStatus = true
                        val subTitle = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")

                        val compoundItem = CompoundItem(subTitle, switchStatus, 0,prefCompoundListener)
                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.ENABLE_SUBTITLES,
                                RootItem(
                                    itemTitle, subTitle, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.ENABLE_SUBTITLES_SWITCH,
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
                    else if (item.id == PrefSubMenu.SUBTITLE_TYPE) {
                        var subtitleTypeItems = mutableListOf<PrefItem<Any>>()
                        data.subtitleTypeStrings?.forEachIndexed { index, subtitleType ->
                            val radioItem = PrefItem(
                                ViewHolderType.VT_RADIO,
                                Pref.SUBTITLE_TYPE_RADIO,
                                CompoundItem(
                                    subtitleType,
                                    subtitleType == data.subtitleTypeStrings!![data.subtitleType],
                                    index,
                                    prefCompoundListener
                                ),
                                listener
                            ) as PrefItem<Any>

                            subtitleTypeItems.add(radioItem)

                        }

                        var itemSubtitle =data.subtitleTypeStrings!![data.subtitleType]
                        zerothColumn.add(
                            PrefItem(
                                ViewHolderType.VT_MENU,
                                Pref.SUBTITLE_TYPE,
                                RootItem(
                                    itemTitle, itemSubtitle, true,
                                    subtitleTypeItems,object :InfoListener{
                                        override fun getInfo(): String? {
                                            val item = subtitleTypeItems[0].data as CompoundItem
                                            subtitleTypeItems.forEach {
                                                val item = it.data as CompoundItem
                                                if(item.isChecked && it.viewHolderType == ViewHolderType.VT_RADIO)
                                                    return it.data.title
                                            }
                                            return (subtitleTypeItems[0].data as CompoundItem ).title
                                        }
                                    }
                                ),
                                listener
                            )
                        )
                    }
                   else if (item.id == PrefSubMenu.GENERAL) {
                        val switchStatus = isSubtitleEnabled
                        val status =
                            ConfigStringsManager.getStringById(if (switchStatus) "on" else "off")
                        var generalItems = mutableListOf<PrefItem<Any>>()
                        val switchItem = PrefItem(
                            ViewHolderType.VT_SWITCH,
                            Pref.GENERAL_SWITCH,
                            CompoundItem(status, switchStatus, 0, prefCompoundListener),
                            listener
                        )
                        generalItems.add(switchItem as PrefItem<Any>)
                        data.subtitleTypeStrings?.forEachIndexed { index, subtitleType ->
                            val radioItem = PrefItem(
                                ViewHolderType.VT_RADIO,
                                Pref.GENERAL_RADIO,
                                CompoundItem(
                                    subtitleType,
                                    subtitleType == data.subtitleTypeStrings!![data.subtitleType],
                                    index,
                                    prefCompoundListener
                                ),
                                listener
                            ) as PrefItem<Any>

                            if (switchStatus) {
                                generalItems.add(radioItem)
                            } else {
                                switchItem.data.hiddenOptions.add(radioItem)
                            }
                        }

                        val itemSubtitle =
                            if (switchStatus && data.subtitleTypeStrings!!.isNotEmpty()) data.subtitleTypeStrings!![data.subtitleType] else status
                        zerothColumn.add(
                            PrefItem(
                                ViewHolderType.VT_MENU,
                                Pref.GENERAL,
                                RootItem(
                                    itemTitle, itemSubtitle, true,
                                    generalItems,object :InfoListener{
                                        override fun getInfo(): String? {
                                            val item = generalItems[0].data as CompoundItem
                                            if(!item.isChecked) return ConfigStringsManager.getStringById(  "off")
                                            generalItems.forEach {
                                                val item = it.data as CompoundItem
                                                if(item.isChecked && it.viewHolderType == ViewHolderType.VT_RADIO)
                                                    return it.data.title
                                            }
                                            return ConfigStringsManager.getStringById(  "on")
                                        }
                                    }
                                ),
                                listener
                            )
                        )
                    } else if (item.id == PrefSubMenu.FIRST_LANGUAGE) {
                        var firstLanguageOptions = mutableListOf<PrefItem<Any>>()
                        data.availableSubtitleLanguages?.forEachIndexed { index, track ->
                            if(!track.hideInSubtitlePref) {
                                val radioItem: PrefItem<Any> = PrefItem(
                                    ViewHolderType.VT_RADIO,
                                    Pref.FIRST_LANGUAGE_RADIO,
                                    CompoundItem(
                                        track.englishName,
                                        (track.languageCodeISO6392 == firstLanguage?.languageCodeISO6392),
                                        index,
                                        prefCompoundListener
                                    ),
                                    listener
                                )
                                firstLanguageOptions.add(radioItem)
                            }
                        }

                        var itemSubtitle =
                            if (firstLanguage?.englishName != null) ConfigStringsManager.getStringById(
                                firstLanguage.englishName.lowercase()
                            ) else null
                        zerothColumn.add(
                            PrefItem(
                                ViewHolderType.VT_MENU,
                                Pref.FIRST_LANGUAGE,
                                RootItem(
                                    itemTitle, itemSubtitle, true,
                                    firstLanguageOptions,
                                    object :InfoListener{
                                        override fun getInfo(): String? {
                                            firstLanguageOptions.forEach {
                                                val compoundItem = it.data as CompoundItem
                                                if(compoundItem.isChecked) return compoundItem.title
                                            }
                                            return null
                                        }
                                    }
                                ),
                                listener
                            )
                        )
                    } else if (item.id == PrefSubMenu.SECOND_LANGUAGE) {
                        val secondLanguageOptions = mutableListOf<PrefItem<Any>>()
                        data.availableSubtitleLanguages?.forEachIndexed { index, track ->
                            if(!track.hideInSubtitlePref) {
                                val radioItem: PrefItem<Any> = PrefItem(
                                    ViewHolderType.VT_RADIO,
                                    Pref.SECOND_LANGUAGE_RADIO,
                                    CompoundItem(
                                        track.englishName,
                                        (track.languageCodeISO6392 == secondLanguage?.languageCodeISO6392),
                                        index,
                                        prefCompoundListener
                                    ),
                                    listener
                                )
                                secondLanguageOptions.add(radioItem)
                            }
                        }

                        val itemSubtitle =
                            if (secondLanguage?.englishName != null) ConfigStringsManager.getStringById(
                                secondLanguage.englishName.lowercase()
                            ) else null
                        zerothColumn.add(
                            PrefItem(
                                ViewHolderType.VT_MENU,
                                Pref.SECOND_LANGUAGE,
                                RootItem(
                                    itemTitle, itemSubtitle, true,
                                    secondLanguageOptions,
                                    object :InfoListener{
                                        override fun getInfo(): String? {
                                            secondLanguageOptions.forEach {
                                                val compoundItem = it.data as CompoundItem
                                                if(compoundItem.isChecked) return compoundItem.title
                                            }
                                            return null
                                        }
                                    }
                                ),
                                listener
                            )
                        )
                    }
                }

                if(isDigitalSubtitleMenuAvailable){
                    zerothColumn.forEach {
                        if((it.id != Pref.DIGITAL_SUBTITLE)) {
                            (it.data as RootItem).isEnabled = isSubtitleEnabled
                        }

                        if (it.id == Pref.ANALOG_SUBTITLE) {
                            (it.data as RootItem).isEnabled = true
                        }
                    }
                }

                preferencesLeftOptionsAdapter = PreferenceSubMenuAdapter(
                    zerothColumn,
                    view!!,
                    object : PreferenceSubMenuAdapter.Listener {
                        override fun onBack(action: Action): Boolean {
                            if(action == Action.LEFT) return false
                            subtitleListener?.onPrefsCategoriesRequestFocus()
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
                            subtitleListener!!.setSpeechText(
                                text = text, importance = importance
                            )
                        }

                        override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                            subtitleListener!!.showToast(text, duration)
                        }

                        override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                            subtitleListener!!.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                        }
                    }
                )
                preferencesLeftOptionsGridView!!.adapter = preferencesLeftOptionsAdapter
            }
        }

    }

    fun setFocusToGrid() {
        preferencesLeftOptionsGridView!!.requestFocus()
    }

    interface PreferencesSubtitleListener : GWidgetListener, TTSSetterInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun isAnalogSubtitleEnabled(): Boolean
        fun isDigitalSubtitleEnabled(): Boolean
        fun onPrefsCategoriesRequestFocus()
        fun getFirstSubtitleLanguage(): LanguageCode?
        fun onFirstSubtitleLanguageSelected(languageCode: LanguageCode)
        fun getSecondSubtitleLanguage(): LanguageCode?
        fun onSecondSubtitleLanguageSelected(languageCode: LanguageCode)
        fun onSubtitlesEnabledClicked(isEnabled: Boolean)
        fun onSubtitlesTypeClicked(position: Int)
        fun onSubtitlesAnalogTypeClicked(position: Int)
        fun onClosedCaptionChanged(isCaptionEnabled: Boolean)
    }
}