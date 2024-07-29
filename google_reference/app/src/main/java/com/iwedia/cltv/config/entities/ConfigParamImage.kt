package com.iwedia.cltv.config.entities

class ConfigParamImage : ConfigParam {

    var image = ""

    constructor(id: Int, name: String, image: String) : super(id, name) {
        this.image = image
        this.value.add(image)
    }
}