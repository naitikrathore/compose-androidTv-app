package com.iwedia.cltv.sdk.entities

class LanguageCode {

    var languageCodeISO6392 = ""
    var languageCodeISO6391 = ""
    var englishName = ""
    var germanName = ""
    var frenchName = ""

    constructor(
        languageCodeISO6392: String,
        languageCodeISO6391: String,
        englishName: String,
        frenchName: String,
        germanName: String,
    ) {
        this.languageCodeISO6392 = languageCodeISO6392
        this.languageCodeISO6391 = languageCodeISO6391
        this.englishName = englishName
        this.germanName = germanName
        this.frenchName = frenchName
    }
}