package com.iwedia.cltv.components

import com.iwedia.cltv.platform.model.PrefMenu
import com.iwedia.cltv.platform.model.PrefType


class CategoryItem {

    var id = 0
    lateinit var prefType : PrefType
    var prefMenu : PrefMenu? = null
    var name = ""

    constructor(id: Int, name: String) {
        this.id = id
        this.name = name
    }

    constructor(prefType: PrefType, name: String) {
        this.prefType = prefType
        this.name = name
    }

    constructor(id: Int, prefMenu: PrefMenu, name: String) {
        this.id = id
        this.prefMenu = prefMenu
        this.name = name
    }
}