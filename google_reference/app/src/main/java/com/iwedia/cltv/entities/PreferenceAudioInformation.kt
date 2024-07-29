package com.iwedia.cltv.entities


import com.iwedia.cltv.components.PreferenceSubMenuItem
import com.iwedia.cltv.platform.model.language.LanguageCode

/**
 * Reference preferences audio information
 *
 * @author Gaurav Jain
 */
class PreferenceAudioInformation(
    var subCategories: MutableList<PreferenceSubMenuItem>,
    var availableAudioLanguages: List<LanguageCode>? = null,
    var audioTypeSelected: Int?=null,
    var isAudioDescriptionEnabled :Boolean = false,
    var isHearingImpairedEnabled :Boolean = false,
    var audioTypeStrings: List<String>? = null,
    var audioFormat : List<String>? =null,
    var selectedAudioFormat: String?=null,
    var faderCtrlList: List<String>?=null,
    var defFaderValue: Int?=null,
    var audioForVisuallyImp: List<String>?=null,
    var defAudioForVisImp: Int?=null,
    var viVolume: Int,
    var viSpeakerStatus: Boolean,
    var viHeadPhoneStatus: Boolean,
    var viPaneFadeStatus: Boolean,
) {}