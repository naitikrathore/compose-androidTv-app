package com.iwedia.cltv.scene.player_scene

import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.TvChannel
import utils.information_bus.Event
import utils.information_bus.EventListener
import utils.information_bus.InformationBus
import utils.information_bus.events.Events
import kotlin.Error


/**
 * AudioSubtitlesHelper
 *
 * @author Aleksandar Milojevic
 */
object AudioSubtitlesHelper {

    private const val TAG = "AudioSubtitlesHelper"

    private val audioPreferredLanguages = Array<String?>(2) { null }
    private val subtitlePreferredLanguages = Array<String?>(2) { null }
    private lateinit var tvModule: TvInterface
    private lateinit var utilsModule: UtilsInterface
    fun setup(tvModule: TvInterface, utilsModule: UtilsInterface) {
        this.tvModule = tvModule
        this.utilsModule = utilsModule
        InformationBus.registerEventListener(AudioSubtitleEventListener())
    }

    fun setAudioPreferredLanguage(audioTrackLanguageCode: String, index: Int) {
        audioPreferredLanguages[index] = audioTrackLanguageCode

        if (index == 0) {
            utilsModule.setPrimaryAudioLanguage(audioTrackLanguageCode)
        } else if (index == 1) {
            utilsModule.setSecondaryAudioLanguage(audioTrackLanguageCode)
        } else {
            throw IllegalStateException("Only 2 preffered languages are supported!")
        }
    }

    fun getAudioPreferredLanguage(index: Int): String? {
        return audioPreferredLanguages[index]
    }

    fun setSubtitlePreferredLanguage(subtitleTrackLanguageCode: String, index: Int, type: PrefType = PrefType.PLATFORM) {
        subtitlePreferredLanguages[index] = subtitleTrackLanguageCode
        if (index == 0) {
            utilsModule.setPrimarySubtitleLanguage(subtitleTrackLanguageCode, type)
        } else if (index == 1) {
            utilsModule.setSecondarySubtitleLanguage(subtitleTrackLanguageCode, type)
        } else {
            throw IllegalStateException("Only 2 preffered languages are supported!")
        }
    }

    fun getSubtitlePreferredLanguage(index: Int, type: PrefType): String? {
        if (index == 0) {
            return utilsModule.getPrimarySubtitleLanguage(type)
        } else if (index == 1) {
            return utilsModule.getSecondarySubtitleLanguage(type)
        } else {
            throw IllegalStateException("Only 2 preffered languages are supported!")
        }
        return null
    }

    fun setTeletextPreferredLanguage(position: Int, index: Int){
        if (index == 0){
            utilsModule.setTeletextDigitalLanguage(position)
        } else if (index == 1) {
            utilsModule.setTeletextDecodeLanguage(position)
        } else {
            throw IllegalStateException("Only 2 preferred languages are supported!")
        }
    }

    class AudioSubtitleEventListener : EventListener {
        constructor() {
            addType(com.iwedia.cltv.platform.model.information_bus.events.Events.AUDIO_TRACKS_UPDATED)
            addType(com.iwedia.cltv.platform.model.information_bus.events.Events.SUBTITLE_TRACKS_UPDATED)
            addType(Events.APP_INITIALIZED)
        }

        override fun callback(event: Event?) {
            if (event!!.type == com.iwedia.cltv.platform.model.information_bus.events.Events.AUDIO_TRACKS_UPDATED) {
                if (event.getData(0) != null && event.getData(0) is TvChannel) {

                    var tvChannel = event.getData(0) as TvChannel
                    var tracks = event.getData(1)

                    utilsModule.updateAudioTracks()

                    //Check if this is active channel
                    tvModule.getActiveChannel(object: IAsyncDataCallback<TvChannel> {
                        override fun onFailed(error: Error) {
                        }

                        override fun onReceive(data: TvChannel) {
                            if (data.id == tvChannel.id) {
                                InformationBus.submitEvent(
                                    Event(
                                        com.iwedia.cltv.platform.model.information_bus.events.Events.AUDIO_TRACKS_SCENE_REFRESH,
                                        tracks
                                    )
                                )
                            }
                        }
                    })
                }
            }

            if (event!!.type == com.iwedia.cltv.platform.model.information_bus.events.Events.SUBTITLE_TRACKS_UPDATED) {
                if (event.getData(0) != null && event.getData(0) is TvChannel) {
                    var tvChannel = event.getData(0) as TvChannel
                    var tracks = event.getData(1)

                    utilsModule.updateSubtitleTracks()

                    //Check if this is active channel
                    tvModule.getActiveChannel(object :
                        IAsyncDataCallback<TvChannel> {
                        override fun onFailed(error: Error) {
                        }

                        override fun onReceive(data: TvChannel) {
                            if (data.id == tvChannel.id) {
                                InformationBus.submitEvent(
                                    Event(
                                        com.iwedia.cltv.platform.model.information_bus.events.Events.SUBTITLE_TRACKS_SCENE_REFRESH,
                                        tracks
                                    )
                                )
                            }
                        }
                    })
                }
            }

            if (event.type == Events.APP_INITIALIZED) {

                    var primaryAudio: String? = utilsModule.getPrimaryAudioLanguage()
                    var secondaryAudio: String? = utilsModule.getSecondaryAudioLanguage()
                    var primarySubtitle: String? = utilsModule.getPrimarySubtitleLanguage()
                    var secondarySubtitle: String? = utilsModule.getSecondarySubtitleLanguage()
                    var ttxDigitalLanguage: Int = utilsModule.getTeletextDigitalLanguage()
                    var ttxDecodeLanguage: Int = utilsModule.getTeletextDecodeLanguage()

                    if(primaryAudio != null) {
                        setAudioPreferredLanguage(primaryAudio, 0)
                    }
                    if(secondaryAudio != null){
                        setAudioPreferredLanguage(secondaryAudio, 1)
                    }
                    if(primarySubtitle != null) {
                        setSubtitlePreferredLanguage(primarySubtitle, 0)
                    }
                    if(secondarySubtitle != null) {
                        setSubtitlePreferredLanguage(secondarySubtitle, 1)
                    }
                    if(ttxDigitalLanguage != -1){
                        setTeletextPreferredLanguage(ttxDigitalLanguage, 0)
                    }
                    if(ttxDecodeLanguage != -1){
                        setTeletextPreferredLanguage(ttxDecodeLanguage,1)
                    }
                    utilsModule.getAudioType(object : IAsyncDataCallback<Int> {
                        override fun onReceive(type: Int) {
                            utilsModule.setAudioType(type)
                        }

                        override fun onFailed(error: Error) {

                        }
                    })
                    utilsModule.enableSubtitles(utilsModule.getSubtitlesState())
                    utilsModule.setSubtitlesType(utilsModule.getSubtitlesType())
            }
        }
    }
}