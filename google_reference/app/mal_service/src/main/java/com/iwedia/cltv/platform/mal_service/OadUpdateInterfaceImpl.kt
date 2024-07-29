package com.iwedia.cltv.platform.mal_service

import android.media.tv.TvContentRating
import android.os.Bundle
import android.util.Log
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.entities.OadEventData
import com.iwedia.cltv.platform.`interface`.OadUpdateInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import kotlin.concurrent.thread

class OadUpdateInterfaceImpl(val playerInterface: PlayerInterface, val serviceImpl: IServiceAPI) : OadUpdateInterface {

    var listeners = ArrayList<OadUpdateInterface.OadEventListener> ()

    enum class OadEvent(val value: Int) {
        OAD_INVALID_EVENT(1),
        OAD_SCAN_START(2),
        OAD_SCAN_PROGRESS(3),
        OAD_FOUND(4),
        OAD_NOT_FOUND(5),
        OAD_DOWNLOAD_START(6),
        OAD_DOWNLOAD_PROGRESS(7),
        OAD_DOWNLOAD_FAIL(8),
        OAD_UPGRADE_SUCCESS(9),
        OAD_NEWEST_VERSION(10);
    }

    companion object {
        fun fromInt(value: Int) = OadEvent.entries.first { it.value == value }
    }
    init {
        playerInterface.registerListener(object : PlayerInterface.PlayerListener {
            override fun onNoPlayback() {
            }

            override fun onPlaybackStarted() {
            }

            override fun onAudioTrackUpdated(audioTracks: List<IAudioTrack>) {
            }

            override fun onSubtitleTrackUpdated(subtitleTracks: List<ISubtitle>) {
            }

            override fun onVideoAvailable(inputId: String) {
            }

            override fun onVideoUnAvailable(reason: Int, inputId: String) {
            }

            override fun onContentAvailable() {
            }

            override fun onContentBlocked(rating: TvContentRating) {
            }

            override fun onTimeShiftStatusChanged(inputId: String, status: Boolean) {
            }

            override fun onEvent(inputId: String, eventType: String, eventArgs: Bundle) {
                try {
                    val oadEventData: OadEventData =
                        serviceImpl.checkOdaUpdate(eventType, eventArgs)
                    oadEventData.oadEventValue?.let { value ->
                        for (listener in listeners) {
                            thread {
                                when (value) {
                                    OadEvent.OAD_INVALID_EVENT.value -> {}
                                    OadEvent.OAD_SCAN_START.value -> {
                                        listener.onScanStart()
                                    }

                                    OadEvent.OAD_SCAN_PROGRESS.value -> {
                                        listener.onScanProgress(oadEventData.progress)
                                    }

                                    OadEvent.OAD_FOUND.value -> {
                                        listener.onFileFound(oadEventData.progress)
                                    }

                                    OadEvent.OAD_NOT_FOUND.value -> {
                                        listener.onFileNotFound()
                                    }

                                    OadEvent.OAD_DOWNLOAD_START.value -> {
                                        listener.onDownloadStarted()
                                    }

                                    OadEvent.OAD_DOWNLOAD_PROGRESS.value -> {
                                        listener.onDownloadProgress(oadEventData.progress)
                                        if (oadEventData.progress >= 100) {
                                            listener.onDownloadSucess()
                                        }
                                    }

                                    OadEvent.OAD_DOWNLOAD_FAIL.value -> {
                                        listener.onDownloadFail()
                                    }

                                    OadEvent.OAD_UPGRADE_SUCCESS.value -> {
                                        listener.onUpgradeSuccess(oadEventData.version)
                                    }

                                    OadEvent.OAD_NEWEST_VERSION.value -> {
                                        listener.onNewestVersion()
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(OadUpdateInterfaceImpl::class.java.toString(), e.message.toString())
                }
            }

            override fun onTrackSelected(inputId: String, type: Int, trackId: String?) {
            }

        })
        enableOad(true)
    }
    override fun enableOad(enable: Boolean) {
        serviceImpl.enableOad(true)
    }

    override fun startScan() {
        serviceImpl.startScan()
    }

    override fun stopScan() {
        serviceImpl.stopScan()
    }

    override fun startDetect() {
        serviceImpl.startDetect()
    }

    override fun stopDetect() {
        serviceImpl.stopDetect()
    }

    override fun startDownload() {
        serviceImpl.startDownload()
        //send to ui
        //Because MTK SDK can't even fallow it's own architecture
        for (listener in listeners) {
            listener.onDownloadStarted()
        }
    }

    override fun stopDownload() {
        serviceImpl.stopDownload()
    }

    override fun applyOad() {
        serviceImpl.applyOad()
    }

    override fun getSoftwareVersion(): Int {
        return serviceImpl.getSoftwareVersion()
    }

    override fun registerListener(listener: OadUpdateInterface.OadEventListener) {
        listeners.add(listener)
    }

    override fun unregisterListener(listener: OadUpdateInterface.OadEventListener) {
        listeners.remove(listener)
    }
}