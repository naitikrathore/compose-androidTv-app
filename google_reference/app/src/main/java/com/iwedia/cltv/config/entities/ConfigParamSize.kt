package com.iwedia.cltv.config.entities

class ConfigParamSize : ConfigParam {

    var sizeX: String = ""
    var sizeY: String = ""

    constructor(id: Int, name: String, sizeX: String, sizeY: String) : super(id, name) {

        this.sizeX = sizeX
        this.sizeY = sizeY
        value.add(sizeX)
        value.add(sizeY)
    }
}