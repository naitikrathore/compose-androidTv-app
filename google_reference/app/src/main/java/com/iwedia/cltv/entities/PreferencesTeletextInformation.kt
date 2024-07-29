package com.iwedia.cltv.entities

import com.iwedia.cltv.components.PreferenceSubMenuItem
import com.iwedia.cltv.platform.model.language.LanguageCode


/**
 * Reference preferences txt information
 *
 * @author Gaurav Jain
 */
class PreferencesTeletextInformation(
    var subCategories: List<PreferenceSubMenuItem>?= null,
    var digitalLanguages: MutableList<LanguageCode>? = null,
    var decodingPageLanguages: List<String>? = null,
    var preferredLanguages:List<String>? =null,
    var defaultDigitalLang: String? =null,
    var defaultDecodingPageLang:Int? =null,
    var defaultPreferredLang:String? =null

    )