package com.iwedia.cltv.platform.refplus5

import android.content.Context
import android.content.Intent
import android.media.tv.TvContentRating
import android.os.Bundle
import com.iwedia.cltv.platform.base.OadUpdateInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.OadUpdateInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.mediatek.dtv.tvinput.client.oad.TvOadController
import java.util.stream.Collectors
import kotlin.concurrent.thread

class OadUpdateInterfaceImpl(var utilsInterface: UtilsInterfaceImpl, val context : Context, val playerInterface: PlayerInterface) : OadUpdateInterfaceBaseImpl(playerInterface) {

    private val tvOadController: TvOadController = TvOadController(context, 0)

    val EVENT_OAD_FILE_FOUND = "EVENT_OAD_FILE_FOUND"
    val KEY_OAD_SW_VERSION = "KEY_OAD_SW_VERSION"
    val EVENT_OAD_NEWEST_VERSION = "EVENT_OAD_NEWEST_VERSION"
    val EVENT_OAD_DOWNLOAD_START = "EVENT_OAD_DOWNLOAD_START"
    val EVENT_OAD_DOWNLOAD_PROGRESS = "EVENT_OAD_DOWNLOAD_PROGRESS"
    val KEY_OAD_DOWNLOAD_PROGRESS_INFO = "KEY_OAD_DOWNLOAD_PROGRESS_INFO"
    val KEY_OAD_DOWNLOAD_PAYLOAD_OFFSET = "KEY_OAD_DOWNLOAD_PAYLOAD_OFFSET"
    val KEY_OAD_DOWNLOAD_PAYLOAD_SIZE = "KEY_OAD_DOWNLOAD_PAYLOAD_SIZE"
    val KEY_OAD_DOWNLOAD_PAYLOAD_PROPERTIES = "KEY_OAD_DOWNLOAD_PAYLOAD_PROPERTIES"
    val EVENT_OAD_DOWNLOAD_FAIL = "EVENT_OAD_DOWNLOAD_FAIL"
    val EVENT_OAD_SCAN_START = "EVENT_OAD_SCAN_START"
    val EVENT_OAD_SCAN_PROGRESS = "EVENT_OAD_SCAN_PROGRESS"
    val KEY_OAD_SCAN_PROGRESS_INFO = "KEY_OAD_SCAN_PROGRESS_INFO"
    val EVENT_OAD_UPGRADE_SUCCESS = "EVENT_OAD_UPGRADE_SUCCESS"
    val OAD_INTO_MARK = "oad_into_mark"
    val OAD_INTO_UNDONE = "oad_into_undone"
    val OAD_REJECT_UPDATE = "rejectOadUpdated"

    val OAD_OFFSET = "oad_offset"
    val OAD_SIZE = "oad_size"
    val OAD_PROPERTIES = "oad_properties"

    var listeners = ArrayList<OadUpdateInterface.OadEventListener> ()

    enum class OadEvent {
        OAD_INVALID_EVENT,
        OAD_SCAN_START,
        OAD_SCAN_PROGRESS,
        OAD_FOUND,
        OAD_NOT_FOUND,
        OAD_DOWNLOAD_START,
        OAD_DOWNLOAD_PROGRESS,
        OAD_DOWNLOAD_FAIL,
        OAD_UPGRADE_SUCCESS,
        OAD_NEWEST_VERSION
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
                var version = 0
                var progress = 0
                var oadEventType = OadEvent.OAD_INVALID_EVENT

                when(eventType) {
                    EVENT_OAD_FILE_FOUND -> {
                        if (eventArgs != null) {
                            version = eventArgs.getInt(KEY_OAD_SW_VERSION, 0)
                        }
                        oadEventType = OadEvent.OAD_FOUND
                    }
                    EVENT_OAD_SCAN_START-> {
                        oadEventType = OadEvent.OAD_SCAN_START
                    }
                    EVENT_OAD_SCAN_PROGRESS -> {
                        if (eventArgs != null) {
                            progress = eventArgs.getInt(KEY_OAD_SCAN_PROGRESS_INFO, 0)
                        }
                        if (progress == 100) {
                            oadEventType = OadEvent.OAD_NOT_FOUND
                        } else {
                            oadEventType = OadEvent.OAD_SCAN_PROGRESS
                        }
                    }
                    EVENT_OAD_DOWNLOAD_START -> {
                        oadEventType = OadEvent.OAD_DOWNLOAD_START

                    }
                    EVENT_OAD_DOWNLOAD_PROGRESS -> {
                        oadEventType = OadEvent.OAD_DOWNLOAD_PROGRESS
                        progress = eventArgs.getInt(KEY_OAD_DOWNLOAD_PROGRESS_INFO, 0)
                        if(progress >= 100) {
                            var offset = eventArgs.getLong(KEY_OAD_DOWNLOAD_PAYLOAD_OFFSET, 0)
                            var size = eventArgs.getLong(KEY_OAD_DOWNLOAD_PAYLOAD_SIZE, 0)
                            val properties = eventArgs.getStringArrayList(KEY_OAD_DOWNLOAD_PAYLOAD_PROPERTIES)

                            utilsInterface.saveMtkInternalGlobalValue(context, OAD_OFFSET, offset.toString(), true)
                            utilsInterface.saveMtkInternalGlobalValue(context, OAD_SIZE, size.toString(), true)
                            var proValue = properties!!.stream().map { pro -> pro.toString() }.collect(Collectors.joining(","))

                            utilsInterface.saveMtkInternalGlobalValue(context, OAD_PROPERTIES, proValue, true)
                        }
                    }
                    EVENT_OAD_DOWNLOAD_FAIL -> {
                        oadEventType = OadEvent.OAD_DOWNLOAD_FAIL

                    }
                    EVENT_OAD_UPGRADE_SUCCESS -> {
                        if (eventArgs != null) {
                            version = eventArgs.getInt(KEY_OAD_SW_VERSION, 0)
                        }
                        oadEventType = OadEvent.OAD_UPGRADE_SUCCESS
                    }
                    EVENT_OAD_NEWEST_VERSION -> {
                        oadEventType = OadEvent.OAD_NEWEST_VERSION
                    }
                    else -> {
                        oadEventType = OadEvent.OAD_INVALID_EVENT
                    }
                }

