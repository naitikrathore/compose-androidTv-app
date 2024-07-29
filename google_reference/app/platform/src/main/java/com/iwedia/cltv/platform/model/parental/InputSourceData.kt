package com.iwedia.cltv.platform.model.parental

class InputSourceData {

    var inputSourceName: String = ""
    var hardwareId: Int = 0
    var isBlocked: Boolean = false
    var inputMainName : String = ""

    constructor(
        inputSourceName: String,
        hardwareId: Int,
        inputMainName : String
    ) {
        this.inputSourceName = inputSourceName
        this.hardwareId = hardwareId
        this.inputMainName = inputMainName

    }
}