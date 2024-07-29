package com.iwedia.cltv.config.entities

class ConfigParamAlignment : ConfigParam {

    object AlignmentX {
        const val LEFT = "left"
        const val CENTER = "center"
        const val RIGHT = "right"
        const val NONE = ""
    }

    object AlignmentY {
        const val TOP = "top"
        const val CENTER = "center"
        const val BOTTOM = "bottom"
        const val NONE = ""
    }

    var alignmentX: String = ""
    var alignmentY: String = ""

    constructor(id: Int, name: String, alignmentX: String, alignmentY: String) : super(id, name) {
        this.alignmentX = alignmentX
        this.alignmentY = alignmentY
        value.add(alignmentX)
        value.add(alignmentY)
    }


}