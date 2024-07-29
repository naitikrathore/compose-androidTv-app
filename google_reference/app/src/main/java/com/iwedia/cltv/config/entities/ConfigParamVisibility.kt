package com.iwedia.cltv.config.entities

class ConfigParamVisibility : ConfigParam {

    object Visibility {
        const val VISIBLE = 0
        const val INVISIBLE = 4
        const val GONE = 8
    }

    companion object {
        fun getVisibility(s: String) : Int {
            if (s == "visible") {
                return Visibility.VISIBLE
            } else if (s == "gone") {
                return Visibility.GONE
            } else if (s == "invisible") {
                return Visibility.INVISIBLE
            }

            return Visibility.VISIBLE
        }
    }


    var visibility : Int = -1

    constructor(id: Int, name: String, value: Int) : super(id, name) {
        this.visibility = value
        this.value.add(value)
    }
}