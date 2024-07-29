package com.iwedia.cltv.entities

import com.iwedia.cltv.components.PreferenceSubMenuItem
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.language.LanguageCode

/**
 * Reference preferences setup information
 *
 * @author Gaurav Jain
 */
class PreferenceSetupInformation(
    var subCategories: MutableList<PreferenceSubMenuItem>,
    var channels: List<TvChannel> ?= null,
    var displayMode: HashMap<Int, String>? = null,
    var defaultChannel: TvChannel?,
    var defaultChannelIndex: Int,
    var defaultDisplayMode: Int,
    var aspectRatioOptions: MutableList<String> = mutableListOf(),
    var defaultAspectRatioOption: Int,
    var isInteractionChannelEnabled:Boolean = false,
    var availableAudioTracks: List<LanguageCode>? = null,
    var epgLanguage: LanguageCode? = null,
    val noSignalPowerOff: List<String>? = null,
    var defaultNoSignalPowerOff : String?=null,
    var noSignalPowerOffEnabled : Boolean,
    var isBlueMuteEnabled : Boolean

) {}