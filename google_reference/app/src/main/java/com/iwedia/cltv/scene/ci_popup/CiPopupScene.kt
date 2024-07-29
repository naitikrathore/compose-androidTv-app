package com.iwedia.cltv.scene.ci_popup

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.PreferencesCamInfoWidget
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.ReferenceCamInfoModuleInformation
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import data_type.GList

class CiPopupScene(context: Context, sceneListener: CiPopupSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.CI_POPUP,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.CI_POPUP),
    sceneListener
) {

    private val TAG = javaClass.simpleName

    private var camInfoWidget: PreferencesCamInfoWidget? = null
    private var container: RelativeLayout? = null

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_scene_ci_popup, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {

                println("%%%%%%%%% CREATE CI POPUP SCENE")

                container = view!!.findViewById(R.id.ciContainer)

                //Cam Info
                if (camInfoWidget == null) {

                    println("%%%%%%%%% CREATE CI CAM INFO WIDGET")
                    //todo musika
                    /*
                    camInfoWidget =
                        PreferencesCamInfoWidget(PreferencesCamInfoWidget.Type.POPUP, object :
                            PreferencesCamInfoWidget.PreferencesCamInfoWidgetListener {
                            override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                                return false
                            }

                            override fun getModuleInfoData() {
                                (sceneListener as CiPopupSceneListener).getCamInfoModuleInfoData()
                            }

                            override fun requestFocusOnPreferencesMenu() {
                                //TODO
                                //preferencesFilterGridView!!.getChildAt(3).requestFocus()
                            }

                            override fun onSoftwareDownloadPressed() {
                                (sceneListener as CiPopupSceneListener).onCamInfoSoftwareDownloadPressed()
                            }

                            override fun onSubscriptionStatusPressed() {
                                (sceneListener as CiPopupSceneListener).onCamInfoSubscriptionStatusPressed()
                            }

                            override fun onEventStatusPressed() {
                                (sceneListener as CiPopupSceneListener).onCamInfoEventStatusPressed()
                            }

                            override fun onTokenStatusPressed() {
                                (sceneListener as CiPopupSceneListener).onCamInfoTokenStatusPressed()
                            }

                            override fun onChangeCaPinPressed() {
                                (sceneListener as CiPopupSceneListener).onCamInfoChangeCaPinPressed()
                            }

                            override fun getMaturityRating(): String {
                                return (sceneListener as CiPopupSceneListener).getCamInfoMaturityRating()
                            }

                            override fun onConaxCaMessagesPressed() {
                                (sceneListener as CiPopupSceneListener).onCamInfoConaxCaMessagesPressed()
                            }

                            override fun onAboutConaxCaPressed() {
                                (sceneListener as CiPopupSceneListener).onCamInfoAboutConaxCaPressed()
                            }

                            override fun getSettingsLanguageList() {
                                (sceneListener as CiPopupSceneListener).getCamInfoSettingsLanguages()
                            }

                            override fun onSettingsLanguageSelected(position: Int) {
                                (sceneListener as CiPopupSceneListener).onCamInfoSettingsLanguageSelected(
                                    position
                                )
                            }

                            override fun activatePopUpMessage(activate: Boolean) {
                                (sceneListener as CiPopupSceneListener).onCamInfoPopUpMessagesActivated(
                                    activate
                                )
                            }

                            override fun isPopUpMessagesActivated(): Boolean {
                                return (sceneListener as CiPopupSceneListener).isCamInfoPopUpMessagesActivated()
                            }

                            override fun onPrefsCategoriesRequestFocus(position: Int) {
                                //TODO
//                            preferencesFilterGridView!!.getChildAt(position)
//                                .requestFocus()
                            }

                            override fun onBackPress() {
                               sceneListener.onBackPressed()
                            }
                        })

                    */
                }

                var params = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
                params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                camInfoWidget!!.view!!.layoutParams = params
                container?.addView(camInfoWidget?.view)


                // camInfoWidget!!.refresh(list)
                camInfoWidget!!.requestFocus()
                //(sceneListener as CiPopupSceneListener).getCamInfoModuleInfoData()
                camInfoWidget!!.setFocusToGrid()

                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {

    }


    override fun onResume() {
        super.onResume()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun refresh(data: Any?) {

        println("%%%%%%%%%% REFRESH CI POPUP SCENE " + data)


        //%%%%%%%%%% REFRESH CI POPUP SCENE [Authentication success, SAC establishment success, URI version negotiation success, CCKey computation success, Content Control Running]
        if (data is GList<*>) {

            camInfoWidget!!.refresh(data)
        }

        if (data is ReferenceCamInfoModuleInformation) {
            camInfoWidget!!.refresh(data)
        }

        super.refresh(data)
    }


}