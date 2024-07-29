package com.iwedia.cltv.entities

import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.parental.InputSourceData


class InputInformation(
    var inputData: ArrayList<InputItem> ?= null,
    var inputDataImg: ArrayList<Int> ?= null,
    var inputDataFocusImg: ArrayList<Int> ?= null,
    var blockedInputData: ArrayList<InputSourceData>? =null,
    var isFactoryMode: Boolean? = false,
    var isParentalEnabled: Boolean? = false
    )