package com.iwedia.cltv.config.entities

class ConfigParamNumber : ConfigParam {

    var number = -1

    constructor(id: Int, name: String, value: Int) : super(id, name) {
        this.number = value
        this.value.add(value)
    }
}