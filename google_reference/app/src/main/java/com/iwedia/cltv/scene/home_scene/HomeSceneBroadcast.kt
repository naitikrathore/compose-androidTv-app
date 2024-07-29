package com.iwedia.cltv.scene.home_scene

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.usb.UsbManager
import android.media.tv.ContentRatingSystem
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.contains
import androidx.leanback.widget.HorizontalGridView
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TimeTextView
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.anoki_fast.epg.BackFromPlayback
import com.iwedia.cltv.anoki_fast.epg.FastLiveTabDataProvider
import com.iwedia.cltv.components.CategoryAdapter
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.components.CategoryItemViewHolder
import com.iwedia.cltv.components.ReferenceWidgetForYou
import com.iwedia.cltv.components.ReferenceWidgetPreferences
import com.iwedia.cltv.components.ReferenceWidgetPreferences.DeepLink
import com.iwedia.cltv.components.ReferenceWidgetPreferences.PreferencesWidgetListener
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.PromotionItem
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.language.LanguageCode
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.home_scene.guide.GuideSceneWidget
import com.iwedia.cltv.scene.home_scene.guide.HorizontalGuideSceneWidget
import com.iwedia.cltv.utils.LoadingPlaceholder
import com.iwedia.cltv.utils.PlaceholderName
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import utils.information_bus.Event
import world.SceneListener
import world.SceneManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class HomeSceneBroadcast(context: Context, sceneListener: SceneListener) : HomeSceneBase(
    context,
    sceneListener
) {
    private val TAG = javaClass.simpleName

    /**
     * Xml views
     */
    override var timeTv: TimeTextView? = null
    override var homeContainer: ConstraintLayout? = null
    override var sceneNameTv: TextView? = null
    override var homeFilterGridView: HorizontalGridView? = null
    override var searchIv: ImageView? = null
    override var searchIconBg: View? = null
    override var settingsIv: ImageView? = null
    override var settingsIconBg: View? = null
    private var profileIv: ImageView? = null
    private var profileIconBg: View? = null
    private var appIconIv: ImageView? = null
    override var preferenceFirstTime = true
    override val animationDuration: Long = 700
    //Current time
    private var date: Date? = null

    override val list = mutableListOf<CategoryItem>()

    //Live tab update timer
    private var liveTabUpdateTimer: CountDownTimer?= null
    private var showLiveTab : Boolean = true
    private var settingsOpened = false

    /**
     * Home scene widgets
     */
    override var homeFilterCategoryAdapter: CategoryAdapter? = null
    override var forYouWidget: ReferenceWidgetForYou? = null
    override var guideSceneWidget: GuideSceneWidget? = null
    override var preferencesSceneWidget: ReferenceWidgetPreferences? = null
    private var upArrow: ImageView? = null

    /**
     * Initial filter position
     */
    override var initialFilterPosition = 0
    override var selectedPosition = 0

    private var focusedButtonId = -1

    /**
     * TV guide selected filter
     */
    private var mSelectedFilter = 0
    override var mainConstraintLayout: ConstraintLayout? = null
    var activeChannel: TvChannel? = null

    // epg refresh handlers
    private var epgEventsRefreshHandler = Handler()
    private var epgChannelsRefreshHandler = Handler()

    companion object {
        //when guide button pressed helps in jumping directly to the current event
        var jumpGuideCurrentEvent = false
        var jumpLiveTabCurrentEvent = false
    }

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(name, R.layout.layout_home_scene_fast, object :
            GAndroidSceneFragmentListener {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onCreated() {
                try {
                    mainConstraintLayout =
                        view!!.findViewById<ConstraintLayout>(R.id.main_layout_id)
                    upArrow = view!!.findViewById(R.id.arrow_up)
                } catch (e: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: ${e.printStackTrace()}")
                    ReferenceApplication.worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
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
                upArrow?.setImageDrawable(selectorDrawable)

                val gradientBackground: View = view!!.findViewById(R.id.gradient_background)
                Utils.makeGradient(
                    gradientBackground,
                    GradientDrawable.LINEAR_GRADIENT,
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    Color.parseColor("#293241"),
                    Color.parseColor("#293241"),
                    Color.parseColor("#00293241"),
                    0.0F,
                    0.38F
                )

                timeTv = view!!.findViewById(R.id.home_tv_time)
                timeTv?.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                val applicationMode = if(ReferenceApplication.worldHandler!!.getApplicationMode() == ApplicationMode.DEFAULT.ordinal)  ApplicationMode.DEFAULT else  ApplicationMode.FAST_ONLY

                (sceneListener as HomeSceneListener).getActiveChannel(object :
                    IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: kotlin.Error) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
                    }

                    override fun onReceive(data: TvChannel) {
                        activeChannel = data
                    }

                }, applicationMode)

                var activeChannelBroadcast : TvChannel? = null
                (sceneListener as HomeSceneListener).getActiveChannel(object :
                    IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: kotlin.Error) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
                    }

                    override fun onReceive(data: TvChannel) {
                        activeChannelBroadcast = data
                    }

                })

                refresh(activeChannelBroadcast?:activeChannel?.let { (sceneListener as HomeSceneListener).getCurrentTime(it) })
                homeContainer = view!!.findViewById(R.id.home_container)
                sceneNameTv = view!!.findViewById(R.id.home_scene_name)
                timeTv?.typeface = TypeFaceProvider.getTypeFace(
                    ReferenceApplication.applicationContext(),
                    ConfigFontManager.getFont("font_regular")
                )

                //App icon
                appIconIv = view!!.findViewById(R.id.app_icon_image)

                //Search button
                searchIconBg = view!!.findViewById(R.id.home_search_icon_selector)
                searchIv = view!!.findViewById(R.id.home_search_icon)
                searchIv?.tag = 1

                searchIv?.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.search_icon
                    )
                )
                try {
                    val color_context =
                        Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                    searchIv?.getDrawable()
                        ?.setTint(color_context)// = ColorStateList.valueOf(color_context)
                } catch (E: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: E search $E")
                }

                initButton(
                    searchIconBg,
                    searchIv
                )

                //Settings button
                settingsIconBg = view!!.findViewById(R.id.home_settings_icon_selector)
                settingsIv = view!!.findViewById(R.id.home_settings_icon)
                settingsIv?.tag = 2

                settingsIv?.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.settings_icon
                    )
                )
                settingsIv?.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                initButton(
                    settingsIconBg,
                    settingsIv
                )

                if ((sceneListener as HomeSceneListener).isProfileButtonEnabled()){
                    //Profile button
                    profileIconBg = view!!.findViewById(R.id.home_profile_btn_selector)
                    profileIv = view!!.findViewById(R.id.home_profile_btn_icon)
                    profileIv?.tag = 3

                    profileIv?.setImageDrawable(
                        ContextCompat.getDrawable(
                            context!!,
                            R.drawable.profile_btn_icon
                        )
                    )
                    profileIv?.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                    initButton(
                        profileIconBg,
                        profileIv
                    )
                } else {
                    view!!.findViewById<ConstraintLayout>(R.id.settings_button_container).apply {
                        (layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, Utils.convertDpToPixel(20.0).toInt(), 0)
                    }
                    view!!.findViewById<ConstraintLayout>(R.id.profile_btn_container).apply{
                        visibility = View.GONE
                    }
                    setProfileIconVisibility(false)
                }
                if (!(sceneListener as HomeSceneListener).kidsModeEnabled()) {
                    setSearchIconVisibility(false)
                }

                homeFilterGridView =
                    view!!.findViewById(R.id.home_filter_list_view)
                homeFilterCategoryAdapter = CategoryAdapter()
                homeFilterCategoryAdapter?.isCategoryFocus = true

                list.add(CategoryItem(0, ConfigStringsManager.getStringById("discover")))
                list.add(CategoryItem(1, ConfigStringsManager.getStringById("broadcast")))

                homeFilterCategoryAdapter?.selectedItem = -1
                homeFilterCategoryAdapter?.refresh(list)
                homeFilterGridView?.setNumRows(1)
                homeFilterGridView?.adapter = homeFilterCategoryAdapter
                homeFilterCategoryAdapter?.shoudKeepFocusOnClick = true
                homeFilterCategoryAdapter?.adapterListener =
                    object : CategoryAdapter.ChannelListCategoryAdapterListener {
                        override fun onItemClicked(position: Int) {
                            //prevent focus to go down
                            if (position == 1 && guideSceneWidget != null && guideSceneWidget?.isLoading == true) {
                                return
                            }
                            if (categoryHasData(position)) {
                                val holder =
                                    homeFilterGridView?.findViewHolderForAdapterPosition(
                                        position
                                    ) as CategoryItemViewHolder
                                homeFilterCategoryAdapter?.setActiveFilter(holder)
                                handleCategoryClicked(position)
                            }
                        }

                        override fun getAdapterPosition(position: Int) {
                            setSearchIconVisibility((sceneListener as HomeSceneListener).kidsModeEnabled())
                            setSettingsIconVisibility(true)
                            setProfileIconVisibility(true)
                            setAppIconVisibility(true)
                            homeFilterCategoryAdapter?.clearPreviousFocus()
                        }

                        override fun onItemSelected(position: Int) {
                            activeWidget = position
                            focusedButtonId = -1
                            if (view != null) {
                                if (settingsIv != null && homeFilterGridView!!.hasFocus() || searchIv!!.hasFocus()) {
                                    try {
                                        handleCategoryFocusedWithCaching(position)
                                    }catch (E: Exception){
                                        println(E)
                                        ReferenceApplication.worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                                    }
                                } else {
                                    homeFilterCategoryAdapter?.setSelected(position)
                                }
                            }
                            checkIsLockedScreenShown()
                        }

                        override fun digitPressed(digit: Int) {
                            if (selectedPosition ==1 ) guideSceneWidget?.onDigitPress(digit)
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            (sceneListener as HomeSceneListener).setSpeechText(text = text, importance = importance)
                        }

                        override fun onKeyDown(currentPosition: Int): Boolean {
                            if (guideScanWidget!=null && currentPosition == 1){
                                if (categoryHasData(currentPosition)) {
                                    val holder =
                                        homeFilterGridView?.findViewHolderForAdapterPosition(
                                            currentPosition
                                        ) as CategoryItemViewHolder

                                    homeFilterCategoryAdapter?.setActiveFilter(holder)
                                    handleCategoryClicked(currentPosition)
                                }
                                return true
                            }

                            //prevent focus to go down
                            if (currentPosition == 1 && guideSceneWidget != null && guideSceneWidget!!.isLoading) {
                                return true
                            }

                            if (categoryHasData(currentPosition)) {
                                val holder =
                                    homeFilterGridView?.findViewHolderForAdapterPosition(
                                        currentPosition
                                    ) as CategoryItemViewHolder

                                homeFilterCategoryAdapter?.setActiveFilter(holder)
                                handleCategoryClicked(currentPosition)
                            }
                            return true
                        }

                        override fun onKeyUp(currentPosition: Int): Boolean {
                            return false
                        }

                        override fun onKeyLeft(currentPosition: Int): Boolean {
                            if (currentPosition > 0) {
                                return false
                            }
                            return true
                        }

                        override fun onKeyRight(currentPosition: Int): Boolean {
                            if ((sceneListener as HomeSceneListener).kidsModeEnabled()) {
                                if (currentPosition == homeFilterCategoryAdapter!!.itemCount - 1) {
                                    homeFilterCategoryAdapter?.clearPreviousFocus()
                                    setActiveFilterOnCurrentActiveItem()
                                    if (settingsIv?.hasFocus() == false) {
                                        searchIv?.requestFocus()
                                        handleCategoryFocusedWithCaching(1)
                                        if (searchIconBg != null)
                                            Utils.focusAnimation(searchIconBg!!)
                                    }
                                    return true
                                }
                            } else {
                                if (currentPosition == homeFilterCategoryAdapter!!.itemCount - 1) {
                                    homeFilterCategoryAdapter?.clearPreviousFocus()
                                    setActiveFilterOnCurrentActiveItem()
                                    if (settingsIv?.hasFocus() == false) {
                                        settingsIv!!.requestFocus()
                                        settingsOpened = false
                                        showPreferences()
                                    }
                                    return true
                                }
                            }
                            return false
                        }

                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onBackPressed(position: Int): Boolean {
                            if (ReferenceApplication.isInitalized) {
                                if ((sceneListener as HomeSceneListener).isRecordingInProgress()
                                ) {

                                    utils.information_bus.InformationBus.submitEvent(
                                        Event(
                                            Events.SHOW_PVR_BANNER, activeChannel
                                        )
                                    )

                                    ReferenceApplication.worldHandler?.triggerAction(
                                        ReferenceWorldHandler.SceneId.HOME_SCENE,
                                        SceneManager.Action.DESTROY
                                    )
                                } else {
                                    if (activeWidget == 0 && settingsIv?.hasFocus() == false && searchIv?.hasFocus() == false) {
                                        (sceneListener as HomeSceneListener).showExitDialog()
                                    } else {
                                        handleCategoryFocusedWithCaching(0)
                                    }
                                    return true
                                }
                            } else {
                                ReferenceApplication.worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                            }
                            return true
                        }
                    }
                sceneListener.onSceneInitialized()
            }
        })
    }

    fun isSettingsSearchButtonFocused():Boolean {
        return focusedButtonId != -1
    }

    override fun onResume() {
        super.onResume()
        if (guideSceneWidget != null && selectedPosition == 1){
            guideSceneWidget?.resume()
        }
        if ((selectedPosition == 0) ) {
            if (homeFilterGridView?.hasFocus() == true) {
                homeFilterGridView?.requestFocus()
            } else {
                forYouWidget?.verticalGridView?.requestFocus()
            }
        }
        checkIsLockedScreenShown()
    }

    private fun setSettingsIconVisibility(shouldBeVisible: Boolean) {
        settingsIv?.visibility = if (shouldBeVisible) View.VISIBLE else View.INVISIBLE
        settingsIconBg?.visibility = if (shouldBeVisible) View.VISIBLE else View.INVISIBLE
    }

    private fun setProfileIconVisibility(shouldBeVisible: Boolean) {
        profileIv?.visibility = if (shouldBeVisible) View.VISIBLE else View.INVISIBLE
        profileIconBg?.visibility = if (shouldBeVisible) View.VISIBLE else View.INVISIBLE
    }

    private fun setAppIconVisibility(shouldBeVisible: Boolean) {
        appIconIv?.visibility = if (shouldBeVisible) View.VISIBLE else View.INVISIBLE
    }

    private fun setSearchIconVisibility(shouldBeVisible: Boolean) {
        searchIv?.visibility = if (shouldBeVisible) View.VISIBLE else View.INVISIBLE
        searchIconBg?.visibility = if (shouldBeVisible) View.VISIBLE else View.INVISIBLE
    }

    /**
     * Disables preview mode if the parental or channel lock screen is shown
     */
    private fun checkIsLockedScreenShown() {
        if ((sceneListener as HomeSceneListener).isLockedScreenShown()) {
            view!!.findViewById<View>(R.id.bg_preview_fade_home_scene).apply {
                setBackgroundColor(
                    Color.parseColor(
                        ConfigColorManager.getColor(
                            "color_background"
                        )
                    )
                )
            }
        }else{
            //when channel is unlocked black background should be removed and respective drawable should be applied.
            var drawable = ContextCompat.getDrawable(context, R.drawable.bg_fade_preview)
            view!!.findViewById<View>(R.id.bg_preview_fade_home_scene).apply {
                background = drawable
            }
        }
    }

    private fun setActiveFilterOnCurrentActiveItem() {
        val position = homeFilterCategoryAdapter?.focusPosition
        if (position != -1) {
            val holder =
                homeFilterGridView?.findViewHolderForAdapterPosition(
                    position!!
                ) as CategoryItemViewHolder
            homeFilterCategoryAdapter?.setPreviousFocus(holder)
        }
    }

    private fun categoryHasData(position: Int): Boolean {
        when (position) {
            0 -> {
                return (forYouWidget != null && forYouWidget?.itemsForAdapter!!.isNotEmpty())
            }

            1 -> {
                return guideSceneWidget != null || guideScanWidget != null
            }

            2 -> {
                return (preferencesSceneWidget != null)
            }
        }
        return true
    }

    private fun isCategoryCreated(position: Int): Boolean {
        when (position) {
            0 -> {
                return forYouWidget != null
            }

            1 -> {
                return guideSceneWidget != null
            }

            2 -> {
                return (preferencesSceneWidget != null)
            }
        }
        return true
    }

    private var epgRefreshHandler = Handler()

    override fun refreshEpgChannelsData(isBroadcastChannelUpdated: Boolean?) {
        //refreshing the broadcast epg.
        if (guideSceneWidget!=null && isBroadcastChannelUpdated == true){
            ReferenceApplication.runOnUiThread {
                if (selectedPosition == 1) {
                    homeFilterGridView?.layoutManager?.findViewByPosition(1)?.requestFocus()
                    homeContainer?.removeView(guideSceneWidget?.view)
                    super.createGuideSceneWidget()
                    homeContainer?.addView(guideSceneWidget?.view)
                }
            }
        }
    }
    override fun refreshEpgData() {

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "refreshEpgData: Refreshing EPG.....")
        guideSceneWidget?.let {
            if (ReferenceApplication.isInForeground) {
                if(it.isLoading ){
                    epgRefreshHandler.removeCallbacksAndMessages(null)
                    // Adding delay to avoid overlap events
                    epgRefreshHandler.postDelayed({
                        refreshEpgData()
                    },500)
                }else{
                    guideSceneWidget?.refreshEpg()
                }
            }
        }
    }

    override fun getEpgActiveFilter(): CategoryItem {
        return guideSceneWidget!!.getActiveCategory()
    }

    private fun showEpgData() {
        (sceneListener as HomeSceneListener).getActiveChannel(object :
            IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: kotlin.Error) {
            }

            override fun onReceive(activeTvChannel: TvChannel) {
                (sceneListener as HomeSceneListener).getAvailableChannelFilters(object :
                    IAsyncDataCallback<ArrayList<Category>> {
                    override fun onFailed(error: kotlin.Error) {}

                    override fun onReceive(filterList: ArrayList<Category>) {
                        ReferenceApplication.runOnUiThread {
                            var index = 0
                            run exitForEach@{
                                filterList.toList().forEach { item ->
                                    if (item.name.equals((sceneListener as HomeSceneListener).getActiveCategory())) {
                                        index = filterList.indexOf(item)
                                        (sceneListener as HomeSceneListener).setActiveEpgFilter(index)
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
                                        override fun onFailed(error: Error) {
                                            guideSceneWidget?.onFetchFailed()
                                        }
                                        override fun onReceive(eventListMap: LinkedHashMap<Int, MutableList<TvEvent>>) {
                                            ReferenceApplication.runOnUiThread {
                                                try {
                                                    guideSceneWidget?.refresh(filterList)
                                                    initGuideWidget(
                                                        (sceneListener as HomeSceneListener).getActiveEpgFilter(),
                                                        eventListMap
                                                    )
                                                } catch (E: Exception) {
                                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: ${E.printStackTrace()}")
                                                    ReferenceApplication.worldHandler?.destroyOtherExisting(
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
    }

    override fun handleCategoryFocusedWithCaching(position: Int) {
        LoadingPlaceholder.hideAllRegisteredLoadingPlaceholders()
        if (selectedPosition == position) return
        //For You is already shown
        homeContainer?.removeAllViews()
        if (position != initialFilterPosition) {
            homeFilterCategoryAdapter?.clearFocus(initialFilterPosition)
        }
        homeFilterCategoryAdapter?.selectedItem = position

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "handleCategoryFocusedWithCaching: position $position list.size ${list.size}")
        if (position >= 0 && position < list.size) {
            if (position != 0 && activeChannel!=null && !(sceneListener as HomeSceneListener).isLockedScreenShown()) {
                (sceneListener as HomeSceneListener).unMuteAudio()
            } else {
                (sceneListener as HomeSceneListener).muteAudio()
            }
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
                    var searchBtnFocused = searchIv?.hasFocus()
                    var settingsBtnFocused = settingsIv?.hasFocus()
                    var profileBtnFocused = profileIv?.hasFocus()
                    if (searchBtnFocused == false && settingsBtnFocused == false && profileBtnFocused == false) {
                        if (homeFilterGridView?.getChildAt(1) != null) {
                            homeFilterGridView?.getChildAt(1)?.requestFocus()
                        } else {
                            homeFilterGridView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
                                ViewTreeObserver.OnGlobalLayoutListener {
                                override fun onGlobalLayout() {
                                    homeFilterGridView?.viewTreeObserver
                                        ?.removeOnGlobalLayoutListener(this)
                                    homeFilterGridView?.getChildAt(1)?.requestFocus()
                                }
                            })
                        }
                    } else {
                        homeFilterGridView?.getChildAt(1)?.requestFocus()
                    }

                    super.createGuideSceneWidget()
                    selectedPosition = position
                    if ((sceneListener as HomeSceneListener).isChannelListEmpty()) {
                        homeContainer?.addView(guideScanWidget)
                    }else{
                        homeContainer?.addView(guideSceneWidget?.view)

                    }
                    return
                }
                else -> {
                    homeContainer?.addView(sceneNameTv)
                    sceneNameTv?.text = list[position].name
                }
            }
        }
    }

    override fun handlerCategoryFocus(position: Int) {
        var searchBtnFocused = searchIv?.hasFocus()
        var settingsBtnFocused = settingsIv?.hasFocus()
        var profileBtnFocused = if (profileIv?.visibility == View.VISIBLE) profileIv?.hasFocus() else false
        if (searchBtnFocused == false && settingsBtnFocused == false && profileBtnFocused == false) {
            homeFilterGridView!!.getChildAt(position).requestFocus()
        } else {
            homeFilterCategoryAdapter!!.setSelected(position)
        }
    }

    override fun handleCategoryClicked(position: Int) {

        //For you
        if (position == 0 && forYouWidget != null) {
            forYouWidget?.requestFocus = true
            forYouWidget?.setFocusToGrid()
            return
        }

        //TV Guide is already shown
        if (position == 1) {
            if (guideScanWidget!=null){
                guideScanWidget?.setFocusToScanButton()
                return
            }
            if (guideSceneWidget != null)
                guideSceneWidget?.setFocusToCategory()
            return
        }

        //Preferences is already shown
        if (preferencesSceneWidget != null && position == 3){
            preferencesClicked()
            return
        }
    }

    private fun showPreferences() {
        selectedPosition = -1
        createPreferencesWidget()
        (sceneListener as HomeSceneListener).getAvailablePreferenceTypes(
            object : IAsyncDataCallback<List<CategoryItem>> {
                override fun onFailed(error: Error) {
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun onReceive(data: List<CategoryItem>) {
                    ReferenceApplication.runOnUiThread {
                        preferencesSceneWidget?.refresh(data)
                    }
                }

            })

        homeContainer?.removeAllViews()
        homeContainer?.addView(preferencesSceneWidget?.view)
    }

    private fun preferencesClicked() {
        if (preferencesSceneWidget != null) {

            setSearchIconVisibility(false)
            setSettingsIconVisibility(false)
            setProfileIconVisibility(false)
            setAppIconVisibility(false)

            upArrow?.alpha = 0f

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

            animationSet.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    homeFilterGridView?.visibility = View.GONE
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })

            arrowAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    upArrow?.alpha = 0.7f
                    upArrow?.visibility = View.VISIBLE
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })

            upArrow?.startAnimation(arrowAnimation)
            homeFilterGridView?.startAnimation(animationSet)
            preferencesSceneWidget?.requestFocus()
            return
        }
    }

    /**
     * Show ReferenceWidgetForYou after initialization and keep focus on the For You item
     */
    override fun sceneInit() {
        /*if (selectedPosition == initialFilterPosition) {
            return
        }*/
        ReferenceApplication.worldHandler?.isEnableUserInteraction = false
        homeContainer?.removeAllViews()
        selectedPosition = 0
        if (initialFilterPosition == 0) {
            createForYouSceneWidget(false)
            forYouWidget?.view?.visibility = View.INVISIBLE
            homeContainer?.addView(forYouWidget?.view)
            ReferenceApplication.runOnUiThread(Runnable {
                homeFilterCategoryAdapter?.keepFocus = false
                homeFilterCategoryAdapter?.selectedItem = 0
                forYouWidget?.view?.visibility = View.VISIBLE
                ReferenceApplication.worldHandler?.isEnableUserInteraction = true
            })
            /*CoroutineHelper.runCoroutineWithDelay({
                homeFilterCategoryAdapter?.keepFocus = false
                homeFilterCategoryAdapter?.selectedItem = 0
                forYouWidget?.view?.visibility = View.VISIBLE
                worldHandler?.isEnableUserInteraction = true
            }, 0, Dispatchers.Main)*/
        } else {
            if (initialFilterPosition == 1 && (HomeSceneFast.jumpLiveTabCurrentEvent || jumpLiveTabCurrentEvent)) {
                HomeSceneBase.jumpGuideCurrentEvent = true
            }
            //for settings initial filter position is 3
            if(initialFilterPosition == 3){
                settingsIv?.requestFocus()
                if (settingsIconBg != null)
                    Utils.focusAnimation(settingsIconBg!!)
                if(ReferenceApplication.parentalControlDeepLink) {
                    homeFilterGridView?.alpha = 0f
                    onDpadDownClick(settingsIv)
                    preferencesSceneWidget?.showBroadcastParental()
                }
            }else {
                homeFilterCategoryAdapter?.requestFocus(initialFilterPosition)
                Handler().post {
                    handleCategoryFocusedWithCaching(initialFilterPosition)
                    initialFilterPosition = 0
                    jumpLiveTabCurrentEvent = false
                    HomeSceneFast.jumpLiveTabCurrentEvent = false
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (guideSceneWidget != null && selectedPosition==1) {
            guideSceneWidget?.pause()
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
        timeTv = null
        sceneNameTv = null
        settingsIv = null
        settingsIconBg = null
        searchIv = null
        searchIconBg = null
        view = null
        selectedPosition = 0

        if (list.isNotEmpty()) {
            list.clear()
        }
        super.onDestroy()
    }

    private fun createPreferencesWidget() {
        preferencesSceneWidget = ReferenceWidgetPreferences(
            context,
            object : PreferencesWidgetListener {
                override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                    (sceneListener as HomeSceneListener).showToast(text, duration)
                }

                override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                    (sceneListener as HomeSceneListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                }

                override fun resetCC() {
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
                    val translate: Animation =
                        TranslateAnimation(0F, 0F, -Utils.getDimens(R.dimen.custom_dim_61), 0f)
                    translate.duration = animationDuration

                    val alpha: Animation = AlphaAnimation(0f, 1f)
                    alpha.duration = animationDuration

                    val animationSet = AnimationSet(true)
                    animationSet.addAnimation(translate)
                    animationSet.addAnimation(alpha)
                    animationSet.interpolator = DecelerateInterpolator()
                    animationSet.fillAfter = true

                    homeFilterGridView?.startAnimation(animationSet)
                    upArrow?.visibility = View.GONE
                    animationSet.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationEnd(animation: Animation?) {
                            setSettingsIconVisibility(true)
                            setSearchIconVisibility((sceneListener as HomeSceneListener).kidsModeEnabled())
                            setProfileIconVisibility(true)
                            setAppIconVisibility(true)
                            if (focusedButtonId == 1) {
                                searchIv?.requestFocus()
                                if (searchIconBg != null)
                                    Utils.focusAnimation(searchIconBg!!)
                            } else if(focusedButtonId == 2) {
                                settingsIv?.requestFocus()
                                if (settingsIconBg != null)
                                    Utils.focusAnimation(settingsIconBg!!)
                            }else{
                                profileIv?.requestFocus()
                                if (profileIconBg != null)
                                    Utils.focusAnimation(profileIconBg!!)
                            }
                        }

                        override fun onAnimationRepeat(animation: Animation?) {}

                        override fun onAnimationStart(animation: Animation?) {}
                    })
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

                override fun onSubtitlesAnalogTypeClicked(position: Int) {
                    (sceneListener as HomeSceneListener).onSubtitlesAnalogTypeClicked(
                        position
                    )
                }

                override fun isAnalogSubtitleEnabled(type: PrefType): Boolean {
                    return (sceneListener as HomeSceneListener).isAnalogSubtitleEnabled(type)
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
                    (sceneListener as HomeSceneListener).onVisuallyImpairedValueChanged(
                        position,
                        enabled
                    )
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
                     checkIsLockedScreenShown()
                }

                override fun isChannelLockAvailable(tvChannel: TvChannel): Boolean {
                    return (sceneListener as HomeSceneListener).isChannelLockAvailable(tvChannel)
                }

                override fun blockInput(inputSourceData: InputSourceData, blocked: Boolean) {
                    (sceneListener as HomeSceneListener).blockInput(inputSourceData, blocked)
                }

                override fun isParentalControlsEnabled(): Boolean {
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
                    (sceneListener as HomeSceneListener).setContentRatingSystemEnabled(
                        contentRatingSystem,
                        enabled
                    )
                }

                override fun setContentRatingLevel(index: Int) {
                    (sceneListener as HomeSceneListener).setContentRatingLevel(index)
                }

                override fun setRatingBlocked(
                    contentRatingSystem: ContentRatingSystem,
                    it: ContentRatingSystem.Rating,
                    data: Boolean
                ): Boolean {
                    return (sceneListener as HomeSceneListener).setRatingBlocked(
                        contentRatingSystem,
                        it,
                        data
                    )
                }

                override fun setRelativeRatingsEnabled(
                    contentRatingSystem: ContentRatingSystem,
                    it: ContentRatingSystem.Rating,
                    data: Boolean
                ) {
                    (sceneListener as HomeSceneListener).setRelativeRatingsEnabled(
                        contentRatingSystem,
                        it,
                        data
                    )
                }

                override fun isRatingBlocked(
                    contentRatingSystem: ContentRatingSystem,
                    it: ContentRatingSystem.Rating
                ): Boolean {
                    return (sceneListener as HomeSceneListener).isRatingBlocked(
                        contentRatingSystem,
                        it
                    )
                }

                override fun isSubRatingEnabled(
                    contentRatingSystem: ContentRatingSystem,
                    it: ContentRatingSystem.Rating,
                    subRating: ContentRatingSystem.SubRating
                ): Boolean {
                    return (sceneListener as HomeSceneListener).isSubRatingEnabled(
                        contentRatingSystem,
                        it,
                        subRating
                    )
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
                    return (sceneListener as HomeSceneListener).setSubRatingBlocked(
                        contentRatingSystem,
                        rating,
                        subRating,
                        data
                    )
                }

                override fun setRelativeRating2SubRatingEnabled(
                    contentRatingSystem: ContentRatingSystem,
                    data: Boolean,
                    rating: ContentRatingSystem.Rating,
                    subRating: ContentRatingSystem.SubRating
                ) {
                    return (sceneListener as HomeSceneListener).setRelativeRating2SubRatingEnabled(
                        contentRatingSystem,
                        data,
                        rating,
                        subRating
                    )
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
                    return (sceneListener as HomeSceneListener).getRRT5LevelInfo(
                        countryIndex,
                        dimIndex
                    )
                }

                override fun getSelectedItemsForRRT5Level(): HashMap<Int, Int> {
                    return (sceneListener as HomeSceneListener).getSelectedItemsForRRT5Level()
                }

                override fun rrt5BlockedList(
                    regionPosition: Int,
                    position: Int
                ): HashMap<Int, Int> {
                    return (sceneListener as HomeSceneListener).rrt5BlockedList(
                        regionPosition,
                        position
                    )
                }

                override fun setSelectedItemsForRRT5Level(
                    regionIndex: Int,
                    dimIndex: Int,
                    levelIndex: Int
                ) {
                    (sceneListener as HomeSceneListener).setSelectedItemsForRRT5Level(
                        regionIndex,
                        dimIndex,
                        levelIndex
                    )
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
                    (sceneListener as HomeSceneListener).onSwapChannelSelected(
                        firstChannel,
                        secondChannel,
                        previousPosition,
                        newPosition
                    )
                }

                override fun onMoveChannelSelected(
                    moveChannelList: java.util.ArrayList<TvChannel>,
                    previousPosition: Int,
                    newPosition: Int,
                    channelMap: HashMap<Int, String>
                ) {
                    (sceneListener as HomeSceneListener).onMoveChannelSelected(
                        moveChannelList,
                        previousPosition,
                        newPosition,
                        channelMap
                    )
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

                override fun getChannelSourceType(tvChannel: TvChannel): String {
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

                override fun setAudioDescriptionEnabled(enable: Boolean) {
                    (sceneListener as HomeSceneListener).setAudioDescriptionEnabled(enable)
                }

                override fun setHearingImpairedEnabled(enable: Boolean) {
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

                override fun getPreferenceDeepLink(): DeepLink? {
                    return (sceneListener as HomeSceneListener).getPreferenceDeepLink()
                }

                override fun startOadScan() {
                    return (sceneListener as HomeSceneListener).startOadScan()
                }

                override fun startCamScan() {
                    return (sceneListener as HomeSceneListener).startCamScan()
                }

                override fun changeCamPin() {
                    return (sceneListener as HomeSceneListener).changeCamPin()
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

                override fun onTimeshiftStatusChanged(enabled: Boolean) {
                    (sceneListener as HomeSceneListener).onTimeshiftStatusChanged(enabled)
                }

                override fun getConfigInfo(nameOfInfo: String): Boolean {
                    return (sceneListener as HomeSceneListener).getConfigInfo(nameOfInfo)
                }

                override fun doCAMReconfiguration(camTypePreference: CamTypePreference) {
                    return (sceneListener as HomeSceneListener).doCAMReconfiguration(camTypePreference)
                }

                override fun getLockedChannelIdsFromPrefs(): MutableSet<String> {
                    return (sceneListener as HomeSceneListener).getLockedChannelIdsFromPrefs()
                }
            })

        preferencesSceneWidget!!.setCategoriesMargin(Utils.getDimensInPixelSize(R.dimen.custom_dim_95_5))

        // To open preference instantly for deeplink
        if((sceneListener as HomeSceneListener).getPreferenceDeepLink()!=null) {
            setSearchIconVisibility(false)
            setSettingsIconVisibility(false)
            setProfileIconVisibility(false)
            setAppIconVisibility(false)
            homeFilterGridView?.visibility = View.GONE
            if ((sceneListener as HomeSceneListener).getPreferenceDeepLink()?.widgetId !=
                ReferenceWorldHandler.WidgetId.PREFERENCES_SETUP) {
                upArrow?.visibility = View.VISIBLE
                upArrow?.alpha = 0.7f
            }
        }
    }

    override fun refreshForYou() {
        if (forYouWidget != null) {
            (sceneListener as HomeSceneListener).getForYouRails()
        }
    }

    override fun createForYouSceneWidget(requestFocus: Boolean) {
        if (forYouWidget != null) {
            return
        }
        homeFilterGridView?.requestFocus()
        forYouWidget = ReferenceWidgetForYou(context,
            object : ReferenceWidgetForYou.ForYouWidgetListener {

                override fun isRegionSupported(): Boolean {
                    return (sceneListener as HomeSceneListener).isRegionSupported()
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    (sceneListener as HomeSceneListener).setSpeechText(text = text, importance = importance)
                }

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
                    return (sceneListener as HomeSceneListener).getPromotionContent(callback)
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
                    return (sceneListener as HomeSceneListener).getParentalRatingDisplayName(
                        parentalRating, tvEvent
                    )
                }

                override fun keepFocusOnFilerView() {
                    try {
                        if (homeFilterCategoryAdapter?.focusPosition == 0 && searchIv?.hasFocus() == false) {
                            homeFilterCategoryAdapter?.requestFocus(0)
                        }
                        if (homeFilterCategoryAdapter?.focusPosition == 0 && searchIv?.hasFocus() == true) {
                            var holder = homeFilterGridView?.findViewHolderForAdapterPosition(
                                0
                            ) as CategoryItemViewHolder
                            homeFilterCategoryAdapter?.setActiveFilter(holder)
                        }
                    } catch (E: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "keepFocusOnFilerView: ${E.printStackTrace()}")
                        ReferenceApplication.worldHandler?.destroyOtherExisting(
                            ReferenceWorldHandler.SceneId.LIVE)
                        return
                    }
                }

                override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                    return dispatchKeyEvent(keyCode, event)
                }

                override fun isEventLocked(tvEvent: TvEvent?) = (sceneListener as HomeSceneListener).isEventLocked(tvEvent)

                override fun isTosAccepted() = (sceneListener as HomeSceneListener).isTosAccepted()

            },(sceneListener as HomeSceneListener).getDateTimeFormat())

        (sceneListener as HomeSceneListener).getPromotionContent(callback = object : IAsyncDataCallback<ArrayList<PromotionItem>> {
            override fun onFailed(error: Error) {
                forYouWidget?.setup()
                forYouWidget?.restartTimerIfListNotEmpty()
            }

            override fun onReceive(data: ArrayList<PromotionItem>) {
                if (data.isEmpty()) {
                    Handler().postDelayed(
                        Runnable {
                            forYouWidget?.setup()
                        }, 2000
                    )
                } else {
                    forYouWidget?.setup()
                }
            }
        })

        //forYouWidget?.setup()
    }

    private fun launchPreferences() {
        if (settingsIv?.hasFocus() == false) {
            homeFilterCategoryAdapter?.selectedItem?.let {
                homeFilterCategoryAdapter?.clearFocus(
                    it
                )
            }
            //isPaused should be true when we are pressing guide button while,
            // we are in live tab,so that it can reset its view when we enter again in fast.
            if (FastLiveTabDataProvider.fastLiveTab != null) {
                FastLiveTabDataProvider.fastLiveTab!!.isPaused = true
            }
            settingsIv?.requestFocus()
            if (settingsIconBg != null)
                Utils.focusAnimation(settingsIconBg!!)
        }
    }

    /**
     * Channel changed
     * @return is channel changed handled by the active scene widget
     */
    override fun channelChanged(): Boolean {
        // Select new active channel inside the guide if it is shown
        if (activeWidget == 1) {
            //Set active channel
            guideSceneWidget?.channelChanged()
        }
        return true
    }

    private fun initGuideWidget(
        filter: Int,
        eventListMap: LinkedHashMap<Int, MutableList<TvEvent>>
    ) {
        //Set channel list
        mSelectedFilter = filter
        var channelList = mutableListOf<TvChannel>()
        for ((key, value) in eventListMap) {
            channelList.add(value[0].tvChannel)

        }
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
                                        (sceneListener as HomeSceneListener).getCurrentTime(
                                            tvChannel
                                        )
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
        ReferenceApplication.runOnUiThread(Runnable {
            if (guideSceneWidget != null) {
                if (!isExtend) {
                    //Set current date
                    guideSceneWidget?.refresh(date!!)

                    var channelList: ArrayList<TvChannel> = arrayListOf()
                    guideChannelList.forEach { channel ->
                        if (channel.isBrowsable || channel.inputId.contains("iwedia") || channel.inputId.contains(
                                "sampletvinput"
                            )
                        ) {
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
                guideSceneWidget?.refresh(date!!)
            } else if (forYouWidget != null) {
            }
        } else if (data is Int) {
            initialFilterPosition = data
        }
    }

    /**
     * Init search/settings button
     */
    override fun initButton(
        selector: View?,
        btnView: ImageView?
    ) {
        selector!!.background = ConfigColorManager.generateButtonNotFocusedBackground()
        btnView?.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                homeFilterCategoryAdapter?.selectedItem = -1
                if (hasFocus) {
                    LoadingPlaceholder.hideAllRegisteredLoadingPlaceholders()
                    if (focusedButtonId != 2 && btnView?.tag == 2) {
                        showPreferences()
                    }
                    focusedButtonId = btnView?.tag as Int
                    selector.background = ConfigColorManager.generateButtonBackground()
                    btnView.setColorFilter(
                        Color.parseColor(
                            ConfigColorManager.getColor(
                                "color_background"
                            )
                        )
                    )
                    if (!(sceneListener as HomeSceneListener).isLockedScreenShown()) {
                        (sceneListener as HomeSceneListener).unMuteAudio()
                    }
                } else {
                    selector.background = ConfigColorManager.generateButtonNotFocusedBackground()
                    btnView?.setColorFilter(
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
                        if (keycode == KeyEvent.KEYCODE_BACK){
                            ReferenceApplication.downActionBackKeyDone = true
                        }
                        if (keycode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                if (btnView.tag == 1) {
                                    settingsIv?.requestFocus()
                                    if (settingsIconBg != null)
                                        Utils.focusAnimation(settingsIconBg!!)
                                    settingsOpened = false
                                    return true
                                }
                                else if (btnView.tag == 2) {
                                    profileIv?.requestFocus()
                                    if (profileIconBg != null)
                                        Utils.focusAnimation(profileIconBg!!)
                                    settingsOpened = false
                                    return true
                                }
                            } else {
                                if (btnView.tag == 3) {
                                    var position = homeFilterGridView?.adapter?.itemCount
                                    if (position != null) {
                                        homeFilterGridView?.layoutManager?.findViewByPosition(
                                            position - 1
                                        )
                                            ?.requestFocus()
                                    }
                                    settingsOpened = false
                                    return true
                                }
                            }
                        }
                        if (keycode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                if (btnView.tag == 1) {
                                    var position = homeFilterGridView?.adapter?.itemCount
                                    if (position != null) {
                                        homeFilterGridView?.layoutManager?.findViewByPosition(
                                            position - 1
                                        )
                                            ?.requestFocus()
                                    }
                                    settingsOpened = false
                                    return true
                                } else if (btnView.tag == 3) {
                                    settingsIv?.requestFocus()
                                    if (settingsIconBg != null)
                                        Utils.focusAnimation(settingsIconBg!!)
                                    settingsOpened = false
                                } else if (btnView.tag == 2) {
                                    if ((sceneListener as HomeSceneListener).kidsModeEnabled()) {
                                        searchIv?.requestFocus()
                                        if (searchIconBg != null)
                                            Utils.focusAnimation(searchIconBg!!)
                                        settingsOpened = false
                                    }
                                    else {
                                        var position = homeFilterGridView?.adapter?.itemCount
                                        if (position != null) {
                                            homeFilterGridView?.layoutManager?.findViewByPosition(position - 1)?.requestFocus()
                                            settingsOpened = false
                                        }
                                    }
                                } else {
                                    searchIv?.requestFocus()
                                    if (searchIconBg != null)
                                        Utils.focusAnimation(searchIconBg!!)
                                    settingsOpened = false
                                }
                            } else {
                                if (btnView.tag == 1) {
                                    homeFilterGridView?.layoutManager?.findViewByPosition(0)
                                        ?.requestFocus()
                                    settingsOpened = false
                                    return true
                                } else if (btnView.tag == 2) {
                                    profileIv?.requestFocus()
                                    if (profileIconBg != null)
                                        Utils.focusAnimation(profileIconBg!!)
                                    settingsOpened = false
                                    return true
                                }
                            }
                        }
                        if (keycode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            if (btnView.tag == 2 ) {
                                onDpadDownClick(btnView)
                                return true
                            } else {
                                if (preferencesSceneWidget != null && homeContainer!!.contains(preferencesSceneWidget!!.view as View)) {
                                    onDpadDownClick(settingsIv)
                                } else {
                                    btnView.clearFocus()
                                    val holder =
                                        homeFilterGridView?.findViewHolderForAdapterPosition(
                                            1
                                        ) as CategoryItemViewHolder
                                    holder.rootView?.callOnClick()
                                }

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
                                            if ((sceneListener as HomeSceneListener).kidsModeEnabled()) {
                                                val intent = Intent(Settings.ACTION_SETTINGS)
                                                intent.flags =
                                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startSettingsIntent(intent)
                                            }
                                            else {
                                                onDpadDownClick(btnView)
                                            }
                                        }
                                    })
                            }

                            if (btnView.tag == 3) {
                                Utils.viewClickAnimation(
                                    profileIconBg,
                                    object : com.iwedia.cltv.utils.AnimationListener {
                                        override fun onAnimationEnd() {
                                            try {
                                                val intent = Intent()
                                                intent.setClassName(
                                                    "com.google.android.apps.tv.launcherx",
                                                    "com.google.android.apps.tv.launcherx.profile.chooser.ProfileChooserActivity"
                                                )
                                                startSettingsIntent(intent)
                                            } catch (e: Exception) {
                                                val intent = Intent(Settings.ACTION_SETTINGS)
                                                startSettingsIntent(intent)
                                            }
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
                        if (!ReferenceApplication.downActionBackKeyDone)return true
                        if (activeWidget == 0 && settingsIv?.hasFocus() == false && searchIv?.hasFocus() == false) {
                            (sceneListener as HomeSceneListener).showExitDialog()
                            return true
                        } else if (!settingsOpened) {
                            if(settingsIv?.hasFocus() == true) settingsIv?.clearFocus()
                            if(searchIv?.hasFocus() == true) searchIv?.clearFocus()
                            handleCategoryFocusedWithCaching(0)
                        }
                        settingsOpened = false
                    }
                } catch (E: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onKey: ${E.printStackTrace()}")
                }
                return true
            }
        })
    }

    fun checkMutePlayback() {
        if (activeWidget == 0) {
            forYouWidget?.checkMutePlayback()
        }
    }
    fun onDpadDownClick(btnView: ImageView?){
        setSettingsIconVisibility(false)
        setSearchIconVisibility(false)
        setProfileIconVisibility(false)
        setAppIconVisibility(false)
        btnView?.clearFocus()

        upArrow?.alpha = 0f

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

        animationSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                homeFilterGridView?.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        arrowAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                upArrow?.alpha = 0.7f
                upArrow?.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })

        upArrow?.startAnimation(arrowAnimation)
        homeFilterGridView?.startAnimation(animationSet)
        preferencesSceneWidget?.requestFocus()
        settingsOpened = false
    }

    private fun startSettingsIntent(intent: Intent) {
        settingsOpened = true
        (sceneListener as HomeSceneListener).onSettingsOpened()
        context.startActivity(intent)
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) &&
            ((homeContainer != null) && (homeContainer?.hasFocus()!!))
        ) {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
                if (!ReferenceApplication.downActionBackKeyDone)return true
                if (activeWidget == 0 && forYouWidget != null) {
                    forYouWidget?.onBackPressed()
                }
                homeFilterGridView?.requestFocus()
                return true
            }
            return true
        }
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) &&
            (settingsIv?.hasFocus() == true || searchIv?.hasFocus() == true)
        ) {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
                if (!ReferenceApplication.downActionBackKeyDone)return true
                (sceneListener as HomeSceneListener).onBackPressed()
                return true
            }
        }

        //handle back from calendar button
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) && (guideSceneWidget != null)) {
            if (!ReferenceApplication.downActionBackKeyDone)return true
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
                homeFilterGridView?.requestFocus()
                return true
            }
        }
        if (keyCode == KeyEvent.KEYCODE_MENU && (keyEvent as KeyEvent).action == KeyEvent.ACTION_UP ){
            if (!ReferenceApplication.downActionBackKeyDone)return true
            launchPreferences()
        }
        if (keyCode == KeyEvent.KEYCODE_MENU && (keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN ){
            ReferenceApplication.downActionBackKeyDone = true
        }
        val dispatchResult = super.dispatchKeyEvent(keyCode, keyEvent)
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_BACK){
                ReferenceApplication.downActionBackKeyDone = false
            }
        }
        return dispatchResult
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
        homeFilterCategoryAdapter?.keepFocus = saveFocus
        if (favouritesSceneWidget != null) {
            favouritesSceneWidget?.saveFocus(saveFocus)
        }
    }

    /**
     * Refresh guide details favorite button
     */
    override fun refreshGuideDetailsFavButton() {
        if (guideSceneWidget != null) {
            ReferenceApplication.runOnUiThread(Runnable {
                guideSceneWidget?.refreshFavoriteButton()
            })
        }
    }

    /**
     * Refresh guide filter list
     *
     * @param filterList a new filter list items
     */
    override fun refreshGuideFilterList(filterList: MutableList<CategoryItem>) {
        if (guideSceneWidget != null && searchIv?.hasFocus() == false && settingsIv?.hasFocus() == false) {
            ReferenceApplication.runOnUiThread(Runnable {
                guideSceneWidget?.refreshFilterList(filterList)
            })
        }
    }

    override fun refreshGuideOnUpdateFavorite() {

        if (guideSceneWidget != null && searchIv?.hasFocus() == false && settingsIv?.hasFocus() == false) {
            ReferenceApplication.runOnUiThread(Runnable {
                guideSceneWidget?.refreshGuideOnUpdateFavorite()
            })
        }
    }

    override fun showWidgetByPosition(position: Int) {
        Handler().postDelayed({
            homeFilterGridView?.getChildAt(position)?.callOnClick()
        }, 500)
    }

    //refresh recordings list
    override fun refreshRecordingsList(data: ArrayList<ScheduledRecording>?) {
        if (recordingsWidget != null) {
            recordingsWidget?.refresh(data!!)
        }
    }

    override fun getActiveCategory(): Int {
        return activeWidget
    }

    private fun isUsbConnected(): Boolean {
        var usbManager = ReferenceApplication.applicationContext()
            .getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.size > 0
    }

    private fun guideVerticalOrientation(): Boolean =
        (sceneListener as HomeSceneListener).getConfigInfo("vertical_epg")

    override fun getActiveCategoryId(): Int {
        return if (homeFilterCategoryAdapter != null && homeFilterCategoryAdapter!!.selectedItem != -1) {
            list[homeFilterCategoryAdapter!!.selectedItem].id
        } else {
            //special case when fast clicking guide button
            1
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun checkSearchFocusOnGuideKeyPress(): Boolean{
        if (settingsIv?.hasFocus() == true &&
            activeWidget == 1) {
            if (guideSceneWidget is HorizontalGuideSceneWidget) {
                (guideSceneWidget as HorizontalGuideSceneWidget).setFocusOnEvent()
            }
            return true
        }
        return false
    }

    override fun isBtnFocused():Boolean = settingsIv?.hasFocus() == true || searchIv?.hasFocus() == true
}