package com.iwedia.cltv.platform.`interface`

import java.io.InputStream

interface GeneralConfigInterface {
    fun setup(raw: InputStream)
    fun getGeneralSettingsInfo(generalParam: String): Boolean
    fun getCountryThatIsSelected(): String
    val getEpgMergeStatus: Boolean
}