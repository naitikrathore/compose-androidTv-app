package com.iwedia.cltv.platform.model.input_source


class InputResolutionItem {

    var iconValue: String = "HD"
    var pixelValue: String = ""
    var hdrValue: String = ""


    constructor(
        iconValue: String,
        pixelValue: String,
        hdrValue: String
    ) {
        this.iconValue = iconValue
        this.pixelValue = pixelValue
        this.hdrValue = hdrValue
    }
}


