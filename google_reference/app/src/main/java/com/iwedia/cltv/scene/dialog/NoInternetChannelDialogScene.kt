package com.iwedia.cltv.scene.dialog

import android.content.Context
import android.graphics.Color
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.*
import com.iwedia.cltv.components.ButtonType
import com.iwedia.cltv.components.CustomButton
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import kotlinx.coroutines.Dispatchers
import world.SceneListener


/**
 * No Internet Dialog scene
 *
 * @author Gaurav Jain
 */
class NoInternetChannelDialogScene(context: Context, sceneListener: SceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.NO_INTERNET_DIALOG_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.NO_INTERNET_DIALOG_SCENE),
    sceneListener
) {

    private var dialogTitle: TextView? = null
    private var dialogMessage: TextView?= null
    private var dialogSubMessage:TextView?=null
    private var dialogImage: ImageView? =null
    private var buttonsContainer: LinearLayout? = null
    private var button: CustomButton? = null
    private var doubleBackToExitPressedOnce = false

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_no_internet_channel_dialog, object :
            GAndroidSceneFragmentListener {
            override fun onCreated() {
                val dialogSceneLayout: ConstraintLayout = view!!.findViewById(R.id.dialog_scene_layout)
                dialogSceneLayout.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
                dialogTitle = view!!.findViewById(R.id.dialog_title)
                dialogTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                dialogMessage = view!!.findViewById(R.id.dialog_message)
                dialogMessage!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                dialogSubMessage = view!!.findViewById(R.id.dialog_sub_message)
                dialogSubMessage!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                dialogImage = view!!.findViewById(R.id.dialog_image)
                buttonsContainer = view!!.findViewById(R.id.buttonsContainer)
                dialogTitle!!.setTextColor(
                    Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                dialogTitle!!.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
                dialogMessage!!.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
                dialogMessage!!.setTextColor(
                    Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                dialogSubMessage!!.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
                dialogSubMessage!!.setTextColor(
                    Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun refresh(data: Any?) {
        if (Utils.isDataType(data, DialogSceneData::class.java)) {
            dialogTitle!!.text = (data as DialogSceneData).title

            // Set dialog message
            if (data.message != null) {
                dialogMessage!!.visibility = View.VISIBLE
                dialogMessage!!.text = data.message
                dialogTitle!!.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_medium"))
                dialogTitle!!.requestLayout()
                dialogSubMessage!!.visibility = View.VISIBLE

            }
            // Set dialog image
            if (data.imageRes != -1) {
                dialogImage!!.visibility = View.VISIBLE
                dialogImage!!.layoutParams.height = 70
                dialogImage!!.layoutParams.width = 70
                dialogImage!!.setImageResource(data.imageRes)
            }

            if (data.positiveButtonEnabled) {
                var buttonType = ButtonType.RETRY
                if (data.title ==  ConfigStringsManager.getStringById("channel_list_not_available"))
                    buttonType = ButtonType.EXIT

                button = CustomButton(context,buttonType,buttonType.text)

                var exitApp =false
                button!!.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_UP){
                        if (keyCode==KeyEvent.KEYCODE_BACK) {
                            if (exitApp) ReferenceApplication.get().activity?.finish()
                            else return@OnKeyListener true

                        }
                    }else if (event.action == KeyEvent.ACTION_DOWN){
                        if (keyCode==KeyEvent.KEYCODE_BACK) {
                            exitApp = true
                        }

                    }
                    return@OnKeyListener false
                })

                button!!.setOnClick {
                    (sceneListener as DialogSceneListener).onPositiveButtonClicked()
                }

                //gone to visible -> remove existing views (bug)
                buttonsContainer!!.visibility = View.GONE
                buttonsContainer!!.removeAllViews()
                buttonsContainer!!.addView(button)
                buttonsContainer!!.visibility = View.VISIBLE
                buttonsContainer!!.getChildAt(0).requestFocus()
            }
        }

        super.refresh(data)
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
            when (keyCode) {
                KeyEvent.KEYCODE_ESCAPE,
                KeyEvent.KEYCODE_BACK -> {
                    if (doubleBackToExitPressedOnce) {
                        (sceneListener as DialogSceneListener).exitApplication()
                        return true
                    }

                    this.doubleBackToExitPressedOnce = true
                    (sceneListener as DialogSceneListener).showToast(ConfigStringsManager.getStringById("please_click_again"))

                    CoroutineHelper.runCoroutineWithDelay({
                        doubleBackToExitPressedOnce = false
                    }, 2000, Dispatchers.Main)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {

    }
}