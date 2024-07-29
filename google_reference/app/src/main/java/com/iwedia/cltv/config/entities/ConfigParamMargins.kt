package com.iwedia.cltv.config.entities

class ConfigParamMargins : ConfigParam {

    var marginLeft: String = ""
    var marginRight: String = ""
    var marginTop: String = ""
    var marginBottom: String = ""

    constructor(id: Int, name: String, left: String, right: String, top: String, bottom: String) : super(id, name) {
        this.marginLeft = left
        this.marginRight = right
        this.marginTop = top
        this.marginBottom = bottom
        value.add(left)
        value.add(right)
        value.add(top)
        value.add(bottom)
    }
}