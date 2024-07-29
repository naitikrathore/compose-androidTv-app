package com.iwedia.cltv.sdk.entities


import data_type.GList

/**
 * Reference preferences hbb tv information
 *
 * @author Aleksandar Lazic
 */
class ReferencePreferencesHbbTvInformation(
    var subCategories: MutableList<PreferenceSubcategoryItem>,
    var isHbbTvSupport: Boolean = false,
    var isTrack: Boolean = false,
    var cookieSettingsSelected: Int = 0,
    var cookieSettingsStrings: GList<String>? = null,
    var isPresistentStorage: Boolean = false,
    var isBlockTrackingSites: Boolean = false,
    var isDeviceId: Boolean = false
) {}