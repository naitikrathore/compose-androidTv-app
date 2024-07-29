package com.iwedia.cltv.scene.home_scene

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.usb.UsbManager
import android.media.tv.ContentRatingSystem
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityEvent
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.leanback.widget.HorizontalGridView
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.runOnUiThread
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TimeTextView
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.anoki_fast.epg.GuideScanListener
import com.iwedia.cltv.anoki_fast.epg.ScanChannelsGuide
import com.iwedia.cltv.components.CategoryAdapter
import com.iwedia.cltv.components.CategoryAdapter.ChannelListCategoryAdapterListener
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.components.CategoryItemViewHolder
import com.iwedia.cltv.components.ReferenceWidgetFavourites
import com.iwedia.cltv.components.ReferenceWidgetForYou
import com.iwedia.cltv.components.ReferenceWidgetPreferences
import com.iwedia.cltv.components.ReferenceWidgetRecordings
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigCompanyDetailsManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.PromotionItem
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.language.LanguageCode
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.scene.home_scene.guide.GuideSceneWidget
import com.iwedia.cltv.scene.home_scene.guide.GuideSceneWidgetListener
import com.iwedia.cltv.scene.home_scene.guide.HorizontalGuideSceneWidget
import com.iwedia.cltv.scene.home_scene.guideVertical.VerticalGuideSceneWidget
import com.iwedia.cltv.utils.LoadingPlaceholder
import com.iwedia.cltv.utils.PlaceholderName
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import core_entities.Error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneListener
import world.SceneManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

