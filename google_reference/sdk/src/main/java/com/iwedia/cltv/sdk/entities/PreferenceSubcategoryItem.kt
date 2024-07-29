package com.iwedia.cltv.sdk.entities



/**
 * Class PreferenceCategoryItem
 *
 * @author Gaurav Jain
 */

class PreferenceSubcategoryItem {
    var id = ""
    var name = ""


    constructor(id: String, name: String) {
        initialise(id, name)
    }

    fun initialise(id: String, name: String) {
        this.id = id
        this.name = name
    }
}