                for(listener in listeners) {
                    thread {
                        when (oadEventType) {
                            OadEvent.OAD_INVALID_EVENT -> {}
                            OadEvent.OAD_SCAN_START -> {
                                listener.onScanStart()
                            }

                            OadEvent.OAD_SCAN_PROGRESS -> {
                                listener.onScanProgress(progress)
                            }

                            OadEvent.OAD_FOUND -> {
                                listener.onFileFound(version)
                            }

                            OadEvent.OAD_NOT_FOUND -> {
                                listener.onFileNotFound()
                            }

                            OadEvent.OAD_DOWNLOAD_START -> {
                                listener.onDownloadStarted()
                            }

                            OadEvent.OAD_DOWNLOAD_PROGRESS -> {
                                listener.onDownloadProgress(progress)
                                if(progress >= 100) {
                                    listener.onDownloadSucess()
                                }
                            }

                            OadEvent.OAD_DOWNLOAD_FAIL -> {
                                listener.onDownloadFail()
                            }

                            OadEvent.OAD_UPGRADE_SUCCESS -> {
                                listener.onUpgradeSuccess(version)
                            }

                            OadEvent.OAD_NEWEST_VERSION -> {
                                if(utilsInterface.getCountryPreferences(UtilsInterface.CountryPreference.DISABLE_OAD_NEWEST_VERSION_NOTIFICATION,false) == false) {
                                    listener.onNewestVersion()
                                }
                            }
                        }
                    }
                }
            }

            override fun onTrackSelected(inputId: String, type: Int, trackId: String?) {

            }

        })
        enableOad(true)
    }

    override fun enableOad(enable: Boolean) {
        tvOadController.setOadStatus(enable)
    }

    override fun startScan() {
        tvOadController.startScan()
    }

    override fun stopScan() {
        tvOadController.stopScan()
        utilsInterface.saveMtkInternalGlobalValue(context, OAD_INTO_MARK, OAD_INTO_UNDONE, false)
    }

    override fun startDetect() {
        tvOadController.startDetect()
    }

    override fun stopDetect() {
        tvOadController.stopDetect()
    }

    override fun startDownload() {
        tvOadController.startDownload()
        //Because MTK SDK can't even fallow it's own architecture
        for(listener in listeners) {
            listener.onDownloadStarted()
        }
    }

    override fun stopDownload() {
        tvOadController.stopDownload()
    }

    override fun applyOad() {
        var zeroValue = 0
        utilsInterface.saveMtkInternalGlobalValue(context, OAD_REJECT_UPDATE, zeroValue.toString(), true)

        val intent = Intent("com.mediatek.tv.action.OTAUPGRADER")
        intent.putExtra("OTA_ZIP_URL", "file://data/ota_package/mtktvoad_download.bin")

        var oadOffset = utilsInterface.readMtkInternalGlobalValue(context, OAD_OFFSET)?.toLong()
        var oadSize = utilsInterface.readMtkInternalGlobalValue(context, OAD_SIZE)?.toLong()
        var oadProperties = utilsInterface.readMtkInternalGlobalValue(context, OAD_PROPERTIES)

        intent.putExtra(OAD_OFFSET, oadOffset)
        intent.putExtra(OAD_SIZE, oadSize)
        intent.putExtra(OAD_PROPERTIES, oadProperties)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        stopDownload()
        utilsInterface.saveMtkInternalGlobalValue(context, OAD_INTO_MARK, OAD_INTO_UNDONE, false)
    }

    override fun getSoftwareVersion(): Int {
        if(tvOadController.getSwVersion() != null) {
            return tvOadController.getSwVersion()!!
        }
        return 0
    }

    override fun registerListener(listener : OadUpdateInterface.OadEventListener) {
        listeners.add(listener)
    }

    override fun unregisterListener(listener : OadUpdateInterface.OadEventListener) {
        listeners.remove(listener)
    }
}