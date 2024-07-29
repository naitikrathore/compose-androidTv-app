package com.iwedia.cltv.manager

import android.util.Log
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.audio_subtitle_scene.AudioSubtitleList
import com.iwedia.cltv.scene.audio_subtitle_scene.AudioSubtitleListener
import com.iwedia.cltv.scene.audio_subtitle_scene.AudioSubtitleScene
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import kotlin.Error

/**
 * A Scene Manager class for AudioSubtitleScene
 */
class AudioSubtitleSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    val tvModule: TvInterface,
    val playerModule: PlayerInterface,
    val utilsModule: UtilsInterface,
    private val textToSpeechModule: TTSInterface
) :
    GAndroidSceneManager(context, worldHandler, ReferenceWorldHandler.SceneId.AUDIO_SUBTITLE_SCENE), AudioSubtitleListener {

    var TAG: String = AudioSubtitleSceneManager::class.simpleName as String

    private var audioTracks: MutableList<IAudioTrack>? = null
    private var subtitleTracks: MutableList<ISubtitle>? = null
    private var activeAudioTrack: IAudioTrack? = null
    private var activeSubtitleTrack: ISubtitle? = null
    private var audioSubtitleList: AudioSubtitleList? = null

    override fun createScene() {
        scene = AudioSubtitleScene(context!!, this)
        registerGenericEventListener(Events.AUDIO_TRACKS_SCENE_REFRESH)
        registerGenericEventListener(Events.SUBTITLE_TRACKS_SCENE_REFRESH)
    }

    override fun collectData(callback: IDataCallback) {

        var collectionCount = 0
        val checkStatusRunnable = Runnable {
            collectionCount++
            if(collectionCount==3){
                // data collection finished
                super.collectData(callback)
            }
        }

        tvModule.getActiveChannel(object: IAsyncDataCallback<TvChannel>{
            override fun onFailed(error: Error) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"$TAG", "Error loading active Channel :: Error - $error")
                checkStatusRunnable.run()
            }

            override fun onReceive(data: TvChannel) {
                audioTracks = playerModule.getAudioTracks() as MutableList<IAudioTrack>
                subtitleTracks = playerModule.getSubtitleTracks() as MutableList<ISubtitle>

                if (audioTracks == null) {
                    audioTracks = mutableListOf()
                }
                if (subtitleTracks!!.isEmpty()) {
                    subtitleTracks = mutableListOf()
                }

                audioSubtitleList = AudioSubtitleList(audioTracks!!, subtitleTracks!!)
                checkStatusRunnable.run()
            }

        })

        activeAudioTrack = playerModule.getActiveAudioTrack()
        checkStatusRunnable.run()
        activeSubtitleTrack = playerModule.getActiveSubtitle()
        checkStatusRunnable.run()
    }

    override fun onAudioTrackClicked(audioTrack: IAudioTrack) {
        playerModule.selectAudioTrack(audioTrack)
    }

    override fun onSubtitleTrackClicked(subtitle: ISubtitle) {
        utilsModule.enableSubtitles(true)
        playerModule.selectSubtitle(subtitle)
    }

    override fun getCurrentSubtitleTrack(): ISubtitle? {
        return playerModule.getActiveSubtitle()
    }

    override fun setSubtitles(isActive: Boolean) {
        if (!isActive) {
            playerModule.selectSubtitle(null)
        } else {
            playerModule.selectSubtitle(getCurrentSubtitleTrack())
        }
        utilsModule.enableSubtitles(isActive)
    }

    override fun isSubtitlesEnabled(): Boolean {
        return utilsModule.getSubtitlesState()
    }

    override fun getLanguageMapper(): LanguageMapperInterface {
        return utilsModule.getLanguageMapper()!!
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
        textToSpeechModule.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun getCurrentAudioTrack(): IAudioTrack? {
        return playerModule.getActiveAudioTrack()
    }

    override fun onSceneInitialized() {
        scene!!.refresh(activeSubtitleTrack)
        scene!!.refresh(activeAudioTrack)
        scene!!.refresh(audioSubtitleList)
    }

    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
        when (event!!.type) {
            Events.SUBTITLE_TRACKS_SCENE_REFRESH -> {
                try {
                    val subtitleTracks: MutableList<ISubtitle> = event.getData(0) as MutableList<ISubtitle>
                    ReferenceApplication.runOnUiThread {
                        if(subtitleTracks.isNotEmpty()) {
                            scene!!.refresh(subtitleTracks)
                        }
                    }
                }catch (E: Exception){
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onEventReceived: $E")
                }
                return
            }

            Events.AUDIO_TRACKS_SCENE_REFRESH -> {
                try {
                    val audioTracks: MutableList<IAudioTrack> = event.getData(0) as MutableList<IAudioTrack>
                    ReferenceApplication.runOnUiThread {
                        if(audioTracks.isNotEmpty()) {
                            scene!!.refresh(audioTracks)
                        }
                    }
                }catch (E: Exception){
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onEventReceived: $E")
                }
                return
            }
        }
    }

}