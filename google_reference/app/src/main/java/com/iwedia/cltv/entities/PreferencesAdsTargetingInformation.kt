package com.iwedia.cltv.entities

import com.iwedia.cltv.components.PreferenceSubMenuItem

class PreferencesAdsTargetingInformation(
    var subCategories: MutableList<PreferenceSubMenuItem>,
    var isAdsTargetingEnabled: Boolean,
    var isInternetConnectionAvailable: Boolean
)