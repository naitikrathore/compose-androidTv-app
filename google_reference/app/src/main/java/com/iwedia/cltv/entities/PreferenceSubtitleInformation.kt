package com.iwedia.cltv.entities

import com.iwedia.cltv.components.PreferenceSubMenuItem
import com.iwedia.cltv.platform.model.language.LanguageCode

/**
 * Preferences Subtitle information
 *
 * @author Gaurav Jain
 */
class PreferenceSubtitleInformation(
    var subCategories: MutableList<PreferenceSubMenuItem>,
    var availableSubtitleLanguages: List<LanguageCode>? = null,
    var subtitleType: Int,
    var subtitleTypeStrings: List<String>? = null,
    var isClosedCaptionEnabled: Boolean
)