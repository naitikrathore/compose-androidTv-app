package com.iwedia.cltv.entities

import android.media.tv.ContentRatingSystem
import com.iwedia.cltv.components.PreferenceSubMenuItem
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.parental.InputSourceData

class PreferencesParentalControlInformation(
    var subCategories: MutableList<PreferenceSubMenuItem>,
    var channelList: List<TvChannel>,
    var blockedInputs: MutableList<InputSourceData>,
    var blockedInputCount: Int = 0,
    var blockUnratedProgramsAvailability: Boolean,
    var isBlockUnratedPrograms: Boolean,
    var contentRatingData: MutableList<ContentRatingSystem>,
    var contentRatingsEnabled: MutableList<ContentRatingSystem>,
    var globalRestrictionsArray: MutableList<String>,
    var anokiRatingSystemArray: MutableList<String>
)