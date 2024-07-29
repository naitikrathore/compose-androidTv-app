package com.iwedia.cltv.components

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.custom_caption.CustomTextView
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.PreferencesClosedCaptionsInformation
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


class PreferencesClosedCaptionsWidget :
    GWidget<ConstraintLayout, PreferencesClosedCaptionsWidget.PreferencesClosedCationsListener> {

    var context: Context? = null
    private val categories = mutableListOf<PreferenceCategoryItem>()

    private var preferencesLeftOptionsGridView: VerticalGridView? = null
    private var preferencesLeftOptionsAdapter: PreferenceSubMenuAdapter? = null

    var ccListener: PreferencesClosedCationsListener? = null

    private var ccPreviewText: TextView? = null
    private var ccPreviewTextOutline: CustomTextView? = null
    private var ccPreviewTextShadow: TextView? = null
    private var ccPreviewView: TextView? = null
    private var verticalGuide: Guideline? = null
    private val vaTextOpacity: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
    private val vaBgOpacity: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
    private var wasEdgeTypeShadow: Boolean = false
    private var wasEdgeTypeOutline: Boolean = false
    private var lastSelectedTextColor: Int = 0
    private var lastSelectedTextOpacity: Int = 0
    private var lastSelectedEdgeColor: Int = 0
    private var isClosedCaptionsEnabled = false

    val zerothColumn = mutableListOf<PrefItem<Any>>()

    constructor(
        context: Context,
        listener: PreferencesClosedCationsListener
    ) : super(
        ReferenceWorldHandler.WidgetId.PREFERENCES_CLOSED_CAPTIONS,
        ReferenceWorldHandler.WidgetId.PREFERENCES_CLOSED_CAPTIONS,
        listener
    ) {
        this.context = context

        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.preferences_closed_captions_layout, null) as ConstraintLayout
        preferencesLeftOptionsGridView = view!!.findViewById(R.id.preferences_cc_left_options)

        //setup adapters
        preferencesLeftOptionsGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferencesLeftOptionsGridView!!.windowAlignmentOffset = Utils.getDimensInPixelSize(R.dimen.custom_dim_25)
        preferencesLeftOptionsGridView!!.setNumColumns(1)
        ccListener = listener
        ccPreviewText = view!!.findViewById(R.id.cc_preview)
        ccPreviewTextShadow = view!!.findViewById(R.id.cc_preview_shadow)
        ccPreviewTextOutline = view!!.findViewById(R.id.cc_preview_outline)
        ccPreviewView = view!!.findViewById(R.id.view_cc_preview)
        verticalGuide = view!!.findViewById(R.id.verticalGuide)

    }

    override fun refresh(data: Any) {

        if (data is PreferencesClosedCaptionsInformation) {
            val listener = object : PrefItemListener {
                override fun onAction(action: Action, id: Pref): Boolean {
                    if (action == Action.DOWN) {
                        if(!isClosedCaptionsEnabled){
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

            zerothColumn.clear()

            data.selectedTextSize?.let { getCCTextSize(it) }
            data.selectedFontFamily?.let { context?.let { it1 -> getCCFontFamily(it, it1) } }
            data.selectedTextColor?.let { context?.let { it1 -> getCCTextColor(it, it1) } }
            data.selectedTextOpacity?.let { context?.let { it1 -> getCCTextOpacity(it, it1) } }
            data.selectedEdgeType?.let { getCCEdgeType(it) }
            data.selectedEdgeColor?.let { context?.let { it1 -> getCCEdgeColor(it, it1) } }
            data.selectedBackgroundColor?.let { context?.let { it1 -> getCCBgColor(it, it1) } }
            data.selectedBackgroundOpacity?.let { getCCBgOpacity(it) }
            lastSelectedTextOpacity = data.lastSelectedTextOpacity!!

            isClosedCaptionsEnabled = data.enableDisplayCC == 1 || data.enableDisplayCC == 2

            val prefCompoundListener = object : PrefCompoundListener {
                override fun onChange(
                    isSelected: Boolean,
                    index: Int,
                    id: Pref,
                    callback: IAsyncDataCallback<Boolean>
                ) {
                    when (id) {
                        Pref.WITH_MUTE_CHECKBOX -> {
                            ccListener?.setCCWithMute(isSelected)
                        }
                        Pref.DISPLAY_CC_SWITCH -> {
                            isClosedCaptionsEnabled = if (isSelected) {
                                ccListener?.saveUserSelectedCCOptions("display_cc", 1)
                                ccListener?.setCCInfo()
                                true
                            } else {
                                ccListener?.saveUserSelectedCCOptions("display_cc", 0)
                                ccListener?.disableCCInfo()
                                false
                            }

                            zerothColumn.forEachIndexed { index, prefItem ->
                                if(prefItem.id != Pref.DISPLAY_CC) {
                                    (prefItem.data as RootItem).isEnabled = isClosedCaptionsEnabled
                                    preferencesLeftOptionsAdapter!!.notifyItemChanged(zerothColumn.indexOf(prefItem))
                                }
                            }
                        }
                        Pref.CAPTION_SERVICES_RADIO -> {
                            ccListener?.saveUserSelectedCCOptions("caption_services", index)
                            ccListener?.setCCInfo()
                        }
                        Pref.ADVANCED_SELECTION_RADIO -> {
                            ccListener?.saveUserSelectedCCOptions("advanced_selection", index)
                            ccListener?.setCCInfo()
                        }
                        Pref.TEXT_SIZE_RADIO -> {
                            ccListener?.saveUserSelectedCCOptions("text_size", index)
                            getCCTextSize(index)
                            ccListener?.setCCInfo()
                        }
                        Pref.FONT_FAMILY_RADIO -> {
                            ccListener?.saveUserSelectedCCOptions("font_family", index)
                            getCCFontFamily(index, context!!)
                            ccListener?.setCCInfo()
                        }
                        Pref.TEXT_COLOR_RADIO -> {
                            ccListener?.saveUserSelectedCCOptions("text_color", index)
                            getCCTextColor(index, context!!)
                            lastSelectedTextColor = index
                            ccListener?.setCCInfo()
                        }
                        Pref.TEXT_OPACITY_RADIO -> {
                            ccListener?.saveUserSelectedCCOptions("text_opacity", index)
                            lastSelectedTextOpacity = index
                            ccListener?.lastSelectedTextOpacity(index)
                            vaTextOpacity.cancel()
                            getCCTextColor(lastSelectedTextColor, context!!)
                            ccPreviewText!!.alpha = 1f
                            if (ccPreviewTextOutline!!.visibility == View.VISIBLE)
                                ccPreviewTextOutline!!.alpha = 1f
                            else ccPreviewTextShadow!!.alpha = 1f
                            if (wasEdgeTypeOutline) ccPreviewTextOutline!!.visibility = View.VISIBLE
                            if (wasEdgeTypeShadow) ccPreviewTextShadow!!.visibility = View.VISIBLE
                            getCCTextOpacity(index, context!!)
                            ccListener?.setCCInfo()
                        }
                        Pref.EDGE_TYPE_RADIO -> {
                            ccListener?.saveUserSelectedCCOptions("edge_type", index)
                            getCCEdgeColor(lastSelectedEdgeColor, context!!)
                            getCCEdgeType(index)
                            ccListener?.setCCInfo()
                        }
                        Pref.EDGE_COLOR_RADIO -> {
                            ccListener?.saveUserSelectedCCOptions("edge_color", index)
                            getCCEdgeColor(index, context!!)
                            lastSelectedEdgeColor = index
                            ccListener?.setCCInfo()
                        }
                        Pref.BACKGROUND_COLOR_RADIO -> {
                            ccListener?.saveUserSelectedCCOptions("background_color", index)
                            getCCBgColor(index, context!!)
                            ccListener?.setCCInfo()
                        }
                        Pref.BACKGROUND_OPACITY_RADIO -> {
                            ccListener?.saveUserSelectedCCOptions("background_opacity", index)
                            vaBgOpacity.cancel()
                            getCCBgOpacity(index)
                            ccListener?.setCCInfo()
                        }
                        else -> {

                        }
                    }
                    callback.onReceive(isSelected)
                }
            }

            if (data.subCategories.isNotEmpty()) {
                categories.clear()
                data.subCategories.forEach { item ->
                    val itemTitle = item.name

                    when (item.id) {
                        PrefSubMenu.DISPLAY_CC -> {
                            val status = ConfigStringsManager.getStringById(if(isClosedCaptionsEnabled) "on" else "off")
                            val displayCCItems = mutableListOf<PrefItem<Any>>()
                            val switchItem = PrefItem(
                                ViewHolderType.VT_SWITCH,
                                Pref.DISPLAY_CC_SWITCH,
                                CompoundItem(status, isClosedCaptionsEnabled, 0, prefCompoundListener),
                                listener
                            )
                            displayCCItems.add(switchItem as PrefItem<Any>)
                            val muteCheckBox = PrefItem<Any>(
                                ViewHolderType.VT_CHECKBOX,
                                Pref.WITH_MUTE_CHECKBOX,
                                CompoundItem(
                                    ConfigStringsManager.getStringById("with_mute"),
                                    data.withMute!!,
                                    0,
                                    prefCompoundListener
                                ),
                                listener
                            )
                            if (isClosedCaptionsEnabled) {
                                displayCCItems.add(muteCheckBox)
                            } else {
                                switchItem.data.hiddenOptions.add(muteCheckBox)
                            }
                            zerothColumn.add(PrefItem(ViewHolderType.VT_MENU,
                                Pref.DISPLAY_CC,
                                RootItem(
                                    itemTitle, status, true,
                                    displayCCItems,
                                    object : InfoListener {
                                        override fun getInfo(): String? {
                                            val item = displayCCItems[0].data as CompoundItem
                                            return ConfigStringsManager.getStringById(if (item.isChecked) "on" else "off")
                                        }
                                    }
                                ),
                                listener
                            ))
                        }
                        PrefSubMenu.CAPTION_SERVICES -> {
                            var captionServiceItems = mutableListOf<PrefItem<Any>>()
                            data.captionServiceList!!.forEachIndexed { index, it ->
                                captionServiceItems.add(
                                    PrefItem(
                                        ViewHolderType.VT_RADIO,
                                        Pref.CAPTION_SERVICES_RADIO,
                                        CompoundItem(
                                            it,
                                            data.selectedCaptionService == index,
                                            index,
                                            prefCompoundListener
                                        ),
                                        listener
                                    )
                                )
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.CAPTION_SERVICES,
                                    RootItem(
                                        itemTitle,
                                        data.selectedCaptionService?.let { data.captionServiceList!![it] },
                                        true,
                                        captionServiceItems,
                                        object : InfoListener {
                                            override fun getInfo(): String? {
                                                captionServiceItems.forEach {
                                                    val compoundItem = it.data as CompoundItem
                                                    if (compoundItem.isChecked) return compoundItem.title
                                                }
                                                return null
                                            }
                                        }),
                                    listener
                                )
                            )
                        }
                        PrefSubMenu.ADVANCED_SELECTION -> {
                            var advancedSelectionItems = mutableListOf<PrefItem<Any>>()
                            data.advanceSelectionsList!!.forEachIndexed { index, it ->
                                advancedSelectionItems.add(
                                    PrefItem(
                                        ViewHolderType.VT_RADIO,
                                        Pref.ADVANCED_SELECTION_RADIO,
                                        CompoundItem(
                                            it,
                                            data.selectedAdvanceSelection == index,
                                            index,
                                            prefCompoundListener
                                        ),
                                        listener
                                    )
                                )
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.ADVANCED_SELECTION,
                                    RootItem(
                                        itemTitle,
                                        data.selectedAdvanceSelection?.let { data.advanceSelectionsList!![it] },
                                        true,
                                        advancedSelectionItems,
                                        object : InfoListener {
                                            override fun getInfo(): String? {
                                                advancedSelectionItems.forEach {
                                                    val compoundItem = it.data as CompoundItem
                                                    if (compoundItem.isChecked) return compoundItem.title
                                                }
                                                return null
                                            }
                                        }),
                                    listener
                                )
                            )
                        }
                        PrefSubMenu.TEXT_SIZE -> {
                            var textSizeItems = mutableListOf<PrefItem<Any>>()
                            data.textSizeList!!.forEachIndexed { index, it ->
                                textSizeItems.add(
                                    PrefItem(
                                        ViewHolderType.VT_RADIO,
                                        Pref.TEXT_SIZE_RADIO,
                                        CompoundItem(
                                            it,
                                            data.selectedTextSize == index,
                                            index,
                                            prefCompoundListener
                                        ),
                                        listener
                                    )
                                )
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.TEXT_SIZE,
                                    RootItem(
                                        itemTitle,
                                        data.selectedTextSize?.let { data.textSizeList!![it] },
                                        true,
                                        textSizeItems,
                                        object : InfoListener {
                                            override fun getInfo(): String? {
                                                textSizeItems.forEach {
                                                    val compoundItem = it.data as CompoundItem
                                                    if (compoundItem.isChecked) return compoundItem.title
                                                }
                                                return null
                                            }
                                        }),
                                    listener
                                )
                            )
                        }
                        PrefSubMenu.FONT_FAMILY -> {
                            var fontFamilyItems = mutableListOf<PrefItem<Any>>()
                            data.fontFamilyList!!.forEachIndexed { index, it ->
                                fontFamilyItems.add(
                                    PrefItem(
                                        ViewHolderType.VT_RADIO,
                                        Pref.FONT_FAMILY_RADIO,
                                        CompoundItem(
                                            it,
                                            data.selectedFontFamily == index,
                                            index,
                                            prefCompoundListener
                                        ),
                                        listener
                                    )
                                )
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.FONT_FAMILY,
                                    RootItem(
                                        itemTitle,
                                        data.selectedFontFamily?.let { data.fontFamilyList!![it] },
                                        true,
                                        fontFamilyItems,
                                        object : InfoListener {
                                            override fun getInfo(): String? {
                                                fontFamilyItems.forEach {
                                                    val compoundItem = it.data as CompoundItem
                                                    if (compoundItem.isChecked) return compoundItem.title
                                                }
                                                return null
                                            }
                                        }),
                                    listener
                                )
                            )
                        }
                        PrefSubMenu.TEXT_COLOR -> {
                            var textColorItems = mutableListOf<PrefItem<Any>>()
                            data.textColorList!!.forEachIndexed { index, it ->
                                textColorItems.add(
                                    PrefItem(
                                        ViewHolderType.VT_RADIO,
                                        Pref.TEXT_COLOR_RADIO,
                                        CompoundItem(
                                            it,
                                            data.selectedTextColor == index,
                                            index,
                                            prefCompoundListener
                                        ),
                                        listener
                                    )
                                )
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.TEXT_COLOR,
                                    RootItem(
                                        itemTitle,
                                        data.selectedTextColor?.let { data.textColorList!![it] },
                                        true,
                                        textColorItems,
                                        object : InfoListener {
                                            override fun getInfo(): String? {
                                                textColorItems.forEach {
                                                    val compoundItem = it.data as CompoundItem
                                                    if (compoundItem.isChecked) return compoundItem.title
                                                }
                                                return null
                                            }
                                        }),
                                    listener
                                )
                            )
                        }
                        PrefSubMenu.TEXT_OPACITY -> {
                            var textOpacityItems = mutableListOf<PrefItem<Any>>()
                            data.textOpacityList!!.forEachIndexed { index, it ->
                                textOpacityItems.add(
                                    PrefItem(
                                        ViewHolderType.VT_RADIO,
                                        Pref.TEXT_OPACITY_RADIO,
                                        CompoundItem(
                                            it,
                                            data.selectedTextOpacity == index,
                                            index,
                                            prefCompoundListener
                                        ),
                                        listener
                                    )
                                )
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.TEXT_OPACITY,
                                    RootItem(
                                        itemTitle,
                                        data.selectedTextOpacity?.let { data.textOpacityList!![it] },
                                        true,
                                        textOpacityItems,
                                        object : InfoListener {
                                            override fun getInfo(): String? {
                                                textOpacityItems.forEach {
                                                    val compoundItem = it.data as CompoundItem
                                                    if (compoundItem.isChecked) return compoundItem.title
                                                }
                                                return null
                                            }
                                        }),
                                    listener
                                )
                            )
                        }
                        PrefSubMenu.EDGE_TYPE -> {
                            var edgeTypeItems = mutableListOf<PrefItem<Any>>()
                            data.edgeTypeList!!.forEachIndexed { index, it ->
                                edgeTypeItems.add(
                                    PrefItem(
                                        ViewHolderType.VT_RADIO,
                                        Pref.EDGE_TYPE_RADIO,
                                        CompoundItem(
                                            it,
                                            data.selectedEdgeType == index,
                                            index,
                                            prefCompoundListener
                                        ),
                                        listener
                                    )
                                )
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.EDGE_TYPE,
                                    RootItem(
                                        itemTitle,
                                        data.selectedEdgeType?.let { data.edgeTypeList!![it] },
                                        true,
                                        edgeTypeItems,
                                        object : InfoListener {
                                            override fun getInfo(): String? {
                                                edgeTypeItems.forEach {
                                                    val compoundItem = it.data as CompoundItem
                                                    if (compoundItem.isChecked) return compoundItem.title
                                                }
                                                return null
                                            }
                                        }),
                                    listener
                                )
                            )
                        }
                        PrefSubMenu.EDGE_COLOR -> {
                            var edgeColorItems = mutableListOf<PrefItem<Any>>()
                            data.edgeColorList!!.forEachIndexed { index, it ->
                                edgeColorItems.add(
                                    PrefItem(
                                        ViewHolderType.VT_RADIO,
                                        Pref.EDGE_COLOR_RADIO,
                                        CompoundItem(
                                            it,
                                            data.selectedEdgeColor == index,
                                            index,
                                            prefCompoundListener
                                        ),
                                        listener
                                    )
                                )
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.EDGE_COLOR,
                                    RootItem(
                                        itemTitle,
                                        data.selectedEdgeColor?.let { data.edgeColorList!![it] },
                                        true,
                                        edgeColorItems,
                                        object : InfoListener {
                                            override fun getInfo(): String? {
                                                edgeColorItems.forEach {
                                                    val compoundItem = it.data as CompoundItem
                                                    if (compoundItem.isChecked) return compoundItem.title
                                                }
                                                return null
                                            }
                                        }),
                                    listener
                                )
                            )
                        }
                        PrefSubMenu.BACKGROUND_COLOR -> {
                            var backGroundColorItems = mutableListOf<PrefItem<Any>>()
                            data.backgroundColorList!!.forEachIndexed { index, it ->
                                backGroundColorItems.add(
                                    PrefItem(
                                        ViewHolderType.VT_RADIO,
                                        Pref.BACKGROUND_COLOR_RADIO,
                                        CompoundItem(
                                            it,
                                            data.selectedBackgroundColor == index,
                                            index,
                                            prefCompoundListener
                                        ),
                                        listener
                                    )
                                )
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.BACKGROUND_COLOR,
                                    RootItem(
                                        itemTitle,
                                        data.selectedBackgroundColor?.let { data.backgroundColorList!![it] },
                                        true,
                                        backGroundColorItems,
                                        object : InfoListener {
                                            override fun getInfo(): String? {
                                                backGroundColorItems.forEach {
                                                    val compoundItem = it.data as CompoundItem
                                                    if (compoundItem.isChecked) return compoundItem.title
                                                }
                                                return null
                                            }
                                        }),
                                    listener
                                )
                            )
                        }
                        PrefSubMenu.BACKGROUND_OPACITY -> {
                            var backGroundOpacityItems = mutableListOf<PrefItem<Any>>()
                            data.backgroundOpacityList!!.forEachIndexed { index, it ->
                                backGroundOpacityItems.add(
                                    PrefItem(
                                        ViewHolderType.VT_RADIO,
                                        Pref.BACKGROUND_OPACITY_RADIO,
                                        CompoundItem(
                                            it,
                                            data.selectedBackgroundOpacity == index,
                                            index,
                                            prefCompoundListener
                                        ),
                                        listener
                                    )
                                )
                            }
                            zerothColumn.add(
                                PrefItem(
                                    ViewHolderType.VT_MENU,
                                    Pref.BACKGROUND_OPACITY,
                                    RootItem(
                                        itemTitle,
                                        data.selectedBackgroundOpacity?.let { data.backgroundOpacityList!![it] },
                                        true,
                                        backGroundOpacityItems,
                                        object : InfoListener {
                                            override fun getInfo(): String? {
                                                backGroundOpacityItems.forEach {
                                                    val compoundItem = it.data as CompoundItem
                                                    if (compoundItem.isChecked) return compoundItem.title
                                                }
                                                return null
                                            }
                                        }),
                                    listener
                                )
                            )
                        }
                        else -> {

                        }
                    }
                }
            }

            zerothColumn.forEachIndexed { index, prefItem ->
                if(prefItem.id != Pref.DISPLAY_CC){
                    (prefItem.data as RootItem).isEnabled = isClosedCaptionsEnabled
                }
            }

            preferencesLeftOptionsAdapter = PreferenceSubMenuAdapter(
                zerothColumn,
                view!!,
                object : PreferenceSubMenuAdapter.Listener {
                    override fun onBack(action: Action): Boolean {
                        if (action == Action.LEFT) return false
                        if(ccListener != null)
                            ccListener?.onPrefsCategoriesRequestFocus()
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
                        ccListener?.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                    }
                })
            preferencesLeftOptionsGridView!!.adapter = preferencesLeftOptionsAdapter
            ccListener?.resetCC()
        }
    }

    private fun getCCTextSize(position: Int) {
        when (position) {
            0, 2 -> {
                ccPreviewText!!.textSize =
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_18).toFloat()
                ccPreviewTextShadow!!.textSize =
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_18).toFloat()
                ccPreviewTextOutline!!.textSize =
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_18).toFloat()
            }
            1 -> {
                ccPreviewText!!.textSize =
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_16).toFloat()
                ccPreviewTextShadow!!.textSize =
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_16).toFloat()
                ccPreviewTextOutline!!.textSize =
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_16).toFloat()
            }
            3 -> {
                ccPreviewText!!.textSize =
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_20).toFloat()
                ccPreviewTextShadow!!.textSize =
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_20).toFloat()
                ccPreviewTextOutline!!.textSize =
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_20).toFloat()
            }
        }
    }

    private fun getCCFontFamily(position: Int, context: Context) {
        when (position) {
            0, 1 -> {
                ccPreviewText!!.typeface = ResourcesCompat.getFont(context, R.font.serif_monospace)
                ccPreviewTextShadow!!.typeface =
                    ResourcesCompat.getFont(context, R.font.serif_monospace)
                ccPreviewTextOutline!!.typeface =
                    ResourcesCompat.getFont(context, R.font.serif_monospace)
            }
            2 -> {
                ccPreviewText!!.typeface = ResourcesCompat.getFont(context, R.font.serif)
                ccPreviewTextShadow!!.typeface = ResourcesCompat.getFont(context, R.font.serif)
                ccPreviewTextOutline!!.typeface = ResourcesCompat.getFont(context, R.font.serif)
            }
            3 -> {
                ccPreviewText!!.typeface =
                    ResourcesCompat.getFont(context, R.font.sans_serif_monospace)
                ccPreviewTextShadow!!.typeface =
                    ResourcesCompat.getFont(context, R.font.sans_serif_monospace)
                ccPreviewTextOutline!!.typeface =
                    ResourcesCompat.getFont(context, R.font.sans_serif_monospace)
            }
            4 -> {
                ccPreviewText!!.typeface = ResourcesCompat.getFont(context, R.font.sans_serif)
                ccPreviewTextShadow!!.typeface = ResourcesCompat.getFont(context, R.font.sans_serif)
                ccPreviewTextOutline!!.typeface =
                    ResourcesCompat.getFont(context, R.font.sans_serif)
            }
            5 -> {
                ccPreviewText!!.typeface = ResourcesCompat.getFont(context, R.font.casual)
                ccPreviewTextShadow!!.typeface = ResourcesCompat.getFont(context, R.font.casual)
                ccPreviewTextOutline!!.typeface = ResourcesCompat.getFont(context, R.font.casual)
            }
            6 -> {
                ccPreviewText!!.typeface = ResourcesCompat.getFont(context, R.font.cursive)
                ccPreviewTextShadow!!.typeface = ResourcesCompat.getFont(context, R.font.cursive)
                ccPreviewTextOutline!!.typeface = ResourcesCompat.getFont(context, R.font.cursive)
            }
            7 -> {
                ccPreviewText!!.typeface = ResourcesCompat.getFont(context, R.font.small_capitals)
                ccPreviewTextShadow!!.typeface =
                    ResourcesCompat.getFont(context, R.font.small_capitals)
                ccPreviewTextOutline!!.typeface =
                    ResourcesCompat.getFont(context, R.font.small_capitals)
            }
        }
    }

    private fun getCCTextColor(position: Int, context: Context) {
        when (lastSelectedTextOpacity) {
            2 -> ccPreviewText!!.alpha = 0.3f
            3 -> ccPreviewText!!.setTextColor(ContextCompat.getColor(context, R.color.shadow))
            else -> {
                when (position) {
                    0, 2 -> ccPreviewText!!.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                    1 -> ccPreviewText!!.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )
                    3 -> ccPreviewText!!.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.green
                        )
                    )
                    4 -> ccPreviewText!!.setTextColor(ContextCompat.getColor(context, R.color.blue))
                    5 -> ccPreviewText!!.setTextColor(ContextCompat.getColor(context, R.color.red))
                    6 -> ccPreviewText!!.setTextColor(ContextCompat.getColor(context, R.color.cyan))
                    7 -> ccPreviewText!!.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.yellow
                        )
                    )
                    8 -> ccPreviewText!!.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.magenta
                        )
                    )
                }
            }
        }
    }

    private fun getCCTextOpacity(position: Int, context: Context) {
        when (position) {
            0, 1 -> ccPreviewText!!.alpha = 1f
            2 -> {
                ccPreviewText!!.alpha = 0.3f
            }
            3 -> {
                ccPreviewText!!.setTextColor(ContextCompat.getColor(context, R.color.shadow))
                if (ccPreviewTextOutline!!.visibility == View.VISIBLE) {
                    wasEdgeTypeOutline = true
                    ccPreviewTextOutline!!.visibility = View.GONE
                } else {
                    wasEdgeTypeShadow = true
                    ccPreviewTextShadow!!.visibility = View.GONE
                }
            }
            4 -> {
                vaTextOpacity.duration = 1000 //in millis
                vaTextOpacity.addUpdateListener {
                    if (it.animatedValue as Float > 0.5) {
                        ccPreviewText!!.alpha = 0f
                        if (ccPreviewTextOutline!!.visibility == View.VISIBLE)
                            ccPreviewTextOutline!!.alpha = 0f
                        else ccPreviewTextShadow!!.alpha = 0f

                    } else {
                        ccPreviewText!!.alpha = 1f
                        if (ccPreviewTextOutline!!.visibility == View.VISIBLE)
                            ccPreviewTextOutline!!.alpha = 1f
                        else ccPreviewTextShadow!!.alpha = 1f
                    }

                }
                vaTextOpacity.repeatCount = ValueAnimator.INFINITE
                vaTextOpacity.start()
            }
        }
    }

    private fun getCCEdgeType(position: Int) {
        when (position) {
            0, 1 -> {
                ccPreviewTextOutline!!.visibility = View.GONE
                ccPreviewTextShadow!!.visibility = View.GONE
            }
            2 -> {
                ccPreviewTextOutline!!.visibility = View.GONE
                ccPreviewTextShadow!!.visibility = View.VISIBLE
                ccPreviewTextShadow!!.translationX = -2f
                ccPreviewTextShadow!!.translationY = -2f
            }
            3 -> {
                ccPreviewTextOutline!!.visibility = View.GONE
                ccPreviewTextShadow!!.visibility = View.VISIBLE
                ccPreviewTextShadow!!.translationX = 2f
                ccPreviewTextShadow!!.translationY = 2f
            }
            4 -> {
                ccPreviewTextOutline!!.visibility = View.VISIBLE
                ccPreviewTextShadow!!.visibility = View.GONE
            }
            5 -> {
                ccPreviewTextOutline!!.visibility = View.GONE
                ccPreviewTextShadow!!.visibility = View.VISIBLE
                ccPreviewTextShadow!!.translationX = -2f
                ccPreviewTextShadow!!.translationY = 0f
            }
        }
    }

    private fun getCCEdgeColor(position: Int, context: Context) {
        when (position) {
            0, 2 -> {
                ccPreviewTextShadow!!.setTextColor(ContextCompat.getColor(context, R.color.white))
                ccPreviewTextOutline!!.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            1 -> {
                ccPreviewTextShadow!!.setTextColor(ContextCompat.getColor(context, R.color.black))
                ccPreviewTextOutline!!.setTextColor(ContextCompat.getColor(context, R.color.black))
            }
            3 -> {
                ccPreviewTextShadow!!.setTextColor(ContextCompat.getColor(context, R.color.green))
                ccPreviewTextOutline!!.setTextColor(ContextCompat.getColor(context, R.color.green))
            }
            4 -> {
                ccPreviewTextShadow!!.setTextColor(ContextCompat.getColor(context, R.color.blue))
                ccPreviewTextOutline!!.setTextColor(ContextCompat.getColor(context, R.color.blue))
            }
            5 -> {
                ccPreviewTextShadow!!.setTextColor(ContextCompat.getColor(context, R.color.red))
                ccPreviewTextOutline!!.setTextColor(ContextCompat.getColor(context, R.color.red))
            }
            6 -> {
                ccPreviewTextShadow!!.setTextColor(ContextCompat.getColor(context, R.color.cyan))
                ccPreviewTextOutline!!.setTextColor(ContextCompat.getColor(context, R.color.cyan))
            }
            7 -> {
                ccPreviewTextShadow!!.setTextColor(ContextCompat.getColor(context, R.color.yellow))
                ccPreviewTextOutline!!.setTextColor(ContextCompat.getColor(context, R.color.yellow))
            }
            8 -> {
                ccPreviewTextShadow!!.setTextColor(ContextCompat.getColor(context, R.color.magenta))
                ccPreviewTextOutline!!.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.magenta
                    )
                )
            }
        }
    }

    private fun getCCBgColor(position: Int, context: Context) {
        when (position) {
            0, 1 -> ccPreviewView!!.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.black
                )
            )
            2 -> ccPreviewView!!.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            3 -> ccPreviewView!!.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
            4 -> ccPreviewView!!.setBackgroundColor(ContextCompat.getColor(context, R.color.blue))
            5 -> ccPreviewView!!.setBackgroundColor(ContextCompat.getColor(context, R.color.red))
            6 -> ccPreviewView!!.setBackgroundColor(ContextCompat.getColor(context, R.color.cyan))
            7 -> ccPreviewView!!.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow))
            8 -> ccPreviewView!!.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.magenta
                )
            )
        }
    }

    private fun getCCBgOpacity(position: Int) {
        when (position) {
            0, 1 -> ccPreviewView!!.alpha = 1f
            2 -> ccPreviewView!!.alpha = 0.3f
            3 -> ccPreviewView!!.alpha = 0f
            4 -> {
                vaBgOpacity.duration = 1000 //in millis
                vaBgOpacity.addUpdateListener {
                    if (it.animatedValue as Float > 0.5) ccPreviewView!!.alpha = 0f
                    else ccPreviewView!!.alpha = 1f
                }
                vaBgOpacity.repeatCount = ValueAnimator.INFINITE
                vaBgOpacity.start()
            }
        }
    }

    fun setFocusToGrid() {
        preferencesLeftOptionsGridView!!.requestFocus()
    }

    interface PreferencesClosedCationsListener : GWidgetListener, TTSSetterInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun onPrefsCategoriesRequestFocus()
        fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int)
        fun resetCC()
        fun setCCInfo()
        fun disableCCInfo()
        fun setCCWithMute(isEnabled: Boolean)
        fun lastSelectedTextOpacity(position: Int)
    }
}
