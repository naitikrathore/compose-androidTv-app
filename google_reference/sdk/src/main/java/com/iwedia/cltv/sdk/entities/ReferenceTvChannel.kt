package com.iwedia.cltv.sdk.entities

import androidx.core.text.isDigitsOnly
import com.iwedia.cltv.sdk.ReferenceSdk
import core_entities.*
import data_type.GList
import java.util.*
import kotlin.collections.ArrayList

/**
 * ReferenceTvChannel entity
 *
 * @author Dejan Nadj
 */
open class ReferenceTvChannel(
    id: Int = -1,
    index: Int = -1,
    name: String = "",
    logoImagePath: String? = "",
    channelUrl: String? = "",
    categoryIds: GList<Int>? = GList<Int>(),
    audioTracks: GList<AudioTrack>? = GList<AudioTrack>(),
    subtitleTracks: GList<SubtitleTrack>? = GList<SubtitleTrack>(),
    videoQuality: GList<Int>? = GList<Int>(),
    videoType: GList<Int>? = GList<Int>(),
    audioType: GList<Int>? = GList<Int>()
) : TvChannel(
    id,
    index,
    name,
    logoImagePath,
    channelUrl,
    categoryIds,
    audioTracks,
    subtitleTracks,
    videoQuality,
    videoType,
    audioType
) {

    var channelId: Long = -1
    var inputId: String = ""
    var serviceType: String = ""
    var displayNumber: String = "0"
    var lcn: Int = 0
    var favListIds: ArrayList<String> = ArrayList()
    var isRadioChannel = false
    var tunerType: Int = -1
    var isSkipped: Boolean = false
    var isLocked: Boolean = false
    get() {
        return  field && ReferenceSdk.tvInputHandler?.isParentalEnabled() ?: false
    }
    var ordinalNumber: Int = -1
    var tsId = 0
    var onId = 0
    var serviceId = 0
    var internalId: Long = -1
    var isBrowsable = true
    var appLinkText: String = ""
    var appLinkIntentUri: String = ""
    var appLinkIconUri: String = ""
    var appLinkPosterUri: String = ""
    var packageName: String = ""
    var type : String? = null

    fun getDisplayNumberDigits(): String {
        if (displayNumber.contains("-")){
            val displayNumbers = displayNumber.split("-")
            return (displayNumbers.get(0) + displayNumbers.get(1))

        }
        else return displayNumber
    }

    override fun toString(): String {
        return "ReferenceTvChannel = [id = $channelId, index = ${index}, name = $name," +
                "display number = $displayNumber, skipped = $isSkipped, isRadioChannel = $isRadioChannel," +
                " tynerType = $tunerType, isLocked = $isLocked, ordinal number = $ordinalNumber," +
                "onid = $onId, tsid = $tsId, serviceId = $serviceId]"
    }

    fun getDisplayNumberText(): String {
        return if (displayNumber.isDigitsOnly())
            String.format(Locale.ENGLISH, "%03d", displayNumber.toInt())
        else displayNumber
    }

    companion object {
        const val DUMMY_CHANNEL_ID = -555
        const val TERRESTRIAL_TUNER_TYPE = 100
        const val CABLE_TUNER_TYPE = 200
        const val SATELLITE_TUNER_TYPE = 300
        const val ANALOG_TUNER_TYPE = 350
        const val VIDEO_RESOLUTION_ED = 400
        const val VIDEO_RESOLUTION_FHD = 500
        const val VIDEO_RESOLUTION_SD = 600
        const val VIDEO_RESOLUTION_HD = 700
        const val VIDEO_RESOLUTION_UHD = 800

        fun compare(channel1: ReferenceTvChannel, channel2: ReferenceTvChannel): Boolean {
            return channel1.onId == channel2.onId &&
                    channel1.tsId == channel2.tsId &&
                    channel1.serviceId == channel2.serviceId
        }
    }
}