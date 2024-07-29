package com.iwedia.cltv.scene.infoBanner

import android.content.Context
import android.media.tv.TvTrackInfo
import android.os.Build
import android.view.KeyEvent
import android.view.KeyEvent.*
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.ReferenceWidgetInfoBanner
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.RecordingInProgress
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import listeners.AsyncDataReceiver

/**
 * Info banner scene
 *
 * @author Aleksandar Lazic
 */
class InfoBannerScene(context: Context, sceneListener: InfoBannerSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.INFO_BANNER,
    "Infobanner",
    sceneListener
) {

    /**
     * Scene container
     */
    var sceneContainer: RelativeLayout? = null

    /**
     * Widget
     */
    var widget: ReferenceWidgetInfoBanner? = null

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(
            name,
            R.layout.layout_scene_infobanner,
            object : GAndroidSceneFragmentListener {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onCreated() {
                    try {
                        sceneContainer = view!!.findViewById(R.id.scene_container)
                    }catch (E: Exception){
                        println(E.message)
                        ReferenceApplication.worldHandler?.destroyOtherExisting(
                            ReferenceWorldHandler.SceneId.LIVE
                        )
                        return
                    }
                    widget = ReferenceWidgetInfoBanner(
                        context,
                        object :
                            ReferenceWidgetInfoBanner.InfoBannerWidgetListener {

                            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                                (sceneListener as InfoBannerSceneListener).setSpeechText(text = text, importance = importance)
                            }

                            override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                                (sceneListener as InfoBannerSceneListener).showToast(text, duration)
                            }

                            override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                                (sceneListener as InfoBannerSceneListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                            }

                            override fun isCurrentEvent(tvEvent: TvEvent): Boolean {
                                return (sceneListener as InfoBannerSceneListener).isCurrentEvent(tvEvent)
                            }

                            override fun getDateTimeFormat(): DateTimeFormat {
                                return (sceneListener as InfoBannerSceneListener).getDateTimeFormat()
                            }

                            override fun isPvrPathSet(): Boolean {
                                return (sceneListener as InfoBannerSceneListener).isPvrPathSet()
                            }

                            override fun isUsbFreeSpaceAvailable(): Boolean {
                                return (sceneListener as InfoBannerSceneListener).isUsbFreeSpaceAvailable()
                            }

                            override fun isUsbWritableReadable(): Boolean {
                                return (sceneListener as InfoBannerSceneListener).isUsbWritableReadable()
                            }

                            override fun isUsbStorageAvailable(): Boolean {
                                return (sceneListener as InfoBannerSceneListener).isUsbStorageAvailable()
                            }

                            override fun getConfigInfo(nameOfInfo: String): Boolean {
                                return (sceneListener as InfoBannerSceneListener).getConfigInfo(nameOfInfo)
                            }

                            override fun getPlatformName(): String {
                                return (sceneListener as InfoBannerSceneListener).getPlatformName()
                            }

                            override fun isScrambled(): Boolean {
                                return (sceneListener as InfoBannerSceneListener).isScrambled()
                            }

                            override fun getCurrentTime(tvChannel: TvChannel): Long {
                                return (sceneListener as InfoBannerSceneListener).getCurrentTime(tvChannel)
                            }
                            override fun getIsInReclist(tvEvent: TvEvent): Boolean {
                                return (sceneListener as InfoBannerSceneListener).getIsInReclist(
                                    tvEvent
                                )
                            }

                            override fun getIsCC(type: Int): Boolean {
                                return (sceneListener as InfoBannerSceneListener).getIsCC(type)
                            }

                            override fun getIsAudioDescription(type: Int): Boolean {
                                return (sceneListener as InfoBannerSceneListener).getIsAudioDescription(type)
                            }

                            override fun getTeleText(type: Int): Boolean {
                                return (sceneListener as InfoBannerSceneListener).getTeleText(type)
                            }

                            override fun getIsDolby(type: Int): Boolean {
                                return (sceneListener as InfoBannerSceneListener).getIsDolby(type)
                            }

                            override fun isHOH(type: Int): Boolean {
                                return (sceneListener as InfoBannerSceneListener).isHOH(type)
                            }

                            override fun getRecordingInProgress(callback: IAsyncDataCallback<RecordingInProgress>) {
                                (sceneListener as InfoBannerSceneListener).getRecordingInProgress(
                                    object : IAsyncDataCallback<RecordingInProgress> {
                                        override fun onFailed(error: Error) {}

                                        override fun onReceive(data: RecordingInProgress) {
                                            callback.onReceive(data)
                                        }

                                    })
                            }

                            override fun getActiveChannel(): TvChannel {
                                return (sceneListener as InfoBannerSceneListener).getActiveChannel()
                            }

                            override fun getRecordingInProgressTvChannel(): TvChannel {
                                return (sceneListener as InfoBannerSceneListener).getRecordingInProgressTvChannel()
                            }

                            override fun RecordingInProgress(): Boolean {
                                return (sceneListener as InfoBannerSceneListener).recordingInProgress()
                            }

                            override fun showDetails(tvEvent: TvEvent) {
                                (sceneListener as InfoBannerSceneListener).showDetailsScene(tvEvent)
                            }

                            override fun onEventClicked(tvEvent: TvEvent) {
                                (sceneListener as InfoBannerSceneListener).playCurrentEvent(tvEvent.tvChannel)
                            }

                            override fun onEventLongUpPressed() {

                            }

                            override fun onEventLongDownPressed() {

                            }

                            override fun onKeyboardClicked() {
                                (sceneListener as InfoBannerSceneListener).onKeyboardClicked()
                            }

                            override fun onRecordButtonClicked(
                                tvEvent: TvEvent,
                                callback: IAsyncCallback
                            ) {
                                (sceneListener as InfoBannerSceneListener).onRecordButtonClicked(
                                    tvEvent, callback
                                )
                            }

                            override fun onAudioTrackClicked(audioTrack: IAudioTrack) {
                                (sceneListener as InfoBannerSceneListener).onAudioTrackClicked(
                                    audioTrack
                                )
                            }

                            override fun onSubtitleTrackClicked(subtitleTrack: ISubtitle) {
                                (sceneListener as InfoBannerSceneListener).onSubtitleTrackClicked(
                                    subtitleTrack
                                )
                            }

                            override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                                return false
                            }

                            override fun isSubtitleAudioButtonPressed(): Boolean {
                                return (sceneListener as InfoBannerSceneListener).isAudioSubtitleButtonPressed()
                            }

                            override fun onSubtitleKeyClicked(callback: AsyncDataReceiver<ISubtitle>) {
                                (sceneListener as InfoBannerSceneListener).requestCurrentSubtitleTrack(
                                    callback
                                )
                            }

                            override fun onAudioKeyClicked(): IAudioTrack {
                                return (sceneListener as InfoBannerSceneListener).getCurrentAudioTrack()!!
                            }

                            override fun addToWatchlist(
                                tvEvent: TvEvent,
                                callback: IAsyncCallback
                            ) {
                                (sceneListener as InfoBannerSceneListener).addToWatchlist(
                                    tvEvent,
                                    callback
                                )
                            }

                            override fun removeFromWatchlist(
                                tvEvent: TvEvent,
                                callback: IAsyncCallback
                            ) {
                                (sceneListener as InfoBannerSceneListener).removeFromWatchlist(
                                    tvEvent,
                                    callback
                                )
                            }

                            override fun onWatchlistClicked(
                                tvEvent: TvEvent,
                                callback: IAsyncCallback
                            ) {
                                (sceneListener as InfoBannerSceneListener).onWatchlistClicked(
                                    tvEvent,
                                    callback
                                )
                            }

                            override fun hasScheduledReminder(
                                tvEvent: TvEvent,
                                callback: IAsyncDataCallback<Boolean>
                            ) {
                                (sceneListener as InfoBannerSceneListener).hasScheduledReminder(
                                    tvEvent,
                                    callback
                                )
                            }

                            override fun hasScheduledRecording(
                                tvEvent: TvEvent,
                                callback: IAsyncDataCallback<Boolean>
                            ) {
                                (sceneListener as InfoBannerSceneListener).hasScheduledRecording(
                                    tvEvent,
                                    callback
                                )
                            }

                            override fun getCurrentAudioTrack(): IAudioTrack? {
                                return (sceneListener as InfoBannerSceneListener).getCurrentAudioTrack()
                            }

                            override fun getCurrentSubtitleTrack(): ISubtitle? {
                                return (sceneListener as InfoBannerSceneListener).getCurrentSubtitleTrack()
                            }

                            override fun getAvailableAudioTracks(): MutableList<IAudioTrack>? {
                                return (sceneListener as InfoBannerSceneListener).getAvailableAudioTracks()
                            }

                            override fun getAvailableSubtitleTracks(): MutableList<ISubtitle>? {
                                return (sceneListener as InfoBannerSceneListener).getAvailableSubtitleTracks()
                            }

                            override fun setSubtitles(isActive: Boolean) {
                                (sceneListener as InfoBannerSceneListener).setSubtitles(isActive)
                            }

                            override fun getClosedCaptionSubtitlesState(): Boolean? {
                                return (sceneListener as InfoBannerSceneListener).getClosedCaptionSubtitlesState()
                            }

                            override fun isSubtitlesEnabled(): Boolean {
                                return (sceneListener as InfoBannerSceneListener).isSubtitlesEnabled()
                            }

                            override fun isClosedCaptionEnabled(): Boolean? {
                                return (sceneListener as InfoBannerSceneListener).isClosedCaptionEnabled()
                            }

                            override fun saveUserSelectedCCOptions(
                                ccOptions: String,
                                newValue: Int
                            ) {
                                (sceneListener as InfoBannerSceneListener).saveUserSelectedCCOptions(ccOptions, newValue)
                            }

                            override fun getClosedCaption(): String? {
                                return (sceneListener as InfoBannerSceneListener).getClosedCaption()
                            }

                            override fun setClosedCaption(): Int? {
                                return (sceneListener as InfoBannerSceneListener).setClosedCaption()
                            }

                            override fun isInWatchlist(event: TvEvent): Boolean? {
                                return (sceneListener as InfoBannerSceneListener).isInWatchlist(event)
                            }

                            override fun getChannelSourceType(tvChannel: TvChannel): String {
                                return (sceneListener as InfoBannerSceneListener).getChannelSourceType(tvChannel)
                            }

                            override fun isParentalOn(): Boolean {
                                return (sceneListener as InfoBannerSceneListener).isParentalOn()
                            }

                            override fun getAudioChannelInfo(type: Int): String {
                                return (sceneListener as InfoBannerSceneListener).getAudioChannelInfo(type)
                            }

                            override fun getAudioFormatInfo(): String {
                                return (sceneListener as InfoBannerSceneListener).getAudioFormatInfo()
                            }

                            override fun refreshData(tvChannel: TvChannel) {
                                (sceneListener as InfoBannerSceneListener).refreshData(tvChannel)
                            }

                            override fun getLanguageMapper(): LanguageMapperInterface {
                                return (sceneListener as InfoBannerSceneListener).getLanguageMapper()
                            }

                            override fun getVideoResolution(): String {
                                return (sceneListener as InfoBannerSceneListener).getVideoResolution()
                            }

                            override fun isParentalControlsEnabled(): Boolean {
                                return (sceneListener as InfoBannerSceneListener).isParentalControlsEnabled()
                            }

                            override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
                                return (sceneListener as InfoBannerSceneListener).getParentalRatingDisplayName(parentalRating, tvEvent)
                            }

                            override fun isCCTrackAvailable(): Boolean? {
                                return (sceneListener as InfoBannerSceneListener).isCCTrackAvailable()
                            }

                            override fun isEventLocked(tvEvent: TvEvent?) = (sceneListener as InfoBannerSceneListener).isEventLocked(tvEvent)


                            override fun defaultAudioClicked() {
                                return (sceneListener as InfoBannerSceneListener).defaultAudioClicked()
                            }
                        },
                    )
                    sceneContainer!!.addView(widget!!.view)

                    parseConfig(configParam)
                    sceneListener.onSceneInitialized()
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any?) {
        super.refresh(data)
        widget!!.refresh(data!!)
    }

    fun refreshActiveAudioTrack() {
        widget!!.refreshActiveAudioTrack()
    }

    fun refreshAudioChannel() {
        ReferenceApplication.runOnUiThread {
            widget?.refreshAudioChannel()
        }
    }

    override fun onPause() {
        (ReferenceApplication.getActivity() as MainActivity).stopSceneInactivityTimer()
    }

    override fun onResume() {
        super.onResume()
        widget?.refreshRecordButton()
        widget?.refreshWatchlistButton()
        //this method is called to restart inactivity timer for info banner scene
        (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()
        if (widget?.sideViewOpen  == 2) refreshActiveAudioTrack()
        else widget!!.view!!.requestFocus()
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }


    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        //this method is called to restart inactivity timer for info banner scene
        (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()
        if ((keyEvent as KeyEvent).action == ACTION_DOWN) {
            if (keyCode == KEYCODE_DPAD_DOWN) {
                if ((widget != null) && (widget!!.recyclerView != null)
                    && widget!!.recyclerView!!.hasFocus()
                ) {
                    if(widget!!.buttonContainer!!.visibility == View.VISIBLE)
                    {
                        widget!!.buttonContainer!!.postDelayed({
                            widget!!.buttonContainer!!.requestFocus()
                        },300)
                    }
                    //TODO WITH NEW EVENT CARD VIEW THIS DELAY WONT BE NEEDED


                    return true
                }
            }
            if (keyCode == KEYCODE_DPAD_UP) {

                if ((widget != null) && (widget!!.recyclerView != null) &&
                    widget!!.recyclerView!!.hasFocus()
                ) {
                    return true
                }
            }

            if (keyCode == KEYCODE_CHANNEL_UP) {

                (sceneListener as InfoBannerSceneListener).onChannelUpPressed()
                return true
            }

            if (keyCode == KEYCODE_CHANNEL_DOWN) {
                (sceneListener as InfoBannerSceneListener).onChannelDownPressed()
                return true
            }
            if (keyCode == KEYCODE_INFO) {
                if (keyEvent.repeatCount == 0 && keyCode == KEYCODE_INFO) {
                    ReferenceApplication.worldHandler?.active?.let {
                        if (it.id != ReferenceWorldHandler.SceneId.DETAILS_SCENE) {
                            widget?.showMoreInfo()
                        }
                    }

                }
                return true
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }


    override fun onDestroy() {
        super.onDestroy()

        widget!!.dispose()
    }

    fun blockRecordButton() {
        widget!!.blockRecordButton()
    }

    fun unblockRecordButton() {
        widget!!.unblockRecordButton()
    }

    companion object {
        const val INFO_BANNER_SUBTITLE_STATE = 1
        const val INFO_BANNER_AUDIO_STATE = 2
    }
}