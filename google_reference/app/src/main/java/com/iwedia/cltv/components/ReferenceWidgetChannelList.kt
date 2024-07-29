package com.iwedia.cltv.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvTrackInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener
import com.google.android.material.card.MaterialCardView
import com.iwedia.cltv.*
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.ReferenceApplication.Companion.runOnUiThread
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.anoki_fast.epg.AnimationHelper
import com.iwedia.cltv.components.custom_card.CustomCardChannelList
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.PIN.PinSceneData
import com.iwedia.cltv.scene.PIN.PinSceneListener
import com.iwedia.cltv.scene.channel_list.*
import com.iwedia.cltv.utils.*
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneManager
import world.widget.custom.channel_list.GTvChannelList
import world.widget.custom.channel_list.GTvChannelListListener


/**
 * ReferenceWidgetChannelList
 *
 * @author Aleksandar Milojevic
 */
@RequiresApi(Build.VERSION_CODES.R)
class ReferenceWidgetChannelList(context: Context, listener: ChannelListWidgetListener) :
    GTvChannelList<ConstraintLayout, GTvChannelListListener>(listener) {
    private var isParentalOn: Boolean = false

    //Current channel position
    private var currentChannelPosition: Int = -1


    var categorioes = mutableListOf<CategoryItem>()

    //Current category position
    private var currentCategoryPosition: Int = 0

    //Channel list container
    var channelListContainer: ConstraintLayout? = null

    //Channel list grid view
    var channelListGridView: VerticalGridView? = null

    //Channel list adapter
    private var channelListAdapter: ChannelListAdapter? = null

    //Channel category grid view
    var channelCategoryGridView: HorizontalGridView? = null

    //Current location in window
    private val currentLocationInWindow = IntArray(2)

    private var eventDetailsContainerValueAnimator: ValueAnimator? = null

    private var isDpadDownEnabled =
        true // used to disable user to go move focus from categories until VerticalGridView for channels is ready to take that focus

    var searchCustomButton: CustomButton? = null

    //Filter view
    var filterCustomButton: CustomButton? = null

    //Event details container
    var eventDetailsContainer: ConstraintLayout? = null

    var isCurrentChannel = false

    //Button container
    var buttonContainer: LinearLayout? = null

    //ChannelListCategoryAdapter
    var channelListCategoryAdapter: CategoryAdapter? = null
    var categoryList = mutableListOf<String>()

    var sortByContainer: LinearLayout? = null

    var catchupButton: CustomButton? = null
    var recordButton: CustomButton? = null
    var addToFavoritesButton: CustomButton? = null

    var parentalrating: TextView? = null

    var customDetails: CustomDetails.CustomDetailsChannelList

    //Channel edit container
    var channelEditContainer: LinearLayout? = null
    var lockButton: CustomButton? = null
    var skipButton: CustomButton? = null
    var deleteButton: CustomButton? = null
    var editChannelButton: CustomButton? = null

    var listVertical = mutableListOf<ChannelListItem>()

    var favoritesOverlay: ConstraintLayout? = null
    private var favoritesGridView: VerticalGridView? = null
    val favoritesListAdapter = MultiCheckListAdapter()

    var hintMessageContainer: ConstraintLayout? = null
    var hintMessageText: TextView? = null

    var pinSceneListener: PinSceneListener? = null

    var isLockedScene = false

    var constraintLayout: ConstraintLayout? = null
    private var onItemSelectedTimer: CountDownTimer? = null
    val TAG = javaClass.simpleName

    var inactivityTimer: SceneInactivityTimer? = null
    var timerTimeout = 60000L

    // Whether edit channel deeplink is opened
    var isPerformingChannelEdit = false

    init {
        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_widget_channel_list, null) as ConstraintLayout

        customDetails = view!!.findViewById(R.id.custom_details)
        val bg = ConfigColorManager.getColor("color_background")
            .replace("#", ConfigColorManager.alfa_light_bg_cl)

        constraintLayout = view!!.findViewById(R.id.channel_list_view_wrapper)
        constraintLayout!!.setBackgroundColor(Color.parseColor(bg))

        val colorStart = Color.parseColor(
            ConfigColorManager.getColor("color_background")
                .replace("#", ConfigColorManager.alfa_zero_per)
        )
        val colorMid = Color.parseColor(
            ConfigColorManager.getColor("color_background")
                .replace("#", ConfigColorManager.alfa_86)
        )
        val colorEnd = Color.parseColor(
            ConfigColorManager.getColor("color_background")
                .replace("#", ConfigColorManager.alfa_hundred_per)
        )

        channelCategoryGridView =
            view!!.findViewById(R.id.channel_category_grid_view)

        channelListCategoryAdapter = CategoryAdapter(
            fadingEdgeLayout = view!!.findViewById(R.id.fading_edge_layout)
        )

        channelListCategoryAdapter!!.isCategoryFocus = true
        searchCustomButton = view!!.findViewById(R.id.search_custom_button)
        searchCustomButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        searchCustomButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        searchCustomButton!!.findViewById<MaterialCardView?>(R.id.custom_button_card_view).apply {
            setCardBackgroundColor(Color.TRANSPARENT)
        }

        filterCustomButton = view!!.findViewById(R.id.filter_custom_button)
        filterCustomButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        filterCustomButton!!.findViewById<MaterialCardView?>(R.id.custom_button_card_view).apply {
            setCardBackgroundColor(Color.TRANSPARENT)
        }

        filterCustomButton!!.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    channelListCategoryAdapter!!.clearPreviousFocus()
                    if (filterCustomButton!!.isAnimationInProgress) return@setOnKeyListener true
                    channelCategoryGridView!!.requestFocus()
                    return@setOnKeyListener true
                }

            }
            false
        }

        channelListContainer =
            view!!.findViewById(R.id.channel_list_container)

        channelListGridView =
            view!!.findViewById(R.id.channel_list_view)
        channelListGridView!!.setNumColumns(1)
        channelListGridView!!.setItemSpacing(Utils.getDimensInPixelSize(R.dimen.custom_dim_3))

        channelListAdapter = ChannelListAdapter()
        channelListGridView!!.adapter = channelListAdapter

        eventDetailsContainer =
            view!!.findViewById(R.id.event_details_container)

        //Init favorites overlay
        favoritesOverlay = view!!.findViewById(R.id.favorites_overlay)
        var drawable3 = GradientDrawable()
        drawable3.setShape(GradientDrawable.RECTANGLE)
        drawable3.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT)
        drawable3.setColors(
            intArrayOf(
                colorStart,
                colorMid,
                colorEnd
            )
        )
        favoritesOverlay!!.setBackground(drawable3)
        favoritesGridView = view!!.findViewById(R.id.favorites_overlay_grid_view)
        initFavoritesOverlay()

        buttonContainer = view!!.findViewById(R.id.event_button_container1)
        parentalrating = view!!.findViewById(R.id.parental_rating1)
        parentalrating!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

        var topFade: View = view!!.findViewById(R.id.bg_top_fade_channel_scene)

        Utils.makeGradient(
            topFade,
            GradientDrawable.LINEAR_GRADIENT,
            GradientDrawable.Orientation.TOP_BOTTOM,
            Color.parseColor(ConfigColorManager.getColor("color_background")),
            Color.parseColor(ConfigColorManager.getColor("color_background")),
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_background"),
                    0.0
                )
            ),
            0.0F,
            0.38F
        )

        customDetails.updateHohImageView(
            listener.isHOH(TvTrackInfo.TYPE_AUDIO)
        )
        customDetails.updateAdImageView(
            listener.isAudioDescription(TvTrackInfo.TYPE_AUDIO)
        )

        val isTeletext = listener.isTeleText(TvTrackInfo.TYPE_SUBTITLE)
        val isTeletextEnabled = listener.getConfigInfo("teletext_enable_column")
        customDetails.updateTtxImageView(isTeletext && isTeletextEnabled)


        //TODO Dummy list for categories, must be deleted after implementation of category handler
        val mList = mutableListOf<CheckListItem>()
        mList.add(CheckListItem(ConfigStringsManager.getStringById("_default"), false))
        mList.add(CheckListItem(ConfigStringsManager.getStringById("name_up"), false))
        mList.add(CheckListItem(ConfigStringsManager.getStringById("name_down"), false))

        hintMessageContainer = view!!.findViewById(R.id.hint_overlay)
        hintMessageText = view!!.findViewById<TextView?>(R.id.hint_overlay_text).apply {
            text = ConfigStringsManager.getStringById("long_press_ok_edit_channel")
            setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
            typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )
        }

        channelCategoryGridView!!.setNumRows(1)
        channelCategoryGridView!!.adapter = channelListCategoryAdapter