abstract class HomeSceneBase(context: Context, sceneListener: SceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.HOME_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.HOME_SCENE),
    sceneListener
) {


    private val TAG = javaClass.simpleName

    /**
     * Xml views
     */
    open var logoIv: ImageView? = null
    open var timeTv: TimeTextView? = null
    open var homeContainer: ConstraintLayout? = null
    open var sceneNameTv: TextView? = null
    open var homeFilterGridView: HorizontalGridView? = null
    open var searchIv: ImageView? = null
    open var searchIconBg: View? = null
    open var settingsIv: ImageView? = null
    open var settingsIconBg: View? = null
    open var favoritesFirstTime = true
    open var recordingsFirstTime = true
    open var preferenceFirstTime = true
    open var canNotClick = false
    open val animationDuration : Long = 700

    //Current time
    private var date: Date? = null

    //active widget is the integer that displays currently displaying...for the for you,its 1, for guide its 2 for favourites its 3.....
    var activeWidget: Int = -1

    //TODO Dummy list for categories
    open val list = mutableListOf<CategoryItem>()

    /**
     * Home scene widgets
     */
    open var homeFilterCategoryAdapter: CategoryAdapter? = null
    open var forYouWidget: ReferenceWidgetForYou? = null
    open var guideSceneWidget: GuideSceneWidget? = null
    open var guideScanWidget: ScanChannelsGuide? = null
    open var favouritesSceneWidget: ReferenceWidgetFavourites? = null
    open var preferencesSceneWidget: ReferenceWidgetPreferences? = null
    open var recordingsWidget: ReferenceWidgetRecordings? = null
    private var upArrow: ImageView? = null
    private var enableCaching = true

    /**
     * Initial filter position
     */
    open var initialFilterPosition = 0
    open var selectedPosition = 0


    /**
     * TV guide selected filter
     */
    private var mSelectedFilter = 0

    open var mainConstraintLayout: ConstraintLayout? = null
    open var mActiveChannel: TvChannel? = null
    //used to scroll epg after certain time
    private var guideScrollUpdateTimer: CountDownTimer?= null

    companion object {
        //when guide button pressed helps in jumping directly to the current event
        var jumpGuideCurrentEvent = false
    }


    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(name, R.layout.layout_scene_home, object :
            GAndroidSceneFragmentListener {

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onCreated() {
                try {
                    //todo view is null
//                    if(view == null) worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    logoIv = view!!.findViewById(R.id.iv_logo)
                    mainConstraintLayout =
                        view!!.findViewById<ConstraintLayout>(R.id.main_layout_id)

                    upArrow = view!!.findViewById(R.id.arrow_up)
                } catch (E: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: ${E.printStackTrace()}")
                    worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    return
                }

                val selectorDrawable = ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.ic_baseline_keyboard_arrow_up_24
                )
                DrawableCompat.setTint(
                    selectorDrawable!!,
                    Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                )
                upArrow!!.setImageDrawable(selectorDrawable)

                Utils.loadImage(
                    ConfigCompanyDetailsManager.getCompanyDetailsInfo("company_logo"),
                    logoIv!!, object : AsyncReceiver {
                        override fun onFailed(error: Error?) {
                        }

                        override fun onSuccess() {
                        }

                    }
                )

                val gradientBackground: View = view!!.findViewById(R.id.gradient_background)
                Utils.makeGradient(
                    gradientBackground,
                    GradientDrawable.LINEAR_GRADIENT,
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    Color.parseColor(
                        ConfigColorManager.getColor(
                            ConfigColorManager.getColor("color_background"),
                            0.5
                        )
                    ),
                    Color.parseColor(
                        ConfigColorManager.getColor(
                            ConfigColorManager.getColor("color_background"),
                            0.9
                        )
                    ),
                    0.0F,
                    0.0F
                )

                timeTv = view!!.findViewById(R.id.home_tv_time)
                timeTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                (sceneListener as HomeSceneListener).getActiveChannel(object : IAsyncDataCallback<TvChannel>{
                    override fun onFailed(error: kotlin.Error) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
                    }

                    override fun onReceive(data: TvChannel) {
                        mActiveChannel = data
                    }

                })
                refresh(mActiveChannel?.let { (sceneListener as HomeSceneListener).getCurrentTime(it) })
                homeContainer = view!!.findViewById(R.id.home_container)
                sceneNameTv = view!!.findViewById(R.id.home_scene_name)
                timeTv?.typeface = TypeFaceProvider.getTypeFace(
                    ReferenceApplication.applicationContext(),
                    ConfigFontManager.getFont("font_regular")
                )

                searchIconBg = view!!.findViewById(R.id.home_search_icon_selector)
                searchIv = view!!.findViewById(R.id.home_search_icon)
                searchIv?.tag = 1

                searchIv!!.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.search_icon
                    )
                )
                try {
                    val color_context =
                        Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                    searchIv!!.getDrawable()
                        .setTint(color_context)// = ColorStateList.valueOf(color_context)
                } catch (E: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: E search $E")
                }

                initButton(
                    searchIconBg,
                    searchIv
                )

                if((sceneListener as HomeSceneListener).isAccessibilityEnabled()) {
                    searchIv!!.setOnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            homeFilterCategoryAdapter!!.clearPreviousFocus()
                        }
                    }
                }

                settingsIconBg = view!!.findViewById(R.id.home_settings_icon_selector)
                settingsIv = view!!.findViewById(R.id.home_settings_icon)
                settingsIv?.tag = 2
                if(!(sceneListener as HomeSceneListener).kidsModeEnabled()) {
                    searchIv?.visibility = View.GONE
                    searchIconBg?.visibility = View.GONE
                }

                settingsIv!!.setImageDrawable(
                    ContextCompat.getDrawable(
                        context!!,
                        R.drawable.settings_icon
                    )
                )
                settingsIv!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                initButton(
                    settingsIconBg,
                    settingsIv
                )

                if((sceneListener as HomeSceneListener).isAccessibilityEnabled()) {
                    settingsIv!!.setOnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            homeFilterCategoryAdapter!!.clearPreviousFocus()
                        }
                    }
                }

                val focusChangeHandler = Handler(Looper.myLooper()!!)
                var runnable: Runnable? = null

                homeFilterGridView =
                    view!!.findViewById(R.id.home_filter_list_view)
                homeFilterCategoryAdapter = CategoryAdapter()
                homeFilterCategoryAdapter!!.isCategoryFocus = true


                list.add(CategoryItem(0, ConfigStringsManager.getStringById("home")))
                list.add(CategoryItem(1, ConfigStringsManager.getStringById("tv_guide")))
                list.add(CategoryItem(2, ConfigStringsManager.getStringById("favorites")))
                if ((sceneListener as HomeSceneListener).getConfigInfo("pvr")) {
                    list.add(CategoryItem(3, ConfigStringsManager.getStringById("recordings")))
                }
                list.add(CategoryItem(4, ConfigStringsManager.getStringById("preferences")))
                homeFilterCategoryAdapter!!.selectedItem = -1
                homeFilterCategoryAdapter?.refresh(list)
                homeFilterGridView!!.setNumRows(1)
                homeFilterGridView!!.adapter = homeFilterCategoryAdapter
                homeFilterCategoryAdapter!!.shoudKeepFocusOnClick = true
                homeFilterCategoryAdapter?.adapterListener = if (enableCaching) {
                    object : ChannelListCategoryAdapterListener {
                        override fun onItemClicked(position: Int) {
                            //prevent focus to go down
                            if (position == 1 && guideSceneWidget != null && guideSceneWidget!!.isLoading) {
                                return
                            }
                            if (categoryHasData(position)) {
                                val holder =
                                    homeFilterGridView!!.findViewHolderForAdapterPosition(
                                        position
                                    ) as CategoryItemViewHolder
                                homeFilterCategoryAdapter!!.setActiveFilter(holder)
                                handleCategoryClicked(position)
                            }
                        }

                        override fun getAdapterPosition(position: Int) {
                            if(!(sceneListener as HomeSceneListener).kidsModeEnabled()) {
                                searchIv!!.visibility = View.GONE
                            }
                            else {
                                searchIv!!.visibility = View.VISIBLE
                            }
                            settingsIv?.visibility = View.VISIBLE
                            homeFilterCategoryAdapter!!.clearPreviousFocus()
                        }

                        override fun onItemSelected(position: Int) {
                            canNotClick = false
                            activeWidget = position
                            if (initialFilterPosition != 2 && initialFilterPosition != 1) {
                                handleCategoryFocusedWithCaching(position)
                            }
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            (sceneListener as HomeSceneListener).setSpeechText(text = text, importance = importance)
                        }

                        override fun onKeyDown(currentPosition: Int): Boolean {
                            //prevent focus to go down
                            if (currentPosition == 1 && guideSceneWidget != null && guideSceneWidget!!.isLoading) {
                                return true
                            }
                            if (canNotClick) return true
                            if (categoryHasData(currentPosition)) {
                                val holder =
                                    homeFilterGridView!!.findViewHolderForAdapterPosition(
                                        currentPosition
                                    ) as CategoryItemViewHolder

                                homeFilterCategoryAdapter!!.setActiveFilter(holder)
                                handleCategoryClicked(currentPosition)
                            }
                            if (recordingsWidget != null) {
                                recordingsWidget!!.customRecordingCustomButton!!.requestFocus()
                                homeFilterCategoryAdapter!!.clearPreviousFocus()
                                return true
                            }

                            return true
                        }

                        override fun onKeyUp(currentPosition: Int): Boolean {
                            return false
                        }

                        override fun onKeyLeft(currentPosition: Int): Boolean {
                            canNotClick = true

                            if (currentPosition == 0) {
                                homeFilterCategoryAdapter!!.clearPreviousFocus()
                                setActiveFilterOnCurrentActiveItem()
                                if((sceneListener as HomeSceneListener).isAccessibilityEnabled()){
                                    searchIv?.isFocusable = true
                                    searchIv?.setFocusable(true)
                                    searchIv?.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                                }
                                searchIv?.requestFocus()
                                Utils.focusAnimation(searchIconBg!!)
                            }
                            return false
                        }

                        override fun digitPressed(digit: Int) {
                            guideSceneWidget?.onDigitPress(digit)
                        }

                        override fun onKeyRight(currentPosition: Int): Boolean {
                            canNotClick = true
                            if (currentPosition == homeFilterCategoryAdapter!!.itemCount - 1) {
                                canNotClick = false
                            }
                            if((sceneListener as HomeSceneListener).kidsModeEnabled()) {
                                if (currentPosition == homeFilterCategoryAdapter!!.itemCount - 1) {
                                    homeFilterCategoryAdapter!!.clearPreviousFocus()
                                    setActiveFilterOnCurrentActiveItem()
                                    if((sceneListener as HomeSceneListener).isAccessibilityEnabled()) {
                                        settingsIv?.isFocusable = true
                                        settingsIv?.setFocusable(true)
                                        settingsIv?.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                                    }
                                    settingsIv?.requestFocus()
                                    Utils.focusAnimation(settingsIconBg!!)

                                    return true
                                }
                            }
                            else {
                                if (currentPosition == homeFilterCategoryAdapter!!.itemCount - 1) {
                                    homeFilterCategoryAdapter?.clearPreviousFocus()
                                    setActiveFilterOnCurrentActiveItem()
                                    if (settingsIv?.hasFocus() == false) {
                                        settingsIv!!.requestFocus()
                                    }
                                    return true
                                }
                            }
                            return false
                        }

                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onBackPressed(position: Int): Boolean {
                            homeContainer?.removeAllViews()
                            forYouWidget?.dispose()
                            forYouWidget = null
                            guideSceneWidget = null
                            favouritesSceneWidget = null
                            favoritesFirstTime = true
                            recordingsWidget = null
                            recordingsFirstTime = true
                            preferencesSceneWidget = null
                            preferenceFirstTime = true
                            if(ReferenceApplication.isInitalized) {
                                if ((sceneListener as HomeSceneListener).isTimeShiftActive() ||
                                    (sceneListener as HomeSceneListener).isRecordingInProgress()) {

                                    var sceneId = if ((sceneListener as HomeSceneListener).isTimeShiftActive()) ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE else
                                        ReferenceWorldHandler.SceneId.PLAYER_SCENE
                                    if ((sceneListener as HomeSceneListener).isTimeShiftActive()) {
                                        worldHandler!!.triggerAction(
                                            sceneId,
                                            SceneManager.Action.SHOW_OVERLAY
                                        )
                                    }
                                    worldHandler!!.triggerAction(
                                        ReferenceWorldHandler.SceneId.HOME_SCENE,
                                        SceneManager.Action.DESTROY
                                    )
                                } else {
                                    handleCategoryFocusedWithCaching(0)
                                }
                            } else {
                                worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                            }
                            return true
                        }

                    }

                } else {
                    object : ChannelListCategoryAdapterListener {
                        override fun onItemClicked(position: Int) {
                            if (categoryHasData(position)) {
                                val holder =
                                    homeFilterGridView!!.findViewHolderForAdapterPosition(
                                        position
                                    ) as CategoryItemViewHolder
                                homeFilterCategoryAdapter!!.setActiveFilter(holder)
                                handleCategoryClicked(position)
                            }
                            if (isCategoryCreated(position)) {
                                handleCategoryFocused(position)
                                homeFilterGridView?.requestFocus()
                            }

                        }

                        override fun getAdapterPosition(position: Int) {
                            if((sceneListener as HomeSceneListener).kidsModeEnabled()) {
                                settingsIv!!.visibility = View.VISIBLE
                            }
                            searchIv!!.visibility = View.VISIBLE
                            homeFilterCategoryAdapter!!.clearPreviousFocus()
                        }

                        override fun onItemSelected(position: Int) {
                            var searchBtnFocused = searchIv!!.hasFocus()
                            var settingsBtnFocused = settingsIv!!.hasFocus()
                            if (searchBtnFocused || settingsBtnFocused) {
                                Handler().postDelayed(Runnable {
                                    runOnUiThread(Runnable {
                                        homeFilterCategoryAdapter!!.clearPreviousFocus()
                                        homeFilterCategoryAdapter!!.setSelected(position)
                                        if (searchBtnFocused) {
                                            if((sceneListener as HomeSceneListener).isAccessibilityEnabled()) {
                                                searchIv?.isFocusable = true
                                                searchIv?.setFocusable(true)
                                                searchIv?.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                                            }
                                            searchIv?.requestFocus()
                                        }
                                        if (settingsBtnFocused) {
                                            if((sceneListener as HomeSceneListener).isAccessibilityEnabled()) {
                                                settingsIv?.isFocusable = true
                                                settingsIv?.setFocusable(true)
                                                settingsIv?.sendAccessibilityEvent(
                                                    AccessibilityEvent.TYPE_VIEW_FOCUSED
                                                )
                                            }
                                            settingsIv?.requestFocus()
                                        }
                                    })
                                }, 0)
                            }

                            canNotClick = false
                            activeWidget = position
                            if (getActiveCategoryId() != position) {
                                homeFilterCategoryAdapter!!.clearPreviousFocus()
                                handleCategoryFocused(position)
                                activeWidget = position
                            }
                        }

                        override fun digitPressed(digit: Int) {
                            guideSceneWidget?.onDigitPress(digit)
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            (sceneListener as HomeSceneListener).setSpeechText(text = text, importance = importance)
                        }

                        override fun onKeyDown(currentPosition: Int): Boolean {
                            if (categoryHasData(currentPosition)) {
                                val holder =
                                    homeFilterGridView!!.findViewHolderForAdapterPosition(
                                        currentPosition
                                    ) as CategoryItemViewHolder
                                homeFilterCategoryAdapter!!.setActiveFilter(holder)
                                handleCategoryClicked(currentPosition)
                            }
                            return true
                        }

                        override fun onKeyUp(currentPosition: Int): Boolean {

                            return false
                        }

                        override fun onKeyLeft(currentPosition: Int): Boolean {
                            runnable?.let { focusChangeHandler.removeCallbacks(it) }

                            var holder =
                                homeFilterGridView!!.findViewHolderForAdapterPosition(activeWidget!!) as CategoryItemViewHolder
                            homeFilterCategoryAdapter!!.setActiveFilter(holder)
                            if (currentPosition == 0) {
                                setActiveFilterOnCurrentActiveItem()
                                if((sceneListener as HomeSceneListener).isAccessibilityEnabled()) {
                                    searchIv?.isFocusable = true
                                    searchIv?.setFocusable(true)
                                    searchIv?.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                                }
                                searchIv?.requestFocus()
                                Utils.focusAnimation(searchIconBg!!)
                                return true
                            }
                            return false
                        }


                        override fun onKeyRight(currentPosition: Int): Boolean {
                            runnable?.let { focusChangeHandler.removeCallbacks(it) }

                            var holder =
                                homeFilterGridView!!.findViewHolderForAdapterPosition(activeWidget!!) as CategoryItemViewHolder
                            homeFilterCategoryAdapter!!.setActiveFilter(holder)

                            if((sceneListener as HomeSceneListener).kidsModeEnabled()) {
                                if (currentPosition == homeFilterCategoryAdapter!!.itemCount - 1) {
                                    setActiveFilterOnCurrentActiveItem()
                                    if((sceneListener as HomeSceneListener).isAccessibilityEnabled()) {
                                        settingsIv?.isFocusable = true
                                        settingsIv?.setFocusable(true)
                                        settingsIv?.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                                    }
                                    settingsIv?.requestFocus()
                                    Utils.focusAnimation(settingsIconBg!!)

                                    return true
                                }
                            }
                            return false
                        }

                        override fun onBackPressed(position: Int): Boolean {
                            return false
                        }

                    }
                }

                //TODO dummy container
                homeContainer?.onFocusChangeListener = object : View.OnFocusChangeListener {
                    override fun onFocusChange(view: View?, hasFocus: Boolean) {
                        if (hasFocus) {
                            sceneNameTv!!.background =
                                ContextCompat.getDrawable(
                                    view!!.context,
                                    R.drawable.reference_button_focus_shape
                                )
                            sceneNameTv!!.typeface =
                                TypeFaceProvider.getTypeFace(
                                    ReferenceApplication.applicationContext(),
                                    ConfigFontManager.getFont("font_medium")
                                )

                            try {
                                val color_context =
                                    Color.parseColor(ConfigColorManager.getColor("color_background"))
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color_context $color_context")
                                sceneNameTv!!.setTextColor(
                                    color_context
                                )
                            } catch (ex: Exception) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
                            }
                        } else {
                            sceneNameTv!!.setBackgroundResource(0)
                            sceneNameTv!!.typeface =
                                TypeFaceProvider.getTypeFace(
                                    ReferenceApplication.applicationContext(),
                                    ConfigFontManager.getFont("font_regular")
                                )

                            try {
                                val color_context =
                                    Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color_context $color_context")
                                sceneNameTv!!.setTextColor(
                                    color_context
                                )
                            } catch (ex: Exception) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
                            }
                        }
                    }
                }

                //TODO OPTIMIZE WIDGET CREATION

                //parseConfig(configParam!!)
                sceneListener.onSceneInitialized()
            }

        })
    }

    private fun setActiveFilterOnCurrentActiveItem() {
        val position = homeFilterCategoryAdapter!!.focusPosition
        if (position != -1) {
            val holder =
                homeFilterGridView!!.findViewHolderForAdapterPosition(
                    position
                ) as CategoryItemViewHolder
            homeFilterCategoryAdapter!!.setPreviousFocus(holder)
        }
    }

    private fun categoryHasData(position: Int): Boolean {
        when (position) {
            0 -> {
                return !(forYouWidget == null || (forYouWidget != null && forYouWidget!!.itemsForAdapter.isEmpty()))
            }
            1 -> {
                return guideSceneWidget != null
            }
            2 -> {
                return (favouritesSceneWidget != null || (favouritesSceneWidget != null && (favouritesSceneWidget!!.categories.isEmpty() || favouritesSceneWidget!!.channels.isEmpty())))
            }
            3 -> {
                if (!(sceneListener as HomeSceneListener).getConfigInfo("pvr")) {
                    return (preferencesSceneWidget != null)
                }
                return !((recordingsWidget != null && recordingsWidget!!.itemsForAdapter.isEmpty()) || recordingsWidget == null)
            }
            4 -> {
                return (preferencesSceneWidget != null)
            }
        }
        return true
    }

    private fun isCategoryCreated(position: Int): Boolean {
        when (position) {
            0 -> {
                return (forYouWidget == null)
            }
            1 -> {
                return (guideSceneWidget == null)
            }
            2 -> {
                return (favouritesSceneWidget == null)
            }
            3 -> {
                return (recordingsWidget == null)
            }
            4 -> {
                return (preferencesSceneWidget == null)
            }
        }
        return true
    }

    open fun refreshEpgData() {
        guideSceneWidget?.let {
            jumpGuideCurrentEvent = true
            if (ReferenceApplication.isInForeground) {
                showEpgData()
            }
        }
    }

    open fun refreshEpgEventsData() {
    }

    open fun refreshEpgChannelsData(isBroadcastChannelUpdated : Boolean? = null) {
    }

    open fun getEpgActiveFilter():CategoryItem{
        return guideSceneWidget!!.getActiveCategory()
    }

    private fun showEpgData() {

        CoroutineHelper.runCoroutine({
            (sceneListener as HomeSceneListener).getActiveChannel(object :
                IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: kotlin.Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ")
                }

                override fun onReceive(activeTvChannel: TvChannel) {
                    (sceneListener as HomeSceneListener).getAvailableChannelFilters(object :
                        IAsyncDataCallback<ArrayList<Category>> {
                        override fun onFailed(error: kotlin.Error) {}

                        override fun onReceive(filterList: ArrayList<Category>) {
                            runOnUiThread {
                                var index = 0
                                run exitForEach@{
                                    filterList.toList().forEach { item ->
                                        if (item.name.equals((sceneListener as HomeSceneListener).getActiveCategory())) {
                                            index = filterList.indexOf(item)
                                            (sceneListener as HomeSceneListener).setActiveEpgFilter(
                                                index
                                            )
                                            return@exitForEach
                                        }
                                    }
                                }

                                guideSceneWidget?.dayOffset?.let {
                                    (sceneListener as HomeSceneListener).getEventsForChannels(
                                        activeTvChannel,
                                        (sceneListener as HomeSceneListener).getActiveEpgFilter(),
                                        object :
                                            IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>> {
                                            override fun onFailed(error: kotlin.Error) {
                                                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onFailed: $error",)
                                                guideSceneWidget?.onFetchFailed()
                                            }

                                            override fun onReceive(eventListMap: LinkedHashMap<Int, MutableList<TvEvent>>) {
                                                runOnUiThread {
                                                    try {
                                                        guideSceneWidget?.refresh(filterList)
                                                        initGuideWidget(
                                                            (sceneListener as HomeSceneListener).getActiveEpgFilter(),
                                                            eventListMap
                                                        )
                                                    } catch (E: Exception) {
                                                        Log.d(Constants.LogTag.CLTV_TAG +
                                                            TAG,
                                                            "onReceive: ${E.printStackTrace()}"
                                                        )
                                                        worldHandler!!.destroyOtherExisting(
                                                            ReferenceWorldHandler.SceneId.LIVE
                                                        )
                                                    }
                                                }
                                            }

                                        },
                                        it, 0, false
                                    )
                                }
                            }
                        }
                    })

                }
            })
        },Dispatchers.IO)
    }

    open fun handleCategoryFocusedWithCaching(position: Int) {
        LoadingPlaceholder.hideAllRegisteredLoadingPlaceholders()
        if (selectedPosition == position) return
        //For You is already shown
        homeContainer?.removeAllViews()
        if (position != initialFilterPosition) {
            homeFilterCategoryAdapter?.clearFocus(initialFilterPosition)
        }
        homeFilterCategoryAdapter?.selectedItem = position

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "handleCategoryFocusedWithCaching: position $position list.size ${list.size}")
        if(position >= 0 && position < list.size){
            when (list[position].id) {
                0 -> {
                    handlerCategoryFocus(0)
                    createForYouSceneWidget(false)
                    homeContainer?.removeAllViews()
                    homeContainer?.addView(forYouWidget?.view)
                    selectedPosition = position
                    return
                }
                1 -> {
                    //TV Guide
                    LoadingPlaceholder(
                        context = ReferenceApplication.applicationContext(),
                        placeholderViewId = R.layout.loading_layout_widget_guide_main,
                        parentConstraintLayout = mainConstraintLayout!!,
                        name = PlaceholderName.GUIDE
                    )

                    var searchBtnFocused = searchIv!!.hasFocus()
                    var settingsBtnFocused = settingsIv!!.hasFocus()
                    if (!searchBtnFocused && !settingsBtnFocused) {
                        if (homeFilterGridView!!.getChildAt(1) != null) {
                            homeFilterGridView!!.getChildAt(1).requestFocus()
                        } else {
                            homeFilterGridView!!.viewTreeObserver.addOnGlobalLayoutListener(object :
                                ViewTreeObserver.OnGlobalLayoutListener {
                                override fun onGlobalLayout() {
                                    homeFilterGridView!!.viewTreeObserver
                                        .removeOnGlobalLayoutListener(this)
                                    homeFilterGridView!!.getChildAt(1).requestFocus()
                                }
                            })
                        }
                    } else {
                        homeFilterCategoryAdapter!!.setSelected(1)
                    }
                    createGuideSceneWidget()
                    selectedPosition = position
                    homeContainer?.addView(guideSceneWidget?.view)
                    return
                }
                2 -> {
                    //Favourites
                    handlerCategoryFocus(2)
                    if (favoritesFirstTime) {
                        createFavouritesWidget()
                        favoritesFirstTime = false

                        (sceneListener as HomeSceneListener).getAvailableFavouriteCategories(
                            object : IAsyncDataCallback<ArrayList<String>> {
                                override fun onFailed(error: kotlin.Error) {
                                }

                                override fun onReceive(data: ArrayList<String>) {
                                    runOnUiThread(Runnable {
                                        try {
                                            favouritesSceneWidget!!.refresh(data)
                                        } catch (e: Exception) {
                                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: ${e.printStackTrace()}")
                                        }
                                    })
                                }

                            })

                        (sceneListener as HomeSceneListener).getAvailableFavouriteChannels(
                            object : IAsyncDataCallback<ArrayList<TvChannel>> {
                                override fun onFailed(error: kotlin.Error) {

                                }

                                override fun onReceive(data: ArrayList<TvChannel>) {
                                    runOnUiThread(Runnable {
                                        favouritesSceneWidget!!.refresh(data)
                                    })
                                }

                            })
                        favoritesFirstTime = false
                    }
                    homeContainer?.removeAllViews()
                    homeContainer?.addView(favouritesSceneWidget?.view)
                    selectedPosition = position
                    return
                }
                3 -> {
                    //initializing placeholder for recordings
                    LoadingPlaceholder(
                        context = ReferenceApplication.applicationContext(),
                        placeholderViewId = R.layout.loading_layout_rail_main,
                        parentConstraintLayout = mainConstraintLayout!!,
                        name = PlaceholderName.RECORDINGS
                    )

                    //Recordings
                    handlerCategoryFocus(3)
                    if (recordingsFirstTime) {
                        createRecordingsSceneWidget(false)
                        recordingsFirstTime = false

                    }
                    homeContainer?.removeAllViews()
                    homeContainer?.addView(recordingsWidget?.view)
                    recordingsWidget?.view!!.requestFocus()

                    homeContainer!!.doOnPreDraw {
                        homeFilterGridView!!.requestFocus()
                    }
                    selectedPosition = position

                    return
                }
                //Preference
                4 -> {
//                if (GeneralConfigManager.getGeneralSettingsInfo("pvr")) {
//                    homeFilterGridView!!.getChildAt(4).requestFocus()
//                } else {
//                    homeFilterGridView!!.getChildAt(3).requestFocus()
//                }
                    var searchBtnFocused = searchIv!!.hasFocus()
                    var settingsBtnFocused = settingsIv!!.hasFocus()
                    if (!searchBtnFocused && !settingsBtnFocused) {
                        if (homeFilterGridView!!.getChildAt(3) != null) {
                            homeFilterGridView!!.getChildAt(3).requestFocus()
                        } else {
                            homeFilterGridView!!.getViewTreeObserver().addOnGlobalLayoutListener(object :
                                ViewTreeObserver.OnGlobalLayoutListener {
                                override fun onGlobalLayout() {
                                    homeFilterGridView!!.getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(this)
                                    homeFilterGridView!!.getChildAt(3).requestFocus()
                                }
                            })
                        }
                    } else {
                        homeFilterCategoryAdapter!!.setSelected(3)
                    }

                    if (preferenceFirstTime) {
                        createPreferencesWidget()
                        (sceneListener as HomeSceneListener).getAvailablePreferenceTypes(
                            object : IAsyncDataCallback<List<CategoryItem>> {

                                override fun onFailed(error: kotlin.Error) {
                                }

                                @RequiresApi(Build.VERSION_CODES.P)
                                override fun onReceive(data: List<CategoryItem>) {
                                    runOnUiThread(Runnable {
                                        preferencesSceneWidget!!.refresh(data)
                                    })
                                }

                            })
                    }
                    homeContainer?.removeAllViews()
                    homeContainer?.addView(preferencesSceneWidget?.view)
                    selectedPosition = position

                }
                else -> {
                    homeContainer?.addView(sceneNameTv)
                    sceneNameTv?.text = list[position].name
                }
            }
        }
    }

    private fun handleCategoryFocused(position: Int) {
        if (position == 0 && forYouWidget != null) {
            return
        }
        //TV Guide is already shown
        if (position == 1 && guideSceneWidget != null) {
            return
        }

        //Favourites is already shown
        if (position == 2 && favouritesSceneWidget != null) {
            return
        }

        //Recordings is already shown
        if (position == 3 && recordingsWidget != null) {
            return
        }

        //Preferences is already shown
        if (position == 4 && preferencesSceneWidget != null) {
            return
        }

        homeContainer?.removeAllViews()
        if (position != initialFilterPosition) {
            homeFilterCategoryAdapter!!.clearFocus(initialFilterPosition)
        }
        homeFilterCategoryAdapter!!.selectedItem = position


        guideSceneWidget = null
        forYouWidget = null
        favouritesSceneWidget = null
        recordingsWidget = null
        preferencesSceneWidget = null
        when (list[position].id) {
            0 -> {
                //For you
                createForYouSceneWidget(false)
                homeContainer?.addView(forYouWidget?.view)
                return
            }
            1 -> {
                //TV Guide
                //to set focus to guide when i am directly opening guide with guide button other wise focus will be the homecontainer not at the list
                handlerCategoryFocus(1)
                createGuideSceneWidget()
                homeContainer?.addView(guideSceneWidget?.view)

            }
            2 -> {
                //Favourites
                handlerCategoryFocus(2)

                createFavouritesWidget()
                favouritesSceneWidget!!.categoryFocus = true
                homeContainer?.addView(favouritesSceneWidget?.view)
                (sceneListener as HomeSceneListener).getAvailableFavouriteCategories(
                    object : IAsyncDataCallback<ArrayList<String>> {
                        override fun onFailed(error: kotlin.Error) {
                        }

                        override fun onReceive(data: ArrayList<String>) {
                            runOnUiThread(Runnable {
                                favouritesSceneWidget!!.refresh(data)
                            })
                        }

                    })
            }
            3 -> {
                //Recordings
                handlerCategoryFocus(3)

                createRecordingsSceneWidget(false)
                homeContainer?.addView(recordingsWidget?.view)
                return
            }
            4 -> {

                handlerCategoryFocus(4)

                createPreferencesWidget()
                (sceneListener as HomeSceneListener).getAvailablePreferenceTypes(
                    object : IAsyncDataCallback<List<CategoryItem>> {


                        override fun onFailed(error: kotlin.Error) {
                        }

                        @RequiresApi(Build.VERSION_CODES.P)
                        override fun onReceive(data: List<CategoryItem>) {
                            runOnUiThread {
                                preferencesSceneWidget!!.refresh(data)
                            }
                        }

                    })

                homeContainer?.addView(preferencesSceneWidget?.view)

            }
            else -> {
                homeContainer?.addView(sceneNameTv)
                sceneNameTv?.text = list[position].name
            }
        }
    }

    open fun handlerCategoryFocus(position: Int) {
        var searchBtnFocused = searchIv!!.hasFocus()
        var settingsBtnFocused = settingsIv!!.hasFocus()
        if (!searchBtnFocused && !settingsBtnFocused) {
            homeFilterGridView!!.getChildAt(position).requestFocus()
        } else {
            homeFilterCategoryAdapter!!.setSelected(position)
        }
    }

    open fun handleCategoryClicked(position: Int) {

        //For you
        if (position == 0 && forYouWidget != null) {
            forYouWidget!!.requestFocus = true
            forYouWidget!!.setFocusToGrid()
            return
        }
        //TV Guide is already shown
        if (position == 1 && guideSceneWidget != null) {
            guideSceneWidget!!.setFocusToCategory()
            return
        }

        //Favourites is already shown
        if (position == 2 && favouritesSceneWidget != null) {
            favouritesSceneWidget!!.setFocusForCategory()
            return
        }

        //Recordings is already shown
        if (position == 3 && (sceneListener as HomeSceneListener).getConfigInfo("pvr") && recordingsWidget != null) {
            recordingsWidget!!.handleFocusToGrid()
            return
        }

        //Preferences is already shown
        if ((preferencesSceneWidget != null && !(sceneListener as HomeSceneListener).getConfigInfo("pvr") && position == 3) ||
            (position == 4 && preferencesSceneWidget != null)
        ) {

            //make the search and setting icon invisible
            searchIv!!.visibility = View.INVISIBLE
            settingsIv!!.visibility = View.INVISIBLE

            upArrow!!.alpha = 0f

            val translate: Animation =
                TranslateAnimation(0F, 0F, 0F, -Utils.getDimens(R.dimen.custom_dim_61))
            translate.duration = animationDuration

            val alpha: Animation = AlphaAnimation(1.0f, 0.0f)
            alpha.duration = animationDuration

            val arrowAnimation: Animation = AlphaAnimation(0.0f, 0.7f)
            alpha.fillAfter = true

            val animationSet = AnimationSet(true)
            animationSet.addAnimation(translate)
            animationSet.addAnimation(alpha)
            animationSet.fillAfter = true

            animationSet.setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    homeFilterGridView!!.visibility = View.GONE
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })

            arrowAnimation.setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    upArrow!!.alpha = 0.7f
                    upArrow!!.visibility = View.VISIBLE
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })

            upArrow!!.startAnimation(arrowAnimation)
            homeFilterGridView!!.startAnimation(animationSet)
            preferencesSceneWidget!!.requestFocus()
            return
        }
    }

    /**
     * Show ReferenceWidgetForYou after initialization and keep focus on the For You item
     */
    open fun sceneInit() {
        worldHandler?.isEnableUserInteraction = false
        homeContainer?.removeAllViews()

        if (initialFilterPosition == 0) {
            createForYouSceneWidget(false)
            forYouWidget?.view?.visibility = View.INVISIBLE
            homeContainer?.addView(forYouWidget?.view)
            ReferenceApplication.runOnUiThread(Runnable {
                homeFilterCategoryAdapter?.keepFocus = false
                homeFilterCategoryAdapter?.selectedItem = 0
                forYouWidget?.view?.visibility = View.VISIBLE
                worldHandler?.isEnableUserInteraction = true
            })
            /*CoroutineHelper.runCoroutineWithDelay({
                homeFilterCategoryAdapter?.keepFocus = false
                homeFilterCategoryAdapter?.selectedItem = 0
                forYouWidget?.view?.visibility = View.VISIBLE
                worldHandler?.isEnableUserInteraction = true
            }, 0, Dispatchers.Main)*/
        } else {
            if (initialFilterPosition == 1) {
                jumpGuideCurrentEvent = true
            }
            homeFilterCategoryAdapter?.requestFocus(initialFilterPosition)
            Handler().post {
                if (enableCaching) {
                    handleCategoryFocusedWithCaching(initialFilterPosition)
                    initialFilterPosition = 0
                } else {
                    handleCategoryFocused(initialFilterPosition)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (guideSceneWidget != null) {
            guideSceneWidget!!.pause()
        }
    }

    override fun onDestroy() {
        homeContainer?.removeAllViews()
        homeContainer = null
        forYouWidget?.dispose()
        forYouWidget?.view = null
        forYouWidget?.context = null
        forYouWidget?.verticalGridView = null
        forYouWidget?.configParam = null
        forYouWidget?.railAdapter = null
        forYouWidget?.noEventsMsg = null
        homeFilterCategoryAdapter = null
        forYouWidget = null
        guideSceneWidget = null
        favouritesSceneWidget = null
        preferencesSceneWidget = null
        recordingsWidget = null
        favoritesFirstTime = true
        recordingsFirstTime = true
        preferenceFirstTime = true
        homeFilterGridView = null
        logoIv = null
        timeTv = null
        sceneNameTv = null
        settingsIv = null
        settingsIconBg = null
        searchIv = null
        searchIconBg = null
        view = null
        selectedPosition = 0
        guideScrollUpdateTimer?.cancel()
        guideScrollUpdateTimer = null


        if (list.isNotEmpty()) {
            list.clear()
        }
        super.onDestroy()
    }

    //refresh favourites widget and its adapter
    open fun refreshFavouritesWidget() {
        if (favouritesSceneWidget != null) {
            if (selectedPosition != 2) {
                (sceneListener as HomeSceneListener).getAvailableFavouriteCategories(
                    object : IAsyncDataCallback<ArrayList<String>> {
                        override fun onFailed(error: kotlin.Error) {}

                        override fun onReceive(data: ArrayList<String>) {
                            runOnUiThread(Runnable {
                                favouritesSceneWidget!!.refresh(data)
                            })
                        }

                    })

                (sceneListener as HomeSceneListener).getAvailableFavouriteChannels(
                    object : IAsyncDataCallback<ArrayList<TvChannel>> {
                        override fun onFailed(error: kotlin.Error) {}
                        override fun onReceive(data: ArrayList<TvChannel>) {
                            runOnUiThread(Runnable {
                                favouritesSceneWidget!!.refresh(data)
                            })
                        }

                    })
            } else {
                favouritesSceneWidget!!.refreshHearts()
            }
        }
    }

    private fun createPreferencesWidget() {
        preferencesSceneWidget = ReferenceWidgetPreferences(
            context,
            object : ReferenceWidgetPreferences.PreferencesWidgetListener {
                override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                    (sceneListener as HomeSceneListener).showToast(text, duration)
                }

                override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                    (sceneListener as HomeSceneListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                }

                override fun getAvailablePreferencesCategories(callback: IAsyncDataCallback<List<CategoryItem>>, type: PrefType) {
                    (sceneListener as HomeSceneListener).getAvailablePreferencesCategories(callback, type)
                }

                override fun stopSpeech() {
                    (sceneListener as HomeSceneListener).stopSpeech()
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    (sceneListener as HomeSceneListener).setSpeechText(text = text, importance = importance)
                }

                override fun requestFocusOnTopMenu() {
                    if ((sceneListener as HomeSceneListener).getConfigInfo("pvr")) {
                        homeFilterGridView?.layoutManager?.findViewByPosition(4)?.requestFocus()
                    } else {
                        homeFilterGridView?.layoutManager?.findViewByPosition(3)?.requestFocus()
                    }

                    val translate: Animation = TranslateAnimation(0F,0F, -Utils.getDimens(R.dimen.custom_dim_61), 0f)
                    translate.duration = animationDuration

                    val alpha: Animation = AlphaAnimation(0f, 1f)
                    alpha.duration = animationDuration

                    val animationSet = AnimationSet(true)
                    animationSet.addAnimation(translate)
                    animationSet.addAnimation(alpha)
                    animationSet.interpolator = DecelerateInterpolator()
                    animationSet.fillAfter = true

                    homeFilterGridView!!.startAnimation(animationSet)
                    upArrow!!.visibility = View.GONE
                }

                override fun getAudioInformationData(type: PrefType) {
                    (sceneListener as HomeSceneListener).getAudioInformation(type)
                }

                override fun getFirstAudioLanguage(type: PrefType): LanguageCode? {
                    return (sceneListener as HomeSceneListener).getAudioFirstLanguage(type)
                }

                override fun onFirstAudioLanguageSelected(languageCode: LanguageCode, type: PrefType) {
                    (sceneListener as HomeSceneListener).onAudioFirstLanguageClicked(languageCode, type)
                }

                override fun getSecondAudioLanguage(type: PrefType): LanguageCode? {
                    return (sceneListener as HomeSceneListener).getAudioSecondLanguage(type)
                }

                override fun onSecondAudioLanguageSelected(languageCode: LanguageCode, type: PrefType) {
                    (sceneListener as HomeSceneListener).onAudioSecondLanguageClicked(languageCode, type)
                }

                override fun onAudioTypeClicked(position: Int) {
                    (sceneListener as HomeSceneListener).onAudioTypeClicked(position)
                }

                override fun onFaderValueChanged(newValue: Int) {
                    (sceneListener as HomeSceneListener).onFaderValueChanged(newValue)
                }

                override fun onAudioViValueChanged(newValue: Int) {
                    (sceneListener as HomeSceneListener).onAudioViValueChanged(newValue)
                }

                override fun onVisuallyImpairedValueChanged(position: Int, enabled: Boolean) {
                    (sceneListener as HomeSceneListener).onVisuallyImpairedValueChanged(position,enabled)
                }

                override fun onVolumeChanged(newVolumeValue: Int) {
                    (sceneListener as HomeSceneListener).onVolumeChanged(newVolumeValue)
                }

                override fun getSubtitleInformationData(type: PrefType) {
                    (sceneListener as HomeSceneListener).getSubtitleInformation(type)
                }

                override fun getFirstSubtitleLanguage(type: PrefType): LanguageCode? {
                    return (sceneListener as HomeSceneListener).getSubtitleFirstLanguage(type)
                }

                override fun onFirstSubtitleLanguageSelected(languageCode: LanguageCode, type: PrefType) {
                    (sceneListener as HomeSceneListener).onSubtitleFirstLanguageClicked(
                        languageCode, type
                    )
                }

                override fun getSecondSubtitleLanguage(type: PrefType): LanguageCode? {
                    return (sceneListener as HomeSceneListener).getSubtitleSecondLanguage(type)
                }

                override fun onSecondSubtitleLanguageSelected(languageCode: LanguageCode, type: PrefType) {
                    (sceneListener as HomeSceneListener).onSubtitleSecondLanguageClicked(
                        languageCode, type
                    )
                }

                override fun onSubtitlesEnabledClicked(isEnabled: Boolean, type: PrefType) {
                    (sceneListener as HomeSceneListener).onSubtitlesEnabledClicked(
                        isEnabled, type
                    )
                }

                override fun onSubtitlesTypeClicked(position: Int) {
                    (sceneListener as HomeSceneListener).onSubtitlesTypeClicked(
                        position
                    )
                }

                override fun isAnalogSubtitleEnabled(type: PrefType): Boolean {
                    return (sceneListener as HomeSceneListener).isAnalogSubtitleEnabled(type)
                }
                override fun onSubtitlesAnalogTypeClicked(position: Int) {
                    (sceneListener as HomeSceneListener).onSubtitlesAnalogTypeClicked(
                        position
                    )
                }

                override fun onClosedCaptionChanged(isCaptionEnabled: Boolean) {
                    (sceneListener as HomeSceneListener).onClosedCaptionChanged(
                        isCaptionEnabled
                    )
                }

                override fun getAdsTargetingData(type: PrefType) {
                    (sceneListener as HomeSceneListener).getAdsTargetingData(type)
                }

                override fun getTermsOfServiceData(type: PrefType) {
                    (sceneListener as HomeSceneListener).getTermsOfServiceData(type)
                }

                override fun getSystemInformationData() {
                    (sceneListener as HomeSceneListener).getSystemInformation()
                }

                override fun getPreferencesSetupData() {
                    (sceneListener as HomeSceneListener).getPreferencesSetup()
                }

                override fun onChannelsScanClicked() {
                    (sceneListener as HomeSceneListener).onChannelsScanClicked()
                }

                override fun onChannelsSettingsClicked() {
                    (sceneListener as HomeSceneListener).onChannelsSettingsClicked()
                }

                override fun onChannelsEditClicked() {
                    (sceneListener as HomeSceneListener).onChannelsEditClicked()
                }

                override fun onParentalControlClicked() {
                    (sceneListener as HomeSceneListener).onParentalControlClicked()
                }

                override fun getPreferencesTxtData() {
                    (sceneListener as HomeSceneListener).getPreferencesTxtData()
                }

                override fun getPreferencesCamData() {
                    (sceneListener as HomeSceneListener).getPreferencesCamData()
                }

                override fun onDefaultChannelSelected(tvChannel: TvChannel) {
                    (sceneListener as HomeSceneListener).onPreferencesDefaultChannelSelected(
                        tvChannel
                    )
                }

                override fun onDisplayModeSelected(selectedMode: Int) {
                    (sceneListener as HomeSceneListener).onDisplayModeSelected(selectedMode)
                }

                override fun getCamInfoModuleInfoData() {
                    (sceneListener as HomeSceneListener).getPreferencesCamInfoModuleInformation()
                }

                override fun onCamInfoSoftwareDownloadPressed() {
                    (sceneListener as HomeSceneListener).onPreferencesSoftwareDownloadPressed()
                }

                override fun onCamInfoSubscriptionStatusPressed() {
                    (sceneListener as HomeSceneListener).onPreferencesSubscriptionStatusPressed()
                }

                override fun onCamInfoEventStatusPressed() {
                    (sceneListener as HomeSceneListener).onPreferencesEventStatusPressed()
                }

                override fun onCamInfoTokenStatusPressed() {
                    (sceneListener as HomeSceneListener).onPreferencesTokenStatusPressed()
                }

                override fun onCamInfoChangeCaPinPressed() {
                    (sceneListener as HomeSceneListener).onPreferencesChangeCaPinPressed()
                }

                override fun getCamInfoMaturityRating(): String {
                    return (sceneListener as HomeSceneListener).getPreferencesCamInfoMaturityRating()
                }

                override fun onCamInfoConaxCaMessagesPressed() {
                    (sceneListener as HomeSceneListener).onPreferencesConaxCaMessagesPressed()
                }

                override fun onCamInfoAboutConaxCaPressed() {
                    (sceneListener as HomeSceneListener).onPreferencesAboutConaxCaPressed()
                }

                override fun getCamInfoSettingsLanguages() {
                    (sceneListener as HomeSceneListener).getPreferencesCamInfoSettingsLanguages()
                }

                override fun onCamInfoSettingsLanguageSelected(position: Int) {
                    (sceneListener as HomeSceneListener).onPreferencesCamInfoSettingsLanguageSelected(
                        position
                    )
                }

                override fun onCamInfoPopUpMessagesActivated(activated: Boolean) {
                    (sceneListener as HomeSceneListener).onPreferencesCamInfoPopUpMessagesActivated(
                        activated
                    )
                }

                override fun isCamInfoPopUpMessagesActivated(): Boolean {
                    return (sceneListener as HomeSceneListener).isPreferencesCamInfoPopUpMessagesActivated()
                }

                override fun getHbbTvInformation() {
                    (sceneListener as HomeSceneListener).getHbbTvInformation()
                }

                override fun onHbbSupportSwitch(isEnabled: Boolean) {
                    (sceneListener as HomeSceneListener).onHbbSupportSwitch(isEnabled)
                }

                override fun onHbbTrackSwitch(isEnabled: Boolean) {
                    (sceneListener as HomeSceneListener).onHbbTrackSwitch(isEnabled)
                }

                override fun onHbbPersistentStorageSwitch(isEnabled: Boolean) {
                    (sceneListener as HomeSceneListener).onHbbPersistentStorageSwitch(isEnabled)
                }

                override fun onHbbBlockTracking(isEnabled: Boolean) {
                    (sceneListener as HomeSceneListener).onHbbBlockTracking(isEnabled)
                }

                override fun onHbbTvDeviceId(isEnabled: Boolean) {
                    (sceneListener as HomeSceneListener).onHbbTvDeviceId(isEnabled)
                }

                override fun onHbbCookieSettingsSelected(position: Int) {
                    (sceneListener as HomeSceneListener).onHbbCookieSettingsSelected(position)
                }

                override fun onHbbTvResetDeviceId() {
                    (sceneListener as HomeSceneListener).onHbbTvResetDeviceId()
                }

                override fun onEvaluationLicenseClicked() {
                    (sceneListener as HomeSceneListener).onEvaluationLicenseClicked()
                }

//                override fun onOpenSourceLicenceClicked() {
//                    (sceneListener as HomeSceneListener).onOpenSourceLicenceClicked()
//                }

                override fun onTTXDigitalLanguageSelected(position: Int, language: String) {
                    (sceneListener as HomeSceneListener).onTTXDigitalLanguageClicked(position, language)
                }

                override fun onTTXDecodeLanguageSelected(position: Int) {
                    (sceneListener as HomeSceneListener).onTTXDecodeLanguageClicked(position)
                }

                override fun onAspectRatioClicked(position: Int) {
                    (sceneListener as HomeSceneListener).onAspectRatioClicked(position)
                }

                override fun setInteractionChannel(isSelected: Boolean) {
                    (sceneListener as HomeSceneListener).setInteractionChannel(isSelected)
                }

                override fun onEpgLanguageSelected(language: String) {
                    (sceneListener as HomeSceneListener).onEpgLanguageChanged(language)
                }

                override fun getParentalInformationData(type: PrefType) {
                    (sceneListener as HomeSceneListener).getParentalInformation(type)
                }

                override fun getClosedCationsInformationData() {
                    (sceneListener as HomeSceneListener).getClosedCationsInformation()
                }

                override fun getPreferencesPvrTimeshiftData() {
                    (sceneListener as HomeSceneListener).getPreferencesPvrTimeshiftData()
                }

                override fun getPrefsValue(key: String, value: Any?): Any? {
                    return (sceneListener as HomeSceneListener).getPrefsValue(key, value)
                }

                override fun getPrefsValue(tag: String, defValue: String): String {
                    return (sceneListener as HomeSceneListener).getPrefsValue(tag, defValue)
                }

                override fun setPrefsValue(key: String, defValue: Any?) {
                    (sceneListener as HomeSceneListener).setPrefsValue(key, defValue)
                }

                override fun lockChannel(tvChannel: TvChannel, selected: Boolean) {
                    (sceneListener as HomeSceneListener).lockChannel(tvChannel, selected)
                }

                override fun isChannelLockAvailable(tvChannel: TvChannel): Boolean {
                    return (sceneListener as HomeSceneListener).isChannelLockAvailable(tvChannel)
                }

                override fun blockInput(inputSourceData: InputSourceData, blocked: Boolean) {
                    (sceneListener as HomeSceneListener).blockInput(inputSourceData, blocked)
                }

                override fun isParentalControlsEnabled():Boolean {
                    return (sceneListener as HomeSceneListener).isParentalControlsEnabled()
                }

                override fun setParentalControlsEnabled(enabled: Boolean) {
                    refreshForYou()
                    return (sceneListener as HomeSceneListener).setParentalControlsEnabled(enabled)
                }

                override fun setContentRatingSystemEnabled(
                    contentRatingSystem: ContentRatingSystem,
                    enabled: Boolean
                ) {
                    (sceneListener as HomeSceneListener).setContentRatingSystemEnabled(contentRatingSystem, enabled)
                }

                override fun setContentRatingLevel(index: Int) {
                    (sceneListener as HomeSceneListener).setContentRatingLevel(index)
                }
                override fun setRatingBlocked(
                    contentRatingSystem: ContentRatingSystem,
                    it: ContentRatingSystem.Rating,
                    data: Boolean
                ): Boolean {
                    return (sceneListener as HomeSceneListener).setRatingBlocked(contentRatingSystem, it, data)
                }

                override fun setRelativeRatingsEnabled(
                    contentRatingSystem: ContentRatingSystem,
                    it: ContentRatingSystem.Rating,
                    data: Boolean
                ) {
                    (sceneListener as HomeSceneListener).setRelativeRatingsEnabled(contentRatingSystem, it, data)
                }

                override fun isRatingBlocked(
                    contentRatingSystem: ContentRatingSystem,
                    it: ContentRatingSystem.Rating
                ): Boolean {
                    return (sceneListener as HomeSceneListener).isRatingBlocked(contentRatingSystem,it)
                }

                override fun isSubRatingEnabled(
                    contentRatingSystem: ContentRatingSystem,
                    it: ContentRatingSystem.Rating,
                    subRating: ContentRatingSystem.SubRating
                ): Boolean {
                    return (sceneListener as HomeSceneListener).isSubRatingEnabled(contentRatingSystem, it, subRating)
                }

                override fun getContentRatingLevelIndex(): Int {
                    return (sceneListener as HomeSceneListener).getContentRatingLevelIndex()
                }

                override fun setSubRatingBlocked(
                    contentRatingSystem: ContentRatingSystem,
                    rating: ContentRatingSystem.Rating,
                    subRating: ContentRatingSystem.SubRating,
                    data: Boolean
                ) {
                    return (sceneListener as HomeSceneListener).setSubRatingBlocked(contentRatingSystem,rating,subRating,data)
                }

                override fun setRelativeRating2SubRatingEnabled(
                    contentRatingSystem: ContentRatingSystem,
                    data: Boolean,
                    rating: ContentRatingSystem.Rating,
                    subRating: ContentRatingSystem.SubRating
                ) {
                    return (sceneListener as HomeSceneListener).setRelativeRating2SubRatingEnabled(contentRatingSystem,data,rating,subRating)
                }

                override fun setBlockUnrated(blocked: Boolean) {
                    (sceneListener as HomeSceneListener).setBlockUnrated(blocked)
                }

                override fun getRRT5DimInfo(index: Int): MutableList<String> {
                    return (sceneListener as HomeSceneListener).getRRT5DimInfo(index)
                }

                override fun getRRT5CrsInfo(regionName: String): MutableList<ContentRatingSystem> {
                    return (sceneListener as HomeSceneListener).getRRT5CrsInfo(regionName)
                }

                override fun getRRT5LevelInfo(
                    countryIndex: Int,
                    dimIndex: Int
                ): MutableList<String> {
                    return (sceneListener as HomeSceneListener).getRRT5LevelInfo(countryIndex, dimIndex)
                }

                override fun getSelectedItemsForRRT5Level(): HashMap<Int, Int> {
                    return (sceneListener as HomeSceneListener).getSelectedItemsForRRT5Level()
                }

                override fun rrt5BlockedList(regionPosition :Int,position: Int): HashMap<Int, Int> {
                    return (sceneListener as HomeSceneListener).rrt5BlockedList(regionPosition,position)
                }

                override fun setSelectedItemsForRRT5Level(
                    regionIndex: Int,
                    dimIndex: Int,
                    levelIndex: Int
                ) {
                    (sceneListener as HomeSceneListener).setSelectedItemsForRRT5Level(regionIndex,
                        dimIndex,
                        levelIndex)
                }

                override fun resetRRT5() {
                    (sceneListener as HomeSceneListener).resetRRT5()
                }

                override fun getRRT5Regions(): MutableList<String> {
                    return (sceneListener as HomeSceneListener).getRRT5Regions()
                }

                override fun onSkipChannelSelected(tvChannel: TvChannel, status: Boolean) {
                    (sceneListener as HomeSceneListener).onSkipChannelSelected(tvChannel, status)
                }

                override fun onSwapChannelSelected(
                    firstChannel: TvChannel,
                    secondChannel: TvChannel,
                    previousPosition: Int,
                    newPosition: Int
                ) {
                    (sceneListener as HomeSceneListener).onSwapChannelSelected(firstChannel, secondChannel, previousPosition, newPosition)
                }

                override fun onMoveChannelSelected(
                    moveChannelList: ArrayList<TvChannel>,
                    previousPosition: Int,
                    newPosition: Int,
                    channelMap: HashMap<Int, String>
                ) {
                    (sceneListener as HomeSceneListener).onMoveChannelSelected(moveChannelList, previousPosition, newPosition, channelMap)
                }

                override fun onDeleteChannelSelected(tvChannel: TvChannel, index: Int) {
                    (sceneListener as HomeSceneListener).onDeleteChannelSelected(tvChannel, index)
                }

                override fun onClearChannelSelected() {
                    (sceneListener as HomeSceneListener).onClearChannelSelected()
                }

                override fun getSignalAvailable(): Boolean {
                    return (sceneListener as HomeSceneListener).getSignalAvailable()
                }

                override fun getChannelSourceType(tvChannel: TvChannel) : String {
                    return (sceneListener as HomeSceneListener).getChannelSourceType(tvChannel)
                }

                override fun setPowerOffTime(value: Int, time: String) {
                    return (sceneListener as HomeSceneListener).setPowerOffTime(value, time)
                }

                override fun onPowerClicked() {
                    (sceneListener as HomeSceneListener).onPowerClicked()
                }

                override fun isDigitalSubtitleEnabled(type: PrefType): Boolean {
                    return (sceneListener as HomeSceneListener).isDigitalSubtitleEnabled(type)
                }

                override fun onAdsTargetingChange(enable: Boolean) {
                    return (sceneListener as HomeSceneListener).onAdsTargetingChange(enable)
                }

                override fun isAccessibilityEnabled(): Boolean {
                    return (sceneListener as HomeSceneListener).isAccessibilityEnabled()
                }

                override fun changeChannelFromTalkback(tvChannel: TvChannel) {
                    (sceneListener as HomeSceneListener).changeChannelFromTalkback(tvChannel)
                }

                override fun onDpadDown(btnView: ImageView?) {

                }

                override fun getAnokiRatingLevel(): Int {
                    return (sceneListener as HomeSceneListener).getAnokiRatingLevel()
                }

                override fun setAnokiRatingLevel(level: Int) {
                    (sceneListener as HomeSceneListener).setAnokiRatingLevel(level)
                }

                override fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int) {
                    (sceneListener as HomeSceneListener).saveUserSelectedCCOptions(
                        ccOptions, newValue
                    )
                }

                override fun resetCC() {
                    (sceneListener as HomeSceneListener).resetCC()
                }

                override fun setCCInfo() {
                    (sceneListener as HomeSceneListener).setCCInfo()
                }

                override fun disableCCInfo() {
                    (sceneListener as HomeSceneListener).disableCCInfo()
                }

                override fun setCCWithMute(isEnabled: Boolean) {
                    (sceneListener as HomeSceneListener).setCCWithMute(isEnabled)
                }

                override fun lastSelectedTextOpacity(position: Int) {
                    (sceneListener as HomeSceneListener).lastSelectedTextOpacity(position)
                }

                override fun onChannelClicked() {
                    (sceneListener as HomeSceneListener).onChannelClicked()
                }

                override fun onBlueMuteChanged(isEnabled: Boolean) {
                    (sceneListener as HomeSceneListener).onBlueMuteChanged(isEnabled)
                }

                override fun noSignalPowerOffChanged(enabled: Boolean) {
                    (sceneListener as HomeSceneListener).noSignalPowerOffChanged(enabled)
                }

                override fun noSignalPowerOffTimeChanged() {
                    (sceneListener as HomeSceneListener).noSignalPowerOffTimeChanged()
                }

                override fun getUtilsModule(): UtilsInterface {
                    return (sceneListener as HomeSceneListener).getUtilsModule()
                }

                override fun isLcnEnabled(): Boolean {
                    return (sceneListener as HomeSceneListener).isLcnEnabled()
                }

                override fun enableLcn(enable: Boolean) {
                    (sceneListener as HomeSceneListener).enableLcn(enable)
                }

                override fun getCiPlusInterface(): CiPlusInterface {
                    return (sceneListener as HomeSceneListener).getCiPlusInterface()
                }

                override fun storePrefsValue(tag: String, defValue: String) {
                    (sceneListener as HomeSceneListener).storePrefsValue(tag, defValue)
                }

                override fun onPictureSettingsClicked() {
                    (sceneListener as HomeSceneListener).onPictureSettingsClicked()
                }

                override fun onScreenSettingsClicked() {
                    (sceneListener as HomeSceneListener).onScreenSettingsClicked()
                }

                override fun onSoundSettingsClicked() {
                    (sceneListener as HomeSceneListener).onSoundSettingsClicked()
                }

                override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                    return false
                }

                override fun setAudioDescriptionEnabled(enable: Boolean){
                    (sceneListener as HomeSceneListener).setAudioDescriptionEnabled(enable)
                }

                override fun setHearingImpairedEnabled(enable: Boolean){
                    (sceneListener as HomeSceneListener).setHearingImpairedEnabled(enable)
                }

                override fun checkNetworkConnection(): Boolean {
                    return (sceneListener as HomeSceneListener).checkNetworkConnection()
                }

                override fun isAnokiParentalControlsEnabled(): Boolean {
                    return (sceneListener as HomeSceneListener).isAnokiParentalControlsEnabled()
                }

                override fun setAnokiParentalControlsEnabled(enabled: Boolean) {
                    (sceneListener as HomeSceneListener).setAnokiParentalControlsEnabled(enabled)
                }

                override fun onExit() {
                    (sceneListener as HomeSceneListener).backToLive()
                }

                override fun getPreferenceDeepLink(): ReferenceWidgetPreferences.DeepLink? {
                    return (sceneListener as HomeSceneListener).getPreferenceDeepLink()
                }

                override fun startOadScan() {
                    return (sceneListener as HomeSceneListener).startOadScan()
                }

                override fun startCamScan() {
                    return (sceneListener as HomeSceneListener).startCamScan()
                }

                override fun changeCamPin() {
                    return (sceneListener as HomeSceneListener).startCamScan()
                }

                override fun selectMenuItem(position: Int) {
                    return (sceneListener as HomeSceneListener).selectMenuItem(position)
                }

                override fun enterMMI() {
                    return (sceneListener as HomeSceneListener).enterMMI()
                }

                override fun deleteProfile(profileName: String) {
                    return (sceneListener as HomeSceneListener).deleteProfile(profileName)
                }

                override fun onClickTermsOfService() {
                    return (sceneListener as HomeSceneListener).onClickTermsOfService()
                }

                override fun onClickPrivacyPolicy() {
                    return (sceneListener as HomeSceneListener).onClickPrivacyPolicy()
                }

                override fun doCAMReconfiguration(camTypePreference: CamTypePreference) {
                    return (sceneListener as HomeSceneListener).doCAMReconfiguration(camTypePreference)
                }

                override fun onTimeshiftStatusChanged(enabled: Boolean) {
                    (sceneListener as HomeSceneListener).onTimeshiftStatusChanged(enabled)
                }

                override fun getConfigInfo(nameOfInfo: String): Boolean {
                    return (sceneListener as HomeSceneListener).getConfigInfo(nameOfInfo)
                }

                override fun getLockedChannelIdsFromPrefs(): MutableSet<String> {
                    return (sceneListener as HomeSceneListener).getLockedChannelIdsFromPrefs()
                }
            })
    }

    open fun createFavouritesWidget() {
        favouritesSceneWidget = ReferenceWidgetFavourites(
            context,
            object : ReferenceWidgetFavourites.FavouritesWidgetListener {
                override fun requestFocusOnTopMenu() {
                    homeFilterGridView?.layoutManager?.findViewByPosition(2)
                        ?.requestFocus()
                }

                override fun onChannelClicked(
                    tvChannel: TvChannel,
                    favListIds: ArrayList<String>
                ) {
                    (sceneListener as HomeSceneListener).onFavChannelClicked(
                        tvChannel,
                        favListIds
                    )
                }

                override fun onAddButtonClicked() {
                    (sceneListener as HomeSceneListener).onAddFavouriteListClicked()
                }

                override fun onRenameButtonClicked(listName: String) {
                    (sceneListener as HomeSceneListener).onRenameFavouriteListClicked(
                        listName
                    )
                }

                override fun onDeleteButtonClicked(categoryName: String) {
                    (sceneListener as HomeSceneListener).onDeleteFavouriteListClicked(
                        categoryName
                    )
                }

                override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                    return false
                }

                override fun changeFilterToAll(category: String) {
                    (sceneListener as HomeSceneListener).changeFilterToAll(
                        category
                    )
                }
            })
    }

    open fun refreshForYou() {
        if (forYouWidget != null) {
            (sceneListener as HomeSceneListener).getForYouRails()
        }
    }

    open  fun createForYouSceneWidget(requestFocus: Boolean) {
        if (forYouWidget != null) {
            return
        }
        homeFilterGridView?.requestFocus()
        forYouWidget = ReferenceWidgetForYou(context,
            object : ReferenceWidgetForYou.ForYouWidgetListener {
                override fun isHomeTabFocused(): Boolean {
                    val focusedChild = homeFilterGridView?.layoutManager?.focusedChild
                    return homeFilterGridView?.layoutManager?.findViewByPosition(0) == focusedChild
                }

                override fun renameRecording(recording: Recording, name: String, callback: IAsyncCallback) {
                    (sceneListener as HomeSceneListener).onRenameRecording(recording, name, callback)
                }

                override fun deleteRecording(recording: Recording, callback: IAsyncCallback) {
                    (sceneListener as HomeSceneListener).onDeleteRecording(recording, callback)
                }

                override fun isRegionSupported(): Boolean {
                    return (sceneListener as HomeSceneListener).isRegionSupported()
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    (sceneListener as HomeSceneListener).setSpeechText(text = text, importance = importance)
                }

                override fun isAnokiServerReachable(): Boolean {
                    return (sceneListener as HomeSceneListener).isAnokiServerReachable()
                }

                override fun muteAudio() {
                    (sceneListener as HomeSceneListener).muteAudio()
                }

                override fun unMuteAudio() {
                    (sceneListener as HomeSceneListener).unMuteAudio()
                }

                override fun hasInternet(): Boolean {
                    return (sceneListener as HomeSceneListener).hasInternet()
                }

                override fun promotionItemActionClicked(promotionItem: PromotionItem) {
                    (sceneListener as HomeSceneListener).promotionItemActionClicked(promotionItem)
                }

                override fun onFastCardClicked(tvEvent: TvEvent) {
                    (sceneListener as HomeSceneListener).onFastCardClicked(tvEvent)
                }
                override fun getPromotionContent(callback: IAsyncDataCallback<ArrayList<PromotionItem>>) {
                    (sceneListener as HomeSceneListener).getPromotionContent(callback)
                }

                override fun getCurrentTime(tvChannel: TvChannel): Long {
                    return (sceneListener as HomeSceneListener).getCurrentTime(tvChannel)
                }

                override fun getRails(sceneConfig: SceneConfig?) {
                    (sceneListener as HomeSceneListener).getForYouRails()
                }

                override fun requestFocusOnTopCategories() {
                    homeFilterGridView?.requestFocus()
                }
                override fun showDetails(event: Any) {
                    (sceneListener as HomeSceneListener).showDetails(event)
                }

                override fun onItemClicked(recording: Any) {
                    (sceneListener as HomeSceneListener).onRecordingClick(recording)
                }

                override fun getRailSize(): Int {
                    return (sceneListener as HomeSceneListener).getRailSize()
                }

                override fun isChannelLocked(channelId: Int): Boolean {
                    return (sceneListener as HomeSceneListener).isChannelLocked(channelId)
                }

                override fun isParentalControlsEnabled(): Boolean {
                    return (sceneListener as HomeSceneListener).isParentalControlsEnabled()
                }

                override fun isInWatchlist(tvEvent: TvEvent): Boolean {
                    return (sceneListener as HomeSceneListener).isInWatchList(tvEvent)
                }

                override fun isInRecList(tvEvent: TvEvent): Boolean {
                    return (sceneListener as HomeSceneListener).isInRecordingList(tvEvent)
                }

                override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
                    return (sceneListener as HomeSceneListener).getParentalRatingDisplayName(parentalRating, tvEvent)
                }

                override fun keepFocusOnFilerView() {
                    try {
                        if (homeFilterCategoryAdapter?.focusPosition == 0 && !searchIv!!.hasFocus()) {
                            homeFilterCategoryAdapter?.requestFocus(0)
                        }
                        if (homeFilterCategoryAdapter!!.focusPosition == 0 && searchIv?.hasFocus() == true) {
                            var holder = homeFilterGridView!!.findViewHolderForAdapterPosition(
                                0
                            ) as CategoryItemViewHolder
                            homeFilterCategoryAdapter!!.setActiveFilter(holder)
                        }
                    } catch (E: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "keepFocusOnFilerView: ${E.printStackTrace()}")
                        worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        return
                    }
                }

                override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                    return dispatchKeyEvent(keyCode, event)
                }

                override fun isEventLocked(tvEvent: TvEvent?) = (sceneListener as HomeSceneListener).isEventLocked(tvEvent)

                override fun isTosAccepted() = (sceneListener as HomeSceneListener).isTosAccepted()
            },
            (sceneListener as HomeSceneListener).getDateTimeFormat())

        forYouWidget!!.setup()
    }

    open fun createGuideSceneWidget() {
        if ((sceneListener as HomeSceneListener).isChannelListEmpty()){
            guideScanWidget = ScanChannelsGuide(context,object : GuideScanListener{
                override fun startScan() {
                    (sceneListener as HomeSceneListener).startScan()
                }

                override fun requestFocusOnTopMenu() {
                    homeFilterCategoryAdapter?.keepFocus = false
                    homeFilterGridView?.findViewHolderForAdapterPosition(1)?.let {
                        if (it is CategoryItemViewHolder) {
                            val text = it.categoryText?.text
                            if (text == ConfigStringsManager.getStringById("live")) {
                                homeFilterGridView?.layoutManager?.findViewByPosition(2)?.requestFocus()
                            } else {
                                homeFilterGridView?.layoutManager?.findViewByPosition(1)?.requestFocus()
                            }
                        } else {
                            homeFilterGridView?.layoutManager?.findViewByPosition(1)?.requestFocus()
                        }
                    }
                }

            })
            return
        }else{
            guideScanWidget = null
            if (!guideVerticalOrientation()) {
                //starting epg scroll timer
                updateScrollingForGuide()
                //Create guide scene widget
                guideSceneWidget = HorizontalGuideSceneWidget(context, object : GuideSceneWidgetListener {

                    override fun onInitialized(){
                        showEpgData()
                        val areChannelsNonBrowsable = (sceneListener as HomeSceneListener).areChannelsNonBrowsable()
                        guideSceneWidget?.refresh(areChannelsNonBrowsable)

                    }

                    override fun getActiveEpgFilter(): Int {
                        return (sceneListener as HomeSceneListener).getActiveEpgFilter()
                    }

                    override fun isAccessibilityEnabled(): Boolean {
                        return (sceneListener as HomeSceneListener).isAccessibilityEnabled()
                    }

                    override fun changeChannelFromTalkback(tvChannel: TvChannel) {
                        (sceneListener as HomeSceneListener).changeChannelFromTalkback(tvChannel)
                    }

                    override fun fetchEventForChannelList(
                        callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>,
                        channelList: MutableList<TvChannel>,
                        dayOffset: Int,
                        additionalDayOffset: Int,
                        isExtend: Boolean
                    ) {
                        (sceneListener as HomeSceneListener).getEventsByChannelList(callback,channelList,dayOffset,additionalDayOffset,isExtend)
                    }

                    override fun onOptionsClicked() {
                        launchPreferences()
                    }

                    override fun getDateTimeFormat() : DateTimeFormat {
                        return (sceneListener as HomeSceneListener).getDateTimeFormat()
                    }

                    override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
                        return (sceneListener as HomeSceneListener).getParentalRatingDisplayName(parentalRating, tvEvent)
                    }

                    override suspend fun getCurrentTime(tvChannel: TvChannel): Long {
                        return (sceneListener as HomeSceneListener).getCurrentTime(tvChannel)
                    }

                    override fun isCurrentEvent(tvEvent: TvEvent): Boolean {
                        return (sceneListener as HomeSceneListener).isCurrentEvent(tvEvent)
                    }

                    override fun getCurrentAudioTrack(): IAudioTrack? {
                        return (sceneListener as HomeSceneListener).getCurrentAudioTrack()
                    }

                    override fun getAvailableAudioTracks(): List<IAudioTrack> {
                        return (sceneListener as HomeSceneListener).getAvailableAudioTracks()
                    }

                    override fun getAvailableSubtitleTracks(): List<ISubtitle> {
                        return (sceneListener as HomeSceneListener).getAvailableSubtitleTracks()
                    }

                    override fun getClosedCaption(): String? {
                        return (sceneListener as HomeSceneListener).getClosedCaption()
                    }

                    override fun requestFocusOnTopMenu() {
                        homeFilterCategoryAdapter?.keepFocus = false
                        homeFilterGridView?.findViewHolderForAdapterPosition(1)?.let {
                            if (it is CategoryItemViewHolder) {
                                val text = it.categoryText?.text
                                if (text == ConfigStringsManager.getStringById("live")) {
                                    homeFilterGridView?.layoutManager?.findViewByPosition(2)?.requestFocus()
                                } else {
                                    homeFilterGridView?.layoutManager?.findViewByPosition(1)?.requestFocus()
                                }
                            } else {
                                homeFilterGridView?.layoutManager?.findViewByPosition(1)?.requestFocus()
                            }
                        }
                    }

                    override fun setGuideButtonAsActiveFilter() {
                        val holder =
                            homeFilterGridView?.findViewHolderForAdapterPosition(
                                selectedPosition
                            ) as CategoryItemViewHolder

                        homeFilterCategoryAdapter?.setActiveFilter(holder)
                    }

                    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                        (sceneListener as HomeSceneListener).setSpeechText(text = text, importance = importance)
                    }

                    override fun stopSpeech() {
                        (sceneListener as HomeSceneListener).stopSpeech()
                    }

                    override fun onFilterSelected(filterId: Int) {
                        guideSceneWidget?.setFocusToCategory()
                        (sceneListener as HomeSceneListener).getActiveChannel(object :
                            IAsyncDataCallback<TvChannel> {
                            override fun onFailed(error: kotlin.Error) {}

                            override fun onReceive(data: TvChannel) {
                                (sceneListener as HomeSceneListener).getEventsForChannels(
                                    data,
                                    filterId,
                                    object :
                                        IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>> {
                                        override fun onFailed(error: kotlin.Error) {
                                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onFailed: $error", )
                                            guideSceneWidget?.onFetchFailed()
                                        }

                                        override fun onReceive(eventListMap:LinkedHashMap<Int, MutableList<TvEvent>>) {
                                            runOnUiThread(Runnable {
                                                initGuideWidget(filterId, eventListMap)
                                            })
                                        }

                                    },
                                    guideSceneWidget?.dayOffset!!, 0, false
                                )
                            }

                        })

                    }

                    override fun onCatchUpButtonPressed(tvEvent: TvEvent) {}

                    @RequiresApi(Build.VERSION_CODES.R)
                    override fun onWatchButtonPressed(tvChannel: TvChannel) {
                        val applicationMode =
                            if (((worldHandler as ReferenceWorldHandler).getApplicationMode()) == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
                        (sceneListener as HomeSceneListener). getActiveChannel(object :IAsyncDataCallback<TvChannel>{
                            override fun onFailed(error: kotlin.Error) {
                                (sceneListener as HomeSceneListener).playTvChannel(tvChannel)
                            }

                            override fun onReceive(data: TvChannel) {
                                if (worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING) {
                                    //if watch is pressed on same channel
                                    // where recording is in progress then no pop up will be shown
                                    if (data.channelId == tvChannel.channelId) {
                                        worldHandler?.triggerAction(
                                            ReferenceWorldHandler.SceneId.HOME_SCENE,
                                            SceneManager.Action.HIDE
                                        )}
                                    else{
                                        InformationBus.submitEvent(
                                            Event(
                                                Events.SHOW_STOP_RECORDING_DIALOG,
                                                tvChannel
                                            )
                                        )
                                    }
                                }else {
                                    if (!((worldHandler) as ReferenceWorldHandler).isT56() && data.channelId == tvChannel.channelId) {
                                        worldHandler?.triggerAction(
                                            ReferenceWorldHandler.SceneId.HOME_SCENE,
                                            SceneManager.Action.HIDE
                                        ) // HIDE is used because whole Home Scene should stay in memory in order to enable it's fast accessing when pressing back from LiveScene
                                    } else {
                                        (sceneListener as HomeSceneListener).playTvChannel(tvChannel)
                                    }
                                    //Use channel changed information bus event to show zap banner
                                    InformationBus.submitEvent(Event(Events.CHANNEL_CHANGED, data))
                                }
                            }
                        },applicationMode)
                    }

                    override fun onRecordButtonPressed(tvEvent: TvEvent, callback: IAsyncCallback) {
                        (sceneListener as HomeSceneListener).onRecordButtonPressed(tvEvent, callback)
                    }

                    override fun onWatchlistButtonPressed(tvEvent: TvEvent): Boolean {
                        return (sceneListener as HomeSceneListener).onGuideWatchlistClicked(tvEvent)
                    }

                    override fun onFavoriteButtonPressed(
                        tvChannel: TvChannel,
                        favListIds: ArrayList<String>
                    ) {
                        (sceneListener as HomeSceneListener).onGuideFavoriteButtonPressed(
                            tvChannel,
                            favListIds
                        )
                    }

                    override fun onMoreInfoButtonPressed(tvEvent: TvEvent) {
                        (sceneListener as HomeSceneListener).showDetails(tvEvent)
                    }

                    override fun getDayWithOffset(
                        additionalDayCount: Int,
                        isExtend: Boolean,
                        channelList: MutableList<TvChannel>
                    ) {
                        getEPGEventsData(channelList, additionalDayCount, isExtend)

                    }

                    override fun onDigitPressed(
                        digit: Int,
                        epgActiveFilter: FilterItemType?,
                        filterMetadata: String?
                    ) {
                        (sceneListener as HomeSceneListener).onDigitPressed(digit,epgActiveFilter,filterMetadata)
                    }

                    override fun setActiveFilterOnGuide() {
                        homeFilterGridView!!.clearFocus()
                        var holder = homeFilterGridView!!.findViewHolderForAdapterPosition(
                            2
                        ) as? CategoryItemViewHolder
                        if (holder != null) {
                            homeFilterCategoryAdapter!!.setActiveFilter(holder)
                        }

                    }

                    override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                        return dispatchKeyEvent(keyCode, event)
                    }

                    override fun clearFocusFromMainMenu() {
                        if (homeFilterGridView?.getChildAt(1)?.isFocused == true) {
                            homeFilterGridView?.getChildAt(1)?.clearFocus()
                            homeFilterCategoryAdapter?.clearPreviousFocus()
                        } else if (!ReferenceApplication.isRegionSupported) {
                            homeFilterGridView?.getChildAt(1)?.clearFocus()
                            homeFilterCategoryAdapter?.clearPreviousFocus()
                            homeFilterCategoryAdapter!!.setSelected(1)
                        }
                    }

                    override fun getChannelsOfSelectedFilter(): MutableList<TvChannel> {
                        return (sceneListener as HomeSceneListener).getChannelsOfSelectedFilter()
                    }

                    override fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>) {
                        (sceneListener as HomeSceneListener).getActiveChannel(callback)
                    }

                    override fun loadNextChannels(
                        anchorChannel: TvChannel,
                        callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>,
                        currentDayOffset: Int,
                        nextdayoffset: Int,
                        extend: Boolean
                    ) {
                        (sceneListener as HomeSceneListener).loadNextEpgData(
                            anchorChannel,
                            callback,
                            currentDayOffset,
                            nextdayoffset,
                            extend
                        )
                    }

                    override fun loadPreviousChannels(
                        anchorChannel: TvChannel,
                        callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>,
                        currentDayOffset: Int,
                        nextdayoffset: Int,
                        extend: Boolean
                    ) {
                        (sceneListener as HomeSceneListener).loadPreviousChannels(
                            anchorChannel,
                            callback,
                            currentDayOffset,
                            nextdayoffset,
                            extend
                        )
                    }

                    override fun getEventsForChannels(
                        anchorChannel: TvChannel,
                        epgActiveFilter: Int,
                        callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>,
                        currentDayOffset: Int,
                        nextdayoffset: Int,
                        extend: Boolean
                    ) {
                        (sceneListener as HomeSceneListener).getEventsForChannels(
                            anchorChannel,
                            epgActiveFilter,
                            callback,
                            currentDayOffset,
                            nextdayoffset,
                            extend
                        )
                    }

                    override fun getRecordingInProgressTvChannel(): TvChannel? {
                        return (sceneListener as HomeSceneListener).getRecordingInProgressTvChannel()
                    }

                    override fun isInRecordingList(tvEvent: TvEvent): Boolean {
                        return (sceneListener as HomeSceneListener).isInRecordingList(tvEvent)
                    }

                    override fun isInWatchList(tvEvent: TvEvent): Boolean {
                        return (sceneListener as HomeSceneListener).isInWatchList(tvEvent)
                    }

                    override fun getFavoriteCategories(callback: IAsyncDataCallback<ArrayList<String>>) {
                        return (sceneListener as HomeSceneListener).getFavoriteCategories(callback)
                    }

                    override fun getFavoriteSelectedItems(tvChannel: TvChannel): ArrayList<String> {
                        return (sceneListener as HomeSceneListener).getFavoriteSelectedItems(tvChannel)
                    }

                    override fun isRecordingInProgress(): Boolean {
                        return (sceneListener as HomeSceneListener).isRecordingInProgress()
                    }

                    override fun isParentalEnabled(): Boolean {
                        return (sceneListener as HomeSceneListener).isParentalControlsEnabled()
                    }

                    override fun isClosedCaptionEnabled(): Boolean {
                        return (sceneListener as HomeSceneListener).isClosedCaptionEnabled()
                    }

                    override fun isCCTrackAvailable(): Boolean? {
                        return (sceneListener as HomeSceneListener).isCCTrackAvailable()
                    }

                    override fun getIsAudioDescription(type: Int): Boolean {
                        return (sceneListener as HomeSceneListener).getIsAudioDescription(type)
                    }

                    override fun getIsDolby(type: Int): Boolean {
                        return (sceneListener as HomeSceneListener).getIsDolby(type)
                    }

                    override fun isHOH(type: Int): Boolean {
                        return (sceneListener as HomeSceneListener).isHOH(type)
                    }

                    override fun isTeleText(type: Int): Boolean {
                        return (sceneListener as HomeSceneListener).isTeleText(type)
                    }

                    override fun isInFavoriteList(tvChannel: TvChannel): Boolean {
                        return (sceneListener as HomeSceneListener).isInFavoriteList(tvChannel)
                    }

                    override fun getAudioChannelInfo(type: Int): String {
                        return (sceneListener as HomeSceneListener).getAudioChannelInfo(type)
                    }

                    override fun getAudioFormatInfo(): String {
                        return (sceneListener as HomeSceneListener).getAudioFormatInfo()
                    }

                    override fun getVideoResolution(): String {
                        return (sceneListener as HomeSceneListener).getVideoResolution()
                    }

                    override fun isEventLocked(tvEvent: TvEvent?) = (sceneListener as HomeSceneListener).isEventLocked(tvEvent)

                    override fun getCountryPreferences(preference: UtilsInterface.CountryPreference, defaultValue: Any?): Any? {
                        return (sceneListener as HomeSceneListener).getUtilsModule().getCountryPreferences(preference, defaultValue)
                    }

                    override fun getConfigInfo(nameOfInfo: String): Boolean {
                        return (sceneListener as HomeSceneListener).getConfigInfo(nameOfInfo)
                    }

                    override fun getCurrentSubtitleTrack(): ISubtitle? {
                        return (sceneListener as HomeSceneListener).getCurrentSubtitleTrack()
                    }

                    override fun isSubtitleEnabled(): Boolean {
                        return (sceneListener as HomeSceneListener).isSubtitleEnabled()
                    }

                    override fun tuneToFocusedChannel(tvChannel: TvChannel) {
                        return (sceneListener as HomeSceneListener).tuneToFocusedChannel(tvChannel)
                    }

                    override fun getActiveFastChannel(callback: IAsyncDataCallback<TvChannel>) {
                        (sceneListener as HomeSceneListener).getActiveChannel(callback, ApplicationMode.FAST_ONLY)
                    }

                    override fun setActiveWindow(
                        tvChannelList: MutableList<TvChannel>,
                        startTime: Long
                    ) {
                        (sceneListener as HomeSceneListener).setActiveWindow(tvChannelList, startTime)
                    }

                    override fun getStartTimeForActiveWindow(): Long {
                        return (sceneListener as HomeSceneListener).getStartTimeForActiveWindow()
                    }

                    override fun getChannelSourceType(tvChannel: TvChannel): String {
                        return (sceneListener as HomeSceneListener).getChannelSourceType(tvChannel)
                    }

                    override fun isScrambled(): Boolean {
                        return (sceneListener as HomeSceneListener).isScrambled()

                    }
                })
            } else {

                //Create vertical guide scene widget
                guideSceneWidget = VerticalGuideSceneWidget(context, object : GuideSceneWidgetListener {

                    override fun onInitialized(){
                        showEpgData()
                        val areChannelsNonBrowsable = (sceneListener as HomeSceneListener).areChannelsNonBrowsable()
                        guideSceneWidget?.refresh(areChannelsNonBrowsable)
                    }

                    override fun getActiveEpgFilter(): Int {
                        return (sceneListener as HomeSceneListener).getActiveEpgFilter()
                    }

                    override fun isAccessibilityEnabled(): Boolean {
                        return (sceneListener as HomeSceneListener).isAccessibilityEnabled()
                    }

                    override fun changeChannelFromTalkback(tvChannel: TvChannel) {
                        (sceneListener as HomeSceneListener).changeChannelFromTalkback(tvChannel)
                    }

                    override fun fetchEventForChannelList(
                        callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>,
                        channelList: MutableList<TvChannel>,
                        dayOffset: Int,
                        additionalDayOffset: Int,
                        isExtend: Boolean
                    ) {
                        (sceneListener as HomeSceneListener).getEventsByChannelList(callback,channelList,dayOffset,additionalDayOffset,isExtend)
                    }

                    override fun onOptionsClicked() {
                        launchPreferences()
                    }

                    override fun getDateTimeFormat() : DateTimeFormat {
                        return (sceneListener as HomeSceneListener).getDateTimeFormat()
                    }

                    override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
                        return (sceneListener as HomeSceneListener).getParentalRatingDisplayName(parentalRating, tvEvent)
                    }

                    override fun requestFocusOnTopMenu() {
                        homeFilterGridView?.layoutManager?.findViewByPosition(1)?.requestFocus()
                    }

                    override fun setGuideButtonAsActiveFilter() {
                        val holder =
                            homeFilterGridView?.findViewHolderForAdapterPosition(
                                selectedPosition
                            ) as CategoryItemViewHolder

                        homeFilterCategoryAdapter?.setActiveFilter(holder)
                    }

                    override suspend fun getCurrentTime(tvChannel: TvChannel): Long {
                     return withContext(Dispatchers.IO) {
                         return@withContext (sceneListener as HomeSceneListener).getCurrentTime(
                             tvChannel
                         )
                     }
                    }

                    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                        (sceneListener as HomeSceneListener).setSpeechText(text = text, importance = importance)
                    }

                    override fun stopSpeech() {
                        (sceneListener as HomeSceneListener).stopSpeech()
                    }

                    override fun onFilterSelected(filterId: Int) {
                        //TODO optimised epg need to be implement, its a dummy
                        var eventListMap =
                            java.util.LinkedHashMap<Int, MutableList<TvEvent>>()

                        initGuideWidget(filterId, eventListMap)
                    }

                    override fun onCatchUpButtonPressed(tvEvent: TvEvent) {
                        //TODO("Not yet implemented")
                    }

                    override fun onWatchButtonPressed(tvChannel: TvChannel) {
                        var activeChannel: TvChannel? = null
                        (sceneListener as HomeSceneListener).getActiveChannel(object :
                            IAsyncDataCallback<TvChannel> {
                            override fun onFailed(error: kotlin.Error) {
                            }

                            @RequiresApi(Build.VERSION_CODES.R)
                            override fun onReceive(data: TvChannel) {
                                activeChannel = data
                                ReferenceApplication.runOnUiThread(Runnable {
                                    if (!((worldHandler) as ReferenceWorldHandler).isT56() && activeChannel!!.channelId == tvChannel.channelId) {
                                        sceneListener.onBackPressed()
                                        worldHandler!!.triggerAction(
                                            ReferenceWorldHandler.SceneId.ZAP_BANNER,
                                            SceneManager.Action.SHOW_OVERLAY
                                        )
                                    } else {
                                        (sceneListener as HomeSceneListener).playTvChannel(tvChannel)
                                    }
                                })
                            }
                        })
                    }

                    override fun isCurrentEvent(tvEvent: TvEvent): Boolean {
                        return (sceneListener as HomeSceneListener).isCurrentEvent(tvEvent)
                    }

                    override fun getCurrentAudioTrack(): IAudioTrack? {
                        return (sceneListener as HomeSceneListener).getCurrentAudioTrack()
                    }

                    override fun getAvailableAudioTracks(): List<IAudioTrack> {
                        return (sceneListener as HomeSceneListener).getAvailableAudioTracks()
                    }

                    override fun getAvailableSubtitleTracks(): List<ISubtitle> {
                        return (sceneListener as HomeSceneListener).getAvailableSubtitleTracks()
                    }

                    override fun getClosedCaption(): String? {
                        return (sceneListener as HomeSceneListener).getClosedCaption()
                    }

                    override fun onRecordButtonPressed(tvEvent: TvEvent, callback: IAsyncCallback) {
                        (sceneListener as HomeSceneListener).onRecordButtonPressed(tvEvent, callback)
                    }

                    override fun onWatchlistButtonPressed(tvEvent: TvEvent): Boolean {
                        return (sceneListener as HomeSceneListener).onGuideWatchlistClicked(tvEvent)
                    }

                    override fun onFavoriteButtonPressed(
                        tvChannel: TvChannel,
                        favListIds: ArrayList<String>
                    ) {
                        (sceneListener as HomeSceneListener).onGuideFavoriteButtonPressed(
                            tvChannel,
                            favListIds
                        )
                    }

                    override fun onMoreInfoButtonPressed(tvEvent: TvEvent) {
                        //TODO Commented due to pending refactor

                        (sceneListener as HomeSceneListener).showDetails(
                            tvEvent
                        )
                    }

                    override fun getDayWithOffset(
                        additionalDayCount: Int,
                        isExtend: Boolean,
                        channelList: MutableList<TvChannel>
                    ) {
                        getEPGEventsData(
                            channelList,
                            additionalDayCount,
                            isExtend
                        )

                    }

                    override fun onDigitPressed(
                        digit: Int,
                        epgActiveFilter: FilterItemType?,
                        filterMetadata: String?
                    ) {
                        (sceneListener as HomeSceneListener).onDigitPressed(digit, epgActiveFilter,filterMetadata)
                    }

                    override fun setActiveFilterOnGuide() {
                        homeFilterGridView!!.clearFocus()
                        var holder = homeFilterGridView!!.findViewHolderForAdapterPosition(
                            1
                        ) as CategoryItemViewHolder
                        homeFilterCategoryAdapter!!.setActiveFilter(holder)
                    }

                    override fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>) {
                        (sceneListener as HomeSceneListener).getActiveChannel(callback)
                    }

                    override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                        return dispatchKeyEvent(keyCode, event)
                    }

                    override fun clearFocusFromMainMenu() {
                        if (homeFilterGridView?.getChildAt(1)?.isFocused == true) {
                            homeFilterGridView?.getChildAt(1)?.clearFocus()
                            homeFilterCategoryAdapter?.clearPreviousFocus()
                        }
                    }

                    override fun getChannelsOfSelectedFilter(): MutableList<TvChannel> {
                        return (sceneListener as HomeSceneListener).getChannelsOfSelectedFilter()
                    }

                    override fun loadNextChannels(
                        anchorChannel: TvChannel,
                        callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>,
                        currentDayOffset: Int,
                        nextdayoffset: Int,
                        extend: Boolean
                    ) {
                        (sceneListener as HomeSceneListener).loadNextEpgData(
                            anchorChannel,
                            callback,
                            currentDayOffset,
                            nextdayoffset,
                            extend
                        )
                    }

                    override fun loadPreviousChannels(
                        anchorChannel: TvChannel,
                        callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>,
                        currentDayOffset: Int,
                        nextdayoffset: Int,
                        extend: Boolean
                    ) {
                        (sceneListener as HomeSceneListener).loadPreviousChannels(
                            anchorChannel,
                            callback,
                            currentDayOffset,
                            nextdayoffset,
                            extend
                        )
                    }

                    override fun getEventsForChannels(
                        anchorChannel: TvChannel,
                        epgActiveFilter: Int,
                        callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>,
                        currentDayOffset: Int,
                        nextdayoffset: Int,
                        extend: Boolean
                    ) {
                        (sceneListener as HomeSceneListener).getEventsForChannels(
                            anchorChannel,
                            epgActiveFilter,
                            callback,
                            currentDayOffset,
                            nextdayoffset,
                            extend
                        )
                    }

                    override fun getRecordingInProgressTvChannel(): TvChannel? {
                        return (sceneListener as HomeSceneListener).getRecordingInProgressTvChannel()
                    }

                    override fun isInRecordingList(tvEvent: TvEvent): Boolean {
                        return (sceneListener as HomeSceneListener).isInRecordingList(tvEvent)
                    }

                    override fun isInWatchList(tvEvent: TvEvent): Boolean {
                        return (sceneListener as HomeSceneListener).isInWatchList(tvEvent)
                    }

                    override fun getFavoriteCategories(callback: IAsyncDataCallback<ArrayList<String>>) {
                        return (sceneListener as HomeSceneListener).getFavoriteCategories(callback)
                    }

                    override fun getFavoriteSelectedItems(tvChannel: TvChannel): ArrayList<String> {
                        return (sceneListener as HomeSceneListener).getFavoriteSelectedItems(tvChannel)
                    }

                    override fun isRecordingInProgress(): Boolean {
                        return (sceneListener as HomeSceneListener).isRecordingInProgress()
                    }

                    override fun isParentalEnabled(): Boolean {
                        return (sceneListener as HomeSceneListener).isParentalControlsEnabled()
                    }

                    override fun isClosedCaptionEnabled(): Boolean {
                        return (sceneListener as HomeSceneListener).isClosedCaptionEnabled()
                    }

                    override fun isCCTrackAvailable(): Boolean? {
                        return (sceneListener as HomeSceneListener).isCCTrackAvailable()
                    }

                    override fun getIsAudioDescription(type: Int): Boolean {
                        return (sceneListener as HomeSceneListener).getIsAudioDescription(type)
                    }

                    override fun getIsDolby(type: Int): Boolean {
                        return (sceneListener as HomeSceneListener).getIsDolby(type)
                    }

                    override fun isHOH(type: Int): Boolean {
                        return (sceneListener as HomeSceneListener).isHOH(type)
                    }

                    override fun isTeleText(type: Int): Boolean {
                        return (sceneListener as HomeSceneListener).isTeleText(type)
                    }

                    override fun getAudioChannelInfo(type: Int): String {
                        return (sceneListener as HomeSceneListener).getAudioChannelInfo(type)
                    }
                    override fun getAudioFormatInfo(): String {
                        return (sceneListener as HomeSceneListener).getAudioFormatInfo()
                    }
                    override fun getVideoResolution(): String {
                        return (sceneListener as HomeSceneListener).getVideoResolution()
                    }

                    override fun isInFavoriteList(tvChannel: TvChannel): Boolean {
                        return (sceneListener as HomeSceneListener).isInFavoriteList(tvChannel)
                    }

                    override fun isEventLocked(tvEvent: TvEvent?) = (sceneListener as HomeSceneListener).isEventLocked(tvEvent)

                    override fun getCountryPreferences(preference: UtilsInterface.CountryPreference, defaultValue: Any?): Any? {
                        return (sceneListener as HomeSceneListener).getUtilsModule().getCountryPreferences(preference, defaultValue)
                    }

                    override fun getConfigInfo(nameOfInfo: String): Boolean {
                        return (sceneListener as HomeSceneListener).getConfigInfo(nameOfInfo)
                    }
                    override fun getCurrentSubtitleTrack(): ISubtitle? {
                        return (sceneListener as HomeSceneListener).getCurrentSubtitleTrack()
                    }

                    override fun isSubtitleEnabled(): Boolean {
                        return (sceneListener as HomeSceneListener).isSubtitleEnabled()
                    }

                    override fun tuneToFocusedChannel(tvChannel: TvChannel) {
                        return (sceneListener as HomeSceneListener).tuneToFocusedChannel(tvChannel)
                    }

                    override fun getActiveFastChannel(callback: IAsyncDataCallback<TvChannel>) {
                        (sceneListener as HomeSceneListener).getActiveChannel(callback, ApplicationMode.FAST_ONLY)
                    }

                    override fun setActiveWindow(
                        tvChannelList: MutableList<TvChannel>,
                        startTime: Long
                    ) {
                        (sceneListener as HomeSceneListener).setActiveWindow(tvChannelList, startTime)
                    }

                    override fun getStartTimeForActiveWindow(): Long {
                        return (sceneListener as HomeSceneListener).getStartTimeForActiveWindow()
                    }

                    override fun getChannelSourceType(tvChannel: TvChannel): String {
                        return (sceneListener as HomeSceneListener).getChannelSourceType(tvChannel)!!
                    }

                    override fun isScrambled(): Boolean {
                        return (sceneListener as HomeSceneListener).isScrambled()
                    }
                })
            }
        }

    }

    /**
     * Channel changed
     * @return is channel changed handled by the active scene widget
     */
    open fun channelChanged(): Boolean {
        return true
    }
    //used when channel is zapped from epg using number keys to show focus not actual zap
    open fun zapOnGuideOnly(channel: TvChannel){
        if ( guideSceneWidget!=null){
            guideSceneWidget?.zapOnGuideOnly(channel)
        }
    }
    private fun createRecordingsSceneWidget(requestFocus: Boolean) {
        //Create recordings widget
        recordingsWidget = ReferenceWidgetRecordings(context,
            object : ReferenceWidgetRecordings.RecordingsWidgetListener {
                override fun getCurrentTime(tvChannel: TvChannel): Long {
                    return (sceneListener as HomeSceneListener).getCurrentTime(tvChannel)
                }

                override fun getDateTimeFormat(): DateTimeFormat {
                    return (sceneListener as HomeSceneListener).getDateTimeFormat()
                }

                override fun isInRecList(tvEvent: TvEvent): Boolean {
                    return (sceneListener as HomeSceneListener).isInRecordingList(tvEvent)
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    (sceneListener as HomeSceneListener).setSpeechText(text = text, importance = importance)
                }

                override fun getRails(sceneConfig: SceneConfig?) {
                    (sceneListener as HomeSceneListener).getRecordingsCategories(
                        sceneConfig,
                        object : AsyncDataReceiver<MutableList<RailItem>> {
                            override fun onFailed(error: Error?) {
                                runOnUiThread {
                                    if (recordingsWidget != null) {
                                        recordingsWidget!!.refresh(mutableListOf<RailItem>())
                                    }
                                }
                            }

                            override fun onReceive(data: MutableList<RailItem>) {
                                runOnUiThread {
                                    if (recordingsWidget != null) {
                                        if (data == null || data.size == 0) {
                                            recordingsWidget!!.refresh(mutableListOf<RailItem>())
                                        } else {
                                            recordingsWidget!!.refresh(data)
                                        }
                                    }
                                }
                            }
                        })
                }

                override fun requestFocusOnTopCategories() {
                    homeFilterGridView?.requestFocus()
                }

                override fun onItemClick(recording: Any) {
                    (sceneListener as HomeSceneListener).onRecordingClick(recording)
                }

                override fun keepFocusOnFilerView() {
                    homeFilterCategoryAdapter!!.requestFocus(3)
                }

                override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                    return dispatchKeyEvent(keyCode, event)
                }

                override fun onCustomRecordingButtonClicked() {
                    (sceneListener as HomeSceneListener).customRecButtonClciked()
                }

                override fun requestFocusForButton() {

                }

                override fun getRailSize(): Int {
                    return (sceneListener as HomeSceneListener).getRailSize()
                }

                override fun isChannelLocked(channelId: Int): Boolean {
                    return (sceneListener as HomeSceneListener).isChannelLocked(channelId)
                }

                override fun isParentalControlsEnabled(): Boolean {
                    return (sceneListener as HomeSceneListener).isParentalControlsEnabled()
                }

                override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
                    return (sceneListener as HomeSceneListener).getParentalRatingDisplayName(parentalRating, tvEvent)
                }

                override fun isEventLocked(tvEvent: TvEvent?) = (sceneListener as HomeSceneListener).isEventLocked(tvEvent)
            }
        )
    }

    private fun initGuideWidget(
        filter: Int,
        eventListMap: LinkedHashMap<Int, MutableList<TvEvent>>
    ) {
        //Set channel list
        mSelectedFilter = filter
        val channelList = mutableListOf<TvChannel>()
        for (value in eventListMap.values) channelList.add(value[0].tvChannel)
        updateTvGuide(eventListMap, channelList, 0, false)
    }

    private fun getEPGEventsData(
        guideChannelList: MutableList<TvChannel>,
        additionalDayCount: Int,
        isExtend: Boolean
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var map =
                LinkedHashMap<Int, MutableList<TvEvent>>()
            // Fetch epd data
            var counter = AtomicInteger(0)
            for (index in 0 until guideChannelList.size) {
                var tvChannel = guideChannelList[index]
                (sceneListener as HomeSceneListener).getGuideEventsForTvChannel(
                    additionalDayCount,
                    tvChannel,
                    object :
                        IAsyncDataCallback<MutableList<TvEvent>> {

                        override fun onReceive(eventList: MutableList<TvEvent>) {
                            if (eventList.isNotEmpty()) {
                                map[tvChannel.id] = eventList
                            } else {

                                map[tvChannel.id] = mutableListOf(
                                    TvEvent.createNoInformationEvent(
                                        tvChannel,
                                        (sceneListener as HomeSceneListener).getCurrentTime(tvChannel)
                                    )
                                )
                            }
                            if (counter.incrementAndGet() == guideChannelList.size) {
                                var channelEventMap =
                                    java.util.LinkedHashMap<Int, MutableList<TvEvent>>()
                                for (index in 0 until guideChannelList.size) {
                                    var tvChannel = guideChannelList[index]
                                    if (map.keys.contains(tvChannel.id)) {
                                        channelEventMap[index] = map[tvChannel.id]!!
                                    }
                                }
                                CoroutineScope(Dispatchers.Main).launch {
                                    updateTvGuide(
                                        channelEventMap,
                                        guideChannelList, additionalDayCount, isExtend
                                    )
                                }

                            }
                        }

                        override fun onFailed(error: kotlin.Error) {
                            if (counter.incrementAndGet() == guideChannelList.size) {
                                var channelEventMap =
                                    java.util.LinkedHashMap<Int, MutableList<TvEvent>>()
                                for (index in 0 until guideChannelList.size) {
                                    var tvChannel = guideChannelList[index]
                                    if (map.keys.contains(tvChannel.id)) {
                                        channelEventMap[index] = map[tvChannel.id]!!
                                    }
                                }
                                CoroutineScope(Dispatchers.Main).launch {
                                    updateTvGuide(
                                        channelEventMap,
                                        guideChannelList, additionalDayCount, isExtend
                                    )
                                }

                            }
                        }
                    }
                )
            }
        }

    }

    private fun updateTvGuide(
        channelEventsMap: LinkedHashMap<Int, MutableList<TvEvent>>,
        guideChannelList: MutableList<TvChannel>,
        additionalDayCount: Int,
        isExtend: Boolean
    ) {
        runOnUiThread(Runnable {
            if (guideSceneWidget != null) {
                if (!isExtend) {
                    //Set current date
                    guideSceneWidget?.refresh(date!!)

                    var channelList: ArrayList<TvChannel> = arrayListOf()
                    guideChannelList.forEach { channel ->
                        if(channel.isBrowsable || channel.inputId.contains("iwedia") || channel.inputId.contains("sampletvinput")){
                            channelList.add(channel)
                        }
                    }
                    //Set channel list
                    guideSceneWidget?.refresh(channelList)

                    //Set event list
                    guideSceneWidget?.refresh(channelEventsMap)
                } else {
                    //extend timeline , merge the previous data with the upcoming data
                    guideSceneWidget?.extendTimeline(channelEventsMap, additionalDayCount)
                }
            }
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any?) {
        super.refresh(data)
        if (data is Long) {
            val simpleDateFormat =
                SimpleDateFormat("HH:mm", Locale("en"))

            timeTv?.time = simpleDateFormat.format(Date(data.toLong()))
            date = Date(data.toLong())
            // Refresh guide scene widget date
            if (guideSceneWidget != null) {
                guideSceneWidget!!.refresh(date!!)
            } else if (forYouWidget != null) {
                //if (activeWidget != 0) refreshForYou()
            }
        } else if (data is Int) {
            initialFilterPosition = data
        }
    }

    /**
     * Init search/settings button
     */
    open fun initButton(
        selector: View?,
        btnView: ImageView?
    ) {
        btnView?.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    selector?.background = ConfigColorManager.generateButtonBackground()
                    btnView!!.setColorFilter(
                        Color.parseColor(
                            ConfigColorManager.getColor(
                                "color_background"
                            )
                        )
                    )
                } else {
                    selector!!.setBackgroundResource(0)
                    btnView!!.setColorFilter(
                        Color.parseColor(
                            ConfigColorManager.getColor(
                                "color_main_text"
                            )
                        )
                    )
                }
            }

        btnView?.setOnKeyListener(object : View.OnKeyListener {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onKey(
                view: View?,
                keycode: Int,
                keyEvent: KeyEvent?
            ): Boolean {

                try {
                    if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                        if (keycode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                if (btnView.tag == 1) {
                                    homeFilterGridView?.layoutManager?.findViewByPosition(0)
                                        ?.requestFocus()
                                    return true
                                }
                            } else {
                                if (btnView.tag == 2) {
                                    var position = homeFilterGridView?.adapter?.itemCount
                                    if (position != null) {
                                        homeFilterGridView?.layoutManager?.findViewByPosition(
                                            position - 1
                                        )
                                            ?.requestFocus()
                                    }
                                    return true
                                }
                            }
                        }
                        if (keycode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                if (btnView.tag == 2) {
                                    var position = homeFilterGridView?.adapter?.itemCount
                                    if (position != null) {
                                        homeFilterGridView?.layoutManager?.findViewByPosition(
                                            position - 1
                                        )
                                            ?.requestFocus()
                                    }
                                    return true
                                }
                            } else {
                                if (btnView.tag == 1) {
                                    homeFilterGridView?.layoutManager?.findViewByPosition(0)
                                        ?.requestFocus()
                                    return true
                                }
                            }
                        }
                        if (keycode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            if (activeWidget == 0) {
                                forYouWidget!!.selectedFilterList()
                                return true
                            }
                            if (activeWidget == 1) {
                                btnView.clearFocus()
                                guideSceneWidget!!.selectedFilterList(
                                    btnView.tag == 1,
                                    btnView.tag == 2
                                )
                                return true

                            }
                            if (activeWidget == 2) {
                                btnView.clearFocus()
                                favouritesSceneWidget!!.selectedFilterList()
                                return true

                            }
                            if (activeWidget == 3) {
                                settingsIv!!.visibility = View.INVISIBLE
                                searchIv!!.visibility = View.INVISIBLE

                                btnView.clearFocus()

                                upArrow!!.alpha = 0f

                                val translate: Animation = TranslateAnimation(
                                    0F,
                                    0F,
                                    0F,
                                    -Utils.getDimens(R.dimen.custom_dim_61)
                                )
                                translate.duration = animationDuration

                                val alpha: Animation = AlphaAnimation(1.0f, 0.0f)
                                alpha.duration = animationDuration

                                val arrowAnimation: Animation = AlphaAnimation(0.0f, 0.7f)
                                alpha.fillAfter = true

                                val animationSet = AnimationSet(true)
                                animationSet.addAnimation(translate)
                                animationSet.addAnimation(alpha)
                                animationSet.fillAfter = true

                                animationSet.setAnimationListener(object : AnimationListener {
                                    override fun onAnimationStart(animation: Animation?) {}
                                    override fun onAnimationEnd(animation: Animation?) {
                                        homeFilterGridView!!.visibility = View.GONE
                                    }

                                    override fun onAnimationRepeat(animation: Animation?) {}
                                })

                                arrowAnimation.setAnimationListener(object : AnimationListener {
                                    override fun onAnimationStart(animation: Animation?) {}
                                    override fun onAnimationEnd(animation: Animation?) {
                                        upArrow!!.alpha = 0.7f
                                        upArrow!!.visibility = View.VISIBLE
                                    }

                                    override fun onAnimationRepeat(animation: Animation?) {
                                    }
                                })

                                upArrow!!.startAnimation(arrowAnimation)
                                homeFilterGridView!!.startAnimation(animationSet)
                                preferencesSceneWidget!!.requestFocus()

                                return true

                            }

                        }


                        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER || keycode == KeyEvent.KEYCODE_ENTER) {
                            //this method is called to restart inactivity timer for no signal power off
                            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                            if (btnView.tag == 1) {
                                Utils.viewClickAnimation(
                                    searchIconBg,
                                    object : com.iwedia.cltv.utils.AnimationListener {
                                        override fun onAnimationEnd() {
                                            (sceneListener as HomeSceneListener).onSearchClicked()
                                        }

                                    })
                            }

                            if (btnView.tag == 2) {
                                Utils.viewClickAnimation(
                                    settingsIconBg,
                                    object : com.iwedia.cltv.utils.AnimationListener {
                                        override fun onAnimationEnd() {
                                            val intent = Intent(Settings.ACTION_SETTINGS)
                                            (ReferenceApplication.getActivity() as MainActivity).settingsIconClicked = true
                                            intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
                                            ReferenceApplication.applicationContext()
                                                .startActivity(intent)
                                        }

                                    })

                            }
                        }
                    }
                } catch (E: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onKey: ${E.printStackTrace()}")
                }

                try {
                    if (keyEvent?.action == KeyEvent.ACTION_UP && (keycode == KeyEvent.KEYCODE_BACK || keycode == KeyEvent.KEYCODE_ESCAPE)) {
                        return false
                    }
                } catch (E: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onKey: ${E.printStackTrace()}")
                }


                return true

            }

        })
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) &&
            ((homeContainer != null) && (homeContainer?.hasFocus()!!))
        ) {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
                if (!ReferenceApplication.downActionBackKeyDone) return true
                homeFilterGridView?.requestFocus()
                return true
            }
            return true
        }

        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) &&
            (settingsIv!!.hasFocus() || searchIv!!.hasFocus())
        ) {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
                if (!ReferenceApplication.downActionBackKeyDone) return true
                (sceneListener as HomeSceneListener).onBackPressed()
                return true
            }
        }

        //handle back from calendar button
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) && (guideSceneWidget != null)) {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
                if (!ReferenceApplication.downActionBackKeyDone) return true
                homeFilterGridView?.requestFocus()
                return true
            }
        }

        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9 -> {
                    val digit = keyCode - KeyEvent.KEYCODE_0
                    if (activeWidget == 1) {
                        handleZap(digit)
                        return true
                    }
                }
                KeyEvent.KEYCODE_MENU -> ReferenceApplication.downActionBackKeyDone = true
            }
        }
        if(keyCode == KeyEvent.KEYCODE_MENU && (keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
            launchPreferences()
            return true
        }
        val dispatchResult = super.dispatchKeyEvent(keyCode, keyEvent)
        if ((keyEvent).action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_BACK){
                ReferenceApplication.downActionBackKeyDone = false
            }
        }
        return dispatchResult
    }

    private fun launchPreferences() {
        if (settingsIv?.hasFocus() == false) {
            homeFilterCategoryAdapter?.selectedItem?.let {
                homeFilterCategoryAdapter?.clearFocus(
                    it
                )
            }
            settingsIv?.requestFocus()
            if (settingsIconBg != null)
                Utils.focusAnimation(settingsIconBg!!)
        }
    }

    private fun handleZap(digit: Int) {
        saveFocus(true)
        (sceneListener as HomeSceneListener).onDigitPressed(digit)
        saveFocus(false)
    }

    /**
     * Save focus inside the scene
     */
    private fun saveFocus(saveFocus: Boolean) {
        homeFilterCategoryAdapter!!.keepFocus = saveFocus
        if (favouritesSceneWidget != null) {
            favouritesSceneWidget!!.saveFocus(saveFocus)
        }
    }

    /**
     * Refresh guide details favorite button
     */
    open fun refreshGuideDetailsFavButton() {
        if (guideSceneWidget != null) {
            runOnUiThread(Runnable {
                guideSceneWidget?.refreshFavoriteButton()
            })
        }
    }

    /**
     * Refresh guide filter list
     *
     * @param filterList a new filter list items
     */
    open fun refreshGuideFilterList(filterList: MutableList<CategoryItem>) {
        if (guideSceneWidget != null && !searchIv!!.hasFocus() && !settingsIv!!.hasFocus()) {
            runOnUiThread(Runnable {
                guideSceneWidget?.refreshFilterList(filterList)
            })
        }
    }

    open fun refreshGuideOnUpdateFavorite() {

        if (guideSceneWidget != null && !searchIv!!.hasFocus() && !settingsIv!!.hasFocus()) {
            runOnUiThread(Runnable {
                guideSceneWidget?.refreshGuideOnUpdateFavorite()
            })
        }
    }
    open fun updateFavoritesData(favorites : ArrayList<String>, filters : ArrayList<CategoryItem>){
        guideSceneWidget?.updateFavSelectedItem(favorites,filters)
    }


    open fun showWidgetByPosition(position: Int) {
        Handler().postDelayed({
            homeFilterGridView!!.getChildAt(position)!!.callOnClick()
        }, 500)
    }

    //refresh recordings list
    open fun refreshRecordingsList(data: ArrayList<ScheduledRecording>?) {
        if (recordingsWidget != null) {
            recordingsWidget!!.refresh(data!!)
        }
    }

    open fun getActiveCategory(): Int {
        return activeWidget
    }

    private fun isUsbConnected(): Boolean {
        var usbManager = ReferenceApplication.applicationContext().getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.size > 0
    }

    private fun guideVerticalOrientation(): Boolean =
        (sceneListener as HomeSceneListener).getConfigInfo("vertical_epg")

    open fun getActiveCategoryId(): Int {
        return if (homeFilterCategoryAdapter != null && homeFilterCategoryAdapter!!.selectedItem != -1) {
            list[homeFilterCategoryAdapter!!.selectedItem].id
        } else {
            //special case when fast clicking guide button
            1
        }
    }

    var epgScrollHandler = Handler()
    /**
     * used for auto scrolling to the right after every 30 mins,
     * usefull in cases when user left epg open for long time
     */
    private fun updateScrollingForGuide(){
        guideScrollUpdateTimer?.cancel()
        guideScrollUpdateTimer = object :
            CountDownTimer(
                1800000,
                60000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                if (selectedPosition == 2){
                    epgScrollHandler.removeCallbacksAndMessages(null)
                    epgScrollHandler.run {
                        handleAutoScrollingToRight()
                    }
                }
                updateScrollingForGuide()
            }
        }
        guideScrollUpdateTimer?.start()
    }

    /**
     * this is used to handle automatic scrolling after a certain time.
     */
    fun handleAutoScrollingToRight() {
        if (ReferenceApplication.isInForeground) {
            guideSceneWidget?.let {
                //if current scene is not homeScene then we are waiting and checking after every 500ms...
                // for ex details scene might be open so we are waiting for it to be closed and then scroll
                if (it.isLoading || worldHandler?.active?.id != ReferenceWorldHandler.SceneId.HOME_SCENE) {

                    epgScrollHandler.removeCallbacksAndMessages(null)
                    // Adding delay to avoid overlap events
                    epgScrollHandler.postDelayed({
                        handleAutoScrollingToRight()
                    }, 500)

                } else {
                    guideSceneWidget?.scrollRight()
                }
            }
        }
    }

    open fun refreshClosedCaption() {
        guideSceneWidget?.let {
            runOnUiThread(Runnable {
                guideSceneWidget?.refreshClosedCaption()
            })
        }
    }

    /**
     * Checks is the search button focused and broadcast tab shown
     * In this case on guide button press focus should be moved on the current event inside the broadcast tab
     *
     * @return true if the focus is moved on the current event false if it is not
     */
    @RequiresApi(Build.VERSION_CODES.R)
    open fun checkSearchFocusOnGuideKeyPress(): Boolean {
        if (searchIv?.hasFocus() == true &&
            activeWidget == 2) {
            if (guideSceneWidget is HorizontalGuideSceneWidget) {
                (guideSceneWidget as HorizontalGuideSceneWidget).setFocusOnEvent()
            }
            return true
        }
        return false
    }

    /**
     * Checks is settings or search button focused
     *
     * @return true if the search or settings button focused
     */
    open fun isBtnFocused():Boolean = settingsIv?.hasFocus() == true || searchIv?.hasFocus() == true
}