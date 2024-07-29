package com.iwedia.cltv.config.entities

class ConfigParamBoolean : ConfigParam {

    var flag = false

    constructor(id: Int, name: String, value: Boolean) : super(id, name) {
        this.flag = value
        this.value.add(value)
    }
}