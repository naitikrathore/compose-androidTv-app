package com.iwedia.cltv.entities

import com.iwedia.cltv.components.PreferenceSubMenuItem


/**
 * Reference preferences hbbtv information
 *
 * @author Gaurav Jain
 */
class PreferencesHbbTVInfromation(
    var subCategories: List<PreferenceSubMenuItem>? = null,
    var isHbbTvSupport: Boolean = false,
    var isTrack: Boolean = false,
    var cookieSettingsSelected: Int = 0,
    var cookieSettingsStrings: List<String>? = null,
    var isPresistentStorage: Boolean = false,
    var isBlockTrackingSites: Boolean = false,
    var isDeviceId: Boolean = false
)