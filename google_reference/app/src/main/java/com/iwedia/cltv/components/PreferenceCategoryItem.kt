package com.iwedia.cltv.components
/**
 * Class PreferenceCategoryItem
 *
 * @author Gaurav Jain
 */

class PreferenceCategoryItem {
    var id = ""
    var name = ""
    var info:String ?=null
    var showArrow:Boolean = false
    var isDisabled:Boolean = false
    var switchStatus: Boolean?= null
    var subOptionsRadioBtnOptionsList : MutableList<String>? = null
    var radioOptionSelected: Int? = null


    constructor(id: String, name: String, info: String?,showArrow:Boolean, switchStatus: Boolean?, subOptionsRadioBtnOptionsList : MutableList<String>? = null, radioButtonSelected: Int?=null) {
        initialise(id,
            name,
            info,
            showArrow,
            false,
            switchStatus,
            subOptionsRadioBtnOptionsList,
            radioButtonSelected
        )
    }

    constructor(id: String, name: String, info: String?,showArrow:Boolean,isDisabled:Boolean,switchStatus:Boolean?, subOptionsRadioBtnOptionsList : MutableList<String>? = null, radioButtonSelected: Int?=null) {
        initialise(id,
                name,
                info,
                showArrow,
                isDisabled,
                switchStatus,
                subOptionsRadioBtnOptionsList,
                radioButtonSelected)
    }

    fun initialise(id: String, name: String, info: String?,showArrow:Boolean,isDisabled:Boolean,switchStatus: Boolean?, subOptionsRadioBtnOptionsList : MutableList<String>?=null, radioButtonSelected: Int?=null) {
        this.id = id
        this.name = name
        this.info = info
        this.showArrow = showArrow
        this.isDisabled = isDisabled
        this.switchStatus = switchStatus
        this.subOptionsRadioBtnOptionsList = subOptionsRadioBtnOptionsList
        this.radioOptionSelected = radioButtonSelected
    }
}