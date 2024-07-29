package com.iwedia.cltv.entities

import com.iwedia.cltv.components.PreferenceSubMenuItem

class PreferencesCamInfoInformation (
    var subCategories: List<PreferenceSubMenuItem>?= null,
    var camMenuPreferenceStrings: List<String>? = null,
    var userPreferenceStrings: List<String>? = null,
    var userPreferenceSelected: Int = 0,
    var camTypePreferenceStrings: List<String>? = null,
    var camTypePreferenceSelected: Int = 0,
    var camOperatorNameStrings: List<String>? = null,
)