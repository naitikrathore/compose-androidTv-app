package com.iwedia.cltv.entities

import com.iwedia.cltv.components.PreferenceSubMenuItem

class PreferencesClosedCaptionsInformation(
    var subCategories: MutableList<PreferenceSubMenuItem>,
    var withMute: Boolean? = false,
    var enableDisplayCC: Int? = 0,
    var captionServiceList: ArrayList<String>? = null,
    var selectedCaptionService: Int? = 0,
    var advanceSelectionsList: ArrayList<String>? = null,
    var selectedAdvanceSelection: Int? = 0,
    var textSizeList: ArrayList<String>? = null,
    var selectedTextSize: Int? = 0,
    var fontFamilyList: ArrayList<String>? = null,
    var selectedFontFamily: Int? = 0,
    var textColorList: ArrayList<String>? = null,
    var selectedTextColor: Int? = 0,
    var textOpacityList: ArrayList<String>? = null,
    var selectedTextOpacity: Int? = 0,
    var edgeTypeList: ArrayList<String>? = null,
    var selectedEdgeType: Int? = 0,
    var edgeColorList: ArrayList<String>? = null,
    var selectedEdgeColor: Int? = 0,
    var backgroundColorList: ArrayList<String>? = null,
    var selectedBackgroundColor: Int? = 0,
    var backgroundOpacityList: ArrayList<String>? = null,
    var selectedBackgroundOpacity: Int? = 0,
    var lastSelectedTextOpacity: Int? = 0

    )