package com.iwedia.cltv.config.entities

class ConfigRailParam : ConfigParam {

    var railId: String? = null

    constructor(id: Int, name: String, railId: String) : super(id, name) {
        this.railId = railId
    }
}