package com.iwedia.cltv.config.entities

class ConfigParamResource : ConfigParam {

    var resource: Int = -1

    constructor(id: Int, name: String, resource: Int) : super(id, name) {
        this.resource = resource
        this.value.add(resource)
    }
}