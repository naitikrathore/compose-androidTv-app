package com.iwedia.cltv.components

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.*
import com.iwedia.cltv.anoki_fast.FastHomeData
import com.iwedia.cltv.anoki_fast.FastErrorInfo
import com.iwedia.cltv.anoki_fast.FastTosMoreInfo
import com.iwedia.cltv.anoki_fast.FastTosMoreInfoButtonActivity
import com.iwedia.cltv.anoki_fast.FastTosMoreInfoListener
import com.iwedia.cltv.anoki_fast.epg.AnimationHelper
import com.iwedia.cltv.anoki_fast.epg.BackFromPlayback
import com.iwedia.cltv.anoki_fast.epg.FastLiveTabDataProvider
import com.iwedia.cltv.config.*
import com.iwedia.cltv.platform.`interface`.ForYouInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PromotionItem
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.scene.home_scene.rail.AdapterListener
import com.iwedia.cltv.scene.home_scene.rail.RailAdapter
import com.iwedia.cltv.utils.LoadingPlaceholder
import com.iwedia.cltv.utils.PlaceholderName
import com.iwedia.cltv.utils.Utils
import tv.anoki.ondemand.domain.model.VODItem
import utils.information_bus.Event
import utils.information_bus.EventListener
import utils.information_bus.InformationBus
import world.widget.GWidget
import world.widget.GWidgetListener
import java.util.Collections
import kotlin.collections.ArrayList

/**
 * For you widget
 * @author Veljko Ilic
 */
