package com.iwedia.cltv.config.entities

class ConfigParamColor : ConfigParam {

    object ColorType {
        const val TEXT_COLOR = "text_color"
        const val BACKGROUND_COLOR = "background_color"
        const val ANIMATION_BACKGROUND_COLOR = "anim_background_color"
        const val PROGRESS_BAR_COLOR = "progress_color"

    }

    var color = ""
    var type = ""

    constructor(id: Int, name: String, color: String, type: String) : super(id, name) {
        this.color = color
        this.value.add(color)
        this.type = type
    }
}