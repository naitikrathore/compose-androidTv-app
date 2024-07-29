package com.iwedia.google_reference.manager

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.ReferenceCamInfoLanguageData
import com.iwedia.cltv.platform.model.ReferenceCamInfoModuleInformation
import com.iwedia.cltv.scene.ci_popup.CiPopupScene
import com.iwedia.cltv.scene.ci_popup.CiPopupSceneListener
import com.iwedia.cltv.scene.home_scene.HomeScene
import com.iwedia.cltv.scene.parental_control.change_pin.ParentalPinSceneData
import com.iwedia.cltv.scene.preferences_info_scene.PreferencesInfoSceneData
import com.iwedia.cltv.scene.preferences_status.PreferencesStatusSceneData
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import world.SceneManager

class CiPopUpManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    var ciPlusModule: CiPlusInterface,
    private var utilsInterface: UtilsInterface
) : GAndroidSceneManager(context, worldHandler, ReferenceWorldHandler.SceneId.CI_POPUP),
    CiPopupSceneListener {

    private val TAG = javaClass.simpleName
    override fun createScene() {
        println("%%%%%%%%%% CI POPUP CREATE SCENE IN MANAGER")
        scene = CiPopupScene(context!!, this)
    }

    override fun getCamInfoModuleInfoData() {
        var camInfoModuleInformation = ReferenceCamInfoModuleInformation(
            "SMARTDTV",
            "Cl0311 -CNX01",
            "11411505428332",
            "07.00.50.03.00.05",
            "SmartCAM 3"
        )
        scene!!.refresh(camInfoModuleInformation)
    }

    override fun onCamInfoSoftwareDownloadPressed() {
        context!!.runOnUiThread {
            var sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("software_download_title")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                }

                override fun onPositiveButtonClicked() {
                    // TODO start software update
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onCamInfoSubscriptionStatusPressed() {
        context!!.runOnUiThread {
            var sceneData = PreferencesStatusSceneData(id, instanceId)
            sceneData.title = ConfigStringsManager.getStringById("subscription_status")
            //TODO dummy data
            var list = ArrayList<PreferencesStatusSceneData.StatusItem>()
            list.add(sceneData.StatusItem(true, "Name", "Start", "End", "ID"))
            list.add(sceneData.StatusItem(false, "SBB 01", "01 Feb 22", "28 Feb 22", "01800006"))
            list.add(sceneData.StatusItem(false, "SBB 01", "01 Jan 22", "31 Jan 22", "01800006"))
            list.add(sceneData.StatusItem(false, "SBB 02", "01 Feb 22", "28 Feb 22", "01000422"))
            list.add(sceneData.StatusItem(false, "SBB 02", "01 Jan 22", "31 Jan 22", "01000422"))
            sceneData.items.addAll(list)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PREFERENCES_STATUS_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onCamInfoEventStatusPressed() {
        context!!.runOnUiThread {
            var sceneData = PreferencesStatusSceneData(id, instanceId)
            sceneData.title = ConfigStringsManager.getStringById("event_status")
            //TODO dummy data
            var list = ArrayList<PreferencesStatusSceneData.StatusItem>()
            list.add(sceneData.StatusItem(true, "Name", "Start", "End", "Minutes/Credits left"))
            sceneData.items.addAll(list)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PREFERENCES_STATUS_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onCamInfoTokenStatusPressed() {
        context!!.runOnUiThread {
            var sceneData = PreferencesStatusSceneData(id, instanceId)
            sceneData.title = ConfigStringsManager.getStringById("token_status")
            //TODO dummy data
            var list = ArrayList<PreferencesStatusSceneData.StatusItem>()
            list.add(sceneData.StatusItem(true, "Purce", "Balance (Token)"))
            sceneData.items.addAll(list)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PREFERENCES_STATUS_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onCamInfoChangeCaPinPressed() {
        context!!.runOnUiThread {
            var sceneData = ParentalPinSceneData(id, instanceId)
            sceneData.sceneType = ParentalPinSceneData.CA_CHANGE_PIN_SCENE_TYPE
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PARENTAL_PIN,
                SceneManager.Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun getCamInfoMaturityRating(): String {
        // TODO dummy data
        return "G"
    }

    override fun onCamInfoConaxCaMessagesPressed() {
        context!!.runOnUiThread {
            var sceneData = PreferencesInfoSceneData(id, instanceId)
            sceneData.title = ConfigStringsManager.getStringById("messages")
            //TODO dummy data
            var list = ArrayList<PreferencesInfoSceneData.InfoData>()
            list.add(
                sceneData.InfoData(
                    "Message 1",
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod\n" +
                            "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim\n" +
                            "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea\n" +
                            "commodo consequat."
                )
            )
            list.add(
                sceneData.InfoData(
                    "Message 2",
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod\n" +
                            "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim\n" +
                            "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea\n" +
                            "commodo consequat."
                )
            )
            list.add(
                sceneData.InfoData(
                    "Message 3",
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod\n" +
                            "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim\n" +
                            "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea\n" +
                            "commodo consequat."
                )
            )
            list.add(
                sceneData.InfoData(
                    "Message 4",
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod\n" +
                            "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim\n" +
                            "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea\n" +
                            "commodo consequat."
                )
            )
            sceneData.items = list
            sceneData.type = PreferencesInfoSceneData.INFO_MESSAGES_TYPE
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PREFERENCES_INFO_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onCamInfoAboutConaxCaPressed() {
        context!!.runOnUiThread {
            var sceneData = PreferencesInfoSceneData(id, instanceId)
            sceneData.title = ConfigStringsManager.getStringById("about_conax_ca")
            //TODO dummy data
            var list = ArrayList<PreferencesInfoSceneData.InfoData>()
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("software_version"),
                    "07.00.50.03.00.05"
                )
            )
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("interface_version"),
                    "0x40"
                )
            )
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("card_number"),
                    "020 9580 7967-3"
                )
            )
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("number_of_sessions"),
                    "10"
                )
            )
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("language"),
                    "381"
                )
            )
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("ca_sys_id"),
                    "0x0B00"
                )
            )
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("chip_id"),
                    "006 2149 5407"
                )
            )
            sceneData.items = list
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PREFERENCES_INFO_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getCamInfoSettingsLanguages() {
        //TODO dummy data
        val languages = ArrayList<String>()
        languages.add("Automatic")
        languages.add("English")
        languages.add("Norsk")
        languages.add("Dansk")
        languages.add("Svenska")
        languages.add("Soumi")
        languages.add("Deutch")
        languages.add("Français")
        languages.add("Italiano")
        languages.add("Nederlands")
        languages.add("Rусский")
        var selectedLanguage: Int =
            utilsInterface!!.getPrefsValue("CamInfoSelectedLanguage", 0) as Int
        var languageData = ReferenceCamInfoLanguageData(selectedLanguage, languages)
        (scene as HomeScene).preferencesSceneWidget!!.refresh(languageData)
    }

    override fun onCamInfoSettingsLanguageSelected(position: Int) {
        utilsInterface!!.setPrefsValue("CamInfoSelectedLanguage", position)
    }

    override fun onCamInfoPopUpMessagesActivated(activated: Boolean) {
        utilsInterface!!.setPrefsValue("CamInfoPopUpMessages", activated)
    }

    override fun isCamInfoPopUpMessagesActivated(): Boolean {
        return utilsInterface.getPrefsValue("CamInfoPopUpMessages", false) as Boolean
    }

    override fun resolveConfigurableKey(keyCode: Int, action: Int): Boolean {
        return false
    }


    override fun onSceneInitialized() {
        //Get channel list and start initial playback

        println("%%%%%%%%%%%% CI POPUP MANAGER ON SCENE INIITALIZED " + data!!.getData())

        // (data.getData()).forEach { println("%%%%%%% RECEIVED ITEMS " + it) }
        scene!!.refresh(data!!.getData())
    }


    override fun onResume() {
        super.onResume()

    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onEventReceived(event: Event?) {

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onEventReceived: event ${event!!.type}")

        super.onEventReceived(event)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBackPressed(): Boolean {
        ReferenceApplication.worldHandler!!.triggerAction(
            ReferenceWorldHandler.SceneId.CI_POPUP,
            SceneManager.Action.DESTROY
        )
        return true;

    }
}
