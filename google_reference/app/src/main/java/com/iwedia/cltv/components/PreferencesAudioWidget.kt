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
import com.iwedia.cltv.entities.PreferenceAudioInformation
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


class PreferencesAudioWidget :
    GWidget<ConstraintLayout, PreferencesAudioWidget.PreferencesAudioListener> {

    var context: Context? = null
    private val categories = mutableListOf<PreferenceCategoryItem>()

    private var preferencesLeftOptionsGridView: VerticalGridView? = null
    private var preferencesLeftOptionsAdapter: PreferenceSubMenuAdapter? = null

    var audioListener : PreferencesAudioListener? = null

    constructor(
        context: Context,
        listener: PreferencesAudioListener
    ) : super(
        ReferenceWorldHandler.WidgetId.PREFERENCES_AUDIO,
        ReferenceWorldHandler.WidgetId.PREFERENCES_AUDIO,
        listener
    ) {

        this.context = context

        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.preferences_audio_layout, null) as ConstraintLayout
        preferencesLeftOptionsGridView = view!!.findViewById(R.id.preferences_audio_left_options)

        //setup adapters
        preferencesLeftOptionsGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferencesLeftOptionsGridView!!.windowAlignmentOffset = Utils.getDimensInPixelSize(R.dimen.custom_dim_25)
        preferencesLeftOptionsGridView!!.setNumColumns(1)
        audioListener = listener

    }

    override fun refresh(data: Any) {

        if (data is PreferenceAudioInformation) {

            // Default value
            val firstLanguage = audioListener!!.getFirstAudioLanguage()
            val secondLanguage = audioListener!!.getSecondAudioLanguage()

            val listener = object : PrefItemListener {
                override fun onAction(action: Action,id:Pref): Boolean {
                    if (action==Action.CLICK){

                    }
                    if (action==Action.FOCUSED){
                        if(id == Pref.AUDIO_FOR_VISUALLY_IMPAIRED) {
                            if (data.audioForVisuallyImp!!.isEmpty()) {
                                showToast(ConfigStringsManager.getStringById("no_audio_visually_impaired"))
                            }
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
            var zerothColumn = mutableListOf<PrefItem<Any>>()
            var visualImpaired :PrefItem<Any>? = null

            val prefCompoundListener = object : PrefCompoundListener {
                override fun onChange(
                    isSelected: Boolean,
                    index: Int,
                    id: Pref,
                    callback: IAsyncDataCallback<Boolean>
                ) {
                    if (id==Pref.AUDIO_TYPE_RADIO){
                        audioListener?.onAudioTypeClicked(index)
                        if(index != 1){
                            zerothColumn.toList().forEach{ item ->
                                if (item.id==Pref.VISUALLY_IMPAIRED){
                                    preferencesLeftOptionsAdapter!!.notifyItemRemoved(zerothColumn.indexOf(item))
                                    zerothColumn.remove(item)
                                }
                            }
                        }else if(!zerothColumn.contains(visualImpaired)){
                                zerothColumn.add(visualImpaired!!)
                                preferencesLeftOptionsAdapter!!.notifyItemInserted(zerothColumn.lastIndex)

                        }
                    }
                    else if(id==Pref.FIRST_LANGUAGE_RADIO){
                        audioListener?.onFirstAudioLanguageSelected(data.availableAudioLanguages!![index])
                    }
                    else if(id==Pref.SECOND_LANGUAGE_RADIO){
                        audioListener?.onSecondAudioLanguageSelected(data.availableAudioLanguages!![index])
                    }
                    else if (id==Pref.AUDIO_DESCRIPTION_KONKA_SWITCH){
                    }
                    else if(id== Pref.FADER_CONTROL_RADIO) {
                        audioListener?.onFaderValueChanged(index)
                    }
                    else if (id == Pref.AUDIO_FOR_VISUALLY_IMPAIRED_RADIO) {
                        audioListener?.onAudioViValueChanged(index)
                    }
                    else if (id == Pref.SPEAKER_SWITCH) {
                        audioListener?.onVisuallyImpairedValueChanged(1, isSelected)
                    }
                    else if (id == Pref.HEADPHONE_SWITCH) {
                        audioListener?.onVisuallyImpairedValueChanged(2, isSelected)
                    }
                    else if (id == Pref.PANE_FADE_SWITCH) {
                        audioListener?.onVisuallyImpairedValueChanged(4, isSelected)
                    }
                    else if (id == Pref.AUDIO_FORMAT_RADIO){ }
                    else if (id == Pref.AUDIO_DESCRIPTION_SWITCH){
                        if (!isSelected){
                            audioListener?.setAudioDescriptionEnabled(false)
                            zerothColumn.toList().forEach{ item ->
                                if (item.id==Pref.VISUALLY_IMPAIRED){
                                    preferencesLeftOptionsAdapter!!.notifyItemRemoved(zerothColumn.indexOf(item))
                                    zerothColumn.remove(item)
                                }
                            }
                        }
                        else{
                            zerothColumn.toList().forEach{ item ->
                                if (item.id==Pref.HEARING_IMPAIRED){
                                    audioListener?.setHearingImpairedEnabled(false)
                                     (item.data as RootItem).itemList?.forEach {
                                         if (it.id==Pref.HEARING_IMPAIRED_SWITCH){
                                             (it.data as CompoundItem).isChecked = false
                                         }
                                     }
                                    (item.data).info = ConfigStringsManager.getStringById("off" )
                                    preferencesLeftOptionsAdapter?.notifyItemChanged(zerothColumn.indexOf(item))

                                }
                            }
                            audioListener?.setAudioDescriptionEnabled(true)
                            zerothColumn.add(visualImpaired!!)
                            preferencesLeftOptionsAdapter!!.notifyItemInserted(zerothColumn.lastIndex)
                        }
                    }
                    else if (id == Pref.VISUALLY_IMPAIRED_SWITCH){ }
                    else if (id == Pref.HEARING_IMPAIRED_SWITCH){
                        if (!isSelected){
                            audioListener?.setHearingImpairedEnabled(false)
                        }else{
                            zerothColumn.toList().forEach { item ->
                                if (item.id == Pref.AUDIO_DESCRIPTION) {
                                    audioListener?.setAudioDescriptionEnabled(false)
                                    (item.data as RootItem).itemList?.forEach {
                                        if (it.id == Pref.AUDIO_DESCRIPTION_SWITCH) {
                                            (it.data as CompoundItem).isChecked =false
                                        }
                                    }
                                    (item.data).info =
                                        ConfigStringsManager.getStringById( "off")
                                    preferencesLeftOptionsAdapter?.notifyItemChanged(
                                        zerothColumn.indexOf(
                                            item
                                        )
                                    )
                                }
                                if (item.id == Pref.VISUALLY_IMPAIRED) {
                                    preferencesLeftOptionsAdapter!!.notifyItemRemoved(
                                        zerothColumn.indexOf(
                                            item
                                        )
                                    )
                                    zerothColumn.remove(item)
                                }
                            }
                            audioListener?.setHearingImpairedEnabled(true)
                        }

                    }
                    callback.onReceive(isSelected)
                }
            }


            if (data.subCategories.isNotEmpty()) {
                categories.clear()
                data.subCategories.forEach { item ->
                    val itemTitle = item.name

                    if (item.id == PrefSubMenu.AUDIO_TYPE) {
                        val audioTypeOptions = mutableListOf<PrefItem<Any>>()
                        val list = mutableListOf(ConfigStringsManager.getStringById( "normal"),
                            ConfigStringsManager.getStringById( "audio_description"),
                            ConfigStringsManager.getStringById( "spoken_subtitile"),
                            ConfigStringsManager.getStringById("subtitle_type_hearing_impaired"),
                            ConfigStringsManager.getStringById("audio_descreiption_and_sop_sub"))

                        list.forEachIndexed { index, title ->
                            val radioItem : PrefItem<Any> = PrefItem(
                                ViewHolderType.VT_RADIO,
                                Pref.AUDIO_TYPE_RADIO,
                                CompoundItem(
                                    title,
                                    index==data.audioTypeSelected,
                                    index,
                                    prefCompoundListener
                                ),
                                listener
                            )
                            audioTypeOptions.add(radioItem)
                        }

                        zerothColumn.add(
                            PrefItem(
                                ViewHolderType.VT_MENU,
                                Pref.AUDIO_TYPE,
                                RootItem(
                                    itemTitle, list[data.audioTypeSelected!!], true,
                                    audioTypeOptions,
                                    object :InfoListener{
                                        override fun getInfo(): String? {
                                            audioTypeOptions.forEach {
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

                    if (item.id == PrefSubMenu.FIRST_LANGUAGE) {
                            var firstLanguageOptions = mutableListOf<PrefItem<Any>>()
                            data.availableAudioLanguages?.forEachIndexed { index, language ->
                                if(!language.hideInAudioPref) {
                                    val radioItem: PrefItem<Any> = PrefItem(
                                        ViewHolderType.VT_RADIO,
                                        Pref.FIRST_LANGUAGE_RADIO,
                                        CompoundItem(
                                            ConfigStringsManager.getStringById(language?.englishName!!.lowercase()),
                                            (language.languageCodeISO6392 == firstLanguage?.languageCodeISO6392),
                                            index,
                                            prefCompoundListener
                                        ),
                                        listener
                                    )
                                    firstLanguageOptions.add(radioItem)
                                }
                            }

                            var itemSubtitle = if (firstLanguage?.englishName!=null)ConfigStringsManager.getStringById(firstLanguage?.englishName!!.lowercase())  else null
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
                    }
                    else if (item.id == PrefSubMenu.SECOND_LANGUAGE) {
                            var secondLanguageOptions = mutableListOf<PrefItem<Any>>()
                            data.availableAudioLanguages?.forEachIndexed { index, language ->
                                if(!language.hideInAudioPref) {
                                    val radioItem: PrefItem<Any> = PrefItem(
                                        ViewHolderType.VT_RADIO,
                                        Pref.SECOND_LANGUAGE_RADIO,
                                        CompoundItem(
                                            ConfigStringsManager.getStringById(language?.englishName!!.lowercase()),
                                            (language.languageCodeISO6392 == secondLanguage?.languageCodeISO6392),
                                            index,
                                            prefCompoundListener
                                        ),
                                        listener
                                    )
                                    secondLanguageOptions.add(radioItem)
                                }
                            }

                            var itemSubtitle = if (secondLanguage?.englishName!=null)ConfigStringsManager.getStringById(secondLanguage?.englishName!!.lowercase())  else null
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
                    else if (item.id == PrefSubMenu.AUDIO_DESCRIPTION) {
                        var switchStatus = data.isAudioDescriptionEnabled
                        val subTitle = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                        val compoundItem = CompoundItem(subTitle, switchStatus, 0,prefCompoundListener)
                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.AUDIO_DESCRIPTION,
                                RootItem(
                                    itemTitle, subTitle, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.AUDIO_DESCRIPTION_SWITCH,
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

                    else if (item.id == PrefSubMenu.VISUALLY_IMPAIRED || item.id == PrefSubMenu.VISUALLY_IMPAIRED_MINIMAL) {

                        var switchStatus = true
                        val audioForVisuallyImpairedTitle = ConfigStringsManager.getStringById( "vi_audio_for_impaired")
                        val faderControlTitle = ConfigStringsManager.getStringById( "vi_fader_control")
                        val subTitle = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                        val speakerTitle = ConfigStringsManager.getStringById( "vi_speaker")
                        val headphoneTitle = ConfigStringsManager.getStringById( "vi_headphone")
                        val paneAndFade = ConfigStringsManager.getStringById( "vi_pane_fade")
                        val volumeTitle = ConfigStringsManager.getStringById( "vi_volume")
                        val compoundItem = CompoundItem(subTitle, switchStatus, 0,prefCompoundListener)
                        val speakerSwitch = CompoundItem(speakerTitle, data.viSpeakerStatus, 1,prefCompoundListener)
                        val headphoneSwitch = CompoundItem(headphoneTitle, data.viHeadPhoneStatus, 2,prefCompoundListener)
                        val paneAndFadeSwitch = CompoundItem(paneAndFade, data.viPaneFadeStatus, 3,prefCompoundListener)
                        val volumeSeekBar = SeekBarItem(volumeTitle, data.viVolume, object :PrefSeekBarListener{
                            override fun onChange(
                                data: Int,
                                id: Pref,
                                callback: IAsyncDataCallback<Int>
                            ) {
                                audioListener?.onVolumeChanged(data)
                                callback.onReceive(data)
                            }

                        })
                        val faderControlRadioOptions =  mutableListOf<PrefItem<Any>>()
                        data.faderCtrlList?.forEachIndexed {index,title ->
                                val radioItem: PrefItem<Any> = PrefItem(
                                    ViewHolderType.VT_RADIO,
                                    Pref.FADER_CONTROL_RADIO,
                                    CompoundItem(
                                        title,
                                       index ==data.defFaderValue,
                                    index,
                                    prefCompoundListener
                                ),
                                listener
                                )
                                faderControlRadioOptions.add(radioItem)

                        }


                        val audioForVisuallyImpRadioOpts =  mutableListOf<PrefItem<Any>>()

                        if (data.audioForVisuallyImp!!.isNotEmpty()) {
                            data.audioForVisuallyImp?.forEachIndexed { index, title ->
                                val radioItem: PrefItem<Any> = PrefItem(
                                    ViewHolderType.VT_RADIO,
                                    Pref.AUDIO_FOR_VISUALLY_IMPAIRED_RADIO,
                                    CompoundItem(
                                        title,
                                        index == data.defAudioForVisImp,
                                        index,
                                        prefCompoundListener
                                    ),
                                    listener
                                )
                                audioForVisuallyImpRadioOpts.add(radioItem)
                            }
                        }

                        var viAudioShowArrow = false
                        var viAudioInfo : String? = null

                        if (data.audioForVisuallyImp!!.isNotEmpty()) {
                            viAudioInfo = data.defAudioForVisImp?.let { data.audioForVisuallyImp?.get(it) }.toString()
                            viAudioShowArrow = true
                        }

                        var visualImpairedSubItems = mutableListOf<PrefItem<Any>>(
                            PrefItem(
                                ViewHolderType.VT_SWITCH,
                                Pref.SPEAKER_SWITCH,
                                speakerSwitch,
                                listener
                            ),
                            PrefItem(
                                ViewHolderType.VT_SWITCH,
                                Pref.HEADPHONE_SWITCH,
                                headphoneSwitch,
                                listener
                            ),
                            PrefItem(
                                ViewHolderType.VT_SEEKBAR,
                                Pref.VOLUME_SEEKBAR,
                                volumeSeekBar,
                                listener
                            ),
                            PrefItem(
                                ViewHolderType.VT_SWITCH,
                                Pref.PANE_FADE_SWITCH,
                                paneAndFadeSwitch,
                                listener
                            ),

                            PrefItem(
                                ViewHolderType.VT_MENU,
                                Pref.AUDIO_FOR_VISUALLY_IMPAIRED,
                                RootItem(
                                    audioForVisuallyImpairedTitle,
                                    viAudioInfo, viAudioShowArrow,
                                    audioForVisuallyImpRadioOpts,
                                    object :InfoListener{
                                        override fun getInfo(): String? {
                                            audioForVisuallyImpRadioOpts.forEach {
                                                val cItem = it.data as CompoundItem
                                                if(cItem.isChecked) return cItem.title
                                            }
                                            return null
                                        }
                                    }),
                                listener
                            ),
                            PrefItem(
                                ViewHolderType.VT_MENU,
                                Pref.FADER_CONTROL,
                                RootItem(
                                    faderControlTitle,
                                    data.defFaderValue?.let { data.faderCtrlList?.get(it) }, true,
                                    faderControlRadioOptions,

                                    object :InfoListener{
                                        override fun getInfo(): String? {
                                            faderControlRadioOptions.forEach {
                                                val cItem = it.data as CompoundItem
                                                if(cItem.isChecked) return cItem.title
                                            }
                                            return null
                                        }
                                    }
                                ),
                                listener
                            ),
                        )

                        if(item.id == PrefSubMenu.VISUALLY_IMPAIRED_MINIMAL){
                            visualImpairedSubItems.toList().forEach {
                                if(it.id !=  Pref.FADER_CONTROL){
                                    visualImpairedSubItems.remove(it)
                                }
                            }
                        }

                        visualImpaired =
                            PrefItem(ViewHolderType.VT_MENU,
                            Pref.VISUALLY_IMPAIRED,
                            RootItem(
                                itemTitle, subTitle, true,
                                visualImpairedSubItems,object : InfoListener{
                                    override fun getInfo(): String? {
                                        return ConfigStringsManager.getStringById(if(compoundItem.isChecked) "on" else "off")
                                    }
                                }
                            ),
                            listener
                        )
                        if (data.isAudioDescriptionEnabled){
                            zerothColumn.add(visualImpaired!!)
                        }

                    }
                    else if (item.id == PrefSubMenu.HEARING_IMPAIRED) {
                        var switchStatus = data.isHearingImpairedEnabled
                        val subTitle = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                        val compoundItem = CompoundItem(subTitle, switchStatus, 0,prefCompoundListener)
                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.HEARING_IMPAIRED,
                                RootItem(
                                    itemTitle, subTitle, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.HEARING_IMPAIRED_SWITCH,
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
                    else if (item.id == PrefSubMenu.AUDIO_DESCRIPTION_KONKA) {

                        var switchStatus = true
                        val subTitle = ConfigStringsManager.getStringById(if(switchStatus) "on" else "off")
                        val compoundItem = CompoundItem(subTitle, switchStatus, 0,prefCompoundListener)
                        zerothColumn.add(
                            PrefItem(ViewHolderType.VT_MENU,
                                Pref.AUDIO_DESCRIPTION_KONKA,
                                RootItem(
                                    itemTitle, subTitle, true,
                                    mutableListOf(PrefItem(
                                        ViewHolderType.VT_SWITCH,
                                        Pref.AUDIO_DESCRIPTION_KONKA_SWITCH,
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
                    else if (item.id == PrefSubMenu.AUDIO_FORMAT) {
                        var audioFormatOptions = mutableListOf<PrefItem<Any>>()
                        data.audioFormat?.forEachIndexed { index, formatType ->
                            val radioItem : PrefItem<Any> = PrefItem(
                                ViewHolderType.VT_RADIO,
                                Pref.AUDIO_FORMAT_RADIO,
                                CompoundItem(
                                    formatType,
                                    (formatType==data.selectedAudioFormat),
                                    index,
                                    prefCompoundListener
                                ),
                                listener
                            )
                            audioFormatOptions.add(radioItem)
                        }

                        zerothColumn.add(
                            PrefItem(
                                ViewHolderType.VT_MENU,
                                Pref.AUDIO_FORMAT,
                                RootItem(
                                    itemTitle, data.selectedAudioFormat, true,
                                    audioFormatOptions,
                                    object :InfoListener{
                                        override fun getInfo(): String? {
                                            audioFormatOptions.forEach {
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

                preferencesLeftOptionsAdapter = PreferenceSubMenuAdapter(
                    zerothColumn,
                    view!!,
                    object : PreferenceSubMenuAdapter.Listener {
                        override fun onBack(action: Action): Boolean {

                            if(action == Action.LEFT) return false
                            audioListener?.onPrefsCategoriesRequestFocus(id)
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
                            audioListener!!.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                        }
                    })
                preferencesLeftOptionsGridView!!.adapter = preferencesLeftOptionsAdapter
            }
        }

    }

    fun setFocusToGrid() {
        preferencesLeftOptionsGridView!!.requestFocus()
    }

    interface PreferencesAudioListener : GWidgetListener, TTSSetterInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun onPrefsCategoriesRequestFocus(position: Int)
        fun getFirstAudioLanguage(): LanguageCode?
        fun onFirstAudioLanguageSelected(languageCode: LanguageCode)
        fun getSecondAudioLanguage(): LanguageCode?
        fun onSecondAudioLanguageSelected(languageCode: LanguageCode)
        fun onAudioTypeClicked(position: Int)
        fun onVisuallyImpairedValueChanged(position: Int, isEnabled: Boolean)
        fun onVolumeChanged(newVolumeValue: Int)
        fun onFaderValueChanged(newValue: Int)
        fun onAudioViValueChanged(newValue: Int)
        fun setPrefsValue(key: String, defValue: Any?)
        fun setAudioDescriptionEnabled(enable: Boolean)
        fun setHearingImpairedEnabled(enable: Boolean)
    }
}