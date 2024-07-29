package com.iwedia.cltv.scene.ci_popup

import com.iwedia.cltv.scene.ReferenceSceneListener

/**
 * Ci popup scene listener
 *
 * @author Dejan Nadj
 */
interface CiPopupSceneListener : ReferenceSceneListener {

    fun getCamInfoModuleInfoData()
    fun onCamInfoSoftwareDownloadPressed()
    fun onCamInfoSubscriptionStatusPressed()
    fun onCamInfoEventStatusPressed()
    fun onCamInfoTokenStatusPressed()
    fun onCamInfoChangeCaPinPressed()
    fun getCamInfoMaturityRating(): String
    fun onCamInfoConaxCaMessagesPressed()
    fun onCamInfoAboutConaxCaPressed()
    fun getCamInfoSettingsLanguages()
    fun onCamInfoSettingsLanguageSelected(position: Int)
    fun onCamInfoPopUpMessagesActivated(activated: Boolean)
    fun isCamInfoPopUpMessagesActivated(): Boolean
}