package com.iwedia.cltv.platform.model.input_source


class InputItem {
    var id: Int? = -1
    var inputMainName : String = ""
    var inputSourceName: String = ""
    var isAvailable: Int? = 0
    var isHidden: Boolean? = false
    var hardwareId: Int = -1
    var inputId: String? = ""
    var tuneURL: String? = ""

    constructor(
        id: Int,
        inputMainName: String,
        inputSourceName: String,
        type: Boolean,
        isHidden: Boolean,
        hardwareID : Int,
        inputID : String,
        tuneURL: String
    ) {
        this.id = id
        this.inputMainName = inputMainName
        this.inputSourceName = inputSourceName
        if(type) {
            this.isAvailable = 1
        } else {
            this.isAvailable = 0
        }
        this.isHidden = isHidden
        this.hardwareId = hardwareID
        this.inputId = inputID
        this.tuneURL = tuneURL
    }
}


