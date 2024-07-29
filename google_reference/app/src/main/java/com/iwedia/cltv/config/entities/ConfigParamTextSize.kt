package com.iwedia.cltv.config.entities

class ConfigParamTextSize : ConfigParam {
    var fontSize: String =""

    constructor(id: Int, name: String, fontSize: String) : super(id, name) {
        this.fontSize = fontSize
        value.add(fontSize)
    }
}