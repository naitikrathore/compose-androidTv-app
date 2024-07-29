package com.iwedia.cltv.components

import PreferencesTeletextWidget
import android.annotation.SuppressLint
import android.content.Context
import android.media.tv.ContentRatingSystem
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.*
import android.view.animation.Animation.AnimationListener
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.HorizontalGridView
import com.iwedia.cltv.*
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.*
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSStopperInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.ReferenceSystemInformation
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.ci_plus.CamInfoLanguageData
import com.iwedia.cltv.platform.model.ci_plus.CamInfoModuleInformation
import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference
import com.iwedia.cltv.platform.model.language.LanguageCode
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.PIN.PinSceneData
import com.iwedia.cltv.utils.PinHelper
import com.iwedia.cltv.utils.Utils
import world.SceneManager
import world.widget.GWidget
import world.widget.GWidgetListener


/**
 * Reference widget preferences
 *
 * @author Aleksandar Lazic
 */
class ReferenceWidgetPreferences :
    GWidget<FrameLayout, ReferenceWidgetPreferences.PreferencesWidgetListener> {
    val TAG = javaClass.simpleName
    var context: Context? = null
    var container: ConstraintLayout? = null

    //Platforms gridview
    private var preferenceTypesGridView: HorizontalGridView? = null

    //Scene layout views references
    private var preferencesFilterGridView: FadingEdgeHorizontalGridView? = null

    //Platforms adapter
    private val preferenceTypesCategoryAdapter = CategoryAdapter(true)

    //List adapters
    private val preferencesFilterCategoryAdapter = CategoryAdapter(true)

    //categories
    private var typeCategories = mutableListOf<CategoryItem>()
    private val categories = mutableListOf<CategoryItem>()
    private var isMultipleTypes = false
    private lateinit var activeType : PrefType

    //views
    private var audio: PreferencesAudioWidget? = null
    private var parentalWidget: PreferencesParentalWidget? = null
    private var subtitle: PreferencesSubtitleWidget? = null
    private var adsTargetingWidget: PreferencesAdsTargetingWidget? = null
    private var termsOfServiceWidget: PreferencesTermsOfServiceWidget? = null
    private var feedbackWidget: PreferencesFeedbackWidget? = null
    private var systemInformation: PreferencesSystemInformationWidget? = null
    private var camInfoWidget: PreferencesCamInfoWidget? = null
    private var setupWidget: PreferencesSetupWidget? = null
    private var txtWidget: PreferencesTeletextWidget? = null
    private var hbbTvWidget: PreferencesHbbTvWidget? = null
    private var closedCaptionsWidget: PreferencesClosedCaptionsWidget? = null
    private var pvrTimeshiftWidget: PreferencesPvrTimeshiftWidget? = null
    private var openSourceLicensesWidget: PreferencesOpenSourceLicensesWidget? = null
    //Current category position
    private var currentCategoryPosition: Int = -1
    private var currentTypePosition: Int = -1
    private var systemInformationClickCounter: Int = 0
    private var selectedMenu =0
    private val animationDuration : Long = 700
    private val animationTransitionDistance : Float = Utils.getDimens(R.dimen.custom_dim_61)

    var isFromInput = false
    var scrollableContainer : ConstraintLayout? = null

    // Status of back press animation is in progress
    var isBackAnimationInProgress  = false

    constructor(
        context: Context,
        listener: PreferencesWidgetListener
    ) : super(
        ReferenceWorldHandler.WidgetId.PREFERENCES,
        ReferenceWorldHandler.WidgetId.PREFERENCES,
        listener
    ) {
        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_widget_preferences, null) as FrameLayout

        scrollableContainer = view!!.findViewById(R.id.scrollable)
        preferenceTypesGridView = view!!.findViewById(R.id.preferences_platforms_gridview)
        preferencesFilterGridView = view!!.findViewById(R.id.preferences_filter_list_view)
        container = view!!.findViewById(R.id.prefs_container)

        if(listener.isAccessibilityEnabled()) {
            container!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            container!!.isFocusable = false
        }

        //platform adapter
        preferenceTypesGridView!!.windowAlignmentOffset= Utils.getDimensInPixelSize(R.dimen.custom_dim_0)
        preferenceTypesGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferenceTypesGridView!!.setNumRows(1)

        preferenceTypesGridView!!.adapter = preferenceTypesCategoryAdapter
        preferenceTypesCategoryAdapter.isCategoryFocus = true

        preferenceTypesCategoryAdapter.adapterListener =
            object : CategoryAdapter.ChannelListCategoryAdapterListener {

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun getAdapterPosition(position: Int) {
                    if (currentTypePosition == position) return

                    activeType = typeCategories[position].prefType
                    preferenceTypesCategoryAdapter.clearPreviousFocus()
                    currentTypePosition = position
                    preferenceTypesCategoryAdapter.selectedItem = position

                    listener.getAvailablePreferencesCategories(object: IAsyncDataCallback<List<CategoryItem>>{
                        override fun onFailed(error: Error) {
                            //ignore
                        }

                        override fun onReceive(data: List<CategoryItem>) {
                            currentCategoryPosition = 0
                            preferencesFilterCategoryAdapter.selectedItem = 0
                            preferencesFilterGridView!!.setSelectedPosition(0)

                            categories.clear()
                            categories.addAll(data)
                            preferencesFilterCategoryAdapter.refresh(categories)
                            container?.removeAllViews()

                            val widgetId = categories[0].id
                            widgetCleanUp(widgetId)
                            handleSubCategoryData(0, widgetId, ReferenceApplication.applicationContext())
                        }
                    }, activeType)
                }

                override fun onItemSelected(position: Int) {
                }

                override fun digitPressed(digit: Int) {}

                override fun onKeyLeft(currentPosition: Int): Boolean {
                    if (isBackAnimationInProgress || PinHelper.isPinActivityStarted()) return true
                    return false
                }

                override fun onKeyRight(currentPosition: Int): Boolean {
                    if (isBackAnimationInProgress || PinHelper.isPinActivityStarted()) return true
                    currentTypePosition = currentPosition
                    return false
                }

                override fun onKeyUp(currentPosition: Int): Boolean {
                    if (PinHelper.isPinActivityStarted()) return true
                    handleBackFromCategory(currentPosition)
                    return true
                }

                override fun onKeyDown(currentPosition: Int): Boolean {
                    if (isBackAnimationInProgress || PinHelper.isPinActivityStarted()) return true

                    currentTypePosition = currentPosition
                    val holder = preferenceTypesGridView!!.findViewHolderForAdapterPosition(currentTypePosition) as CategoryItemViewHolder
                    preferenceTypesCategoryAdapter.setActiveFilter(holder)

                    val alpha: Animation = AlphaAnimation(1.0f, 0.0f)
                    alpha.duration = animationDuration
                    alpha.fillAfter = true
                    preferenceTypesGridView!!.startAnimation(alpha)

                    setFocusToSubcategory()
                    return true
                }

                @SuppressLint("Range")
                override fun onItemClicked(position: Int) {
                    if (PinHelper.isPinActivityStarted()) {
                        return
                    }
                    if(listener.isAccessibilityEnabled()) {
                        listener.onDpadDown(null)
                    }
                    onKeyDown(position)
                }

                override fun onBackPressed(position: Int): Boolean {
                    if (PinHelper.isPinActivityStarted()) {
                        return true
                    }
                    handleBackFromCategory(position)
                    return true
                }

                private fun handleBackFromCategory(currentPosition:Int){
                    if (isBackAnimationInProgress || PinHelper.isPinActivityStarted()) {
                        return
                    }

                    if (!isFromInput) {
                        isBackAnimationInProgress = true
                        val translate: Animation = TranslateAnimation(0F, 0F, -animationTransitionDistance, 0f)
                        translate.duration = animationDuration
                        translate.interpolator = DecelerateInterpolator()
                        translate.fillAfter = true
                        translate.setAnimationListener(object : AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {
                                // ignore
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                currentTypePosition = currentPosition
                                val holder = preferenceTypesGridView!!.findViewHolderForAdapterPosition(currentTypePosition) as CategoryItemViewHolder
                                preferenceTypesCategoryAdapter.setActiveFilter(holder)

                                isBackAnimationInProgress = false
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                                // ignore
                            }
                        })
                        scrollableContainer!!.startAnimation(translate)
                    }
                    listener.requestFocusOnTopMenu()
                }
            }


        //setup adapters

        preferencesFilterGridView!!.windowAlignmentOffset= Utils.getDimensInPixelSize(R.dimen.custom_dim_0)
        preferencesFilterGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        preferencesFilterGridView!!.setNumRows(1)
        preferencesFilterCategoryAdapter.selectedItem=-1

        preferencesFilterGridView!!.adapter = preferencesFilterCategoryAdapter
        preferencesFilterCategoryAdapter.isCategoryFocus = true

        preferencesFilterCategoryAdapter.adapterListener =
            object : CategoryAdapter.ChannelListCategoryAdapterListener {
                @RequiresApi(Build.VERSION_CODES.P)
                override fun getAdapterPosition(position: Int) {
                    if(currentCategoryPosition == position) {
                        return
                    }
                    if(container != null)
                        container?.removeAllViews()

                    preferencesFilterCategoryAdapter.clearPreviousFocus()

                    val widgetId = categories[position].id
                    widgetCleanUp(widgetId)
                    currentCategoryPosition = position
                    preferencesFilterCategoryAdapter.selectedItem = position
                    handleSubCategoryData(position, widgetId,context)
                }

                override fun onItemSelected(position: Int) {
                }

                override fun digitPressed(digit: Int) {}

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

                override fun onKeyLeft(currentPosition: Int): Boolean {
                    if (isBackAnimationInProgress || PinHelper.isPinActivityStarted() || ReferenceApplication.parentalControlDeepLink) return true
                    return false
                }

                override fun onKeyRight(currentPosition: Int): Boolean {
                    if (isBackAnimationInProgress || PinHelper.isPinActivityStarted() || ReferenceApplication.parentalControlDeepLink) return true
                    currentCategoryPosition = currentPosition
                    return false
                }

                override fun onKeyUp(currentPosition: Int): Boolean {
                    if (PinHelper.isPinActivityStarted()) return true
                    handleBackFromCategory(currentPosition)
                    return true
                }

                override fun onKeyDown(currentPosition: Int): Boolean {
                    if (isBackAnimationInProgress || PinHelper.isPinActivityStarted()) return true
                    val widgetId = categories[currentPosition].id

                    // If internet is not available then focus should stay on parental control tab itself.
                    handleCategoryFocus(widgetId)
                    handleCategoryClicked(widgetId)
                    return true
                }

                @SuppressLint("Range")
                override fun onItemClicked(position: Int) {
                    if (PinHelper.isPinActivityStarted()) return
                    val widgetId = categories[position].id
                    if (widgetId != ReferenceWorldHandler.WidgetId.PREFERENCES_SYSTEM_INFORMATION &&
                        widgetId != ReferenceWorldHandler.WidgetId.FEEDBACK) {
                        val holder =
                            preferencesFilterGridView!!.findViewHolderForAdapterPosition(
                                position
                            ) as CategoryItemViewHolder
                        preferencesFilterCategoryAdapter.setActiveFilter(holder)
                        handleCategoryClicked(widgetId)
                        systemInformationClickCounter = 0
                    }
                }

                override fun onBackPressed(position: Int): Boolean {
                    if (PinHelper.isPinActivityStarted()) return true
                    handleBackFromCategory(position)
                    return true
                }

                private fun handleBackFromCategory(currentPosition:Int){
                    if (isBackAnimationInProgress || PinHelper.isPinActivityStarted()) return

                    currentCategoryPosition = currentPosition
                    val holder = preferencesFilterGridView!!.findViewHolderForAdapterPosition(currentPosition) as CategoryItemViewHolder
                    preferencesFilterCategoryAdapter.setActiveFilter(holder)

                    if(isMultipleTypes){
                        // focus to types menu
                        val alpha: Animation = AlphaAnimation(0f, 1f)
                        alpha.duration = animationDuration
                        alpha.fillAfter = true
                        alpha.interpolator = DecelerateInterpolator()
                        preferenceTypesGridView!!.startAnimation(alpha)

                        preferenceTypesGridView!!.requestFocus()
                    }else{
                        // focus to home menu
                        listener.requestFocusOnTopMenu()
                    }

                    if(isMultipleTypes || !isFromInput){
                        val translate: Animation =
                            if(isMultipleTypes)
                                TranslateAnimation(0F,0F, -2*animationTransitionDistance, -animationTransitionDistance)
                            else
                                TranslateAnimation(0F,0F, -animationTransitionDistance, 0F)

                        translate.duration = animationDuration
                        translate.interpolator = DecelerateInterpolator()
                        translate.fillAfter = true
                        if(!isMultipleTypes){
                            isBackAnimationInProgress = true
                            translate.setAnimationListener(object : AnimationListener {
                                override fun onAnimationStart(animation: Animation?) {
                                    // ignore
                                }

                                override fun onAnimationEnd(animation: Animation?) {
                                    isBackAnimationInProgress = false
                                }

                                override fun onAnimationRepeat(animation: Animation?) {
                                    // ignore
                                }
                            })
                        }
                        scrollableContainer!!.startAnimation(translate)
                    }
                }
            }
    }

    private fun handleCategoryFocus(widgetId: Int) {
        if (widgetId == ReferenceWorldHandler.WidgetId.PREFERENCES_ADS_TARGETING && adsTargetingWidget?.information?.isInternetConnectionAvailable == false) return
        if (widgetId != ReferenceWorldHandler.WidgetId.PREFERENCES_SYSTEM_INFORMATION &&
            widgetId != ReferenceWorldHandler.WidgetId.FEEDBACK) {
                val holder =
            preferencesFilterGridView!!.findViewHolderForAdapterPosition(
                currentCategoryPosition
            ) as CategoryItemViewHolder
            preferencesFilterCategoryAdapter.setActiveFilter(holder)
        }
    }
    @RequiresApi(Build.VERSION_CODES.P)
    fun handleSubCategoryData(position: Int, widgetId: Int, context: Context) {
        selectedMenu=position
        when (widgetId) {
            ReferenceWorldHandler.WidgetId.PREFERENCES_SETUP -> {
                //Setup
                if (setupWidget == null) {
                    setupWidget = PreferencesSetupWidget(
                        context,
                        object : PreferencesSetupWidget.PreferencesSetupListener {
                            override fun onPrefsCategoriesRequestFocus() {
                                requestFocusOnCategory()
                            }

                            override fun onChannelScanClicked() {
                                listener.onChannelsScanClicked()
                            }

                            override fun onChannelsSettingsClicked() {
                                listener.onChannelsSettingsClicked()
                            }

                            override fun onPictureSettingsClicked() {
                                listener.onPictureSettingsClicked()
                            }

                            override fun onScreenSettingsClicked() {
                                listener.onScreenSettingsClicked()
                            }

                            override fun onSoundSettingsClicked() {
                                listener.onSoundSettingsClicked()
                            }

                            override fun onChannelEditClicked() {
                                listener.onChannelsEditClicked()
                            }

                            override fun onParentalControlClicked() {
                                listener.onParentalControlClicked()
                            }

                            override fun onDefaultChannelSelected(tvChannel: TvChannel) {
                                listener.onDefaultChannelSelected(tvChannel)
                            }

                            override fun onDisplayModeSelected(selectedMode: Int) {
                                listener.onDisplayModeSelected(selectedMode)
                            }

                            override fun onEvaluationLicenseClicked() {
                                listener.onEvaluationLicenseClicked()
                            }

//                            override fun onOpenSourceLicenceClicked() {
//                                listener.onOpenSourceLicenceClicked()
//                            }

                            override fun onAspectRatioClicked(position: Int) {
                                listener.onAspectRatioClicked(position)
                            }

                            override fun setInteractionChannel(isSelected: Boolean) {
                                listener.setInteractionChannel(isSelected)
                            }

                            override fun onEpgLanguageSelected(language: String) {
                                listener.onEpgLanguageSelected(language)

                            }

                            override fun getPrefsValue(key: String, value: Any?): Any? {
                                return listener.getPrefsValue(key, value)
                            }

                            override fun setPrefsValue(key: String, defValue: Any?) {
                                listener.setPrefsValue(key, defValue)
                            }

                            override fun onSkipChannelSelected(
                                tvChannel: TvChannel,
                                status: Boolean
                            ) {
                                listener.onSkipChannelSelected(tvChannel, status)
                            }

                            override fun onSwapChannelSelected(
                                firstChannel: TvChannel,
                                secondChannel: TvChannel,
                                previousPosition: Int,
                                newPosition: Int
                            ) {
                                listener.onSwapChannelSelected(firstChannel, secondChannel, previousPosition, newPosition)
                            }

                            override fun onMoveChannelSelected(
                                moveChannelList: ArrayList<TvChannel>,
                                previousPosition: Int,
                                newPosition: Int,
                                channelMap: HashMap<Int, String>
                            ) {
                                listener.onMoveChannelSelected(moveChannelList, previousPosition, newPosition, channelMap)
                            }

                            override fun onDeleteChannelSelected(tvChannel: TvChannel, index: Int) {
                                listener.onDeleteChannelSelected(tvChannel, index)
                            }

                            override fun getSignalAvailable() : Boolean {
                                return listener.getSignalAvailable()
                            }

                            override fun setPowerOffTime(value: Int, time: String) {
                                return listener.setPowerOffTime(value, time)
                            }
                            
                            override fun onPowerClicked() {
                                listener.onPowerClicked()
                            }

                            override fun onExit() {
                                listener.onExit()
                            }

                            override fun getPreferenceDeepLink(): DeepLink? {
                                return listener.getPreferenceDeepLink()
                            }

                            override fun startOadScan() {
                                return listener.startOadScan()
                            }

                            override fun onClearChannelSelected() {
                                listener.onClearChannelSelected()
                            }

                            override fun onChannelClicked() {
                                listener.onChannelClicked()
                            }

                            override fun onBlueMuteChanged(isEnabled: Boolean) {
                                listener.onBlueMuteChanged(isEnabled)
                            }

                            override fun noSignalPowerOffChanged(isEnabled: Boolean) {
                                listener.noSignalPowerOffChanged(isEnabled)
                            }

                            override fun noSignalPowerOffTimeChanged() {
                                listener.noSignalPowerOffTimeChanged()
                            }

                            override fun getUtilsModule(): UtilsInterface {
                                return  listener.getUtilsModule()
                            }

                            override fun isLcnEnabled(): Boolean {
                                return listener.isLcnEnabled()
                            }

                            override fun enableLcn(enable: Boolean) {
                                listener.enableLcn(enable)
                            }

                            override fun dispatchKey(
                                keyCode: Int,
                                event: Any
                            ): Boolean {
                                return false
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
                }
                listener.getPreferencesSetupData()
                container?.addView(setupWidget?.view)
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_AUDIO -> {
                //Audio
                if (audio == null) {
                    audio =
                        PreferencesAudioWidget(
                            context,
                            object : PreferencesAudioWidget.PreferencesAudioListener {
                                override fun onPrefsCategoriesRequestFocus(position: Int) {
                                    requestFocusOnCategory()

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

                                override fun getFirstAudioLanguage(): LanguageCode? {
                                    return listener.getFirstAudioLanguage(activeType)
                                }

                                override fun onFirstAudioLanguageSelected(languageCode: LanguageCode) {
                                    listener.onFirstAudioLanguageSelected(languageCode, activeType)
                                }

                                override fun getSecondAudioLanguage(): LanguageCode? {
                                    return listener.getSecondAudioLanguage(activeType)
                                }

                                override fun onSecondAudioLanguageSelected(languageCode: LanguageCode) {
                                    listener.onSecondAudioLanguageSelected(languageCode, activeType)
                                }

                                override fun onAudioTypeClicked(position: Int) {
                                    listener.onAudioTypeClicked(position)
                                }

                                override fun onVisuallyImpairedValueChanged(
                                    position: Int,
                                    isEnabled: Boolean
                                ) {
                                    listener.onVisuallyImpairedValueChanged(position, isEnabled)
                                }

                                override fun onVolumeChanged(newVolumeValue: Int) {
                                    listener.onVolumeChanged(newVolumeValue)
                                }

                                override fun onFaderValueChanged(newValue: Int) {
                                    listener.onFaderValueChanged(newValue)
                                }

                                override fun onAudioViValueChanged(newValue: Int) {
                                    listener.onAudioViValueChanged(newValue)
                                }

                                override fun setPrefsValue(key: String, defValue: Any?) {
                                    listener.setPrefsValue(key, defValue)
                                }

                                override fun setAudioDescriptionEnabled(enable: Boolean) {
                                    listener.setAudioDescriptionEnabled(enable)
                                }

                                override fun setHearingImpairedEnabled(enable: Boolean) {
                                    listener.setHearingImpairedEnabled(enable)
                                }

                                override fun dispatchKey(
                                    keyCode: Int,
                                    event: Any
                                ): Boolean {
                                    return false
                                }
                            })
                }
                listener.getAudioInformationData(activeType)
                container?.addView(audio?.view)
                preferencesFilterCategoryAdapter.requestFocus(
                    preferencesFilterCategoryAdapter.selectedItem
                )
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_PARENTAL_CONTROL -> {
                if (parentalWidget == null) {
                    parentalWidget = PreferencesParentalWidget(
                            context,
                            object : PreferencesParentalWidget.PreferenceParentalListener {
                                override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                                    listener.showToast(text, duration)
                                }

                                override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                                    listener.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                                }

                                override fun isAccessibilityEnabled(): Boolean {
                                    return listener.isAccessibilityEnabled()
                                }

                                override fun onPrefsCategoriesRequestFocus() {
                                    requestFocusOnCategory()

                                }

                                override fun getPrefsValue(key: String, value: Any?): Any? {
                                    return listener.getPrefsValue(key, value)
                                }

                                override fun setPrefsValue(key: String, defValue: Any?) {
                                    listener.setPrefsValue(key, defValue)
                                }

                                override fun lockChannel(tvChannel: TvChannel, selected: Boolean) {
                                    listener.lockChannel(tvChannel, selected)
                                }

                                override fun blockInput(
                                    inputSourceData: InputSourceData,
                                    blocked: Boolean
                                ) {
                                    listener.blockInput(inputSourceData, blocked)
                                }

                                override fun isParentalControlsEnabled():Boolean {
                                    if (activeType == PrefType.BASE) {
                                        return listener.isAnokiParentalControlsEnabled()
                                    }
                                    return listener.isParentalControlsEnabled()
                                }

                                override fun setParentalControlsEnabled(enabled: Boolean): Boolean {
                                    if (activeType == PrefType.BASE) {
                                        if (listener.checkNetworkConnection()) {
                                            listener.setAnokiParentalControlsEnabled(enabled)
                                            return true
                                        }
                                        return false
                                    } else {
                                        listener.setParentalControlsEnabled(enabled)
                                        return true
                                    }
                                }

                                override fun setContentRatingSystemEnabled(
                                    contentRatingSystem: ContentRatingSystem,
                                    enabled: Boolean
                                ) {
                                    listener.setContentRatingSystemEnabled(contentRatingSystem, enabled)
                                }

                                override fun setContentRatingLevel(index: Int) {
                                    listener.setContentRatingLevel(index)
                                }

                                override fun getContentRatingLevelIndex(): Int {
                                    return listener.getContentRatingLevelIndex()
                                }

                                override fun setRatingBlocked(
                                    contentRatingSystem: ContentRatingSystem,
                                    it: ContentRatingSystem.Rating,
                                    data: Boolean
                                ): Boolean {
                                    return listener.setRatingBlocked(contentRatingSystem, it, data)
                                }

                                override fun setRelativeRatingsEnabled(
                                    contentRatingSystem: ContentRatingSystem,
                                    it: ContentRatingSystem.Rating,
                                    data: Boolean
                                ) {
                                    listener.setRelativeRatingsEnabled(contentRatingSystem, it, data)
                                }

                                override fun isRatingBlocked(
                                    contentRatingSystem: ContentRatingSystem,
                                    it: ContentRatingSystem.Rating
                                ): Boolean {
                                    return listener.isRatingBlocked(contentRatingSystem,it)
                                }

                                override fun isSubRatingEnabled(
                                    contentRatingSystem: ContentRatingSystem,
                                    it: ContentRatingSystem.Rating,
                                    subRating: ContentRatingSystem.SubRating
                                ): Boolean {
                                    return listener.isSubRatingEnabled(contentRatingSystem, it, subRating)
                                }

                                override fun setSubRatingBlocked(
                                    contentRatingSystem: ContentRatingSystem,
                                    rating: ContentRatingSystem.Rating,
                                    subRating: ContentRatingSystem.SubRating,
                                    data: Boolean
                                ) {
                                    return listener.setSubRatingBlocked(contentRatingSystem,rating,subRating,data)
                                }

                                override fun setRelativeRating2SubRatingEnabled(
                                    contentRatingSystem: ContentRatingSystem,
                                    data: Boolean,
                                    rating: ContentRatingSystem.Rating,
                                    subRating: ContentRatingSystem.SubRating
                                ) {
                                    return listener.setRelativeRating2SubRatingEnabled(contentRatingSystem,data,rating,subRating)
                                }

                                override fun setBlockUnrated(blocked: Boolean) {
                                    listener.setBlockUnrated(blocked)
                                }

                                override fun getRRT5DimInfo(index: Int): MutableList<String> {
                                    return listener.getRRT5DimInfo(index)
                                }

                                override fun getRRT5CrsInfo(regionName: String): MutableList<ContentRatingSystem> {
                                    return listener.getRRT5CrsInfo(regionName)
                                }

                                override fun getRRT5LevelInfo(
                                    countryIndex: Int,
                                    dimIndex: Int
                                ): MutableList<String> {
                                    return listener.getRRT5LevelInfo(countryIndex, dimIndex)
                                }

                                override fun getSelectedItemsForRRT5Level(): HashMap<Int, Int> {
                                    return listener.getSelectedItemsForRRT5Level()
                                }

                                override fun rrt5BlockedList(regionPosition :Int,position: Int): HashMap<Int, Int> {
                                    return listener.rrt5BlockedList(regionPosition,position)
                                }

                                override fun setSelectedItemsForRRT5Level(
                                    regionIndex: Int,
                                    dimIndex: Int,
                                    levelIndex: Int
                                ) {
                                    listener.setSelectedItemsForRRT5Level(regionIndex,
                                            dimIndex,
                                            levelIndex)
                                }

                                override fun resetRRT5() {
                                    listener.resetRRT5()
                                }

                                override fun getRRT5Regions(): MutableList<String> {
                                    return listener.getRRT5Regions()
                                }

                                override fun getChannelSourceType(tvChannel: TvChannel): String {
                                    return listener.getChannelSourceType(tvChannel)
                                }

                                override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                                        return false
                                }

                                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                                    listener.setSpeechText(text = text, importance = importance)
                                }

                                override fun getAnokiRatingLevel(): Int {
                                    return listener.getAnokiRatingLevel()
                                }

                                override fun setAnokiRatingLevel(level: Int) {
                                    listener.setAnokiRatingLevel(level)
                                }

                                override fun checkNetworkConnection(): Boolean {
                                    return listener.checkNetworkConnection()
                                }

                                override fun startChangePinActivity() {
                                    PinHelper.setPinResultCallback(object : PinHelper.PinCallback {
                                        override fun pinCorrect() {}
                                        override fun pinIncorrect() {}
                                    })
                                    val title = ConfigStringsManager.getStringById("change_pin")
                                    val desc = "Press Forgot PIN button"
                                    PinHelper.startPinActivity(title, desc)
                                }

                                override fun getLockedChannelIdsFromPrefs() : MutableSet<String> {
                                    return listener.getLockedChannelIdsFromPrefs()
                                }
                            })
                }
                listener.getParentalInformationData(activeType)
                container?.addView(parentalWidget?.view)
                if (ReferenceApplication.parentalControlDeepLink) {
                    preferencesFilterCategoryAdapter.selectedItem = 1
                    parentalWidget?.view?.alpha = 0f
                }
                preferencesFilterCategoryAdapter.requestFocus(
                    preferencesFilterCategoryAdapter.selectedItem
                )
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_CLOSED_CAPTIONS -> {
                if (closedCaptionsWidget == null) {
                    closedCaptionsWidget = PreferencesClosedCaptionsWidget(
                            context,
                            object : PreferencesClosedCaptionsWidget.PreferencesClosedCationsListener {
                                override fun onPrefsCategoriesRequestFocus() {
                                    requestFocusOnCategory()

                                }

                                override fun saveUserSelectedCCOptions(
                                    ccOptions: String,
                                    newValue: Int
                                ) {
                                    listener.saveUserSelectedCCOptions(ccOptions, newValue)
                                }

                                override fun resetCC() {
                                    listener.resetCC()
                                }

                                override fun setCCInfo() {
                                    listener.setCCInfo()
                                }

                                override fun disableCCInfo() {
                                    listener.disableCCInfo()
                                }

                                override fun setCCWithMute(isEnabled: Boolean) {
                                    listener.setCCWithMute(isEnabled)
                                }

                                override fun lastSelectedTextOpacity(position: Int) {
                                    listener.lastSelectedTextOpacity(position)
                                }

                                override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                                    return false
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
                }
                listener.getClosedCationsInformationData()
                container?.addView(closedCaptionsWidget?.view)
                preferencesFilterCategoryAdapter.requestFocus(
                    preferencesFilterCategoryAdapter.selectedItem
                )
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_SUBTITLES -> {
                //Subtitles
                if (subtitle == null) {
                    subtitle =
                        PreferencesSubtitleWidget(
                            context,
                            object :
                                PreferencesSubtitleWidget.PreferencesSubtitleListener {
                                override fun isAnalogSubtitleEnabled(): Boolean {
                                    return listener.isAnalogSubtitleEnabled(activeType)
                                }
                                override fun isDigitalSubtitleEnabled(): Boolean {
                                    return listener.isDigitalSubtitleEnabled(activeType)
                                }

                                override fun onPrefsCategoriesRequestFocus() {
                                    requestFocusOnCategory()
                                }

                                override fun getFirstSubtitleLanguage(): LanguageCode? {
                                    return listener.getFirstSubtitleLanguage(activeType)
                                }

                                override fun onFirstSubtitleLanguageSelected(
                                    languageCode: LanguageCode
                                ) {
                                    listener.onFirstSubtitleLanguageSelected(
                                        languageCode, activeType
                                    )
                                }

                                override fun getSecondSubtitleLanguage(): LanguageCode? {
                                    return listener.getSecondSubtitleLanguage(activeType)
                                }

                                override fun onSecondSubtitleLanguageSelected(
                                    languageCode: LanguageCode
                                ) {
                                    listener.onSecondSubtitleLanguageSelected(
                                        languageCode, activeType
                                    )
                                }

                                override fun onSubtitlesEnabledClicked(isEnabled: Boolean) {
                                    listener.onSubtitlesEnabledClicked(
                                        isEnabled, activeType
                                    )
                                }

                                override fun onSubtitlesTypeClicked(position: Int) {
                                    listener.onSubtitlesTypeClicked(
                                        position
                                    )
                                }

                                override fun onSubtitlesAnalogTypeClicked(position: Int) {
                                    listener.onSubtitlesAnalogTypeClicked(
                                        position
                                    )
                                }

                                override fun onClosedCaptionChanged(isCaptionEnabled: Boolean) {
                                    listener.onClosedCaptionChanged(isCaptionEnabled)
                                }

                                override fun dispatchKey(
                                    keyCode: Int,
                                    event: Any
                                ): Boolean {
                                    return false
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
                }
                listener.getSubtitleInformationData(activeType)
                container?.addView(subtitle?.view)

                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_CAM_INFO -> {
                //Cam Info
                if (camInfoWidget == null) {
                    camInfoWidget = PreferencesCamInfoWidget(object :
                        PreferencesCamInfoWidget.PreferencesCamInfoWidgetListener {
                        override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                            return false
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

                        override fun getModuleInfoData() {
                            listener.getCamInfoModuleInfoData()
                        }

                        override fun requestFocusOnPreferencesMenu() {
                            preferencesFilterGridView!!.getChildAt(selectedMenu).requestFocus()
                        }

                        override fun onSoftwareDownloadPressed() {
                            listener.onCamInfoSoftwareDownloadPressed()
                        }

                        override fun onSubscriptionStatusPressed() {
                            listener.onCamInfoSubscriptionStatusPressed()
                        }

                        override fun onEventStatusPressed() {
                            listener.onCamInfoEventStatusPressed()
                        }

                        override fun onTokenStatusPressed() {
                            listener.onCamInfoTokenStatusPressed()
                        }

                        override fun onChangeCaPinPressed() {
                            listener.onCamInfoChangeCaPinPressed()
                        }

                        override fun getMaturityRating(): String {
                            return listener.getCamInfoMaturityRating()
                        }

                        override fun onConaxCaMessagesPressed() {
                            listener.onCamInfoConaxCaMessagesPressed()
                        }

                        override fun onAboutConaxCaPressed() {
                            listener.onCamInfoAboutConaxCaPressed()
                        }

                        override fun getSettingsLanguageList() {
                            listener.getCamInfoSettingsLanguages()
                        }

                        override fun onSettingsLanguageSelected(position: Int) {
                            listener.onCamInfoSettingsLanguageSelected(position)
                        }

                        override fun activatePopUpMessage(activate: Boolean) {
                            listener.onCamInfoPopUpMessagesActivated(activate)
                        }

                        override fun isPopUpMessagesActivated(): Boolean {
                            return listener.isCamInfoPopUpMessagesActivated()
                        }

                        override fun onPrefsCategoriesRequestFocus(position: Int) {
                            requestFocusOnCategory()

                        }

                        override fun getCiPlusInterface(): CiPlusInterface {
                            return listener.getCiPlusInterface()
                        }

                        override fun storePrefsValue(tag: String, defValue: String) {
                            listener.storePrefsValue(tag, defValue)
                        }

                        override fun getPrefsValue(tag: String, defValue: String): String {
                            return listener.getPrefsValue(tag, defValue)
                        }

                        override fun startCamScan() {
                            return listener.startCamScan()
                        }

                        override fun changeCamPin() {
                            return listener.changeCamPin()
                        }

                        override fun selectMenuItem(position: Int) {
                            return listener.selectMenuItem(position)
                        }

                        override fun enterMMI() {
                            return listener.enterMMI()
                        }

                        override fun deleteProfile(profileName: String) {
                            listener.deleteProfile(profileName)
                        }

                        override fun doCAMReconfiguration(camTypePreference: CamTypePreference) {
                            listener.doCAMReconfiguration(camTypePreference)
                        }
                    })
                }
                listener.getPreferencesCamData()
                container?.addView(camInfoWidget?.view)
                return
            }

            ReferenceWorldHandler.WidgetId.PREFERENCES_HBBTV_SETTINGS -> {
                //HBBTV Settings
                if (hbbTvWidget == null) {
                    hbbTvWidget =
                        PreferencesHbbTvWidget(
                            context,
                            object : PreferencesHbbTvWidget.PreferencesHbbListener {

                                override fun onPrefsCategoriesRequestFocus() {
                                    requestFocusOnCategory()
                                }

                                override fun onHbbSupportSwitch(isEnabled: Boolean) {
                                    listener.onHbbSupportSwitch(isEnabled)
                                }

                                override fun onHbbTrackSwitch(isEnabled: Boolean) {
                                    listener.onHbbTrackSwitch(isEnabled)
                                }

                                override fun onHbbPersistentStorageSwitch(isEnabled: Boolean) {
                                    listener.onHbbPersistentStorageSwitch(isEnabled)
                                }

                                override fun onHbbBlockTracking(isEnabled: Boolean) {
                                    listener.onHbbBlockTracking(isEnabled)
                                }

                                override fun onHbbTvDeviceId(isEnabled: Boolean) {
                                    listener.onHbbTvDeviceId(isEnabled)
                                }

                                override fun onHbbTvCookieOptionSelected(position: Int) {
                                    listener.onHbbCookieSettingsSelected(position)
                                }

                                override fun onHbbTvResetDeviceId() {
                                    listener.onHbbTvResetDeviceId()
                                }

                                override fun dispatchKey(
                                    keyCode: Int,
                                    event: Any
                                ): Boolean {
                                    return false
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
                }
                listener.getHbbTvInformation()
                container?.addView(hbbTvWidget?.view)

                return
            }

            ReferenceWorldHandler.WidgetId.PREFERENCES_TELETEXT -> {
                //Teletext
                if (txtWidget == null) {
                    txtWidget = PreferencesTeletextWidget(
                        context,
                        object : PreferencesTeletextWidget.PreferencesTeletextListener {

                            override fun onPrefsCategoriesRequestFocus() {
                                requestFocusOnCategory()
                            }

                            override fun onTTXDigitalLanguageSelected(position: Int, language: String
                            ) {
                                listener.onTTXDigitalLanguageSelected(position, language)
                            }

                            override fun onTTXDecodeLanguageSelected(position: Int) {
                                listener.onTTXDecodeLanguageSelected(position)
                            }

                            override fun dispatchKey(
                                keyCode: Int,
                                event: Any
                            ): Boolean {
                                return false
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
                }
                listener.getPreferencesTxtData()
                container?.addView(txtWidget?.view)
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_PVR_TIMESHIFT -> {
                //Teletext
                if (pvrTimeshiftWidget == null) {
                    pvrTimeshiftWidget = PreferencesPvrTimeshiftWidget(
                        context,
                        object : PreferencesPvrTimeshiftWidget.PreferencesPvrTimeshiftWidgetListener {

                            override fun onPrefsCategoriesRequestFocus() {
                                requestFocusOnCategory()
                            }

                            override fun requestDeviceList() {

                            }

                            override fun onTimeshiftStatusChanged(enabled: Boolean) {
                                listener.onTimeshiftStatusChanged(enabled)
                            }

                            override fun onExit() {
                                listener.onExit()

                            }

                            override fun getPreferenceDeepLink() : DeepLink? {
                               return listener.getPreferenceDeepLink()
                            }

                            override fun dispatchKey(
                                keyCode: Int,
                                event: Any
                            ): Boolean {
                                return false
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
                }
                listener.getPreferencesPvrTimeshiftData()
                container?.addView(pvrTimeshiftWidget?.view)
                return
            }

            ReferenceWorldHandler.WidgetId.PREFERENCES_ADS_TARGETING -> {
                //Ads Targeting
                if (adsTargetingWidget == null) {
                    adsTargetingWidget =
                        PreferencesAdsTargetingWidget(context, object :PreferencesAdsTargetingWidget.PreferenceAdsTargetingListener{
                            override fun onPrefsCategoriesRequestFocus() {
                                requestFocusOnCategory()
                            }

                            override fun onAdsTargetingChange(enable: Boolean) {
                                listener.onAdsTargetingChange(enable)
                            }

                            override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                                return false
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
                }
                listener.getAdsTargetingData(activeType)
                container?.addView(adsTargetingWidget?.view)
                return
            }

            ReferenceWorldHandler.WidgetId.PREFERENCES_SYSTEM_INFORMATION -> {
                //System Information
                if (systemInformation == null) {
                    systemInformation =
                        PreferencesSystemInformationWidget(object :
                            PreferencesSystemInformationWidget.PreferencesSystemInformationListener {
                            override fun getConfigInfo(nameOfInfo: String): Boolean {
                                return listener.getConfigInfo(nameOfInfo)
                            }

                            override fun dispatchKey(
                                keyCode: Int,
                                event: Any
                            ): Boolean {
                                return false
                            }
                        })
                }
                listener.getSystemInformationData()
                container?.addView(systemInformation?.view)
                return
            }

            ReferenceWorldHandler.WidgetId.PREFERENCES_OPEN_SOURCE_LICENSES -> {
                //System Information
                if (openSourceLicensesWidget == null) {
                    openSourceLicensesWidget =
                        PreferencesOpenSourceLicensesWidget(context, object:
                            PreferencesOpenSourceLicensesWidget.PreferencesOpenSourceLicensesWidgetListener {
                            override fun onPrefsCategoriesRequestFocus() {
                                requestFocusOnCategory()
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

                            override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                                return false
                            }
                        })
                }
                container?.addView(openSourceLicensesWidget!!.view)
                return
            }

            ReferenceWorldHandler.WidgetId.TERMS_OF_SERVICE -> {
                //Terms Of Service
                if (termsOfServiceWidget == null) {
                    termsOfServiceWidget =
                        PreferencesTermsOfServiceWidget(context, object :PreferencesTermsOfServiceWidget.PreferenceTermsOfServiceListener{
                            override fun onPrefsCategoriesRequestFocus() {
                                requestFocusOnCategory()
                            }

                            override fun onClickTermsOfService() {
                                listener.onClickTermsOfService()
                            }

                            override fun onClickPrivacyPolicy() {
                                listener.onClickPrivacyPolicy()
                            }

                            override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                                return false
                            }

                            override fun setSpeechText(
                                vararg text: String,
                                importance: SpeechText.Importance
                            ) {
                                listener.setSpeechText(text = text, importance = importance)
                            }

                            override fun showToast(
                                text: String,
                                duration: UtilsInterface.ToastDuration
                            ) {
                                listener.showToast(text, duration)
                            }

                            override fun setSpeechTextForSelectableView(
                                vararg text: String,
                                importance: SpeechText.Importance,
                                type: Type,
                                isChecked: Boolean
                            ) {
                                listener.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                            }

                        })
                }
                listener.getTermsOfServiceData(activeType)
                container?.addView(termsOfServiceWidget?.view)
                return
            }

            ReferenceWorldHandler.WidgetId.FEEDBACK -> {
                //Feedback
                if (feedbackWidget == null) {
                    feedbackWidget =
                        PreferencesFeedbackWidget(context, object :GWidgetListener{
                            override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                                return false
                            }
                        })
                }
                container?.addView(feedbackWidget?.view)
                return
            }
        }
    }

    private fun startPinInsertActivity() {
        PinHelper.setPinResultCallback(object : PinHelper.PinCallback {
            @SuppressLint("SuspiciousIndentation")
            override fun pinCorrect() {
                Handler().post {
                    val holder =
                        preferencesFilterGridView!!.findViewHolderForAdapterPosition(
                            currentCategoryPosition
                        ) as CategoryItemViewHolder

                        preferencesFilterCategoryAdapter?.setActiveFilter(holder)
                        parentalWidget?.setFocusToGrid()
                    }
                }
            override fun pinIncorrect() {}
        })

        val title = ConfigStringsManager.getStringById("enter_parental_settings")
        PinHelper.startPinActivity(title, "")
    }
    private  fun showPin(){
        ReferenceApplication.runOnUiThread {
            val sceneData = PinSceneData(
                ReferenceApplication.worldHandler!!.active!!.id,
                ReferenceApplication.worldHandler!!.active!!.instanceId,
                ConfigStringsManager.getStringById("enter_parental_settings"),
                object : PinSceneData.PinSceneDataListener {
                    override fun onPinSuccess() {
                        Handler().post{
                            val holder =
                                preferencesFilterGridView!!.findViewHolderForAdapterPosition(
                                currentCategoryPosition
                            ) as CategoryItemViewHolder
                            preferencesFilterCategoryAdapter.setActiveFilter(holder)
                            parentalWidget!!.setFocusToGrid()
                        }
                        listener.stopSpeech()
                    }

                    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                        listener.showToast(text, duration)
                    }
                })

            ReferenceApplication.worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PIN_SCENE,
                SceneManager.Action.SHOW_OVERLAY, sceneData
            )

        }
    }

    private fun handleCategoryClicked(position: Int) {
        when(position){
            ReferenceWorldHandler.WidgetId.PREFERENCES_SETUP ->setupWidget?.setFocusToGrid()
            ReferenceWorldHandler.WidgetId.PREFERENCES_AUDIO->audio?.setFocusToGrid()
            ReferenceWorldHandler.WidgetId.PREFERENCES_SUBTITLES -> subtitle?.setFocusToGrid()
            ReferenceWorldHandler.WidgetId.PREFERENCES_ADS_TARGETING -> {
                if(adsTargetingWidget?.information!!.isInternetConnectionAvailable) adsTargetingWidget?.setFocusToGrid()
            }
            ReferenceWorldHandler.WidgetId.TERMS_OF_SERVICE -> termsOfServiceWidget?.setFocusToGrid()
            ReferenceWorldHandler.WidgetId.PREFERENCES_CAM_INFO->camInfoWidget?.setFocusToGrid()
            ReferenceWorldHandler.WidgetId.PREFERENCES_HBBTV_SETTINGS ->hbbTvWidget?.setFocusToGrid()
            ReferenceWorldHandler.WidgetId.PREFERENCES_TELETEXT -> txtWidget?.setFocusToGrid()
            ReferenceWorldHandler.WidgetId.PREFERENCES_PARENTAL_CONTROL -> {
                if (PinHelper.USE_PIN_INSERT_ACTIVITY) {
                    startPinInsertActivity()
                } else {
                    showPin()
                }
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_CLOSED_CAPTIONS -> closedCaptionsWidget?.setFocusToGrid()
            ReferenceWorldHandler.WidgetId.PREFERENCES_PVR_TIMESHIFT -> pvrTimeshiftWidget?.setFocusToGrid()
            ReferenceWorldHandler.WidgetId.PREFERENCES_OPEN_SOURCE_LICENSES -> openSourceLicensesWidget?.setFocusToGrid()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun refresh(data: Any) {
        if (data is List<*>) {
            if (data.isNotEmpty() && data[0] is CategoryItem) {
                typeCategories.clear()
                data.forEach {
                    typeCategories.add(it as CategoryItem)
                }
                isMultipleTypes = typeCategories.size>1

                // Preparing types list
                currentTypePosition = 0
                if (ReferenceApplication.parentalControlDeepLink) {
                    if (isMultipleTypes) {
                        currentTypePosition = 1
                        preferenceTypesCategoryAdapter.selectedItem = currentTypePosition
                    }
                }

                if(isMultipleTypes){

                    if(listener.getPreferenceDeepLink()!=null){
                        typeCategories.forEach {
                            if(it.prefType == listener.getPreferenceDeepLink()!!.prefType){
                                currentTypePosition = typeCategories.indexOf(it)
                            }
                        }
                    }

                    preferenceTypesCategoryAdapter.selectedItem = currentTypePosition
                    preferenceTypesCategoryAdapter.refresh(typeCategories)
                    scrollableContainer!!.translationY = animationTransitionDistance
                }

                // To instantly open preference menu when it is opened from deeplink
                if (listener.getPreferenceDeepLink() != null) {
                    scrollableContainer!!.translationY = -Utils.getDimens(R.dimen.custom_dim_61)
                    preferenceTypesGridView!!.alpha = 0f
                    if (listener.getPreferenceDeepLink()!!.widgetId ==
                        ReferenceWorldHandler.WidgetId.PREFERENCES_SETUP) {
                         preferencesFilterGridView!!.alpha = 0f
                    }
                }

                if (ReferenceApplication.parentalControlDeepLink) {
                    scrollableContainer?.translationY = -Utils.getDimens(R.dimen.custom_dim_61)
                    preferenceTypesGridView!!.alpha = 0f
                }

                // Preparing categories list
                activeType = typeCategories[currentTypePosition].prefType
                listener.getAvailablePreferencesCategories(object: IAsyncDataCallback<List<CategoryItem>>{
                    override fun onFailed(error: Error) {
                        //ignore
                    }

                    override fun onReceive(data: List<CategoryItem>) {
                        currentCategoryPosition = 0

                        if(listener.getPreferenceDeepLink()!=null){
                            data.forEach {
                                if(it.id == listener.getPreferenceDeepLink()!!.widgetId){
                                    currentCategoryPosition = data.indexOf(it)
                                }
                            }
                        }

                        if (ReferenceApplication.parentalControlDeepLink) {
                            currentCategoryPosition = 1
                        }

                        preferencesFilterCategoryAdapter.selectedItem = currentCategoryPosition

                        categories.clear()
                        categories.addAll(data)
                        preferencesFilterCategoryAdapter.refresh(categories)
                        container?.removeAllViews()

                        val widgetId = categories[currentCategoryPosition].id
                        widgetCleanUp(widgetId)
                        handleSubCategoryData(currentCategoryPosition, widgetId, ReferenceApplication.applicationContext())
                    }
                }, activeType)
            }
        }

        if (data is CategoryItem) {
            categories.add(data)
            preferencesFilterCategoryAdapter.refresh(categories)
        }

        if (data is PreferencesCamInfoInformation) {
            if (camInfoWidget != null) {
                camInfoWidget?.refresh(data)
            }
        }


        if (data is PreferenceSetupInformation) {
            if (setupWidget != null) {
                setupWidget?.refresh(data)
            }
        }

        if (data is PreferenceAudioInformation) {
            if (audio != null) {
                audio?.refresh(data)
            }
        }

        if (data is PreferenceSubtitleInformation) {
            if (subtitle != null) {
                subtitle?.refresh(data)
            }
        }

        if (data is PreferencesAdsTargetingInformation) {
            if (adsTargetingWidget != null) {
                adsTargetingWidget?.refresh(data)
            }
        }

        if (data is PreferencesTermsOfServiceInformation) {
            if (termsOfServiceWidget != null) {
                termsOfServiceWidget?.refresh(data)
            }
        }

        if (data is ReferenceSystemInformation) {
            if (systemInformation != null) {
                systemInformation?.refresh(data)
            }
        }

        if (data is CamInfoModuleInformation) {
            if (camInfoWidget != null) {
                camInfoWidget?.refresh(data)
            }
        }

        if (data is PreferencesTeletextInformation) {
            if (txtWidget != null) {
                txtWidget?.refresh(data)
            }
        }

        if (data is CamInfoLanguageData) {
            if (camInfoWidget != null) {
                camInfoWidget?.refresh(data)
            }
        }

        if (data is PreferencesHbbTVInfromation) {
            if (hbbTvWidget != null) {
                hbbTvWidget?.refresh(data)
            }
        }

        if (data is PreferencesParentalControlInformation) {
            if (parentalWidget != null) {
                parentalWidget?.refresh(data)
            }
        }

        if (data is PreferencesClosedCaptionsInformation) {
            if (closedCaptionsWidget != null) {
                closedCaptionsWidget?.refresh(data)
            }
        }
        if (data is PreferencesPvrTimeshiftInformation) {
            if (pvrTimeshiftWidget != null) {
                pvrTimeshiftWidget?.refresh(data)
            }
        }
        if(data is Boolean) {
            isFromInput = data
        }
        super.refresh(data)
    }

    private fun widgetCleanUp(id: Int) {
        when (id) {
            ReferenceWorldHandler.WidgetId.PREFERENCES_SETUP -> {
                audio = null
                subtitle = null
                camInfoWidget = null
                hbbTvWidget = null
                txtWidget = null
                systemInformation = null
                parentalWidget = null
                closedCaptionsWidget= null
                pvrTimeshiftWidget = null
                adsTargetingWidget = null
                openSourceLicensesWidget = null
                termsOfServiceWidget = null
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_AUDIO -> {
                setupWidget = null
                subtitle = null
                camInfoWidget = null
                hbbTvWidget = null
                txtWidget = null
                systemInformation = null
                parentalWidget = null
                closedCaptionsWidget= null
                pvrTimeshiftWidget = null
                adsTargetingWidget = null
                openSourceLicensesWidget = null
                termsOfServiceWidget = null
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_SUBTITLES -> {
                setupWidget = null
                audio = null
                camInfoWidget = null
                hbbTvWidget = null
                txtWidget = null
                systemInformation = null
                parentalWidget = null
                closedCaptionsWidget= null
                pvrTimeshiftWidget = null
                adsTargetingWidget = null
                openSourceLicensesWidget = null
                termsOfServiceWidget = null
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_CAM_INFO -> {
                setupWidget = null
                audio = null
                subtitle = null
                hbbTvWidget = null
                txtWidget = null
                systemInformation = null
                parentalWidget = null
                closedCaptionsWidget= null
                pvrTimeshiftWidget = null
                adsTargetingWidget = null
                openSourceLicensesWidget = null
                termsOfServiceWidget = null
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_HBBTV_SETTINGS -> {
                setupWidget = null
                audio = null
                subtitle = null
                camInfoWidget = null
                txtWidget = null
                systemInformation = null
                parentalWidget = null
                closedCaptionsWidget= null
                pvrTimeshiftWidget = null
                adsTargetingWidget = null
                openSourceLicensesWidget = null
                termsOfServiceWidget = null
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_TELETEXT -> {
                setupWidget = null
                audio = null
                subtitle = null
                camInfoWidget = null
                hbbTvWidget = null
                systemInformation = null
                parentalWidget = null
                closedCaptionsWidget= null
                pvrTimeshiftWidget = null
                adsTargetingWidget = null
                openSourceLicensesWidget = null
                termsOfServiceWidget = null
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_ADS_TARGETING -> {
                setupWidget = null
                audio = null
                subtitle = null
                txtWidget = null
                camInfoWidget = null
                hbbTvWidget = null
                systemInformation = null
                parentalWidget = null
                closedCaptionsWidget= null
                pvrTimeshiftWidget = null
                openSourceLicensesWidget = null
                termsOfServiceWidget = null
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_SYSTEM_INFORMATION -> {
                setupWidget = null
                audio = null
                subtitle = null
                txtWidget = null
                camInfoWidget = null
                hbbTvWidget = null
                parentalWidget = null
                closedCaptionsWidget= null
                pvrTimeshiftWidget = null
                adsTargetingWidget = null
                openSourceLicensesWidget = null
                termsOfServiceWidget = null
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_PARENTAL_CONTROL -> {
                setupWidget = null
                audio = null
                subtitle = null
                txtWidget = null
                camInfoWidget = null
                hbbTvWidget = null
                systemInformation= null
                closedCaptionsWidget= null
                pvrTimeshiftWidget = null
                adsTargetingWidget = null
                openSourceLicensesWidget = null
                termsOfServiceWidget = null
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_CLOSED_CAPTIONS -> {
                setupWidget = null
                audio = null
                subtitle = null
                txtWidget = null
                camInfoWidget = null
                hbbTvWidget = null
                systemInformation= null
                parentalWidget= null
                pvrTimeshiftWidget = null
                adsTargetingWidget = null
                openSourceLicensesWidget = null
                termsOfServiceWidget = null
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_PVR_TIMESHIFT -> {
                setupWidget = null
                audio = null
                subtitle = null
                txtWidget = null
                camInfoWidget = null
                hbbTvWidget = null
                systemInformation= null
                parentalWidget= null
                closedCaptionsWidget = null
                adsTargetingWidget = null
                openSourceLicensesWidget = null
                termsOfServiceWidget = null
                return
            }
            ReferenceWorldHandler.WidgetId.PREFERENCES_OPEN_SOURCE_LICENSES -> {
                setupWidget = null
                audio = null
                subtitle = null
                txtWidget = null
                camInfoWidget = null
                hbbTvWidget = null
                systemInformation= null
                parentalWidget= null
                pvrTimeshiftWidget = null
                closedCaptionsWidget = null
                adsTargetingWidget = null
                termsOfServiceWidget = null
                return
            }
            ReferenceWorldHandler.WidgetId.TERMS_OF_SERVICE -> {
                setupWidget = null
                audio = null
                subtitle = null
                txtWidget = null
                camInfoWidget = null
                hbbTvWidget = null
                systemInformation = null
                parentalWidget = null
                closedCaptionsWidget= null
                pvrTimeshiftWidget = null
                openSourceLicensesWidget = null
                adsTargetingWidget = null
                return
            }
        }
    }

    fun requestFocus() {
        if(isMultipleTypes){
            setFocusToTypes()
        }else{
            setFocusToSubcategory()
        }
    }

    fun setFocusToSubcategory() {
        val translate: Animation =
            if(isMultipleTypes)
                TranslateAnimation(0F, 0F, -animationTransitionDistance, -2*animationTransitionDistance)
            else
                TranslateAnimation(0F, 0F, 0F, -animationTransitionDistance)

        translate.fillAfter = true
        translate.duration = animationDuration
        if (!ReferenceApplication.parentalControlDeepLink) {
            scrollableContainer!!.startAnimation(translate)
        }
        if (preferencesFilterCategoryAdapter.selectedItem == -1) {
            preferencesFilterCategoryAdapter.selectedItem = 0
        }
        preferencesFilterGridView!!.preserveFocusAfterLayout = true
        preferencesFilterCategoryAdapter.shoudKeepFocusOnClick = true
        preferencesFilterGridView!!.requestFocus(preferencesFilterCategoryAdapter.selectedItem)

    }

    private fun setFocusToTypes() {
        val translate: Animation = TranslateAnimation(0F, 0F, 0f, -animationTransitionDistance)
        translate.fillAfter = true
        translate.duration = animationDuration
        if (!ReferenceApplication.parentalControlDeepLink) {
            scrollableContainer!!.startAnimation(translate)
        }
        if (preferenceTypesCategoryAdapter.selectedItem == -1) {
            preferenceTypesCategoryAdapter.selectedItem = 0
        }
        preferenceTypesGridView!!.preserveFocusAfterLayout = true
        preferenceTypesCategoryAdapter.shoudKeepFocusOnClick = true
        preferenceTypesGridView!!.requestFocus(preferenceTypesCategoryAdapter.selectedItem)
    }

    fun setFocusToPrefSub() {
        scrollableContainer!!.translationY = -animationTransitionDistance
        if (preferencesFilterCategoryAdapter.selectedItem == -1) {
            preferencesFilterCategoryAdapter.selectedItem = 0
        }
        preferencesFilterGridView!!.preserveFocusAfterLayout = true
        preferencesFilterCategoryAdapter.shoudKeepFocusOnClick = true
        currentCategoryPosition=preferencesFilterCategoryAdapter.selectedItem
        preferencesFilterGridView!!.requestFocus(preferencesFilterCategoryAdapter.selectedItem)

    }
    private fun requestFocusOnCategory(){
        preferencesFilterGridView!!.findViewHolderForAdapterPosition(selectedMenu)?.itemView?.requestFocus()
    }

    /**
     * To set left margin for top menus based on home scene
     *
     * @param startMarginInPixels start margin
     */
    fun setCategoriesMargin(startMarginInPixels : Int){
        val filterParams = preferencesFilterGridView!!.layoutParams as ConstraintLayout.LayoutParams
        filterParams.marginStart = startMarginInPixels
        preferencesFilterGridView!!.layoutParams = filterParams

        val typesParams = preferenceTypesGridView!!.layoutParams as ConstraintLayout.LayoutParams
        typesParams.marginStart = startMarginInPixels
        preferenceTypesGridView!!.layoutParams = typesParams
    }

    interface PreferencesWidgetListener : GWidgetListener, TTSSetterInterface, TTSStopperInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun getAvailablePreferencesCategories(callback: IAsyncDataCallback<List<CategoryItem>>, type: PrefType)
        fun requestFocusOnTopMenu()
        fun getAudioInformationData(type: PrefType)
        fun getFirstAudioLanguage(type: PrefType): LanguageCode?
        fun onFirstAudioLanguageSelected(languageCode: LanguageCode, type: PrefType)
        fun getSecondAudioLanguage(type: PrefType): LanguageCode?
        fun onSecondAudioLanguageSelected(languageCode: LanguageCode, type: PrefType)
        fun onAudioTypeClicked(position: Int)
        fun onFaderValueChanged(newValue: Int)
        fun onAudioViValueChanged(newValue: Int)
        fun onVisuallyImpairedValueChanged(position: Int, enabled: Boolean)
        fun onVolumeChanged(newVolumeValue: Int)
        fun getSubtitleInformationData(type: PrefType)
        fun getFirstSubtitleLanguage(type: PrefType): LanguageCode?
        fun onFirstSubtitleLanguageSelected(languageCode: LanguageCode, type: PrefType)
        fun getSecondSubtitleLanguage(type: PrefType): LanguageCode?
        fun onSecondSubtitleLanguageSelected(languageCode: LanguageCode, type: PrefType)
        fun onSubtitlesEnabledClicked(isEnabled: Boolean, type: PrefType)
        fun onSubtitlesTypeClicked(position: Int)
        fun onSubtitlesAnalogTypeClicked(position: Int)
        fun onClosedCaptionChanged(isCaptionEnabled :Boolean)
        fun getAdsTargetingData(type: PrefType)
        fun getTermsOfServiceData(type: PrefType)
        fun getSystemInformationData()
        fun getPreferencesSetupData()
        fun getPreferencesTxtData()
        fun getPreferencesCamData()
        fun onChannelsScanClicked()
        fun onChannelsEditClicked()
        fun onChannelsSettingsClicked()
        fun onParentalControlClicked()
        fun onDefaultChannelSelected(tvChannel: TvChannel)
        fun onDisplayModeSelected(selectedMode: Int)
        fun getCamInfoModuleInfoData()
        fun onCamInfoSoftwareDownloadPressed()
        fun onCamInfoSubscriptionStatusPressed()
        fun onCamInfoEventStatusPressed()
        fun onCamInfoTokenStatusPressed()
        fun onCamInfoChangeCaPinPressed()
        fun getCamInfoMaturityRating(): String
        fun onCamInfoConaxCaMessagesPressed()
        fun onCamInfoAboutConaxCaPressed()
        fun getCamInfoSettingsLanguages()
        fun onCamInfoSettingsLanguageSelected(position: Int)
        fun onCamInfoPopUpMessagesActivated(activated: Boolean)
        fun isCamInfoPopUpMessagesActivated(): Boolean
        fun getHbbTvInformation()
        fun onHbbSupportSwitch(isEnabled: Boolean)
        fun onHbbTrackSwitch(isEnabled: Boolean)
        fun onHbbPersistentStorageSwitch(isEnabled: Boolean)
        fun onHbbBlockTracking(isEnabled: Boolean)
        fun onHbbTvDeviceId(isEnabled: Boolean)
        fun onHbbCookieSettingsSelected(position: Int)
        fun onHbbTvResetDeviceId()
        fun onEvaluationLicenseClicked()
//        fun onOpenSourceLicenceClicked()
        fun onTTXDigitalLanguageSelected(position: Int, language: String)
        fun onTTXDecodeLanguageSelected(position: Int)
        fun onAspectRatioClicked(position: Int)
        fun setInteractionChannel(isSelected: Boolean)
        fun onEpgLanguageSelected(language: String)
        fun getParentalInformationData(type: PrefType)
        fun getClosedCationsInformationData()
        fun getPreferencesPvrTimeshiftData()
        fun getPrefsValue(key: String, value: Any?): Any?
        fun setPrefsValue(key: String, defValue: Any?)
        fun lockChannel(tvChannel: TvChannel, selected: Boolean)
        fun isChannelLockAvailable(tvChannel: TvChannel): Boolean
        fun blockInput(inputSourceData: InputSourceData, blocked: Boolean)
        fun isParentalControlsEnabled(): Boolean
        fun setParentalControlsEnabled(enabled : Boolean)
        fun setContentRatingSystemEnabled(contentRatingSystem: ContentRatingSystem, enabled: Boolean)
        fun setContentRatingLevel(index: Int)
        fun setRatingBlocked(contentRatingSystem: ContentRatingSystem, it: ContentRatingSystem.Rating, data: Boolean): Boolean
        fun setRelativeRatingsEnabled(contentRatingSystem: ContentRatingSystem, it: ContentRatingSystem.Rating, data: Boolean)
        fun isRatingBlocked(contentRatingSystem: ContentRatingSystem,it: ContentRatingSystem.Rating): Boolean
        fun isSubRatingEnabled(contentRatingSystem: ContentRatingSystem, it: ContentRatingSystem.Rating, subRating: ContentRatingSystem.SubRating): Boolean
        fun getContentRatingLevelIndex(): Int
        fun setSubRatingBlocked(contentRatingSystem: ContentRatingSystem,rating: ContentRatingSystem.Rating,subRating: ContentRatingSystem.SubRating,data: Boolean)
        fun setRelativeRating2SubRatingEnabled(contentRatingSystem: ContentRatingSystem,data: Boolean,rating: ContentRatingSystem.Rating,subRating: ContentRatingSystem.SubRating)
        fun setBlockUnrated(blocked: Boolean)
        fun getRRT5DimInfo(index: Int): MutableList<String>
        fun getRRT5CrsInfo(regionName: String): MutableList<ContentRatingSystem>
        fun getRRT5LevelInfo(countryIndex: Int, dimIndex: Int): MutableList<String>
        fun getSelectedItemsForRRT5Level(): HashMap<Int, Int>
        fun rrt5BlockedList(regionPosition :Int,position: Int): HashMap<Int, Int>
        fun setSelectedItemsForRRT5Level(regionIndex: Int, dimIndex: Int, levelIndex: Int)
        fun resetRRT5()
        fun onSkipChannelSelected(tvChannel: TvChannel, status: Boolean)
        fun onSwapChannelSelected(firstChannel: TvChannel, secondChannel: TvChannel, previousPosition:Int,newPosition:Int)
        fun onMoveChannelSelected(moveChannelList: ArrayList<TvChannel>, previousPosition:Int,newPosition:Int, channelMap: HashMap<Int, String>)
        fun onDeleteChannelSelected(tvChannel: TvChannel, index: Int)
        fun onClearChannelSelected()
        fun saveUserSelectedCCOptions(ccOptions: String,newValue: Int)
        fun resetCC()
        fun setCCInfo()
        fun disableCCInfo()
        fun setCCWithMute(isEnabled: Boolean)
        fun lastSelectedTextOpacity(position: Int)
        fun onChannelClicked()
        fun onBlueMuteChanged(isEnabled: Boolean)
        fun noSignalPowerOffChanged(enabled: Boolean)
        fun noSignalPowerOffTimeChanged()
        fun getUtilsModule(): UtilsInterface
        fun isLcnEnabled(): Boolean
        fun enableLcn(enable: Boolean)
        fun getCiPlusInterface(): CiPlusInterface
        fun storePrefsValue(tag: String, defValue: String)
        fun getPrefsValue(tag: String, defValue: String): String
        fun onPictureSettingsClicked()
        fun onScreenSettingsClicked()
        fun onSoundSettingsClicked()
        fun getSignalAvailable() : Boolean
        fun getRRT5Regions(): MutableList<String>
        fun getChannelSourceType(tvChannel: TvChannel) : String
        fun setPowerOffTime(value: Int, time: String)
        fun setAudioDescriptionEnabled(enable: Boolean)
        fun setHearingImpairedEnabled(enable: Boolean)
        fun onPowerClicked()
        fun isAnalogSubtitleEnabled(type: PrefType): Boolean
        fun isDigitalSubtitleEnabled(type: PrefType): Boolean
        fun onAdsTargetingChange(enable: Boolean)
        fun isAccessibilityEnabled(): Boolean
        fun changeChannelFromTalkback(tvChannel: TvChannel)
        fun onDpadDown(btnView: ImageView?)
        fun setAnokiRatingLevel(level: Int)
        fun getAnokiRatingLevel(): Int
        fun checkNetworkConnection(): Boolean
        fun isAnokiParentalControlsEnabled(): Boolean
        fun setAnokiParentalControlsEnabled(enabled : Boolean)
        fun onExit()
        fun getPreferenceDeepLink(): DeepLink?
        fun startOadScan()
        fun startCamScan()
        fun changeCamPin()
        fun selectMenuItem(position: Int)
        fun enterMMI()
        fun onTimeshiftStatusChanged(enabled: Boolean)
        fun getConfigInfo(nameOfInfo: String): Boolean
        fun deleteProfile(profileName: String)
        fun onClickTermsOfService()
        fun onClickPrivacyPolicy()
        fun doCAMReconfiguration(camTypePreference: CamTypePreference)

        fun getLockedChannelIdsFromPrefs() : MutableSet<String>
    }

    class DeepLink (
        val prefType : PrefType,
        val widgetId : Int,
        val prefId : Pref
    )

    fun showBroadcastParental() {
        preferencesFilterGridView?.alpha = 0f
        parentalWidget?.view?.alpha = 0f
        preferenceTypesCategoryAdapter.selectedItem = 1
        preferenceTypesGridView?.postDelayed({
            if (isMultipleTypes) {
                preferenceTypesGridView!!.layoutManager!!.findViewByPosition(1)!!
                    .requestFocus()
                preferenceTypesGridView!!.layoutManager!!.findViewByPosition(1)!!
                    .callOnClick()
            }
            preferencesFilterGridView?.postDelayed({
                preferencesFilterGridView!!.layoutManager!!.findViewByPosition(1)!!.requestFocus()
                preferencesFilterGridView?.alpha = 1f
                parentalWidget?.view?.alpha = 1f
            }, 10)
        }, 200)
    }
}