class ReferenceWidgetForYou :
    GWidget<ConstraintLayout, ReferenceWidgetForYou.ForYouWidgetListener> {

    var configParam: SceneConfig? = null
    var context: Context? = null
    val dateTimeFormat: DateTimeFormat

    private var  scrollView: ScrollView? =null
    var verticalGridView: VerticalGridView? = null
    var railAdapter: RailAdapter? = null
    var itemsForAdapter: ArrayList<RailItem> = ArrayList()
    var noEventsMsg: TextView? = null
    var backgroundView: View? = null
    var broadcastBackgroundView: View? = null
    //Request focus after initialization
    var requestFocus = false

    //Enable fast data (promotions and recommendations)
    private val enableFastData = ForYouInterface.ENABLE_FAST_DATA

    /**
     * Widget information bus event listener
     */
    private var eventListener: WidgetEventListener? = null

    private var customDetailsFastHome: CustomDetails.CustomDetailsFastHome? = null

    private var detailsContainer: ConstraintLayout?= null

    private var updateTimer: CountDownTimer? = null

    /**
     * Promotion banner view
     */
    private var fastHomeData: FastHomeData?= null

    /**
     * Promotion banner background
     */
    private var bannerBg: ImageView?= null
    private lateinit var fastErrorInfo: FastErrorInfo
    var fastTosMoreInfo: FastTosMoreInfo

    private var valueAnimator: ValueAnimator? = null

    private var isFocusOnBroadcastRow = false
    private var defaultGradientBackground: GradientDrawable?=null
    private var forYouGradientBackground: GradientDrawable?=null
    private var onNowGradientBackground: GradientDrawable?=null
    private var upNextGradientBackground: GradientDrawable?=null
    private var watchlistGradientBackground: GradientDrawable?=null
    private var recordedGradientBackground: GradientDrawable?=null
    private var scheduledRecordingGradientBackground: GradientDrawable?=null
    private val onDetailsSet = {
        if (!listener.hasInternet() || !listener.isAnokiServerReachable()) {
            fastErrorInfo.visibility = View.GONE
        }
        if (!listener.isRegionSupported()) {
            fastErrorInfo.visibility = View.GONE
        }
    }
    // Do not add muteAudio() in below function as it causes to mute audio at multiple places like
    // clicking on watch button from on Now rail, removing watchlist event from discovery and again adding it from EPG etc.
    private val onShowPromotion = {
        if (!listener.hasInternet() || !listener.isAnokiServerReachable()) {
            fastErrorInfo.visibility = View.VISIBLE
//            fastHomeData?.removeDetails() // TODO BORIS handle this
        }

        valueAnimator?.cancel()
        customDetailsFastHome!!.visibility = View.GONE
        if (!listener.isRegionSupported()) {
            detailsContainer!!.visibility = View.GONE
        } else {
            detailsContainer!!.visibility = View.VISIBLE
        }
        customDetailsFastHome!!.alpha = 0f
    }

    /**
     * Updates the rail list by refreshing the list of items if a certain condition is met.
     * The function checks if the `refreshCount` has reached 60s and then proceeds to filter
     * the items in the list based on specific criteria.
     */
    fun updateRailList() {
        val tmpList = arrayListOf<RailItem>()
        tmpList.addAll(itemsForAdapter)
        var indexNow = 0
        var indexNext = 0
        var itemsToRemoveNow = arrayListOf<Int>()
        var itemsToRemoveNext = arrayListOf<Int>()

        tmpList.forEachIndexed { index, railItem ->

            if (railItem.railName == ConfigStringsManager.getStringById("on_now")) {
                indexNow =  index
                railItem.rail?.forEachIndexed { indexNow, item ->
                    if (item is TvEvent && item.endTime < listener.getCurrentTime(item.tvChannel)) {
                        itemsToRemoveNow.add(indexNow)
                    }
                }
            } else if (railItem.railName == ConfigStringsManager.getStringById("up_next")) {
                indexNext = index
                railItem.rail?.forEachIndexed { indexNext, item ->
                    if (item is TvEvent && item.startTime < listener.getCurrentTime(item.tvChannel)) {
                        itemsToRemoveNext.add(indexNext)
                    }
                }
            }
        }

        tmpList.forEachIndexed { index, _ ->
            if (index == indexNow) {
                removeItems(tmpList, index, itemsToRemoveNow)
            }
            else if (index == indexNext) {
                removeItems(tmpList, index, itemsToRemoveNext)
            }
        }
        itemsForAdapter = tmpList
        railAdapter?.update(itemsForAdapter)
    }

    private fun removeItems(
        tmpList: ArrayList<RailItem>,
        index: Int,
        itemsToRemove: ArrayList<Int>
    ) {
        var list = arrayListOf<Any>()
        tmpList[index].rail?.forEachIndexed { index, tvEvent ->
            if (!itemsToRemove.contains(index)) {
                list.add(tvEvent)
            }
        }
        if (list.isNotEmpty()) {
            itemsForAdapter[index].rail?.clear()
            itemsForAdapter[index].rail?.addAll(list)
        }
    }

    inner class WidgetEventListener : EventListener() {
        init {
            addType(Events.NO_ETHERNET_EVENT)
            addType(Events.ETHERNET_EVENT)
            addType(Events.CHANNEL_CHANGED)
            addType(Events.WAITING_FOR_CHANNEL)
            addType(Events.NO_PLAYBACK)
            addType(Events.PVR_RECORDING_REMOVED)
            addType(Events.RECORDING_RENAMED)
            addType(Events.FAST_DATA_UPDATED)
            addType(Events.USB_DEVICE_CONNECTED)
            addType(Events.USB_DEVICE_DISCONNECTED)
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun callback(event: Event?) {
            if (event!!.type == Events.WAITING_FOR_CHANNEL || event!!.type == Events.NO_PLAYBACK || event!!.type == Events.CHANNEL_CHANGED) {
                verticalGridView?.setBackgroundColor(0)
            } else if (event!!.type == Events.FAST_DATA_UPDATED){
                ReferenceApplication.runOnUiThread(Runnable {
                    checkNetwork()
                    setup()
                    requestFocus = false
                    verticalGridView?.clearFocus()
                    view?.let {
                        if (view!!.isAttachedToWindow) {
                            listener.requestFocusOnTopCategories()
                        }
                    }
                    fastHomeData?.removeFocus()
                })
            }
            else if (event!!.type == Events.PVR_RECORDING_REMOVED){
                railAdapter?.onRecordingDeleted(object : IAsyncCallback {
                    override fun onFailed(error: Error) {}

                    override fun onSuccess() {
                        ReferenceApplication.runOnUiThread {
                            onBackPressed()
                        }
                        listener.getRails(configParam)
                    }
                })
            }
            else if (event!!.type == Events.USB_DEVICE_CONNECTED){
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    listener.getRails(configParam)
                },3000)
            }
            else if (event!!.type == Events.USB_DEVICE_DISCONNECTED){
                listener.getRails(configParam)
            }
        }
    }

    private fun checkNetwork() {
        if (!listener.isRegionSupported()) {
            fastErrorInfo.visibility = View.GONE
            return
        }

        if (!listener.hasInternet()) {
            fastErrorInfo.setTextInfo(FastErrorInfo.InfoType.NO_INTERNET)
            fastErrorInfo.visibility = View.VISIBLE
            return
        }
        if (!listener.isAnokiServerReachable()) {
            fastErrorInfo.setTextInfo(FastErrorInfo.InfoType.ANOKI_SERVER_DOWN)
            fastErrorInfo.visibility = View.VISIBLE
            return
        }
        fastErrorInfo.visibility = View.GONE
        fastHomeData?.visibility = View.VISIBLE
    }

    constructor(
        context: Context,
        listener: ForYouWidgetListener,
        dateTimeFormat: DateTimeFormat
        ) : super(
        ReferenceWorldHandler.WidgetId.FOR_YOU,
        ReferenceWorldHandler.WidgetId.FOR_YOU,
        listener
    ) {
        this.context = context
        this.dateTimeFormat = dateTimeFormat

        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_widget_for_you, null) as ConstraintLayout
        view!!.setBackgroundColor(
            Color.parseColor(
                ConfigColorManager.getColor(
                    "color_background"
                )
            )
        )
        backgroundView = view!!.findViewById(R.id.background_view)
        backgroundView!!.setBackgroundColor(
            Color.parseColor(
                ConfigColorManager.getColor(
                    "color_background"
                )
            )
        )
        broadcastBackgroundView = view!!.findViewById(R.id.broadcast_background_view)
        initGradientBackgrounds()
        broadcastBackgroundView?.background = defaultGradientBackground

        customDetailsFastHome = view!!.findViewById(R.id.custom_details_fast_home)
        detailsContainer = view!!.findViewById(R.id.details_container)

        verticalGridView = view!!.findViewById(R.id.for_you_recycler)
        noEventsMsg = view!!.findViewById(R.id.no_events_msg)
        noEventsMsg!!.setText(ConfigStringsManager.getStringById("no_categories_msg"))
        noEventsMsg!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )
        noEventsMsg!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        fastErrorInfo = view!!.findViewById(R.id.no_internet_info)
        fastErrorInfo.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        fastErrorInfo.visibility = View.GONE

        fastTosMoreInfo = view!!.findViewById(R.id.tos_more_info)
        fastTosMoreInfo.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        fastTosMoreInfo.visibility = View.GONE

        setupAdapter()

        configParam = ConfigHandler.getSceneConfigParam(ReferenceWorldHandler.WidgetId.FOR_YOU)
        requestFocus = false
        noEventsMsg!!.visibility = View.INVISIBLE

        //Promotion banner initialisation
        fastHomeData = view!!.findViewById(R.id.promotion_banner)
        bannerBg = view!!.findViewById(R.id.banner_bg)
        fastHomeData?.setBackgroundImageView(bannerBg!!)
        fastHomeData?.setBackgroundView(backgroundView!!)
        scrollView = view!!.findViewById<ScrollView>(R.id.scrollView)
        var scrollViewLayoutParams = scrollView!!.layoutParams as ConstraintLayout.LayoutParams
        if (enableFastData && listener.isRegionSupported()) {
            scrollViewLayoutParams.topMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_102_5)
            scrollView!!.layoutParams = scrollViewLayoutParams
            scrollView!!.invalidate()
            fastHomeData?.visibility = View.VISIBLE
            Log.d(Constants.LogTag.CLTV_TAG + Utils.TAG, "initialisation: BIR 2")
            fastHomeData?.setListener(object : FastHomeData.FastHomeDataListener {
                override fun onButtonClicked(promotionItem: PromotionItem) {
                    listener.promotionItemActionClicked(promotionItem)
                }

                override fun onDown() {
                    verticalGridView!!.requestFocus()
                }

                override fun onUp() {
                    listener.requestFocusOnTopCategories()
                }

                override fun mute() {
                    listener.muteAudio()
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }
            })
        } else {
            //initializeForYouLoadingPlaceholder()
            scrollViewLayoutParams.topMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_124)
            scrollView!!.layoutParams = scrollViewLayoutParams
            scrollView!!.invalidate()
            fastHomeData?.visibility = View.GONE
        }

        registerEventListener()
    }

    private fun initGradientBackgrounds() {
        defaultGradientBackground = GradientDrawable()
        defaultGradientBackground!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        defaultGradientBackground!!.orientation = GradientDrawable.Orientation.BL_TR
        val colors1 = intArrayOf( Color.parseColor("#0d0f12"), Color.parseColor("#0d0f12"), Color.parseColor("#242934"))
        defaultGradientBackground!!.colors = colors1

        onNowGradientBackground = GradientDrawable()
        onNowGradientBackground!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        onNowGradientBackground!!.orientation = GradientDrawable.Orientation.BL_TR
        val colors2 = intArrayOf( Color.parseColor("#0a0b0e"), Color.parseColor("#0a0b0e"/*"#324258"*/), Color.parseColor("#3f5d6f"))
        onNowGradientBackground!!.colors = colors2

        upNextGradientBackground = GradientDrawable()
        upNextGradientBackground!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        upNextGradientBackground!!.orientation = GradientDrawable.Orientation.BL_TR
        val colors3 = intArrayOf( Color.parseColor("#0e0f12"), Color.parseColor("#0e0f12"/*"#22152a"*/), Color.parseColor("#4f336e"))
        upNextGradientBackground!!.colors = colors3

        watchlistGradientBackground = GradientDrawable()
        watchlistGradientBackground!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        watchlistGradientBackground!!.orientation = GradientDrawable.Orientation.BL_TR
        val colors4 = intArrayOf( Color.parseColor("#090a0a"), Color.parseColor("#090a0a"), Color.parseColor("#2d5652"))
        watchlistGradientBackground!!.colors = colors4

        recordedGradientBackground = GradientDrawable()
        recordedGradientBackground!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        recordedGradientBackground!!.orientation = GradientDrawable.Orientation.BL_TR
        val colors5 = intArrayOf( Color.parseColor("#060606"), Color.parseColor("#060606"), Color.parseColor("#4b437e"))
        recordedGradientBackground!!.colors = colors5

        scheduledRecordingGradientBackground = GradientDrawable()
        scheduledRecordingGradientBackground!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        scheduledRecordingGradientBackground!!.orientation = GradientDrawable.Orientation.BL_TR
        val colors6 = intArrayOf( Color.parseColor("#080808"), Color.parseColor("#080808"), Color.parseColor("#414318"))
        scheduledRecordingGradientBackground!!.colors = colors6

        forYouGradientBackground = GradientDrawable()
        forYouGradientBackground!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        forYouGradientBackground!!.orientation = GradientDrawable.Orientation.BL_TR
        val colors7 = intArrayOf( Color.parseColor("#080909"), Color.parseColor("#080909"), Color.parseColor("#693f17"))
        forYouGradientBackground!!.colors = colors7
    }

    private fun initializeForYouLoadingPlaceholder() {

        LoadingPlaceholder(
            context = ReferenceApplication.applicationContext(),
            placeholderViewId = R.layout.loading_layout_rail_main,
            parentConstraintLayout = view!!,
            name = PlaceholderName.FOR_YOU
        )
        LoadingPlaceholder.showLoadingPlaceholder(PlaceholderName.FOR_YOU)
    }


    fun setup() {
        noEventsMsg?.visibility = View.INVISIBLE
        if (!listener.isTosAccepted()) {
            fastTosMoreInfo.setListener(object : FastTosMoreInfoListener {
                override fun showTos() {
                    val intent =
                        Intent(ReferenceApplication.applicationContext(), FastTosMoreInfoButtonActivity::class.java)
                    ReferenceApplication.getActivity().startActivityForResult(intent, 11111)
                }

                override fun requestFocusOnRail() {
                    verticalGridView!!.requestFocus()
                    fastTosMoreInfo.visibility = View.GONE
                }

                override fun requestFocusOnTopMenu() {
                    listener.requestFocusOnTopCategories()
                }
            })
            fastTosMoreInfo?.visibility = View.VISIBLE
        }
        showLoading()
        configParam = ConfigHandler.getSceneConfigParam(ReferenceWorldHandler.WidgetId.FOR_YOU)
        listener.getRails(configParam)
        if (listener.isRegionSupported()) {
            listener.getPromotionContent(object : IAsyncDataCallback<ArrayList<PromotionItem>> {
                override fun onFailed(error: Error) {
                    // If an error appears, hide the details container to prevent empty space above the rails
                    detailsContainer?.visibility = View.GONE
                }

                override fun onReceive(data: ArrayList<PromotionItem>) {
                    if (data.isNotEmpty()) {
                        // If the received data is not empty, update the promotion list
                        fastHomeData?.setPromotionList(data)
                    } else {
                        // If the received data is empty, hide the details container to prevent empty space above the rails
                        detailsContainer?.visibility = View.GONE
                    }
                }
            })
        }
        if (listener.isRegionSupported() && !listener.isAnokiServerReachable()) {
            fastErrorInfo.setTextInfo(FastErrorInfo.InfoType.ANOKI_SERVER_DOWN)
            fastErrorInfo.visibility = View.VISIBLE
        }
    }

    private fun registerEventListener() {
        if (eventListener == null) {
            eventListener = WidgetEventListener()
        }
        InformationBus.registerEventListener(eventListener)
    }

    //Called when back key is pressed in home scene and this widget is active
    fun onBackPressed() {
        verticalGridView?.apply {
            if (selectedPosition > 0) {
                if (!listener.isRegionSupported()) {
                    detailsContainer?.visibility = View.GONE
                    customDetailsFastHome?.visibility = View.GONE
                }

                scrollToPosition(0)
                startAnimation(
                    AlphaAnimation(0f, 1f).apply {
                        duration = 700
                    }
                )
            }
            clearFocus()
        }

        fastHomeData?.show(onShow = onShowPromotion, false)
        listener.requestFocusOnTopCategories()
    }

    override fun dispose() {
        view = null
        context = null
        noEventsMsg = null
        eventListener = null
        unregisterEventListener()
        railAdapter!!.dispose()

        railAdapter = null
        configParam = null
        verticalGridView = null

        if (itemsForAdapter.isNotEmpty()) {
            itemsForAdapter.clear()
        }
        super.dispose()
    }

    private fun unregisterEventListener() {
        if (eventListener != null) {
            InformationBus.unregisterEventListener(eventListener!!)
            eventListener = null
        }
    }

    override fun refresh(data: Any) {
        itemsForAdapter.clear()

        if ((data as ArrayList<*>).size > 0) {
            data.forEach { item ->
                (item as? RailItem)?.let {
                    //if item rail size will be empty then it would make problem in adapter
                    // and it wold show empty rails with on now title in for you
                if (it.rail?.isNotEmpty() == true) itemsForAdapter.add(it)
                }
            }
            updateRailList()
        }
        //sort rails
        if (itemsForAdapter.isNotEmpty()) {
            ReferenceApplication.runOnUiThread {

                itemsForAdapter =
                    itemsForAdapter.sortedWith(compareBy { it.id })
                        .toMutableList() as ArrayList<RailItem>
                railAdapter!!.refresh(itemsForAdapter)
                noEventsMsg!!.visibility = View.GONE
                hideLoading()
            }
        } else {
            verticalGridView?.setBackgroundColor(0)
            //updating rail after deleting last item in for you
            railAdapter?.refresh(itemsForAdapter)
            if (listener.isRegionSupported() && !listener.isAnokiServerReachable()) {
                fastErrorInfo.setTextInfo(FastErrorInfo.InfoType.ANOKI_SERVER_DOWN)
                fastErrorInfo.visibility = View.VISIBLE
            } else if (listener.isTosAccepted()){
                noEventsMsg!!.visibility = View.VISIBLE
            }
            hideLoading()
        }

        if (requestFocus && noEventsMsg!!.visibility == View.GONE && (!listener.hasInternet() || !listener.isAnokiServerReachable())) {
            verticalGridView!!.requestFocus()
        } else {
            if (enableFastData) {
                if(!listener.isRegionSupported()) {
                    detailsContainer!!.visibility = View.GONE
                } else {
                    if (fastHomeData?.isPromotionListEmpty() == false) {
                        detailsContainer!!.visibility = View.VISIBLE
                        fastHomeData?.show(onShow =  onShowPromotion, false)
                    }
                }

                customDetailsFastHome!!.visibility = View.GONE // TODO BORIS hide in CustomDetails class?
            }
        }

        super.refresh(data)
    }

    fun update(railItemList: ArrayList<RailItem>) {
        if (railItemList.isNotEmpty()) {
            ReferenceApplication.runOnUiThread {
                var rails =
                    railItemList.sortedWith(compareBy { it.id })
                        .toMutableList() as ArrayList<RailItem>
                itemsForAdapter.clear()
                itemsForAdapter.addAll(rails)
                railAdapter!!.refresh(itemsForAdapter)
                noEventsMsg!!.visibility = View.GONE
                hideLoading()
            }
        }
    }

    /**
     * Hide loading animation
     */
    private fun hideLoading() {
        if (enableFastData && listener.isRegionSupported()) return // wrapped with this check because only if enableFastData == false LoadingPlaceholder can exist

        ReferenceApplication.worldHandler!!.isEnableUserInteraction = true
        try {
            LoadingPlaceholder.hideLoadingPlaceholder(PlaceholderName.FOR_YOU, onHidden = {
                view!!.visibility = View.VISIBLE
            })
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    /**
     * Request focus on the list
     */
    fun requestFocus() {
        if (!listener.isTosAccepted()) {
            fastTosMoreInfo?.requestFocus()
        } else if (enableFastData && fastHomeData?.isPromotionListEmpty() == false) {
            detailsContainer!!.visibility = View.VISIBLE
            fastHomeData?.show(onShow =  onShowPromotion, true)
            customDetailsFastHome!!.visibility = View.GONE // TODO BORIS hide in CustomDetails class?
        } else {
            verticalGridView!!.requestFocus()
        }
    }

    private fun showGradientBackground(backgroundImage: Drawable) {
        valueAnimator?.cancel()
        valueAnimator = AnimationHelper.fadeOutFadeIn(
            listOfViews = listOf(
                broadcastBackgroundView!!
            ),
            onBetweenAnimation = {
                broadcastBackgroundView!!.background = backgroundImage
            },
            toAlpha = 0.1f,
            duration = 1000L
        )
    }

    private fun setupAdapter() {
        railAdapter = RailAdapter(dateTimeFormat)
        railAdapter?.isRailDetailsEnabled = !enableFastData
        railAdapter!!.setListener(object : AdapterListener {

            override fun onScrollDown(position: Int) {
                verticalGridView?.smoothScrollToPosition(position)
                verticalGridView?.layoutManager?.findViewByPosition(position)?.requestFocus()
            }

            override fun onScrollUp(position: Int) {
                if (!listener.isRegionSupported() && position == 0) {
                    detailsContainer?.visibility = View.GONE
                }
                verticalGridView?.smoothScrollToPosition(position)
                verticalGridView?.layoutManager?.findViewByPosition(position)?.requestFocus()
            }

            override fun getCurrentTime(tvChannel: TvChannel): Long {
                return listener.getCurrentTime(tvChannel)
            }

            override fun onBroadcastChannelSelected(tvChannel: TvChannel?, railItem: RailItem) {
                detailsContainer?.visibility = View.GONE
                if (!listener.hasInternet()) {
                    fastErrorInfo.visibility = View.VISIBLE
                }

                if (verticalGridView!!.hasFocus() && !isFocusOnBroadcastRow) {
                    broadcastBackgroundView!!.background = defaultGradientBackground
                }
                isFocusOnBroadcastRow = true
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onEventSelected(
                tvEvent: TvEvent?,
                vodItem: VODItem?,
                parentalRatingDisplayName: String,
                railItem: RailItem,
                currentTime: Long,
                isStillFocused: () -> Boolean
            ) {
                if (verticalGridView!!.hasFocus()) {
                    onDetailsSet.invoke()
                    detailsContainer!!.visibility = View.VISIBLE
                    customDetailsFastHome!!.updateData(
                        tvEvent!!,
                        railItem,
                        parentalRatingDisplayName,
                        currentTime,
                        dateTimeFormat,
                        isEventLocked = listener.isEventLocked(tvEvent)
                    )
                    fastHomeData!!.hide(
                        onAnimationEnded = {
                            if (isStillFocused.invoke()) {
                                valueAnimator?.cancel()
                                valueAnimator = AnimationHelper.fadeInAnimation(
                                    view = customDetailsFastHome!!
                                )
                            }
                        }
                    )

                    if (railItem.railName == ConfigStringsManager.getStringById("on_now")) {
                        showGradientBackground(onNowGradientBackground!!)
                    } else if (railItem.railName == ConfigStringsManager.getStringById("up_next")) {
                        showGradientBackground(upNextGradientBackground!!)
                    } else if (railItem.railName == ConfigStringsManager.getStringById("watchlist")) {
                        showGradientBackground(watchlistGradientBackground!!)
                    } else if (railItem.railName == ConfigStringsManager.getStringById("recorded")) {
                        showGradientBackground(recordedGradientBackground!!)
                    } else if (railItem.railName == ConfigStringsManager.getStringById("scheduled")) {
                        showGradientBackground(scheduledRecordingGradientBackground!!)
                    } else if (railItem.railName == ConfigStringsManager.getStringById("for_you")) {
                        showGradientBackground(forYouGradientBackground!!)
                    } else {
                        showGradientBackground(defaultGradientBackground!!)
                    }
                    isFocusOnBroadcastRow = false
                }
            }

            override fun isEventLocked(tvEvent: TvEvent?) = listener.isEventLocked(tvEvent)

            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                listener.setSpeechText(text = text, importance = importance)
            }

            override fun onKeyUp(onKeyUpFinished: () -> Unit) {
                if (!listener.isTosAccepted()) {
                    fastTosMoreInfo?.visibility = View.VISIBLE
                    fastTosMoreInfo?.setFocusToMoreInfoButton()
                    verticalGridView?.clearFocus()
                    detailsContainer!!.visibility = View.GONE
                } else
                if (enableFastData) {
                    if (fastHomeData!!.isPromotionListEmpty()) {
                        onShowPromotion.invoke()
                        verticalGridView?.clearFocus()
                        detailsContainer!!.visibility = View.GONE
                        listener.requestFocusOnTopCategories()
                        onKeyUpFinished.invoke() // crucial for collapsing the card.
                        return
                    }
                        verticalGridView?.clearFocus()
                        fastHomeData?.show(onShow =  onShowPromotion, true)
                        if (!listener.isRegionSupported()) {
                           AnimationHelper.fadeOutAnimation(
                                view = detailsContainer!!, onAnimationEnded = {
                                   detailsContainer!!.visibility = View.GONE
                               }
                            )
                        } else {
                            customDetailsFastHome!!.visibility = View.GONE
                        }
                        onKeyUpFinished.invoke() // crucial for collapsing the card.
                } else {
                    onKeyUpFinished.invoke() // crucial for collapsing the card.
                    detailsContainer!!.visibility = View.GONE
                    listener.requestFocusOnTopCategories()
                }
            }

            /**
             * opens Details Scene for passed item. Item can be one of those: TvEvent, Recordings, Scheduled Recordings
             */
            override fun onItemClicked(item: Any) {
                if (item is TvEvent) {
                    if (listener.isEventLocked(item)) {
                        // TODO DEJAN - ne sme zap - treba da se prikaze PIN SCENA
//                        return
                    }
                    if (item.tvChannel.isFastChannel()) {
                        FastLiveTabDataProvider.activeGenre = railAdapter!!.items.get(railAdapter!!.selectedItemPosition).railName.lowercase().split(' ')
                            .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
                        listener.onFastCardClicked(item)
                        BackFromPlayback.resetKeyPressedState()
                    } else if (item.tag != null && item.tag is String) {
                        if (item.tag == "guide" || item.tag == "vod" || item.tag == "channel") {
                            listener.onFastCardClicked(item)
                        } else {
                            listener.showDetails(item)
                        }
                    } else {
                        listener.showDetails(item)
                    }
                } else {
                    listener.showDetails(item)
                }
            }

            /**
             * used to check whether Channel with passed id is locked or not.
             */
            override fun isChannelLocked(channelId: Int): Boolean {
                return listener.isChannelLocked(channelId)
            }

            override fun isParentalControlsEnabled(): Boolean {
                return listener.isParentalControlsEnabled()
            }

            override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
                return listener.getParentalRatingDisplayName(parentalRating, tvEvent)
            }
            override fun isInWatchlist(tvEvent: TvEvent): Boolean {
                return listener.isInWatchlist(tvEvent)
            }

            override fun isInRecList(tvEvent: TvEvent): Boolean {
                return listener.isInRecList(tvEvent)
            }

            override fun isRegionSupported(): Boolean {
                return listener.isRegionSupported()
            }
        })

        verticalGridView!!.setNumColumns(1)
        verticalGridView!!.adapter = railAdapter

        //make focus fixed on th top side of the screen
        verticalGridView!!.itemAlignmentOffset =
            context!!.resources.getDimensionPixelSize(R.dimen.custom_dim_0)
        verticalGridView!!.itemAlignmentOffsetPercent =
            VerticalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED
        verticalGridView!!.windowAlignmentOffset = 0
        verticalGridView!!.windowAlignmentOffsetPercent =
            VerticalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
        verticalGridView!!.clipToOutline = true
        verticalGridView!!.windowAlignment = VerticalGridView.WINDOW_ALIGN_NO_EDGE
    }

    /**
     * Show loading animation
     */
    private fun showLoading() {
        ReferenceApplication.worldHandler!!.isEnableUserInteraction = true
        noEventsMsg?.visibility = View.GONE
        fastErrorInfo?.visibility = View.GONE
    }

    /**
     * Gets the channel URI to handle events.
     *
     * isPassThrough true , If appLink intent is available , false appLink intent is not available.
     */
    private fun getUri(isPassThrough: Boolean, channel: TvChannel): Uri? {
        //ToDo need to check whether isPassThrough check needed
        return if (isPassThrough) {
            TvContract.buildChannelUriForPassthroughInput(channel.inputId)
        } else {
            TvContract.buildChannelUri(channel.channelId)
        }
    }

    fun selectedFilterList() {
        if (verticalGridView?.layoutManager!!.findViewByPosition(0) != null) {
            verticalGridView?.layoutManager!!.findViewByPosition(0)!!.requestFocus()
        }
    }

    fun setFocusToGrid() {
        if (!listener.isTosAccepted()) {
            fastTosMoreInfo.setFocusToMoreInfoButton()
        } else if (requestFocus && noEventsMsg!!.visibility == View.GONE) {
            if (enableFastData) {
                requestFocus()
            } else {
                verticalGridView!!.requestFocus()
            }
        }
    }

    fun stopRefreshTimer() {
        fastHomeData?.stopRefreshTimer()
    }

    fun restartTimerIfFocusOnPromotionButton() {
        fastHomeData?.restartTimerIfFocusOnPromotionButton()
    }

    fun restartTimerIfListNotEmpty() {
        fastHomeData?.restartTimerIfListNotEmpty()
        checkNetwork()
    }

    fun checkMutePlayback() {
        listener.muteAudio()
    }


    interface ForYouWidgetListener : GWidgetListener, TTSSetterInterface {
        fun getRails(sceneConfig: SceneConfig?)
        fun requestFocusOnTopCategories()
        fun showDetails(event: Any)
        fun keepFocusOnFilerView()
        fun onItemClicked(recording: Any)
        fun getRailSize(): Int
        fun isChannelLocked(channelId: Int): Boolean
        fun isParentalControlsEnabled():Boolean
        fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
        fun getCurrentTime(tvChannel: TvChannel): Long
        fun isInWatchlist(tvEvent: TvEvent): Boolean
        fun isInRecList(tvEvent: TvEvent): Boolean
        fun getPromotionContent(callback: IAsyncDataCallback<ArrayList<PromotionItem>>)
        fun onFastCardClicked(tvEvent: TvEvent)
        fun promotionItemActionClicked(promotionItem: PromotionItem)
        fun hasInternet(): Boolean
        fun isAnokiServerReachable(): Boolean
        fun muteAudio()
        fun unMuteAudio()
        fun isEventLocked(tvEvent: TvEvent?): Boolean
        fun isRegionSupported(): Boolean
        fun isHomeTabFocused(): Boolean
        fun renameRecording(recording: Recording, name: String, callback: IAsyncCallback)
        fun deleteRecording(recording: Recording, callback: IAsyncCallback)
        fun isTosAccepted():Boolean
    }
}