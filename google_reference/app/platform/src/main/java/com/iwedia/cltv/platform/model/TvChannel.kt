package com.iwedia.cltv.platform.model

import android.media.tv.TvContract
import androidx.core.text.isDigitsOnly
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.channel.VideoResolution
import com.iwedia.cltv.platform.model.player.PlayableItem
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import java.util.*
import kotlin.collections.ArrayList


data class TvChannel(
    var id: Int = -1,
    var index: Int = -1,
    var name: String = "",
    var logoImagePath: String = "",
    var channelUrl: String = "",
    var categoryIds: ArrayList<Int> = ArrayList(),
    var audioTracks: ArrayList<IAudioTrack> = ArrayList(),
    var subtitleTracks: ArrayList<ISubtitle> = ArrayList(),
    var videoQuality: ArrayList<VideoResolution> = ArrayList(),
    var videoType: ArrayList<Int> = ArrayList(),
    var audioType: ArrayList<Int> = ArrayList(),
    var channelId: Long = -1,     // TODO: See if this attribute is necessary (id.toLong)
    var inputId: String = "",
    var serviceType: String = "",
    var displayNumber: String = "0",
    var lcn: Int = 0,       // TODO: See if this attribute is necessary (
    var favListIds: ArrayList<String> = ArrayList(),
    var isRadioChannel: Boolean = false,
    var tunerType: TunerType = TunerType.DEFAULT,
    var isSkipped: Boolean = false,
    var isLocked: Boolean = false,
    var ordinalNumber: Int = -1,
    var tsId: Int = 0,
    var onId: Int = 0,
    var serviceId: Int = 0,
    var internalId: Long = -1,
    var isBrowsable: Boolean = true,
    var appLinkText: String = "",
    var appLinkIntentUri: String = "",
    var appLinkIconUri: String = "",
    var appLinkPosterUri: String = "",
    var packageName: String = "",
    var genres: ArrayList<String> = arrayListOf(),
    // this is where you put all SDK specific stuff related to service but not
    // according to TIF
    var platformSpecific : Any? = null,
    var type : String? = null,
    var providerFlag1: Int? = null,
    var providerFlag2: Int? = null,
    var providerFlag3: Int? = null,
    var providerFlag4: Int? = null,
    var internalProviderId: String? = null
): PlayableItem {
    override fun toString(): String {
        return "TvChannel(id=$id, index=$index, name='$name', logoImagePath='$logoImagePath', channelUrl='$channelUrl', categoryIds=$categoryIds, audioTracks=$audioTracks, subtitleTracks=$subtitleTracks, videoQuality=$videoQuality, videoType=$videoType, audioType=$audioType, channelId=$channelId, inputId='$inputId', serviceType='$serviceType', displayNumber=$displayNumber, lcn=$lcn, favListIds=$favListIds, isRadioChannel=$isRadioChannel, tunerType=$tunerType, isSkipped=$isSkipped, isLocked=$isLocked, ordinalNumber=$ordinalNumber, tsId=$tsId, onId=$onId, serviceId=$serviceId, internalId=$internalId, isBrowsable=$isBrowsable, appLinkText='$appLinkText', appLinkIntentUri='$appLinkIntentUri', appLinkIconUri='$appLinkIconUri', appLinkPosterUri='$appLinkPosterUri', packageName='$packageName', genres=$genres)"
    }

    fun getDisplayNumberText(): String {
        return if (displayNumber.isNotEmpty() && displayNumber.isDigitsOnly())
            String.format(Locale.ENGLISH, "%04d", displayNumber.toInt())
        else displayNumber
    }

    fun getDisplayNumberDigits(): String {
        if (displayNumber.contains("-")){
            val displayNumbers = displayNumber.split("-")
            return (displayNumbers.get(0) + displayNumbers.get(1))
        } else if (displayNumber.contains(".")) {
            val displayNumbers = displayNumber.split(".")
            return (displayNumbers[0] + displayNumbers[1])
        }
        else return displayNumber
    }

    fun isFastChannel(): Boolean = inputId.contains("Anoki")

    fun isBroadcastChannel(): Boolean = inputId.contains("mediatek") || inputId.contains("realtek")

    fun isAnalogChannel(): Boolean = ((this.type == TvContract.Channels.TYPE_NTSC) || (this.type == TvContract.Channels.TYPE_PAL) || (this.type == TvContract.Channels.TYPE_SECAM))

    /**
     * Generates a unique identifier to identify channel
     * @return A unique String.
     */
    fun getUniqueIdentifier(): String {
        return if (isFastChannel()) {
            // Return the display number if it's a fast channel.
            displayNumber
        } else {
            // Concatenate onId, tsId, serviceId, channelId for broadcast.
            "${onId}_${tsId}_${serviceId}_${channelId}"
        }
    }

    companion object {
        fun compare(channel1: TvChannel, channel2: TvChannel): Boolean {
            return channel1.onId == channel2.onId &&
                    channel1.tsId == channel2.tsId &&
                    channel1.serviceId == channel2.serviceId
        }
    }
}
