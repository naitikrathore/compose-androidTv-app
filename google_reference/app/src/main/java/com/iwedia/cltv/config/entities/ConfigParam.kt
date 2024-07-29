package com.iwedia.cltv.config.entities

abstract class ConfigParam {

    var id: Int = -1
    var name: String = ""
    var value: MutableList<Any> = mutableListOf()

    constructor(id: Int, name: String) {
        this.id = id
        this.name = name
    }
}