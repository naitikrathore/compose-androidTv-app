package com.iwedia.cltv.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.*
import com.iwedia.cltv.config.*
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.scene.home_scene.rail.AdapterListener
import com.iwedia.cltv.scene.home_scene.rail.RailAdapter
import com.iwedia.cltv.utils.Utils
import tv.anoki.ondemand.domain.model.VODItem
import utils.information_bus.Event
import utils.information_bus.EventListener
import utils.information_bus.InformationBus
import utils.information_bus.events.Events
import world.widget.GWidget
import world.widget.GWidgetListener

/**
 * Recordings widget
 * @author Dragan Krnjaic
 */
class ReferenceWidgetRecordings :
    GWidget<ConstraintLayout, ReferenceWidgetRecordings.RecordingsWidgetListener> {

    var configParam: SceneConfig? = null
    var context: Context? = null
    val TAG = javaClass.simpleName
    var verticalGridView: VerticalGridView? = null
    var events: HorizontalGridView? = null
    var railAdapter: RailAdapter? = null
    var itemsForAdapter: ArrayList<RailItem> = arrayListOf()
    var temp: ArrayList<RailItem> = arrayListOf()

    var noEventsMsg: TextView? = null

    var customRecordingCustomButton: CustomButton? = null

    var loadingLayout: RelativeLayout? = null

    var loadingProgressBar: ProgressBar? = null

    //Information bus event listener
    private var eventListener = object : EventListener() {
        override fun callback(event: Event?) {
            super.callback(event)
            if (event?.type == Events.USB_DEVICE_DISCONNECTED) {
                verticalGridView!!.visibility = View.INVISIBLE
                noEventsMsg!!.text = ConfigStringsManager.getStringById("pvr_no_usb_message")
                noEventsMsg!!.visibility = View.VISIBLE
                customRecordingCustomButton!!.visibility = View.INVISIBLE
            }
            if (event?.type == Events.USB_DEVICE_CONNECTED) {
                noEventsMsg!!.visibility = View.GONE
                verticalGridView!!.visibility = View.VISIBLE
                customRecordingCustomButton!!.visibility = View.VISIBLE
            }
            if (event?.type == Events.PVR_RECORDING_REMOVED) {
                railAdapter!!.onRecordingDeleted(object : IAsyncCallback {
                    override fun onFailed(error: Error) {}

                    override fun onSuccess() {
                        customRecordingCustomButton!!.requestFocus()
                    }
                })
                listener.getRails(configParam)
            }
            if (event?.type == Events.PVR_RECORDING_RENAMED) {
                val name = event.getData(0) as String
                railAdapter!!.onRecordingRenamed(name)
            }
        }
    }

    constructor(
        context: Context,
        listener: RecordingsWidgetListener
    ) : super(
        ReferenceWorldHandler.WidgetId.RECORDINGS,
        ReferenceWorldHandler.WidgetId.RECORDINGS,
        listener
    ) {
        this.context = context
        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_widget_recordings, null) as ConstraintLayout

        verticalGridView = view!!.findViewById(R.id.recordings_recycler)
        loadingLayout = view!!.findViewById(R.id.loading)
        noEventsMsg = view!!.findViewById(R.id.no_events_msg)
        noEventsMsg!!.setText(ConfigStringsManager.getStringById("no_categories_msg"))
        noEventsMsg!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )
        noEventsMsg!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        customRecordingCustomButton = view!!.findViewById(R.id.custom_recordings_custom_button)
        loadingProgressBar = view!!.findViewById(R.id.loadingProgressBar)
        loadingProgressBar!!.getIndeterminateDrawable().setColorFilter(
            Color.parseColor(
                ConfigColorManager.getColor("color_selector")
            ), PorterDuff.Mode.MULTIPLY
        )
        setupAdapter()

        configParam = ConfigHandler.getSceneConfigParam(ReferenceWorldHandler.WidgetId.RECORDINGS)
        Handler().post {
            listener.getRails(configParam)
        }
        showLoading()

        InformationBus.registerEventListener(eventListener)

        customRecordingCustomButton!!.apply {
            setOnClick {
                listener.onCustomRecordingButtonClicked()
            }
            setOnKeyListener { _, keyCode, keyEvent ->
                if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        //this method is called to restart inactivity timer for no signal power off
                        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                        listener.requestFocusOnTopCategories()
                        return@setOnKeyListener true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return@setOnKeyListener true // disable clicking left
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return@setOnKeyListener true // disable clicking right
                    }
                    if (itemsForAdapter.size == 0) {
                        return@setOnKeyListener true // disable clicking down when there is no rails on which focus can go
                    }
                }
                return@setOnKeyListener false
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED)
        ReferenceApplication.applicationContext()
            .registerReceiver(TimeDataChangeReceiver(), intentFilter)

    }

    inner class TimeDataChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            railAdapter!!.notifyDataSetChanged()
            requestFocus()
        }
    }

    override fun refresh(data: Any) {
        if (Utils.isDataType(data, MutableList::class.java)) {
            itemsForAdapter.clear()
            for (i in 0 until (data as MutableList<RailItem>).size) {
                try {
                    //Attempt to invoke virtual method 'java.util.List com.iwedia.cltv.scene.home_scene.recordings.RecordingsRailItem.getRailItems()' on a null object reference
                    if (data[i].rail!!.size == 0) {
                        customRecordingCustomButton!!.visibility = View.VISIBLE
                    } else if (data[i].rail!!.size > 0) {
                        if (!Utils.isUsbConnected()) {
                            itemsForAdapter.add(data[i])
                            itemsForAdapter.forEach { item ->
                                if (item.railName == ConfigStringsManager.getStringById("recorded")) {
                                    itemsForAdapter.remove(item)
                                }
                            }

                            if (itemsForAdapter.size == 0) {
                                noEventsMsg!!.text =
                                    ConfigStringsManager.getStringById("pvr_no_usb_message")
                                noEventsMsg!!.visibility = View.VISIBLE
                            } else {
                                noEventsMsg!!.visibility = View.INVISIBLE
                            }
                        } else {
                            itemsForAdapter.add(data[i])
                        }
                    }
                } catch (E: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "refresh: ${E.printStackTrace()}")
                }
            }

            //sort rails
            val temp = itemsForAdapter.sortedWith(compareBy { it.id })
            itemsForAdapter.clear()
            itemsForAdapter.addAll(temp)
            railAdapter!!.refresh(itemsForAdapter)

            if (itemsForAdapter.size == 0) {
                noEventsMsg!!.visibility = View.VISIBLE // if there is no any rail -- show message
            }
            customRecordingCustomButton!!.visibility = View.VISIBLE
            hideLoading()
        }
        super.refresh(data)
    }

    /**
     * Request focus on the list
     */
    fun requestFocus() {
        if (itemsForAdapter.isNotEmpty()) {
            verticalGridView!!.requestFocus()
        }
    }

    fun clearFocus() {
        verticalGridView!!.clearFocus()
    }

    private fun setupAdapter() {
        railAdapter = RailAdapter(listener.getDateTimeFormat())

        railAdapter!!.setListener(object : AdapterListener {
            override fun onEventSelected(
                tvEvent: TvEvent?,
                vodItem: VODItem?,
                parentalRatingDisplayName: String,
                railItem: RailItem,
                currentTime: Long,
                isStillFocused: () -> Boolean
            ) {
            }

            override fun onBroadcastChannelSelected(tvChannel: TvChannel?, railItem: RailItem) {
            }

            override fun getCurrentTime(tvChannel: TvChannel): Long {
                return listener.getCurrentTime(tvChannel)
            }

            override fun onScrollDown(position: Int) {
                verticalGridView?.smoothScrollToPosition(position)
                verticalGridView?.layoutManager?.findViewByPosition(position)?.requestFocus()
            }

            override fun onScrollUp(position: Int) {
                verticalGridView?.smoothScrollToPosition(position)
                verticalGridView?.layoutManager?.findViewByPosition(position)?.requestFocus()
            }

            override fun onKeyUp(onKeyUpFinished: () -> Unit) {
                customRecordingCustomButton!!.requestFocus()
                onKeyUpFinished.invoke() // crucial for collapsing the card.
            }

            override fun onItemClicked(item: Any) {
                listener.onItemClick(item)
            }

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
                return false // recording can't be in watchList, this method is not needed in ReferenceWidgetRecording and that's reason why false is always returned - to avoid setting Watchlist Icon to be visible
            }

            override fun isEventLocked(tvEvent: TvEvent?) = listener.isEventLocked(tvEvent)

            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                listener.setSpeechText(text = text, importance = importance)
            }

            override fun isInRecList(tvEvent: TvEvent): Boolean {
                return listener.isInRecList(tvEvent)
            }

            override fun isRegionSupported(): Boolean {
                return true
            }
        })

        verticalGridView!!.setNumColumns(1)
        verticalGridView!!.adapter = railAdapter

        //make focus fixed on th top side of the screen
        verticalGridView!!.itemAlignmentOffset =
            context!!.resources.getDimensionPixelSize(R.dimen.custom_dim_0)
        verticalGridView!!.itemAlignmentOffsetPercent =
            HorizontalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED
        verticalGridView!!.windowAlignmentOffset = 0
        verticalGridView!!.windowAlignmentOffsetPercent =
            HorizontalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
        verticalGridView!!.windowAlignment = HorizontalGridView.WINDOW_ALIGN_NO_EDGE
    }

    /**
     * Show loading animation
     */
    private fun showLoading() {
        customRecordingCustomButton!!.visibility = View.INVISIBLE
        ReferenceApplication.worldHandler!!.isEnableUserInteraction = true
        loadingLayout!!.visibility = View.VISIBLE
    }

    /**
     * Hide loading animation
     */
    private fun hideLoading() {
        ReferenceApplication.runOnUiThread {
            loadingLayout!!.visibility = View.INVISIBLE
        }
        ReferenceApplication.worldHandler!!.isEnableUserInteraction = true
    }

    override fun dispose() {
        super.dispose()
        InformationBus.unregisterEventListener(eventListener)
    }

    fun handleFocusToGrid() {
        requestFocus()
    }

    interface RecordingsWidgetListener : GWidgetListener, TTSSetterInterface {
        fun getRails(sceneConfig: SceneConfig?)
        fun requestFocusOnTopCategories()
        fun onItemClick(recording: Any)
        fun keepFocusOnFilerView()
        fun onCustomRecordingButtonClicked()
        fun requestFocusForButton()
        fun getRailSize(): Int
        fun isChannelLocked(channelId: Int): Boolean
        fun isParentalControlsEnabled(): Boolean
        fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
        fun getCurrentTime(tvChannel: TvChannel): Long
        fun getDateTimeFormat(): DateTimeFormat
        fun isEventLocked(tvEvent: TvEvent?): Boolean
        fun isInRecList(tvEvent: TvEvent): Boolean
    }
}