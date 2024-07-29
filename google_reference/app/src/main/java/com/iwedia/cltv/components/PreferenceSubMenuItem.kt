package com.iwedia.cltv.components

import com.iwedia.cltv.platform.model.PrefSubMenu

class PreferenceSubMenuItem {
    var id: PrefSubMenu ? =null
    var name = ""


    constructor(id: PrefSubMenu, name: String) {
        this.id = id
        this.name = name
    }

}