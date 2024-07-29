package com.iwedia.cltv.platform.`interface`.language

import com.iwedia.cltv.platform.model.language.LanguageCode

interface LanguageMapperInterface {
    var preferredLanguageCodes:MutableList<LanguageCode>
    var countryCodeToLanguageCodeMap: MutableMap<String, String?>
    fun getLanguageName(languageCode: String): String?
    fun getPreferredLanguageName(languageCode: String): String?
    fun getLanguageCodeByCountryCode(countryCode: String?): String?
    fun getTxtDigitalLanguageMapByCountryCode(countryCode: String?): Int?
    fun getTxtDigitalLanguageMapByPosition(position: Int?): Int?
    fun getLanguageCodes(): MutableList<LanguageCode>
    fun getDefaultLanguageCode(): LanguageCode
    fun getLanguageCode(trackLanguage: String) : String
}