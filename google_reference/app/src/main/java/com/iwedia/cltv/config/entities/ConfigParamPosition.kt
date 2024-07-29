package com.iwedia.cltv.config.entities

class ConfigParamPosition : ConfigParam {

    var positionX: Int = -1
    var positionY: Int = -1

    constructor(id: Int, name: String, positionX: Int, positionY: Int) : super(id, name) {

        this.positionX = positionX
        this.positionY = positionY
        value.add(positionX)
        value.add(positionY)
    }


}