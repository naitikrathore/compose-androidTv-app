package com.iwedia.cltv.sdk.entities

/**
 * References cam info language data
 *
 * @author Dejan Nadj
 */
class ReferenceCamInfoLanguageData constructor(
    // Selected language index in list
    var selectedItemIndex: Int = 0,
    // Language list
    var languages: ArrayList<String> = ArrayList()
) {
}