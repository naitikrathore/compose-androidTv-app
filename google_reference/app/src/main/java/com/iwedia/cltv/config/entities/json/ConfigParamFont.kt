package com.iwedia.cltv.config.entities.json

import com.iwedia.cltv.config.entities.ConfigParam

/**
 * Config param font
 *
 * @author Aleksandar Lazic
 */
class ConfigParamFont : ConfigParam {
    var fontResource : String? = null

    constructor(id: Int, name: String, value: String) : super(id, name) {
        this.fontResource = value
    }
 }