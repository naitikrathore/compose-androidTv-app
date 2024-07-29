package com.iwedia.cltv.scene.home_scene.guide

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvTrackInfo
import android.os.Build
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.leanback.widget.HorizontalGridView
import com.iwedia.cltv.*
import com.iwedia.cltv.ReferenceApplication.Companion.isBlockedContent
import com.iwedia.cltv.components.ButtonType
import com.iwedia.cltv.components.CustomDetails
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.components.HorizontalButtonsAdapter
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSStopperInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import java.util.*

/**
 * Guide event details view
 *
 * @author Dejan Nadj
 */
class GuideEventDetailsView(
    context: Context?,
    private var listener: GuideEventDetailViewListener,
    private val dateTimeFormat: DateTimeFormat
) :
    RelativeLayout(context) {
    private var buttonsHorizontalGridView: HorizontalGridView? = null
    private var buttonsList = mutableListOf<ButtonType>()
    private var selectedButton = -1
    private var tvEvent: TvEvent? = null
    private var viewContainer : View? = null
    private var guideButtonAdapter: HorizontalButtonsAdapter? = null
    private var isCurrentEventOnCurrentChannel: Boolean = false

    private lateinit var customDetails: CustomDetails.CustomDetailsEPG // TODO BORIS this shouldn't be here.
    private lateinit var eventBackgroundImageView: ImageView

    init {
        setup()
        this.listener = listener

    }

    private fun setup() {
        val view = LayoutInflater.from(context).inflate(R.layout.guide_event_details_view, this, true)

        customDetails = view.findViewById(R.id.custom_details)

        customDetails.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)

        eventBackgroundImageView = view.findViewById(R.id.event_background_image_view)
        buttonsHorizontalGridView = view.findViewById(R.id.buttons_horizontal_grid_view)
        buttonsHorizontalGridView!!.layoutManager!!.isAutoMeasureEnabled = true

        viewContainer = view.findViewById(R.id.viewContainer)

        Utils.makeGradient(
            viewContainer!!,
            GradientDrawable.LINEAR_GRADIENT,
            GradientDrawable.Orientation.LEFT_RIGHT,
            Color.parseColor(ConfigColorManager.getColor("color_background")),
            Color.parseColor(ConfigColorManager.getColor("color_background")),
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_not_selected"),
                    0.0
                )
            ),
            0.0F,
            0.0F
        )

        guideButtonAdapter =
            HorizontalButtonsAdapter(
                preventClip = true,
                ttsSetterInterface = object :
                    TTSSetterInterface {
                    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                        listener.setSpeechText(text = text, importance = importance)
                    }
                }
            )
        guideButtonAdapter!!.listener =
            object : HorizontalButtonsAdapter.HorizontalButtonsAdapterListener {
                override fun itemClicked(buttonType: ButtonType, callback: IAsyncCallback) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                        selectedButton = guideButtonAdapter!!.adapterPosition
                        listener.onButtonClick(buttonType, callback)
                        callback.onSuccess()
                }

                override fun onKeyUp(position: Int): Boolean {
                    listener.stopSpeech()
                    listener.onKeyUp()
                    return true
                }

                override fun onKeyDown(position: Int): Boolean {
                    listener.stopSpeech()
                    listener.onKeyDown()
                    return true
                }

                override fun onKeyRight(position: Int): Boolean {
                    if (position == guideButtonAdapter!!.itemCount - 1) {
                        return true
                    }
                    return false
                }

                override fun onKeyLeft(position: Int): Boolean {
                    return false
                }

                override fun onKeyBookmark() {
                    var button = buttonsList[0]
                }

                override fun onCCPressed(): Boolean {
                    return true
                }

                override fun onBackPressed() :Boolean{
                    return listener.onBackPressed()
                }

                override fun onChannelDownPressed(): Boolean {
                    return listener.onChannelDownPressed()
                }

                override fun onChannelUpPressed(): Boolean {
                    return listener.onChannelUpPressed()
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

                override fun onFocusChanged(hasFocus: Boolean) {
                }

                override fun onDigitPressed(digit: Int) {
                    listener.onDigitPressed(digit)
                }

            }
        buttonsHorizontalGridView!!.adapter = guideButtonAdapter
        buttonsHorizontalGridView!!.setItemSpacing(
            Utils.getDimensInPixelSize(R.dimen.custom_dim_20)
        )
    }

    /**
     * Set event details data
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun updateDetails(tvEvent: TvEvent, activeTvChannel: TvChannel?, parentalRating: String) {
        this.tvEvent = tvEvent

        val isCurrentChannel = activeTvChannel!!.id == tvEvent.tvChannel.id // important in updateData method in CustomDetail for displaying icons

        //TODO BORIS customDetails is implemented here in this class but it should be implemented in HorizontalGridView in TvGuide as it is in ForYou.
        // this will work totally fine but in the future this should be shifted in HorizontalGridView.
        customDetails.updateData(
            tvEvent = tvEvent,
            isCurrentChannel = isCurrentChannel,
            parentalRatingDisplayName = parentalRating,
            listener.isCCTrackAvailable(),
            dateTimeFormat = dateTimeFormat,
            isEventLocked = listener.isEventLocked(tvEvent)
        )

        tvEvent.imagePath?.let {
            Utils.loadImage(
                path = it,
                view = eventBackgroundImageView,
                callback = object : AsyncReceiver{
                    override fun onFailed(error: Error?) {
                        eventBackgroundImageView.visibility = GONE
                    }

                    override fun onSuccess() {
                        eventBackgroundImageView.visibility = VISIBLE
                    }
                }
            )
        }

        val channel = tvEvent.tvChannel
        val currentTime = listener.getCurrentTime(channel)
        val eventStartTime = Date(tvEvent.startTime)
        val eventEndTime = Date(tvEvent.endTime)

        val isPastEvent = eventStartTime.before(Date(currentTime)) && eventEndTime.before(Date(currentTime))
        val isCurrentEvent = eventStartTime.before(Date(currentTime)) && eventEndTime.after(Date(currentTime))
        val isFutureEvent = eventStartTime.after(Date(currentTime)) && eventEndTime.after(Date(currentTime))

        //CURRENT EVENT ----------------------------------------------------------------------------
        if (isCurrentEvent) {
            buttonsList.add(ButtonType.WATCH)
            if (ReferenceApplication.IS_CATCH_UP_SUPPORTED) {
                buttonsList.add(ButtonType.START_OVER)
            }

            if (listener.getConfigInfo("pvr")) { ///asdasdasdsad
                if (channel.tunerType != TunerType.ANALOG_TUNER_TYPE) {
                    val isRecordingInProcess =
                        listener.isRecordingInProgress()
                    if (isRecordingInProcess) {
                        if (TvChannel.compare(
                                listener.getRecordingInProgressTvChannel()!!,
                                tvEvent.tvChannel
                            )
                        ) {
                            buttonsList.add(ButtonType.CANCEL_RECORDING)
                        } else {
                            buttonsList.add(ButtonType.RECORD)
                        }
                    } else {
                        buttonsList.add(ButtonType.RECORD)
                    }
                }
            }
        }
        //PAST EVENT -------------------------------------------------------------------------------
        else if (isPastEvent) {
            if (ReferenceApplication.IS_CATCH_UP_SUPPORTED) {
                buttonsList.add(ButtonType.START_OVER)
            }
        }
        //FUTURE EVENT -----------------------------------------------------------------------------
        else if (isFutureEvent) {
            if (tvEvent.id != -1) {
                if (listener.isInWatchList(tvEvent)) {
                    buttonsList.add(ButtonType.WATCHLIST_REMOVE)
                } else {
                    if(tvEvent.isSchedulable) {
                        buttonsList.add(ButtonType.WATCHLIST)
                    }
                }
            }
            if (listener.getConfigInfo("pvr")) {
                if (tvEvent.tvChannel.tunerType != TunerType.ANALOG_TUNER_TYPE) {
                    val isScheduledRecording =
                        listener.isInRecordingList(tvEvent)
                    if (isScheduledRecording) {
                        buttonsList.add(ButtonType.CANCEL_RECORDING)
                    } else {
                        if(tvEvent.isSchedulable) {
                            buttonsList.add(ButtonType.RECORD)
                        }
                    }
                }
            }
        }
        else {
            buttonsList = mutableListOf()
        }

        if (listener.isInFavoriteList(tvEvent.tvChannel)) {
            buttonsList.add(ButtonType.EDIT_FAVORITES)
        }
        else {
            buttonsList.add(ButtonType.ADD_TO_FAVORITES)
        }

        if (!isBlockedContent(tvEvent)) {
            buttonsList.add(ButtonType.MORE_INFO)
        }
        selectedButton = 0

        guideButtonAdapter!!.refresh(buttonsList)

        /**
         * isCurrentEventOnCurrentChannel used to enable setting CustomDetails values only if event is current on current channel
         */
        isCurrentEventOnCurrentChannel = (listener.isCurrentEvent(tvEvent)  && activeTvChannel.id == tvEvent.tvChannel.id)

        if (isCurrentEventOnCurrentChannel){ // those updates on CustomDetails are allowed only if event is currently being played.

            customDetails.updateVideoQuality(tvEvent.tvChannel,listener.getVideoResolution())
            customDetails.updateDolbyImageView(listener.getIsDolby(TvTrackInfo.TYPE_AUDIO))
            customDetails.updateAdImageView(listener.getIsAudioDescription(TvTrackInfo.TYPE_AUDIO))

            val isTeletext = listener.isTeleText(TvTrackInfo.TYPE_SUBTITLE)
            val isTeletextEnabled = listener.getConfigInfo("teletext_enable_column")
            customDetails.updateTtxImageView(isTeletext && isTeletextEnabled)

            customDetails.updateHohImageView(listener.isHOH(TvTrackInfo.TYPE_SUBTITLE)|| listener.isHOH(TvTrackInfo.TYPE_AUDIO))
            customDetails.updateAudioChannelInfo(listener.getAudioChannelInfo(TvTrackInfo.TYPE_AUDIO))
            customDetails.updateAudioChannelFormatInfo(listener.getAudioFormatInfo())
            customDetails.updateAudioTracks(listener.getAvailableAudioTracks(),listener.getCurrentAudioTrack())
            customDetails.updateSubtitleTracks(listener.getAvailableSubtitleTracks(), listener.getCurrentSubtitleTrack(),listener.isSubtitleEnabled())

            updateCCInfo()
        }
    }

    fun clearAllButtons(){
        var buttonsList = mutableListOf<ButtonType>()
        guideButtonAdapter!!.refresh(mutableListOf<ButtonType>())
    }

    /**
     * Refresh favorite button
     */
    fun refreshFavoriteButton() {
        if (tvEvent != null) {
                var favListItems = (listener as GuideEventDetailViewListener).isInFavoriteList(tvEvent!!.tvChannel)
                var position = -1
                for (index in 0 until buttonsList.size) {
                    var button = buttonsList[index]
                    if (button == ButtonType.ADD_TO_FAVORITES || button == ButtonType.EDIT_FAVORITES) {
                        if (!favListItems){
                            buttonsList.set(index, ButtonType.ADD_TO_FAVORITES)
                        }
                        else{
                            buttonsList.set(index, ButtonType.EDIT_FAVORITES)
                        }
                        position = index
                        break
                    }
                }
                if (position != -1) {
                    guideButtonAdapter!!.updateButton(position, buttonsList.get(position))
                }
        }
    }

    /**
     * Refresh record button
     */
    fun refreshRecordButton() {
        if (tvEvent != null) {
            if (listener.isCurrentEvent(tvEvent!!)) {
                if (listener.getConfigInfo("pvr")) {
                    val isRecordingInProcess = listener.isRecordingInProgress()
                    var position = -1
                    for (index in 0 until buttonsList.size) {
                        val button = buttonsList[index]
                        if (button == ButtonType.RECORD || button == ButtonType.CANCEL_RECORDING) {
                            if (isRecordingInProcess) {
                                if (TvChannel.compare(
                                        listener.getRecordingInProgressTvChannel()!!,
                                        tvEvent!!.tvChannel
                                    )
                                ) {
                                    buttonsList[index] = ButtonType.CANCEL_RECORDING
                                } else {
                                    buttonsList[index] = ButtonType.RECORD
                                }
                            } else {
                                buttonsList[index] = ButtonType.RECORD
                            }
                            position = index
                            break
                        }
                    }
                    if (position != -1) {
                        ReferenceApplication.runOnUiThread {
                            guideButtonAdapter!!.updateButton(position, buttonsList[position])
                        }
                    }
                }

            } else {
                //Future recording
                var position = -1
                for (index in 0 until buttonsList.size) {
                    val button = buttonsList[index]
                    if (button == ButtonType.RECORD || button == ButtonType.CANCEL_RECORDING) {
                        if (listener.isInRecordingList(tvEvent!!)) {
                            buttonsList[index] = ButtonType.CANCEL_RECORDING
                        } else {
                            buttonsList[index] = ButtonType.RECORD
                        }
                        position = index
                        break
                    }
                }
                if (position != -1) {
                    ReferenceApplication.runOnUiThread {
                        guideButtonAdapter!!.updateButton(position, buttonsList[position])
                    }
                }
            }
        }
    }

    fun refreshWatchlistButton() {
        if (tvEvent != null) {
            var position = -1
            for (index in 0 until buttonsList.size) {
                var button = buttonsList[index]
                if (button == ButtonType.WATCHLIST || button == ButtonType.WATCHLIST_REMOVE) {
                    if (listener.isInWatchList(tvEvent!!)){
                        buttonsList.set(index, ButtonType.WATCHLIST_REMOVE)
                    }
                    else{
                        buttonsList.set(index, ButtonType.WATCHLIST)
                    }
                    position = index
                    break
                }
            }
            if (position != -1) {
                ReferenceApplication.runOnUiThread(Runnable {
                    guideButtonAdapter!!.updateButton(position, buttonsList.get(position))
                })
            }
        }
    }

    /**
     * Select favorite button
     */
    fun selectFavoriteButton(hasFocus: Boolean) {
        buttonsList[selectedButton]
    }

    fun dispatchKey(keyCode: Int, keyEvent: KeyEvent): Boolean {
        return true
    }

    /**
     * Get selected button id
     */
    fun getSelectedButtonType(): ButtonType {
        return buttonsList[selectedButton]
    }

    fun updateCCInfo() {
        if (listener.isClosedCaptionEnabled() && isCurrentEventOnCurrentChannel) {
            val isCCTrackAvailable = listener.isCCTrackAvailable()
            customDetails.updateCcInfo(listener.getClosedCaption(), isCCTrackAvailable)
        }
    }

    interface GuideEventDetailViewListener: TTSSetterInterface, TTSStopperInterface{
        fun onButtonClick(buttonType: ButtonType, callback: IAsyncCallback)
        fun getRecordingInProgressTvChannel():TvChannel?
        fun isInWatchList(tvEvent: TvEvent): Boolean
        fun isInRecordingList(tvEvent: TvEvent): Boolean
        fun isInFavoriteList(tvChannel: TvChannel) : Boolean
        fun isRecordingInProgress(): Boolean
        fun onKeyUp(): Boolean
        fun onKeyDown(): Boolean
        fun onBackPressed() : Boolean
        fun isClosedCaptionEnabled(): Boolean
        fun getIsAudioDescription(type: Int): Boolean
        fun getIsDolby(type: Int): Boolean
        fun isHOH(type:Int): Boolean
        fun isTeleText(type:Int): Boolean
        fun getAudioChannelInfo(type:Int): String
        fun getVideoResolution(): String
        fun getCurrentTime(tvChannel: TvChannel): Long
        fun isCurrentEvent(tvEvent: TvEvent) : Boolean
        fun getAvailableAudioTracks(): List<IAudioTrack>
        fun getAvailableSubtitleTracks(): List<ISubtitle>
        fun getClosedCaption(): String?
        fun getCurrentAudioTrack(): IAudioTrack?
        fun isCCTrackAvailable(): Boolean?
        fun onChannelDownPressed() : Boolean
        fun onChannelUpPressed() : Boolean
        fun isEventLocked(tvEvent: TvEvent?): Boolean
        fun getConfigInfo(nameOfInfo: String): Boolean
        fun getCurrentSubtitleTrack() : ISubtitle?
        fun isSubtitleEnabled() : Boolean
        fun getAudioFormatInfo(): String
        fun onDigitPressed(digit: Int)
    }

}