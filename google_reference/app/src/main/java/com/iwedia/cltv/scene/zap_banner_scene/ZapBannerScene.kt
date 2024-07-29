package com.iwedia.cltv.scene.zap_banner_scene

import android.content.Context
import android.os.Build
import android.view.KeyEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.ReferenceWidgetZapBanner
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import world.widget.custom.zap_banner.GZapBannerListener

class ZapBannerScene(context: Context, sceneListener: ZapBannerSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.ZAP_BANNER,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.ZAP_BANNER),
    sceneListener
) {
    var sceneContainer: ConstraintLayout? = null
    var widget: ReferenceWidgetZapBanner? = null

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(name, R.layout.layout_scene_zap_banner, object :
            GAndroidSceneFragmentListener {

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onCreated() {

                if (view != null) {

                    sceneContainer = view!!.findViewById(R.id.scene_container)

                    widget = ReferenceWidgetZapBanner(
                        context,
                        object : GZapBannerListener, CustomGzapBannerListener {

                            override fun getCurrentTime(tvChannel: TvChannel): Long {
                                return (sceneListener as ZapBannerSceneListener).getCurrentTime(tvChannel)
                            }

                            override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                                //Implement if needed
                                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                                    if ((event as KeyEvent).action == KeyEvent.ACTION_UP) {
                                        (sceneListener as ZapBannerSceneListener).onOKPressed()
                                    }

                                    return true
                                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                    if ((event as KeyEvent).action == KeyEvent.ACTION_DOWN) {
                                        (sceneListener as ZapBannerSceneListener).showPlayer()
                                        return true
                                    }
                                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                    if ((event as KeyEvent).action == KeyEvent.ACTION_DOWN) {
                                        (sceneListener as ZapBannerSceneListener).showHomeScene()
                                        return true
                                    }
                                }

                                return false
                            }

                            override fun onNextChannelClicked() {
                                (sceneListener as ZapBannerSceneListener).channelUp()
                            }

                            override fun onPreviousChannelClicked() {
                                (sceneListener as ZapBannerSceneListener).channelDown()
                            }

                            override fun onLastActiveChannelClicked() {
                                (sceneListener as ZapBannerSceneListener).lastActiveChannel()
                            }

                            override fun getDateTimeFormat(): DateTimeFormat {
                                return (sceneListener as ZapBannerSceneListener).getDateTimeFormat()
                            }

                            override fun onNextChannelZapHold() {
                                (sceneListener as ZapBannerSceneListener).channelUpAfterLongPress()
                            }

                            override fun onPreviousZapHold() {
                                (sceneListener as ZapBannerSceneListener).channelDownAfterLongPress()
                            }

                            override fun onNextChannelInfoBannerHold() {
                                (sceneListener as ZapBannerSceneListener).channelUpZapBanner()
                            }

                            override fun onPreviousChannelInfoBannerHold() {
                                (sceneListener as ZapBannerSceneListener).channelDownZapBanner()
                            }

                            override fun getIsCC(type: Int): Boolean {
                                return (sceneListener as ZapBannerSceneListener).getIsCC(type)
                            }

                            override fun getIsAudioDescription(type: Int): Boolean {
                                return (sceneListener as ZapBannerSceneListener).getIsAudioDescription(type)
                            }

                            override fun getTeleText(type: Int): Boolean {
                                return (sceneListener as ZapBannerSceneListener).getTeleText(type)
                            }

                            override fun getIsDolby(type: Int): Boolean {
                                return (sceneListener as ZapBannerSceneListener).getIsDolby(type)
                            }

                            override fun isHOH(type: Int): Boolean {
                                return (sceneListener as ZapBannerSceneListener).isHOH(type)
                            }

                            override fun onTimerEnd() {
                                sceneListener.onBackPressed()
                            }

                            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                                (sceneListener as ZapBannerSceneListener).setSpeechText(text = text, importance = importance)
                            }

                            override fun onBackClicked() {
                                (sceneListener as ZapBannerSceneListener).onBackClicked()
                            }

                            override fun isClosedCaptionEnabled(): Boolean? {
                                return (sceneListener as ZapBannerSceneListener).isClosedCaptionEnabled()
                            }

                            override fun getClosedCaption(): String? {
                                return (sceneListener as ZapBannerSceneListener).getClosedCaption()
                            }

                            override fun isCCTrackAvailable(): Boolean? {
                                return (sceneListener as ZapBannerSceneListener).isCCTrackAvailable()
                            }

                            override fun getAudioChannelInfo(type: Int): String {
                                return (sceneListener as ZapBannerSceneListener).getAudioChannelInfo(type)
                            }

                            override fun getAudioFormatInfo(): String {
                                return (sceneListener as ZapBannerSceneListener).getAudioFormatInfo()
                            }

                            override fun getChannelSourceType(tvChannel: TvChannel): String {
                                return (sceneListener as ZapBannerSceneListener).getChannelSourceType(tvChannel)
                            }

                            override fun getVideoResolution(): String {
                                return (sceneListener as ZapBannerSceneListener).getVideoResolution()
                            }

                            override fun isParentalEnabled(): Boolean {
                                return (sceneListener as ZapBannerSceneListener).isParentalEnabled()
                            }

                            override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
                                return (sceneListener as ZapBannerSceneListener).getParentalRatingDisplayName(parentalRating, tvEvent)
                            }

                            override fun showVirtualKeyboard() {
                                (sceneListener as ZapBannerSceneListener).showVirtualKeyboard()
                            }

                            override fun getAvailableAudioTracks(): List<IAudioTrack> {
                                return (sceneListener as ZapBannerSceneListener).getAvailableAudioTracks()
                            }

                            override fun getCurrentAudioTrack(): IAudioTrack? {
                                return (sceneListener as ZapBannerSceneListener).getCurrentAudioTrack()
                            }

                            override fun showInfoBanner() {
                                (sceneListener as ZapBannerSceneListener).showInfoBanner()
                            }

                            override fun showHomeScene() {
                                (sceneListener as ZapBannerSceneListener).showHomeScene()
                            }

                            override fun showChannelList() {
                                (sceneListener as ZapBannerSceneListener).showChannelList()
                            }
                            override fun getAvailableSubtitleTracks(): List<ISubtitle> {
                                return (sceneListener as ZapBannerSceneListener).getAvailableSubtitleTracks()
                            }

                            override fun getCurrentSubtitleTrack(): ISubtitle? {
                                return (sceneListener as ZapBannerSceneListener).getCurrentSubtitleTrack()
                            }

                            override fun isSubtitleEnabled(): Boolean {
                                return (sceneListener as ZapBannerSceneListener).isSubtitleEnabled()
                            }
                            override fun isEventLocked(tvEvent: TvEvent?)= (sceneListener as ZapBannerSceneListener).isEventLocked(tvEvent)

                            override fun getNextEvent(
                                tvChannel: TvChannel,
                                callback: IAsyncDataCallback<TvEvent>
                            ) {
                                (sceneListener as ZapBannerSceneListener).getNextEvent(tvChannel,object :IAsyncDataCallback<TvEvent>{
                                    override fun onFailed(error: Error) {}

                                    override fun onReceive(data: TvEvent) {
                                        callback.onReceive(data)
                                    }
                                })
                            }

                            override fun isScrambled(): Boolean {
                                return (sceneListener as ZapBannerSceneListener).isScrambled()
                            }
                        })


                    //add the view with 0dp width and height
                    val layoutParams = ConstraintLayout.LayoutParams(0, 0)
                    val view = widget!!.view
                    view!!.layoutParams = layoutParams
                    view.id = View.generateViewId()
                    sceneContainer!!.addView(view)

                    val constraints = ConstraintSet()
                    constraints.connect(
                        view.id,
                        ConstraintSet.LEFT,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.LEFT
                    );
                    constraints.connect(
                        view.id,
                        ConstraintSet.RIGHT,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.RIGHT
                    );
                    constraints.connect(
                        view.id,
                        ConstraintSet.TOP,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.TOP
                    );
                    constraints.connect(
                        view.id,
                        ConstraintSet.BOTTOM,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.BOTTOM
                    );
                    constraints.applyTo(sceneContainer)

                    parseConfig(configParam)
                    sceneContainer?.visibility = View.GONE
                    sceneListener.onSceneInitialized()
                }
            }
        })
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if (widget != null) {
            return widget?.dispatchKeyEvent(keyCode, keyEvent)!!
        } else {
            return super.dispatchKeyEvent(keyCode, keyEvent)
        }
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any?) {
        super.refresh(data)
        if (data != null) {
            if (data is TvChannel) {
                if ((sceneListener as ZapBannerSceneListener).isParentalEnabled() && data.isLocked && !(sceneListener as ZapBannerSceneListener).isChannelUnlocked()) {
                    (sceneListener as ZapBannerSceneListener).getCurrentTime(data)
                    sceneListener.onBackPressed()
                } else {
                    sceneContainer?.visibility = View.VISIBLE
                    widget?.refresh(data)
                }
            } else if (data is TvEvent){
                if ((sceneListener as ZapBannerSceneListener).isParentalEnabled() && (data as TvEvent).tvChannel.isLocked && !(sceneListener as ZapBannerSceneListener).isChannelUnlocked()) {
                    (sceneListener as ZapBannerSceneListener).getCurrentTime(data.tvChannel)
                    sceneListener.onBackPressed()
                } else {
                    widget?.refresh(data)
                }
            } else {
                widget?.refresh(data)
            }
        }
    }

    fun updateResolution(tvChannel: TvChannel, resolution: String){
        if (widget != null) {
            (widget as ReferenceWidgetZapBanner).updateResolution(tvChannel, resolution)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (widget != null) {
            widget?.dispose()
        }
    }
}