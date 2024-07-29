package com.iwedia.cltv.config.entities

class ConfigParamText : ConfigParam {

    var text = ""

    constructor(id: Int, name: String, text: String) : super(id, name) {
        this.text = text
        this.value.add(text)
    }
}