//        channelListCategoryAdapter!!.selectedItem = 0

        //access inactivity timer object instanced in main activity
        inactivityTimer = (ReferenceApplication.getActivity() as MainActivity).sceneInactivityTimer

        channelListCategoryAdapter!!.adapterListener =
            object : CategoryAdapter.ChannelListCategoryAdapterListener {
                override fun onItemClicked(position: Int) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    //this method is called to restart inactivity timer for channel list scene
                    (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                    if (position == 0) {
                        currentCategoryPosition = position
                        if (!channelListGridView!!.hasFocus()) {
                            listener.onCategoryChannelClicked(position)
                        }
                    }
                    if(channelCategoryGridView == null){
                        channelListGridView?.requestFocus()
                        return
                    }

                    val holder =
                        channelCategoryGridView!!.findViewHolderForAdapterPosition(
                            position
                        ) as CategoryItemViewHolder
                    channelListCategoryAdapter!!.setActiveFilter(holder)

                    channelListGridView!!.addOnChildAttachStateChangeListener(object :
                        OnChildAttachStateChangeListener {
                        override fun onChildViewAttachedToWindow(view: View) {
                            if (!channelListGridView!!.hasFocus()) {
                                channelListGridView!!.requestFocus()
                            }
                        }

                        override fun onChildViewDetachedFromWindow(view: View) {
                        }
                    })
                }

                override fun getAdapterPosition(position: Int) { // there is no need for using this method in ChannelList, it is equivalent to onItemSelected
                    // if having something to put here, consider putting it in onItemSelected method
                    // when naming method with get...() convention is that that method should have returned value. As this method doesn't have it,
                    // probably it should be removed in the future.
                }

                override fun onItemSelected(position: Int) {
                    stopOnItemSelectedTimer()
                    isDpadDownEnabled = false
                    channelListCategoryAdapter!!.selectedItem = position
                    startOnItemSelectedTimer() {
                        if (currentCategoryPosition != position) {
                            listener.onCategoryChannelClicked(position) // move focus in horizontalGridView on position
                        }

                        currentCategoryPosition = position // remember current position

                        if (eventDetailsContainer!!.visibility == View.VISIBLE) {
                            eventDetailsContainer!!.visibility = View.GONE
                        }

                        if (hintMessageContainer!!.visibility == View.VISIBLE) {
                            hintMessageContainer!!.visibility = View.GONE
                        }
                    }

                    isDpadDownEnabled = true
                }

                override fun digitPressed(digit: Int) {}

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

                override fun onKeyLeft(currentPosition: Int): Boolean {
                    channelListCategoryAdapter!!.clearPreviousFocus()
                    if (currentPosition == 0) {
                        searchCustomButton?.requestFocus()
                        channelListCategoryAdapter!!.setSelected(currentCategoryPosition)
                        return true
                    }
                    return false
                }

                override fun onKeyRight(currentPosition: Int): Boolean {
                    channelListCategoryAdapter!!.clearPreviousFocus()
                    if (currentPosition == channelListCategoryAdapter!!.itemCount - 1) {
                        filterCustomButton!!.requestFocus()
                        channelListCategoryAdapter!!.setSelected(currentCategoryPosition)
                        return true
                    }
                    return false
                }

                override fun onKeyUp(currentPosition: Int): Boolean {
                    return false
                }

                override fun onKeyDown(currentPosition: Int): Boolean {
                    if (channelListContainer?.visibility != View.VISIBLE) return true
                    if (!isDpadDownEnabled) return true // user is not allowed to change focus to channels yet
                    channelListCategoryAdapter?.clearPreviousFocus()
                    val holder =
                        channelCategoryGridView!!.findViewHolderForAdapterPosition(
                            currentCategoryPosition
                        ) as CategoryItemViewHolder
                    channelListCategoryAdapter!!.setActiveFilter(holder)
//                    currentCategoryPosition = currentPosition
                    /*
                    next requestFocus() is crucial for avoiding losing focus when changing it from categories to VerticalGridView in the moment when VerticalGridView is not visible yet.
                    That happens when user presses left or right and after 500 ms but within period when VerticalGrid is not loaded yet, if not having this requestFocus() focus would be lost
                    */
                    channelListGridView!!.postDelayed({
                        channelListGridView!!.requestFocus()
                    }, 100)
                    return false
                }

                override fun onBackPressed(position: Int): Boolean {
                    return false
                }
            }

        searchCustomButton!!.setOnClick {
            //this method is called to restart inactivity timer for no signal power off
            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

            //this method is called to restart inactivity timer for channel list scene
            (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

            listener.onSearchClicked()
        }
        searchCustomButton?.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    channelListCategoryAdapter!!.clearPreviousFocus()
                }
            }
            false
        }

        if (ReferenceApplication.shouldShowStartOverButton) {
            catchupButton = view!!.findViewById<CustomButton?>(R.id.start_over_button).apply {
                visibility = View.VISIBLE
            }
            catchupButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
            catchupButton!!.setOnKeyListener(object : View.OnKeyListener {
                override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                    if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            if (catchupButton!!.isAnimationInProgress) return true
                            if (currentChannelPosition > 0) {
                                catchupButton!!.clearFocus()
                                eventDetailsContainer!!.visibility = View.INVISIBLE
                                channelListGridView!!.scrollToPosition(currentChannelPosition)
                                val holder =
                                    channelListGridView!!.findViewHolderForAdapterPosition(
                                        currentChannelPosition
                                    ) as ChannelListViewHolder
                                holder.requestFocus()
                            } else {
                                return true
                            }
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            if (catchupButton!!.isAnimationInProgress) return true
                            if (currentChannelPosition < channelListAdapter!!.itemCount - 1) {
                                catchupButton!!.clearFocus()
                                eventDetailsContainer!!.visibility = View.INVISIBLE
                                channelListGridView!!.scrollToPosition(currentChannelPosition)
                                val holder =
                                    channelListGridView!!.findViewHolderForAdapterPosition(
                                        currentChannelPosition
                                    )
                                if (holder != null) {
                                    (holder as ChannelListViewHolder).requestFocus()
                                }
                            } else {
                                return true
                            }
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            if (catchupButton!!.isAnimationInProgress) return true
                            val holder =
                                channelListGridView!!.findViewHolderForAdapterPosition(
                                    currentChannelPosition
                                )
                            if (holder != null) {
                                (holder as ChannelListViewHolder).requestFocus()
                                hideChannelInformation()
                            }
                            return true
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            if (catchupButton!!.isAnimationInProgress) return true
                            recordButton?.let {
                                it.requestFocus()
                                return true
                            }
                            addToFavoritesButton?.let {
                                it.requestFocus()
                                return true
                            }
                            return true
                        }
                        return false
                    }
                    if (keyEvent.action == KeyEvent.ACTION_UP) {
                        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                            if(catchupButton!!.isAnimationInProgress) return true
                            hideChannelInformation()
                            channelListGridView!!.requestFocus()
                            return true
                        }
                    }
                    return false
                }
            })
        }

        if (listener.getConfigInfo("pvr")) {
            recordButton = view!!.findViewById<CustomButton?>(R.id.record_button).apply {
                visibility = View.VISIBLE
            }
            recordButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
            recordButton!!.setOnClick {
                //this method is called to restart inactivity timer for no signal power off
                (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                //this method is called to restart inactivity timer for channel list scene
                (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                when (recordButton!!.getTextLabel()) {
                    ConfigStringsManager.getStringById("record") -> {
                        if (!listener.isUsbStorageAvailable()) {
                            listener.showToast(ConfigStringsManager.getStringById("usb_not_connected_connect_usb_to_record"))
                            return@setOnClick
                        }
                        if (!listener.isUsbWritableReadable()) {
                            listener.showToast(ConfigStringsManager.getStringById("usb_storage_corrupted"))
                            return@setOnClick
                        }
                        if (!listener.isPvrPathSet()) {
                            listener.showToast(ConfigStringsManager.getStringById("please_select_storage_path_for_recording_pvr"))
                            InformationBus.submitEvent(Event(Events.SHOW_DEVICE_INFO))
                            return@setOnClick
                        }

                        if (!listener.isUsbFreeSpaceAvailable()) {
                            listener.showToast(ConfigStringsManager.getStringById("insufficient_disk_space"))
                            return@setOnClick
                        }

                        listVertical[currentChannelPosition].event?.let {
                            listener.onRecordButtonPressed(
                                it
                            )
                        }
                    }

                    ConfigStringsManager.getStringById("cancel") -> {
                        listVertical[currentChannelPosition].event?.let {
                            listener.onRecordButtonPressed(it)
                        }
                    }
                }
            }
            recordButton!!.setOnKeyListener(object : View.OnKeyListener {
                override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                    if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            if (recordButton!!.isAnimationInProgress) return true
                            if (currentChannelPosition > 0) {
                                if (catchupButton != null) {
                                    catchupButton!!.clearFocus()
                                }
                                hideChannelInformation()
                                channelListGridView!!.scrollToPosition(currentChannelPosition)
                                val holder =
                                    channelListGridView!!.findViewHolderForAdapterPosition(
                                        currentChannelPosition
                                    ) as ChannelListViewHolder
                                holder.requestFocus()
                            } else {
                                return true
                            }
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            if (recordButton!!.isAnimationInProgress) return true
                            if (currentChannelPosition < channelListAdapter!!.itemCount - 1) {
                                if (catchupButton != null) {
                                    catchupButton!!.clearFocus()
                                }
                                hideChannelInformation()
                                channelListGridView!!.scrollToPosition(currentChannelPosition)
                                val holder =
                                    channelListGridView!!.findViewHolderForAdapterPosition(
                                        currentChannelPosition
                                    )
                                if (holder != null) {
                                    (holder as ChannelListViewHolder).requestFocus()
                                }
                            } else {
                                return true
                            }
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            if (recordButton!!.isAnimationInProgress) return true
                            addToFavoritesButton?.let {
                                it.requestFocus()
                                return true
                            }
                            return true
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            if (recordButton!!.isAnimationInProgress) return true
                            catchupButton?.let {
                                it.requestFocus()
                                return true
                            }
                            val holder =
                                channelListGridView!!.findViewHolderForAdapterPosition(
                                    currentChannelPosition
                                )
                            if (holder != null) {
                                (holder as ChannelListViewHolder).requestFocus()
                                hideChannelInformation()
                            }
                            return true
                        }
                        if (keyCode == KeyEvent.KEYCODE_CAPTIONS) {
                            if (listener.isClosedCaptionEnabled() == true && isCurrentChannel) {
                                listener.setClosedCaption() //update CC
                                updateCCInfo()
                            }
                            return (isCurrentChannel && listener.isClosedCaptionEnabled() == true)
                        }
                    }
                    if (keyEvent.action == KeyEvent.ACTION_UP) {
                        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                            if(recordButton!!.isAnimationInProgress) return true
                            channelListGridView!!.requestFocus()
                            hideChannelInformation()
                            return true
                        }
                    }
                    return false
                }
            })
        }

        if (ReferenceApplication.shouldShowAddToFavoritesButtonInChannelList) {
            addToFavoritesButton = view!!.findViewById<CustomButton?>(R.id.add_to_favorite_button).apply {
                visibility = View.VISIBLE
            }
            addToFavoritesButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
            addToFavoritesButton!!.setOnClick {
                //this method is called to restart inactivity timer for no signal power off
                (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                //this method is called to restart inactivity timer for channel list scene
                (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                favoritesOverlay?.bringToFront()
                favoritesOverlay?.elevation = 10f
                favoritesOverlay?.visibility = View.VISIBLE
                (favoritesGridView?.adapter as MultiCheckListAdapter).setSelectedItems(
                    listVertical[currentChannelPosition].channel.favListIds
                )
                favoritesGridView?.post {
                    favoritesGridView?.layoutManager?.findViewByPosition(0)?.requestFocus()
                }
                favoritesGridView!!.setItemSpacing(
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_5)
                )

                saveChannelInFav()
            }

            addToFavoritesButton!!.setOnKeyListener(object : View.OnKeyListener {
                override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                    if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {

                        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            if (addToFavoritesButton!!.isAnimationInProgress) return true
                            if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                                recordButton?.let {
                                    it.requestFocus()
                                    return true
                                }
                                catchupButton?.let {
                                    it.requestFocus()
                                    return true
                                }
                                val holder =
                                    channelListGridView!!.findViewHolderForAdapterPosition(
                                        currentChannelPosition
                                    )
                                if (holder != null) {
                                    (holder as ChannelListViewHolder).requestFocus()
                                }
                                return true
                            } else {
                                return true
                            }
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            if (addToFavoritesButton!!.isAnimationInProgress) return true
                            if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                                return true // handle this in future if needed - probably focus should be set to record button here
                            } else {
                                recordButton?.let { // it is possible that record button is not initialized if pvr is disabled.
                                    it.requestFocus()
                                    return true
                                }
                                catchupButton?.let {
                                    it.requestFocus()
                                    return true
                                }
                                val holder =
                                    channelListGridView!!.findViewHolderForAdapterPosition(
                                        currentChannelPosition
                                    )
                                if (holder != null) {
                                    (holder as ChannelListViewHolder).requestFocus()
                                    hideChannelInformation()
                                }
                            }
                        }

                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            if (addToFavoritesButton!!.isAnimationInProgress) return true
                            if (currentChannelPosition > 0) {
                                addToFavoritesButton!!.clearFocus()
                                eventDetailsContainer!!.visibility = View.INVISIBLE
                                channelListGridView!!.scrollToPosition(currentChannelPosition)
                                val holder =
                                    channelListGridView!!.findViewHolderForAdapterPosition(
                                        currentChannelPosition
                                    ) as ChannelListViewHolder
                                holder.requestFocus()
                            } else {
                                return true
                            }
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            if (addToFavoritesButton!!.isAnimationInProgress) return true
                            if (currentChannelPosition < channelListAdapter!!.itemCount - 1) {
                                addToFavoritesButton!!.clearFocus()
                                hideChannelInformation()
                                channelListGridView!!.scrollToPosition(currentChannelPosition)
                                val holder =
                                    channelListGridView!!.findViewHolderForAdapterPosition(
                                        currentChannelPosition
                                    )
                                if (holder != null) {
                                    (holder as ChannelListViewHolder).requestFocus()
                                }
                            } else {
                                return true
                            }
                        }
                        if (keyCode == KeyEvent.KEYCODE_CAPTIONS) {
                            if (listener.isClosedCaptionEnabled() == true && isCurrentChannel) {
                                listener.setClosedCaption() //update CC
                                updateCCInfo()
                            }
                            return (isCurrentChannel && listener.isClosedCaptionEnabled() == true)
                        }
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            downActionBackKeyDone = true
                        }
                    }
                    if (keyEvent.action == KeyEvent.ACTION_UP) {
                        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                            if (addToFavoritesButton!!.isAnimationInProgress) return true
                            if (!downActionBackKeyDone) return true
                            hideChannelInformation()
                            channelListGridView!!.requestFocus()
                            return true
                        }
                    }
                    return false
                }
            })
        }

        channelListAdapter!!.channelListAdapterListener =
            object : ChannelListAdapter.ChannelListAdapterListener {
                override fun getChannelSourceType(tvChannel: TvChannel): String {
                    return listener.getChannelSourceType(tvChannel)
                }

                override fun onItemClick(position: Int) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    //this method is called to restart inactivity timer for channel list scene
                    (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onItemClick: CLICKED POSITION $position")
                    if (worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING) {
                        listener.showRecordingStopPopUp(object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                            }

                            override fun onSuccess() {
                                currentChannelPosition = position
                                listener.channelClicked(listVertical[position].channel)
                            }
                        })

                    } else {
                        currentChannelPosition = position
                        listener.channelClicked(listVertical[position].channel)
                    }

                }

                override fun onItemLongClick(position: Int) {

                    currentChannelPosition = position

                    eventDetailsContainer!!.visibility = View.INVISIBLE

                    setEditOptions(listVertical[currentChannelPosition].channel)
                    channelEditContainer!!.visibility = View.VISIBLE
                    hintMessageContainer!!.visibility = View.GONE

                    if (listener.isParentalEnabled())
                        lockButton?.requestFocus()
                    else {
                        lockButton!!.visibility = View.GONE
                        skipButton?.requestFocus()
                    }
                }

                override fun onItemSelected(position: Int) {
                    if (position == -1 || position >= listVertical.size) {
                        return
                    }

                    currentChannelPosition = position
                    refreshRecordButton()
                    if (LoadingPlaceholder.isCurrentStateShow(PlaceholderName.CHANNEL_LIST) == false) {
                        hintMessageContainer!!.visibility = View.VISIBLE
                    }

                    refreshFavoriteButton()
                    if (eventDetailsContainer!!.isVisible && position == currentChannelPosition) {
                        return
                    }

                    /**
                     * this usedto not trigger before,but now, after we request focus to edit, this is getting triggered and visibility is gone and making focus gone
                     */
                    if(!isPerformingChannelEdit) {
                        eventDetailsContainer!!.visibility = View.INVISIBLE
                        channelEditContainer!!.visibility = View.INVISIBLE
                    } else {
                        isPerformingChannelEdit = false
                    }

                    isCurrentChannel = listVertical[position].isCurrentChannel

                    channelListGridView?.post {
                        updateEventDetailsContainerPosition(position)
                    }

                    refreshTracks()

                    if (listVertical[position].event != null) {
                        buttonContainer!!.visibility = View.VISIBLE
                        eventDetailsContainer!!.layoutParams.height =
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                    }
                }

                override fun onKeyUp(currentPosition: Int): Boolean {
                    if (currentPosition == 0) {
                        if (LoadingPlaceholder.isCurrentStateShow(PlaceholderName.CHANNEL_LIST) == false) {
                            eventDetailsContainer!!.visibility = View.INVISIBLE
                            channelCategoryGridView?.layoutManager?.findViewByPosition(
                                currentCategoryPosition
                            )!!.requestFocus()
                            currentChannelPosition = -1
                            hintMessageContainer!!.visibility = View.GONE
                        }
                        return true
                    }
                    return false
                }

                override fun onKeyRight(): Boolean {
                    hintMessageContainer!!.visibility = View.GONE

                    if (!eventDetailsContainer!!.isVisible) {
                        return true
                    }

                    if (catchupButton != null) {
                        catchupButton!!.requestFocus()
                    } else if (recordButton != null) {
                        recordButton!!.requestFocus()
                    } else if (addToFavoritesButton != null) {
                        addToFavoritesButton!!.requestFocus()
                    } else {
                        return true
                    }
                    channelCategoryGridView!!.isFocusable = false
                    channelCategoryGridView!!.isClickable = false
                    channelCategoryGridView!!.isSelected = false
                    channelCategoryGridView!!.clearFocus()
                    return true
                }

                override fun onCCPressed() {
                    if (listener.isClosedCaptionEnabled() == true) {
                        listener.setClosedCaption()
                        updateCCInfo()
                    }
                }

                override fun onChannelUpPressed() {
                    onChannelUpClicked()
                }

                override fun onChannelDownPressed() {
                    onChannelDownClicked()
                }

                override fun isEventLocked(tvEvent: TvEvent?) = listener.isEventLocked(tvEvent)

                override fun isParentalEnabled() = listener.isParentalEnabled()

                override fun showChannelInformation(position: Int) {
                    eventDetailsContainerValueAnimator?.cancel()
                    eventDetailsContainerValueAnimator =
                        AnimationHelper.fadeInAnimation(eventDetailsContainer!!)
                    val parentalRatingDisplayName: String? = listVertical[position].event?.let {
                        listener.getParentalRatingDisplayName(it.parentalRating, it)
                    }


                    customDetails.updateData(
                        channelListItem = listVertical[position],
                        videoResolution = listener.getVideoResolution(),
                        parentalRatingDisplayName = parentalRatingDisplayName ?: "",
                        currentTime = listener.getCurrentTime(listVertical[position].channel),
                        isCCTrackAvailable = listener.isCCTrackAvailable(),
                        dateTimeFormat = listener.getDateTimeFormat(),
                        isEventLocked = listener.isEventLocked(listVertical[position].event)
                    )

                    refreshTracks()
                    // TODO BORIS* HANDLE THIS INSIDE CustomCardBase class - see todo in CustomCardChannelList at the begining for more information
//                    customDetails.updateDolbyImageView(
//                        listener.isDolby(TvTrackInfo.TYPE_AUDIO)
//                    )
                    listener.setSpeechText(
                        (listVertical[position].event?.name.takeIf { it == "null" }
                            ?: ConfigStringsManager.getStringById("no_information")),
                        (listVertical[position].event?.shortDescription.takeUnless { it == "null" }
                            ?: ConfigStringsManager.getStringById("no_information"))

                    )
                }

                override fun hideChannelInformation() {
                    this@ReferenceWidgetChannelList.hideChannelInformation()
                }

                override fun onBackPressed() {
                    channelCategoryGridView?.layoutManager?.findViewByPosition(
                        currentCategoryPosition
                    )?.requestFocus()
                }

                override fun isScrambled(): Boolean {
                    return listener.isScrambled()
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }
            }

        sortByContainer = view!!.findViewById(R.id.sort_by_container)
        val sortByView: VerticalGridView = view!!.findViewById(R.id.side_view_vertical_grid_view)
        sortByView.setNumColumns(1)
        sortByView!!.setItemSpacing(
            Utils.getDimensInPixelSize(R.dimen.custom_dim_5)
        )

        val sortAdapter = CheckListAdapter(
            fadingEdgeLayout = view!!.findViewById(R.id.sort_by_items_fading_edge_layout),
            fadeAdapterType = FadeAdapter.FadeAdapterType.VERTICAL
        )
        sortByView.adapter = sortAdapter
        var selectedItemPosition: Int

        filterCustomButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        filterCustomButton!!.setOnClick {
            //this method is called to restart inactivity timer for no signal power off
            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

            //this method is called to restart inactivity timer for channel list scene
            (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

            selectedItemPosition = listener.getSelectedSortListPosition()

            mList.forEachIndexed { index, item ->
                item.isChecked =
                    if (index == selectedItemPosition) true
                    else false
            }
            sortAdapter.refresh(mList)

            sortByView.post {
                sortByView.layoutManager!!.scrollToPosition(selectedItemPosition)
                sortByView.requestFocus()
            }

            //delay visibilty because adapter needs time to scroll to the selected position when there are more items
            sortByContainer!!.postDelayed({
                filterCustomButton!!.visibility = View.INVISIBLE
                sortByContainer!!.visibility = View.VISIBLE
                sortByContainer!!.translationZ = 50f
            }, 10)

        }

        Utils.makeGradient(
            view = view!!.findViewById(R.id.sort_by_gradient_view),
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            startColor = Color.parseColor(
                ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)
            ),
            endColor = Color.TRANSPARENT,
            centerX = 0.8f,
            centerY = 0f
        )
        Utils.makeGradient(
            view = view!!.findViewById(R.id.sort_by_gradient_linear_layout),
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            startColor = Color.parseColor(ConfigColorManager.getColor("color_dark")),
            endColor = Color.parseColor(
                ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)
            ),
            centerX = 0.8f,
            centerY = 0f
        )

        val sortByTitle = view!!.findViewById<TextView>(R.id.sort_by_title)
        sortByTitle.setText(ConfigStringsManager.getStringById("sort_by"))
        sortByTitle.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        sortByTitle.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )

        sortByTitle.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        sortAdapter.adapterListener =
            object : CheckListAdapter.CheckListAdapterListener {
                override fun onItemClicked(position: Int) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    //this method is called to restart inactivity timer for channel list scene
                    (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                    selectedItemPosition = position

                    listener.onSelectedSortListPosition(selectedItemPosition)
                    resolveSort(selectedItemPosition)
                }

                override fun onAdditionalItemClicked() {
                    // this is not possible in Channel List. This callback is important in InfoBanner and DetailsScene when disabling Audio or Subtitle.
                }

                override fun onUpPressed(position: Int): Boolean {
                    return false
                }

                override fun onDownPressed(position: Int): Boolean {
                    return false
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

                override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                    listener.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                }

                override fun onBackPressed(): Boolean {
                    filterCustomButton?.visibility = View.VISIBLE
                    return false
                }
            }

        channelEditContainer = view!!.findViewById(R.id.channel_edit_container)
        lockButton = view!!.findViewById(R.id.lock_button)
        skipButton = view!!.findViewById(R.id.skip_button)
        deleteButton = view!!.findViewById(R.id.delete_button)
        editChannelButton = view!!.findViewById(R.id.edit_channel_button)
        if (BuildConfig.FLAVOR.contains("base") || BuildConfig.FLAVOR.contains("mtk")
            || (BuildConfig.FLAVOR.contains("rtk") && (Region.US == listener.getInstalledRegion()))
            || (BuildConfig.FLAVOR.contains("refplus5") && (Region.US == listener.getInstalledRegion()))
        ) {
            deleteButton?.visibility = View.GONE
            editChannelButton!!.visibility = View.GONE
        }
        initChannelEditContainer()

        initializeLoadingPlaceholder(onStartLoading = {
            constraintLayout!!.visibility = View.INVISIBLE
            hintMessageContainer!!.visibility = View.INVISIBLE
        })
    }

    fun refreshCurrentEvent(currentTime: Long) {
        //returns, if user is on channelEdit container, otherwise if refreshed then info and edit container both will collapse
        if (channelEditContainer?.visibility == View.VISIBLE) return

        //if focus is on top filters, search option or filter option then no need to show the event container.
        if (channelCategoryGridView?.hasFocus() == true || searchCustomButton?.hasFocus() == true || filterCustomButton!!.hasFocus()) return

        if (currentChannelPosition >= 0 && currentChannelPosition < listVertical.size) {
            listVertical[currentChannelPosition].event.let { tvEvent ->
                if (tvEvent != null && tvEvent!!.endTime < currentTime) {
                    listener.onCategoryChannelClicked(currentCategoryPosition)
                } else {
                    listVertical[currentChannelPosition].event?.let {
                        (listener as ChannelListWidgetListener).getParentalRatingDisplayName(
                            listVertical[currentChannelPosition].event?.parentalRating,
                            it
                        )
                    }?.let {
                        customDetails.updateData(
                            channelListItem = listVertical[currentChannelPosition],
                            (listener as ChannelListWidgetListener).getVideoResolution(),
                            onDataUpdated = {
                                channelListGridView?.post {
                                    updateEventDetailsContainerPosition(currentChannelPosition)
                                    refreshTracks()
                                }
                            },
                            it,
                            (listener as ChannelListWidgetListener).getCurrentTime(listVertical[currentChannelPosition].channel),
                            (listener as ChannelListWidgetListener).isCCTrackAvailable(),
                            dateTimeFormat = (listener as ChannelListWidgetListener).getDateTimeFormat(),
                            isEventLocked = (listener as ChannelListWidgetListener).isEventLocked(
                                tvEvent
                            )
                        )
                    }
                }
            }
        }
    }

    private fun hideChannelInformation() {
        eventDetailsContainerValueAnimator?.cancel()
        (listener as ChannelListWidgetListener).setSpeechText("") // important to stop speaking information for previously visible event's details. Avoid calling stopSpeech() because that would stop Toast or High importance speech text which is not allowed.
        eventDetailsContainerValueAnimator = AnimationHelper.fadeOutAnimation(eventDetailsContainer!!)
    }
    private fun initializeLoadingPlaceholder(onStartLoading: () -> Unit = {}) {
        LoadingPlaceholder(
            context = ReferenceApplication.applicationContext(),
            placeholderViewId = R.layout.loading_layout_channel_list_main,
            parentConstraintLayout = view!!,
            name = PlaceholderName.CHANNEL_LIST
        )
        LoadingPlaceholder.showLoadingPlaceholder(
            PlaceholderName.CHANNEL_LIST,
            onShown = onStartLoading
        )
    }

    private fun resolveSort(position: Int) {
        if (listVertical.isEmpty()) {
            return
        }
        if (position == 0) {
            var list: MutableList<ChannelListItem> =
                (listener as ChannelListWidgetListener).sortChannelList(listVertical.toMutableList())
                    .toMutableList()
            listVertical.clear()
            listVertical.addAll(list)
            channelListAdapter!!.refresh(listVertical)
        }
        if (position == 1) {
            var list =
                listVertical.sortedWith(compareBy { it.channel.name.toLowerCase() }) as MutableList<ChannelListItem>
            listVertical.clear()
            listVertical.addAll(list)
            channelListAdapter!!.refresh(listVertical)
        }
        if (position == 2) {
            var list =
                listVertical.sortedByDescending { it.channel.name.toLowerCase() } as MutableList<ChannelListItem>
            listVertical.clear()
            listVertical.addAll(list)
            channelListAdapter!!.refresh(listVertical)
        }
    }

    companion object {
        var pinSuccess = 0
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun initChannelEditContainer() {

        if (lockButton!!.visibility != View.GONE) {
            lockButton!!.setOnClick {
                //this method is called to restart inactivity timer for no signal power off
                (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                //this method is called to restart inactivity timer for channel list scene
                (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                var tvChannel = listVertical[currentChannelPosition].channel
                isLockedScene = true

                PinHelper.setPinResultCallback(object : PinHelper.PinCallback {
                    override fun pinCorrect() {
                        Handler().post {
                            runOnUiThread {
                                var lock =
                                    lockButton!!.getTextLabel() == ConfigStringsManager.getStringById(
                                        "lock"
                                    )

                                (listener as ChannelListWidgetListener).lockUnlockChannel(
                                    tvChannel,
                                    lock,
                                    object : IAsyncCallback {
                                        override fun onFailed(error: Error) {}

                                        override fun onSuccess() {
                                            if (lock) {
                                                lockButton!!.setTextLabel(
                                                    ConfigStringsManager.getStringById(
                                                        "unlock"
                                                    )
                                                )
                                            } else {
                                                lockButton!!.setTextLabel(
                                                    ConfigStringsManager.getStringById(
                                                        "lock"
                                                    )
                                                )
                                            }
                                            listVertical[currentChannelPosition].channel.isLocked =
                                                lock
                                            channelListAdapter!!.updateLockSkipIcons(
                                                currentChannelPosition,
                                                channelListGridView?.findViewHolderForAdapterPosition(
                                                    currentChannelPosition
                                                ) as ChannelListViewHolder
                                            )
                                        }

                                    }
                                )
                            }
                        }
                    }

                    override fun pinIncorrect() {}
                })

                val title = ConfigStringsManager.getStringById("enter_parental_settings")
                PinHelper.startPinActivity(title, "")
            }

            lockButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener as ChannelListWidgetListener)
            lockButton!!.setOnKeyListener(object : View.OnKeyListener {
                override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                    if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            if (lockButton!!.isAnimationInProgress) return true
                            skipButton!!.requestFocus()
                            lockButton!!.clearFocus()
                            return true
                        }

                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            if (lockButton!!.isAnimationInProgress) return true
                            channelListGridView!!.requestFocus()
                            channelEditContainer!!.visibility = View.INVISIBLE
                            return true
                        }

                        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            if (lockButton!!.isAnimationInProgress) return true
                            onUpDownFromEditContainer(keyCode)
                            return true
                        }
                        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                            downActionBackKeyDone = true
                        }
                    }
                    if (keyEvent.action == KeyEvent.ACTION_UP) {
                        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                            if (!downActionBackKeyDone) return true
                            if (lockButton!!.isAnimationInProgress) return true
                            channelListGridView!!.requestFocus()
                            channelEditContainer?.visibility = View.INVISIBLE
                            return true
                        }
                    }
                    return false
                }
            })
        }

        skipButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener as ChannelListWidgetListener)
        skipButton!!.setOnClick {
            //this method is called to restart inactivity timer for no signal power off
            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

            //this method is called to restart inactivity timer for channel list scene
            (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

            var tvChannel = listVertical[currentChannelPosition].channel
            val skip = skipButton!!.getTextLabel() == ConfigStringsManager.getStringById("skip")
            if (((listener as ChannelListWidgetListener).skipUnskipChannel(tvChannel, skip))) {
                if (skip) {
                    skipButton!!.setTextLabel(ConfigStringsManager.getStringById("not_skip"))
                } else {
                    skipButton!!.setTextLabel(ConfigStringsManager.getStringById("skip"))
                }
                listVertical[currentChannelPosition].channel.isSkipped = skip
                channelListAdapter!!.updateLockSkipIcons(
                    currentChannelPosition,
                    channelListGridView?.findViewHolderForAdapterPosition(currentChannelPosition) as ChannelListViewHolder
                )
            }
        }

        skipButton!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (skipButton!!.isAnimationInProgress) return true
                        if (deleteButton!!.visibility == View.VISIBLE) {
                            deleteButton!!.requestFocus()
                        } else {
                            if (editChannelButton!!.visibility == View.VISIBLE) {
                                editChannelButton!!.requestFocus()
                            }
                        }
                        return true
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (skipButton!!.isAnimationInProgress) return true
                        if (lockButton!!.visibility == View.GONE) {
                            channelListGridView!!.requestFocus()
                            channelEditContainer!!.visibility = View.INVISIBLE
                        } else {
                            lockButton!!.requestFocus()
                        }
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (skipButton!!.isAnimationInProgress) return true
                        onUpDownFromEditContainer(keyCode)
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                        downActionBackKeyDone = true
                    }
                }
                if (keyEvent.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                        if (!downActionBackKeyDone) return true
                        if (skipButton!!.isAnimationInProgress) return true
                        channelListGridView!!.requestFocus()
                        channelEditContainer!!.visibility = View.INVISIBLE
                        return true
                    }
                }
                return false
            }
        })

        editChannelButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener as ChannelListWidgetListener)
        editChannelButton!!.setOnClick {
            channelListContainer?.visibility = View.INVISIBLE
            channelEditContainer!!.visibility = View.INVISIBLE
            isPerformingChannelEdit = true
            (listener as ChannelListWidgetListener).onClickEditChannel()
        }

        editChannelButton!!.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (editChannelButton!!.isAnimationInProgress) return@setOnKeyListener true
                    if (deleteButton!!.visibility == View.VISIBLE) {
                        deleteButton!!.requestFocus()
                    } else if (skipButton!!.visibility == View.VISIBLE) {
                        skipButton!!.requestFocus()
                    } else if (lockButton!!.visibility == View.VISIBLE) {
                        lockButton!!.requestFocus()
                    } else {
                        channelListGridView!!.requestFocus()
                        channelEditContainer!!.visibility = View.INVISIBLE
                    }
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (editChannelButton!!.isAnimationInProgress) return@setOnKeyListener true
                    onUpDownFromEditContainer(keyCode)
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    downActionBackKeyDone = true
                }
            }

            if (keyEvent.action == KeyEvent.ACTION_UP) {
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    if (!downActionBackKeyDone) return@setOnKeyListener true

                    if (editChannelButton!!.isAnimationInProgress) return@setOnKeyListener true
                    channelListGridView!!.requestFocus()
                    channelEditContainer!!.visibility = View.INVISIBLE
                    return@setOnKeyListener true
                }
            }

            return@setOnKeyListener false
        }

        deleteButton!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener as ChannelListWidgetListener)

        //Delete button setup
        deleteButton!!.setOnClick { onDeleteButtonClicked() }

        deleteButton!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (deleteButton!!.isAnimationInProgress) return true
                        if (editChannelButton!!.isVisible) {
                            editChannelButton!!.requestFocus()
                        }
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (deleteButton!!.isAnimationInProgress) return true
                        deleteButton!!.clearFocus()
                        skipButton!!.requestFocus()
                        return true
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (deleteButton!!.isAnimationInProgress) return true
                        onUpDownFromEditContainer(keyCode)
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                        downActionBackKeyDone = true
                    }
                }
                if (keyEvent.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                        if (!downActionBackKeyDone) return true
                        if (deleteButton!!.isAnimationInProgress) return true
                        channelListGridView!!.requestFocus()
                        channelEditContainer!!.visibility = View.INVISIBLE
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun onUpDownFromEditContainer(direction: Int) {
        channelListGridView!!.requestFocus()
        channelEditContainer!!.visibility = View.INVISIBLE
        if (direction == KeyEvent.KEYCODE_DPAD_DOWN) {
            channelListGridView!!.scrollToPosition(currentChannelPosition + 1)
        } else if (direction == KeyEvent.KEYCODE_DPAD_UP) {
            channelListGridView!!.scrollToPosition(currentChannelPosition - 1)
        }
    }

    private fun setEditOptions(tvChannel: TvChannel) {
        if (tvChannel.isLocked) {
            lockButton!!.setTextLabel(ConfigStringsManager.getStringById("unlock"))
        } else {
            lockButton!!.setTextLabel(ConfigStringsManager.getStringById("lock"))
        }

        if (tvChannel.isSkipped) {
            skipButton!!.setTextLabel(ConfigStringsManager.getStringById("not_skip"))
        } else {
            skipButton!!.setTextLabel(ConfigStringsManager.getStringById("skip"))
        }
    }

    private fun updateEventDetailsContainerPosition(position: Int) {
        if (currentChannelPosition == -1) {
            return
        }

        //Refresh favorite button text
        refreshFavoriteButton()
        LoadingPlaceholder.hideLoadingPlaceholder(PlaceholderName.CHANNEL_LIST, onHidden = {
            constraintLayout?.visibility = View.VISIBLE
            hintMessageContainer?.visibility = View.VISIBLE
        })
    }

    //Prevent scrolling to current playing channel on changing filter
    var firstRefresh = false

    override fun refresh(data: Any) {
        if (data is ArrayList<*>) {
            if (data[0] is Category) {
                val list = mutableListOf<CategoryItem>()
                categoryList.clear()
                categorioes = list
                var indexToFocus = 0
                var activeCategoryName = (listener as ChannelListWidgetListener).getActiveCategory()
                data.forEachIndexed { index, any ->
                    if (any is Category && (any.name!! !in mutableListOf(
                            "TV Channels",
                            "Tuner(DTV)"
                        ))
                    ) {
                        categoryList.add(any.name!!)
                        list.add(CategoryItem(index, any.name!!))
                        if (any.name!! == activeCategoryName) {
                            indexToFocus = index
                        }
                    }
                }

                channelListCategoryAdapter!!.refresh(list)
            }
        } else if (data is MutableList<*>) {
            val previousSelectedItem = listVertical.getOrNull(currentChannelPosition)

            listVertical = mutableListOf()

            data.forEach { item ->
                var added = false
                listVertical.forEach { item2 ->
                    if (item2.channel.channelId == (item as ChannelListItem).channel.channelId) {
                        added = true
                    }
                }
                if (!added) {
                    listVertical.add(item as ChannelListItem)
                }
            }
            resolveSort((listener as ChannelListWidgetListener).getSelectedSortListPosition())

            var list = mutableListOf<ChannelListItem>()
            list.addAll(listVertical)

            // To instantly open channel edit menu while returning back from channel edit deeplink
            if (isPerformingChannelEdit) {
                var foundSameChannel = false
                for (channelListItem in listVertical) {
                    if (previousSelectedItem!!.channel.channelId == channelListItem.channel.channelId) {
                        currentChannelPosition = listVertical.indexOf(channelListItem)
                        foundSameChannel = true
                        break
                    }
                }

                if (currentChannelPosition > list.lastIndex) currentChannelPosition = list.lastIndex

                channelListAdapter?.refresh(list)
                channelListGridView?.adapter = channelListAdapter
                channelListGridView?.setSelectedPosition(currentChannelPosition, 1)
                channelListGridView?.selectedPosition = currentChannelPosition
                channelListContainer?.visibility = View.VISIBLE
                channelListGridView?.post {
                    channelListGridView?.layoutManager?.findViewByPosition(currentChannelPosition)
                        ?.let {
                            if (foundSameChannel) {
                                // To open edit menu only if same channel is found
                                ((it as ViewGroup).getChildAt(0) as CustomCardChannelList).apply {
                                    updateCardToSelectedState()
                                }
                                channelListAdapter!!.channelListAdapterListener!!.onItemLongClick(
                                    currentChannelPosition
                                )
                                editChannelButton?.requestFocus()
                            } else {
                                // To Focus on channel item
                                it.requestFocus()
                            }
                        }
                }
                return
            }

            channelListContainer?.visibility = View.INVISIBLE
            channelListAdapter?.refresh(list)
            channelListGridView?.postDelayed({
                channelListContainer?.visibility = View.VISIBLE
                run exitForEach@{
                    listVertical.forEachIndexed { index, channelListItem ->
                        if (channelListItem.isCurrentChannel) {
                            channelListGridView?.scrollToPosition(index)
                            if (!firstRefresh) {
                                channelListGridView?.layoutManager?.findViewByPosition(index)?.let {
                                    it.requestFocus()
                                }
                            }
                            firstRefresh = true
                            return@exitForEach
                        }
                    }
                }
            }, 500)
        } else if (data is ChannelListItem) {
            channelListGridView?.post(Runnable {
                channelListAdapter?.update(data)
            })
        }

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "refresh: refreshRecordButton")
        refreshRecordButton()
    }

    override fun dispose() {
        (ReferenceApplication.getActivity() as MainActivity).stopSceneInactivityTimer()

        channelListAdapter?.dispose()
        channelListCategoryAdapter?.dispose()
        favoritesListAdapter.dispose()
        channelListCategoryAdapter?.dispose()
        channelListContainer = null
        channelListGridView = null
        channelCategoryGridView = null
        channelListAdapter = null
        channelListCategoryAdapter = null

        if (categoryList.isNotEmpty()) {
            categoryList.clear()
        }
        if (listVertical.isNotEmpty()) {
            listVertical.clear()
        }
        if (categorioes.isNotEmpty()) {
            categorioes.clear()
        }
        firstRefresh = false
        super.dispose()
    }


    fun onChannelUpClicked() {
        var indexToFocus = currentChannelPosition - 5
        if (indexToFocus < 0) {
            indexToFocus = 0
        }

        channelListGridView!!.scrollToPosition(indexToFocus)

    }

    fun onChannelDownClicked() {
        var indexToFocus = currentChannelPosition + 5
        if (indexToFocus > channelListAdapter!!.itemCount - 1) {
            indexToFocus = channelListAdapter!!.itemCount - 1
        }

        channelListGridView!!.scrollToPosition(indexToFocus)
    }

    fun refreshTracks() {

        runOnUiThread(Runnable {
            val tvChannel: TvChannel = (listener as ChannelListWidgetListener).getActiveChannel()
            customDetails.updateVideoQuality(
                tvChannel,
                (listener as ChannelListWidgetListener).getVideoResolution()
            )

            customDetails.updateDolbyImageView(
                (listener as ChannelListWidgetListener).isDolby(TvTrackInfo.TYPE_AUDIO)
            )

            customDetails.updateHohImageView(
                (listener as ChannelListWidgetListener).isHOH(TvTrackInfo.TYPE_SUBTITLE) || (listener as ChannelListWidgetListener).isHOH(TvTrackInfo.TYPE_AUDIO)
            )

            val isTeletext =
                (listener as ChannelListWidgetListener).isTeleText(TvTrackInfo.TYPE_SUBTITLE)
            val isTeletextEnabled =
                (listener as ChannelListWidgetListener).getConfigInfo("teletext_enable_column")
            customDetails.updateTtxImageView(isTeletext && isTeletextEnabled)

            customDetails.updateAudioTracks(
                (listener as ChannelListWidgetListener).getAvailableAudioTracks(),
                (listener as ChannelListWidgetListener).getCurrentAudioTrack()
            )

            customDetails.updateSubtitleTracks(
                (listener as ChannelListWidgetListener).getAvailableSubtitleTracks(),
                (listener as ChannelListWidgetListener).getCurrentSubtitleTrack(),
                (listener as ChannelListWidgetListener).isSubtitlesEnabled(),

            )

            customDetails.updateAdImageView(
                (listener as ChannelListWidgetListener).isAudioDescription(TvTrackInfo.TYPE_AUDIO)
            )

            customDetails.updateAudioChannelInfo(
                (listener as ChannelListWidgetListener).getAudioChannelInfo(TvTrackInfo.TYPE_AUDIO)
            )
            customDetails.updateAudioChannelFormatInfo(
                (listener as ChannelListWidgetListener).getAudioFormatInfo()
            )

            if (tvChannel.tunerType == TunerType.ANALOG_TUNER_TYPE) {
                // getAudioChannelInfo(TvTrackInfo.TYPE_AUDIO) does not always return the correct track name for fluki so we need to use direct access
                if ((listener as ChannelListWidgetListener).getCurrentAudioTrack()?.trackName != null) {
                    customDetails.updateAudioChannelInfo(
                        (listener as ChannelListWidgetListener).getCurrentAudioTrack()?.trackName!!
                    )
                }
            }

            updateCCInfo()
        })
    }

    /**
     * Refresh favorite button
     */
    fun refreshFavoriteButton(channelList: MutableList<TvChannel> = mutableListOf()) {
        if (channelList.isNotEmpty()) {
            listVertical.forEachIndexed{ index, channelListItem ->
                var channel = channelList.find { it.channelId == channelListItem.channel.channelId }
                if (channel != null) {
                    listVertical[index].channel = channel
                }
            }
        }

        if (currentChannelPosition == -1 || listVertical.size == 0 || currentChannelPosition >= listVertical.size) {
            return
        }
        addToFavoritesButton?.let {
            if (listVertical[currentChannelPosition].channel.favListIds.isNotEmpty()) {
                var numberOfEmpty = 0
                listVertical[currentChannelPosition].channel.favListIds.forEach {
                    if (it == "")
                        numberOfEmpty++
                }
                if (numberOfEmpty == listVertical[currentChannelPosition].channel.favListIds.size) {
                    it.setTextLabel(ConfigStringsManager.getStringById("add_to_favorites"))
                } else {
                    it.setTextLabel(ConfigStringsManager.getStringById("edit_favorites"))
                }
            } else {
                it.setTextLabel(ConfigStringsManager.getStringById("add_to_favorites"))
            }
        }
    }

    fun refreshCategoryList(data: ArrayList<Category>) {
        val list = mutableListOf<CategoryItem>()
        var selectedCategoryItem: String? = null
        if (channelListCategoryAdapter!!.selectedItem < data.size) {
            if(channelListCategoryAdapter!!.selectedItem < 0){
                channelListCategoryAdapter!!.selectedItem = 0
            }
            selectedCategoryItem = categoryList[channelListCategoryAdapter!!.selectedItem]
        }

        categoryList.clear()
        categorioes = list
        var indexToFocus = 0

        data.forEachIndexed { index, any ->
            if (any is Category && (any.name!! !in mutableListOf(
                    "TV Channels",
                    "Tuner(DTV)"
                ))
            ) {
                categoryList.add(any.name!!)
                list.add(CategoryItem(index, any.name!!))
                if (any.name!! == selectedCategoryItem) {
                    indexToFocus = index
                    currentCategoryPosition = index
                }
            }
        }
        var activeCategoryDeleted = true
        list.forEach { item ->
            if (selectedCategoryItem == item.name) {
                activeCategoryDeleted = false
            }
        }
        channelListCategoryAdapter!!.selectedItem = indexToFocus
        channelListCategoryAdapter!!.refresh(list)
        if (activeCategoryDeleted) {
            isPerformingChannelEdit = false
            channelCategoryGridView!!.post {
                channelCategoryGridView?.layoutManager?.findViewByPosition(indexToFocus)
                    ?.requestFocus()
                channelCategoryGridView?.layoutManager?.findViewByPosition(indexToFocus)
                    ?.callOnClick()
            }
        } else if (isPerformingChannelEdit) {
            //To instantly reload channel list after opening channel edit deeplink
            listener.onCategoryChannelClicked(indexToFocus)
        }
    }

    /**
     * Get active category name
     *
     * @return active category name
     */
    fun getActiveCategoryName(): String {
        return categoryList[channelListCategoryAdapter!!.selectedItem]
    }

    fun updateCCInfo() {
        if ((listener as ChannelListWidgetListener).isClosedCaptionEnabled() != true) return
        val ccText = (listener as ChannelListWidgetListener).getClosedCaption()
        val isCCTrackAvailable = (listener as ChannelListWidgetListener).isCCTrackAvailable()
        customDetails.updateCcInfo(ccText, isCCTrackAvailable)
    }


    /**
     * Init favorites overlay
     */
    private fun initFavoritesOverlay() {
        val favoriteContainerTitle = view!!.findViewById<TextView>(R.id.favorites_overlay_title)
        favoriteContainerTitle!!.text = ConfigStringsManager.getStringById("add_to")
        favoriteContainerTitle.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        favoriteContainerTitle.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )

        Utils.makeGradient(
            view = view!!.findViewById(R.id.favorites_gradient_view),
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            startColor = Color.parseColor(
                ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)
            ),
            endColor = Color.TRANSPARENT,
            centerX = 0.8f,
            centerY = 0f
        )
        Utils.makeGradient(
            view = view!!.findViewById(R.id.favorites_linear_layout),
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            startColor = Color.parseColor(ConfigColorManager.getColor("color_dark")),
            endColor = Color.parseColor(
                ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)
            ),
            centerX = 0.8f,
            centerY = 0f
        )

        val favoritesItems = arrayListOf<String>()
        (listener as ChannelListWidgetListener).getFavoriteCategories(object :
            IAsyncDataCallback<ArrayList<String>> {
            override fun onFailed(error: Error) {
                favoritesListAdapter.refresh(favoritesItems)
            }

            override fun onReceive(data: ArrayList<String>) {
                favoritesItems.addAll(data)
                favoritesListAdapter.refresh(favoritesItems)
            }
        })
        favoritesGridView!!.selectedPosition = 0
        favoritesGridView!!.setNumColumns(1)
        favoritesGridView!!.preserveFocusAfterLayout = true
        favoritesGridView!!.adapter = favoritesListAdapter

        favoritesGridView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == 0) {
                    val position = favoritesListAdapter.focusedItem
                    favoritesGridView!!.layoutManager!!.findViewByPosition(
                        position
                    )?.requestFocus()
                }
            }
        })

        favoritesListAdapter.adapterListener =
            object : MultiCheckListAdapter.MultiCheckListAdapterListener {
                override fun onItemClicked(button: String, callback: IAsyncCallback) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    //this method is called to restart inactivity timer for channel list scene
                    (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                    callback.onSuccess()
                }

                override fun onKeyUp(position: Int): Boolean {
                    return false
                }

                override fun onKeyDown(position: Int): Boolean {
                    return false
                }

                override fun onKeyRight(position: Int): Boolean {
                    return true
                }

                override fun onKeyLeft(position: Int): Boolean {
                    return true
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    (listener as ChannelListWidgetListener).setSpeechText(text = text, importance = importance)
                }

                override fun onBackPressed(position: Int): Boolean {
                    saveChannelInFav()
                    favoritesGridView?.scrollToPosition(0)
                    favoritesOverlay?.visibility = View.GONE
                    runOnUiThread {
                        refreshFavoriteButton()
                        addToFavoritesButton!!.requestFocus()
                    }

                    return true
                }
            }
    }

    fun saveChannelInFav() {
        val selectedFavListItems = ArrayList<String>()
        selectedFavListItems.addAll(favoritesListAdapter.getSelectedItems())
        (listener as ChannelListWidgetListener).onAddFavoritesClicked(
            listVertical[currentChannelPosition].channel,
            selectedFavListItems
        )
    }

    fun blockRecordButton() {
        recordButton!!.setButtonClickable(false)
    }

    fun unblockRecordButton() {
        recordButton!!.setButtonClickable(true)
    }

    fun refreshRecordButton() {
        if (currentChannelPosition == -1) {
            return
        }
        if (recordButton != null) {
            var tvChannelReference: TvChannel?
            (listener as ChannelListWidgetListener).getRecordingInProgress(object :
                IAsyncDataCallback<com.iwedia.cltv.platform.model.recording.RecordingInProgress> {
                override fun onFailed(error: Error) {}

                override fun onReceive(data: com.iwedia.cltv.platform.model.recording.RecordingInProgress) {
                    tvChannelReference = data.tvChannel
                    //array out of bound exception could come, so using try catch.
                    try {
                        if ((listVertical[currentChannelPosition].channel.name) == (tvChannelReference?.name)) {
                            recordButton!!.setTextLabel(ConfigStringsManager.getStringById("cancel"))
                        } else {
                            recordButton!!.setTextLabel(ConfigStringsManager.getStringById("record"))
                        }
                    } catch (e: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "error:: $e")
                    }
                }

            })

        }
    }

    fun onActiveChannelChanged(activeTvChannel: TvChannel) {
        listVertical.forEach {
            if (it.channel == activeTvChannel) {
                it.isCurrentChannel = true
                channelListAdapter?.update(it)
            }
        }
    }

    fun deleteChannel(channelItem: ChannelListItem) {
        if ((listener as ChannelListWidgetListener).deleteChannel(channelItem.channel)) {
            listVertical.remove(channelItem)
            (listener as ChannelListWidgetListener).addDeletedChannel(channelItem.channel)
            channelListAdapter?.removeElement(currentChannelPosition)
            eventDetailsContainer!!.visibility = View.GONE
            val allChannelList = (listener as ChannelListWidgetListener).getChannelList()
            if (allChannelList.isEmpty()) {
                (listener as ChannelListWidgetListener).onChannelListEmpty()
            } else {
                channelEditContainer!!.visibility = View.INVISIBLE
                if (listVertical.isEmpty()) {

                    categorioes.removeAt(currentCategoryPosition)
                    channelListCategoryAdapter!!.clearFocus(currentCategoryPosition)
                    channelListCategoryAdapter!!.selectedItem = 0

                    currentCategoryPosition = 0
                    channelListCategoryAdapter!!.refresh(categorioes)
                    channelCategoryGridView!!.post {
                        channelCategoryGridView?.layoutManager?.findViewByPosition(0)
                            ?.requestFocus()
                        channelCategoryGridView?.layoutManager?.findViewByPosition(0)
                            ?.callOnClick()

                    }
                } else {
                    if (channelItem.isCurrentChannel) {
                        channelListAdapter!!.apply {
                            if (currentChannelPosition > itemCount - 1) {
                                currentChannelPosition = itemCount - 1
                            }
                        }
                    }

                        channelListAdapter?.refresh(listVertical)
                        channelListGridView!!.postDelayed({
                            channelListGridView!!.requestFocus()
                        }, 300)
                    }

                if (channelItem.isCurrentChannel) {
                    (listener as ChannelListWidgetListener).onActiveChannelDeleted()
                }
            }
        }
    }

    fun setActiveCategory(index: Int) {
        channelCategoryGridView?.postDelayed(Runnable {
            channelCategoryGridView?.scrollToPosition(index)
            channelCategoryGridView?.post {
                channelCategoryGridView?.layoutManager?.findViewByPosition(currentCategoryPosition)
                    ?.clearFocus()
                channelCategoryGridView?.layoutManager?.findViewByPosition(index)?.callOnClick()

                currentCategoryPosition = index
                channelListCategoryAdapter?.clearPreviousFocus()
                if (channelCategoryGridView?.findViewHolderForAdapterPosition(index) != null) {
                    val holder =
                        channelCategoryGridView?.findViewHolderForAdapterPosition(
                            index
                        ) as CategoryItemViewHolder
                    channelListCategoryAdapter?.setActiveFilter(holder)
                }

                if (index != 0) {
                    listener.onCategoryChannelClicked(index)
                }
            }
        }, 500)
    }

    private fun startOnItemSelectedTimer(callback: () -> Unit) {
        //Cancel timer if it's already started
        stopOnItemSelectedTimer()

        //Start new count down timer
        onItemSelectedTimer = object :
            CountDownTimer(
                100,
                100
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                callback.invoke()
            }
        }
        onItemSelectedTimer!!.start()
    }

    private fun stopOnItemSelectedTimer() {
        if (onItemSelectedTimer != null) {
            onItemSelectedTimer!!.cancel()
            onItemSelectedTimer = null
        }
    }
    private fun onDeleteButtonClicked() {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        //this method is called to restart inactivity timer for channel list scene
        (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

        val channelItem = listVertical[currentChannelPosition]

        var isInWatchlist = false

        if ((listener as ChannelListWidgetListener).getWatchlist() != null) {
            (listener as ChannelListWidgetListener).getWatchlist()?.forEach {
                if (it.tvChannelId == channelItem.channel.id) {
                    isInWatchlist = true
                }
            }
        }

        if (isInWatchlist) {
            val sceneData = DialogSceneData(
                worldHandler!!.active!!.id,
                worldHandler!!.active!!.instanceId
            )
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("channel_delete_confirmation")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("Yes")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("No")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    //this method is called to restart inactivity timer for channel list scene
                    (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        SceneManager.Action.DESTROY
                    )
                }

                override fun onPositiveButtonClicked() {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    //this method is called to restart inactivity timer for channel list scene
                    (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                    if ((listener as ChannelListWidgetListener).getWatchlist() != null) {
                        (listener as ChannelListWidgetListener).getWatchlist()?.forEach {
                            if (it.tvChannelId == channelItem.channel.id) {
                                (listener as ChannelListWidgetListener).removeScheduledReminder(
                                    it
                                )
                            }
                        }
                    }
                    runOnUiThread {
                        deleteChannel(channelItem)
                    }
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        SceneManager.Action.DESTROY
                    )
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                SceneManager.Action.SHOW_OVERLAY, sceneData
            )
        }
        else if (worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING) {
            if (channelItem.channel == (listener as ChannelListWidgetListener).getActiveChannel()) {
                val sceneData =
                    DialogSceneData(worldHandler!!.active!!.id, worldHandler!!.active!!.instanceId)
                sceneData.title = ConfigStringsManager.getStringById("recording_exit_msg")
                sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
                sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
                sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                    override fun onNegativeButtonClicked() {
                        //this method is called to restart inactivity timer for no signal power off
                        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                        //this method is called to restart inactivity timer for channel list scene
                        (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                        worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                            SceneManager.Action.DESTROY
                        )
                    }

                    override fun onPositiveButtonClicked() {
                        //this method is called to restart inactivity timer for no signal power off
                        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                        //this method is called to restart inactivity timer for channel list scene
                        (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                        (listener as ChannelListWidgetListener).stopRecordingByChannel(
                            channelItem.channel, object : IAsyncCallback {
                                override fun onFailed(error: Error) {}
                                override fun onSuccess() {
                                    runOnUiThread {
                                        deleteChannel(channelItem)
                                    }
                                }
                            }
                        )
                        worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                            SceneManager.Action.DESTROY
                        )
                    }
                }
                worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    SceneManager.Action.SHOW_OVERLAY, sceneData
                )
            } else {
                runOnUiThread { deleteChannel(channelItem) }
            }
        }
        else {
            runOnUiThread {
                deleteChannel(channelItem)
            }
        }
    }

    interface ChannelListWidgetListener : GTvChannelListListener, TTSSetterInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun getChannelSourceType(tvChannel: TvChannel): String
        fun getRecordingInProgress(callback: IAsyncDataCallback<com.iwedia.cltv.platform.model.recording.RecordingInProgress>)
        fun onAddFavoritesClicked(tvChannel: TvChannel, favListIds: ArrayList<String>)
        fun onSelectedSortListPosition(position: Int)
        fun sortChannelList(channelList: MutableList<ChannelListItem>): MutableList<ChannelListItem>
        fun getSelectedSortListPosition(): Int
        fun onRecordButtonPressed(tvEvent: TvEvent)
        fun onChannelListEmpty()
        fun getAvailableAudioTracks(): List<IAudioTrack>
        fun getAvailableSubtitleTracks(): List<ISubtitle>
        fun getActiveChannel(): TvChannel
        fun addDeletedChannel(tvChannel: TvChannel)
        fun channelClicked(tvChannel: TvChannel)
        fun deleteChannel(tvChannel: TvChannel): Boolean
        fun lockUnlockChannel(tvChannel: TvChannel, lockUnlock: Boolean, callback: IAsyncCallback)
        fun skipUnskipChannel(tvChannel: TvChannel, skipUnskip: Boolean): Boolean
        fun getFavoriteCategories(callback: IAsyncDataCallback<ArrayList<String>>)
        fun isAudioDescription(type: Int): Boolean
        fun isHOH(type: Int): Boolean
        fun isDolby(type: Int): Boolean
        fun isTeleText(type: Int): Boolean
        fun isParentalEnabled(): Boolean
        fun getChannelList(): ArrayList<TvChannel>
        fun getWatchlist(): MutableList<ScheduledReminder>?
        fun removeScheduledReminder(reminder: ScheduledReminder)
        fun onActiveChannelDeleted()
        fun isClosedCaptionEnabled(): Boolean?
        fun setClosedCaption(): Int?
        fun getClosedCaption(): String?
        fun getAudioChannelInfo(type: Int): String
        fun getAudioFormatInfo():String
        fun getVideoResolution(): String
        fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
        fun getCurrentTime(tvChannel: TvChannel): Long
        fun getCurrentAudioTrack(): IAudioTrack?
        fun isCCTrackAvailable(): Boolean
        fun getInstalledRegion(): Region
        fun getActiveCategory(): String
        fun getDateTimeFormat(): DateTimeFormat
        fun isPvrPathSet(): Boolean
        fun isUsbFreeSpaceAvailable(): Boolean
        fun isUsbStorageAvailable(): Boolean
        fun isEventLocked(tvEvent: TvEvent?): Boolean
        fun isUsbWritableReadable(): Boolean
        fun onClickEditChannel()
        fun getConfigInfo(nameOfInfo: String): Boolean
        fun showRecordingStopPopUp(callback: IAsyncCallback)
        fun getCurrentSubtitleTrack() : ISubtitle?
        fun isSubtitlesEnabled() : Boolean
        fun stopRecordingByChannel(tvChannel: TvChannel, callback: IAsyncCallback)
        fun isScrambled(): Boolean
    }

}