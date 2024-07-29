package com.iwedia.cltv.scene.dialog

import android.content.Context
import android.graphics.Color
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.*
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import kotlinx.coroutines.Dispatchers
import world.SceneListener


/**
 * Dialog scene
 *
 * @author Aleksandar Lazic
 */
class DialogScene(context: Context, sceneListener: SceneListener) : ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.DIALOG_SCENE),
        sceneListener
) {

    private var dialog_scene_layout: ConstraintLayout? = null
    private var dialogTitle: TextView? = null
    private var dialogMessage: TextView?= null
    private var dialogSubMessage:TextView?=null
    private var dialogImage: ImageView? =null
    private var buttonsContainer: LinearLayout? = null
    private var positiveButton: ReferenceDrawableButton? = null
    private var negativeButton: ReferenceDrawableButton? = null
    private var hasUserClicked = false
    private var defaultInputType : String ? = "TV"
    private var type: DialogSceneData.DialogType? = null

    val TAG = javaClass.simpleName

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_scene_dialog, object :
                GAndroidSceneFragmentListener {
            override fun onCreated() {
                try {
                    dialog_scene_layout = view?.findViewById(R.id.dialog_scene_layout)
                }catch (E: Exception){
                    println(E)
                    ReferenceApplication.worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.HOME_SCENE)
                }
                dialog_scene_layout?.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
                dialogTitle = view?.findViewById(R.id.dialog_title)
                dialogTitle?.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                dialogMessage = view?.findViewById(R.id.dialog_message)
                dialogMessage?.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                dialogSubMessage = view?.findViewById(R.id.dialog_sub_message)
                dialogSubMessage?.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                dialogImage = view?.findViewById(R.id.dialog_image)
                buttonsContainer = view?.findViewById(R.id.buttonsContainer)
                dialogTitle?.setTextColor(
                    Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                dialogTitle?.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
                dialogMessage?.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
                dialogMessage?.setTextColor(
                    Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                dialogSubMessage?.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
                dialogSubMessage?.setTextColor(
                    Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun refresh(data: Any?) {
        if (Utils.isDataType(data, DialogSceneData::class.java)) {
            dialogTitle?.text = (data as DialogSceneData).title
            data.title?.let {
                (sceneListener as DialogSceneListener).setSpeechText(it, importance =  SpeechText.Importance.HIGH)
            }
            type = (data as DialogSceneData).type
            if(data.type == DialogSceneData.DialogType.INACTIVITY_TIMER){
                dialogSubMessage?.visibility = View.GONE
                var timer = object :
                    CountDownTimer(
                        60000,
                        1000
                    ) {
                    override fun onTick(millisUntilFinished: Long) {
                        ReferenceApplication.runOnUiThread(Runnable {
                            dialogTitle?.text = data.title + "\n 0:${millisUntilFinished / 1000}"
                        })
                    }
                    override fun onFinish() {
                        ReferenceApplication.runOnUiThread(Runnable {
                            dialogTitle?.text = data.title + "\n 0:0"
                        })
                    }
                }
                timer!!.start()
            }

            // Set dialog message
            if (data.message != null) {
                if(dialogTitle == null){
                    ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    return
                }
                dialogMessage?.visibility = View.VISIBLE
                dialogMessage?.text = data.message

                (sceneListener as DialogSceneListener).setSpeechText(
                    data.title ?: ConfigStringsManager.getStringById("no_information"),
                    data.message!!,
                    importance = SpeechText.Importance.HIGH
                )
                dialogTitle?.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_medium"))
                var titleParams = dialogTitle?.layoutParams as ConstraintLayout.LayoutParams
                titleParams.setMargins(0, Utils.getDimensInPixelSize(R.dimen.custom_dim_221), 0, 0)
                dialogTitle?.requestLayout()

                var buttonsParams = buttonsContainer?.layoutParams as ConstraintLayout.LayoutParams
                buttonsParams.setMargins(0, Utils.getDimensInPixelSize(R.dimen.custom_dim_77_5), 0, 0)

                if(data.subMessage != null){
                    if(data.type == DialogSceneData.DialogType.SCHEDULED_REMINDER){
                        var timer = object :
                            CountDownTimer(
                                30000,
                                1000
                            ) {
                            override fun onTick(millisUntilFinished: Long) {
                                ReferenceApplication.runOnUiThread(Runnable {
                                    dialogSubMessage?.text = data.subMessage + "0:${millisUntilFinished / 1000}"
                                })
                            }
                            override fun onFinish() {
                                ReferenceApplication.runOnUiThread(Runnable {
                                    dialogSubMessage?.text = data.subMessage + "0:0"
                                })
                            }
                        }
                        timer!!.start()
                    }else{
                        val remainingMillis :Long

                        if(data.getData() !=null ){
                            remainingMillis = (data.getData() as Array<*> ).get(0) as Long
                            Log.d(Constants.LogTag.CLTV_TAG + "SCHEDULE_", "DIALOG DURATION BEFORE START (IN MILLIS) "+remainingMillis)
                        }  else  remainingMillis = 60000L
                        var timer = object :
                            CountDownTimer(
                                remainingMillis,
                                1000
                            ) {
                            override fun onTick(millisUntilFinished: Long) {
                                ReferenceApplication.runOnUiThread(Runnable {
                                    if(data.type == DialogSceneData.DialogType.SCHEDULER){
                                        dialogSubMessage?.text = data.subMessage + "${millisUntilFinished / 1000}" + "\n" + "\n" +
                                                ConfigStringsManager.getStringById("scheduled_recording_reminder_usb_message")
                                    }else{
                                        dialogSubMessage?.text = data.subMessage + "${millisUntilFinished / 1000}"
                                    }
                                })

                            }
                            override fun onFinish() {
                                //Close pvr banner if it's still visible
                                ReferenceApplication.runOnUiThread(Runnable {
                                    dialogSubMessage?.text = data.subMessage + "0"
                                    if(!hasUserClicked) {
                                        if(data.type == DialogSceneData.DialogType.SCHEDULER){
                                            (sceneListener as DialogSceneListener).onPositiveButtonClicked()
                                        } else {
                                            (sceneListener as DialogSceneListener).onNegativeButtonClicked()
                                        }
                                    }
                                })
                            }
                        }
                        timer!!.start()
                    }
                    dialogSubMessage?.visibility = View.VISIBLE
                }
            }
            // Set dialog image
            if(data.imageRes != -1) {
                dialogImage?.visibility = View.VISIBLE
                if(data.imageRes!=R.drawable.ic_watchlist_reminder){
                    dialogImage?.getLayoutParams()?.height = 120
                    dialogImage?.getLayoutParams()?.width = 120
                }
                dialogImage?.setImageResource(data.imageRes)
            }

            negativeButton = ReferenceDrawableButton(context)
            if (data.negativeButtonText != null) {
                negativeButton?.setText(data.negativeButtonText!!)
            } else {
                negativeButton?.visibility = View.GONE
            }
            negativeButton?.isFocusable = true
            negativeButton?.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            negativeButton?.setDrawable(null)
            negativeButton?.setOnClickListener { hasUserClicked = true
                (sceneListener as DialogSceneListener).onNegativeButtonClicked() }

            negativeButton?.onFocusChangeListener = object : View.OnFocusChangeListener {
                override fun onFocusChange(view: View?, hasFocus: Boolean) {
                    if (hasFocus) {
                        try {
                            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color_context $color_context")
                            negativeButton?.setTextColor(color_context)
                            (sceneListener as DialogSceneListener).setSpeechText(data.negativeButtonText!!)
                        } catch(ex: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
                        }
                    } else {
                        try {
                            val color_context = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color_context $color_context")
                            negativeButton?.setTextColor(color_context)
                        } catch(ex: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
                        }
                    }
                }
            }

            if (data.positiveButtonEnabled) initPositiveButton(data)

            //gone to visible -> remove existing views (bug)
            buttonsContainer?.visibility = View.GONE
            buttonsContainer?.removeAllViews()

            addButtonsBasedOnDialogType(data)

            CoroutineHelper.runCoroutine({
                buttonsContainer?.visibility = View.VISIBLE
                try {
                    if (data.positiveButtonEnabled) buttonsContainer!!.getChildAt(0).requestFocus()
                }catch (E: Exception){
                    println(E.printStackTrace())
                }
            }, Dispatchers.Main)

            //if keyboard is open (in search scene) close the keyboard
            val imm = ReferenceApplication.get()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (imm.isActive) {
                imm.hideSoftInputFromWindow(dialog_scene_layout?.windowToken, 0)
            }


        }

        if(data is String) {
            defaultInputType = data
        }
        super.refresh(data)
    }

    fun hideButtons(){
        dialogTitle?.text = ConfigStringsManager.getStringById("create_tsfile")
        positiveButton?.visibility = View.GONE
        negativeButton?.visibility = View.GONE
    }

    private fun addButtonsBasedOnDialogType(data: DialogSceneData) {
        if (data.type == DialogSceneData.DialogType.TEXT || data.type == DialogSceneData.DialogType.INACTIVITY_TIMER) {
            addButtonIfEnabled(positiveButton, data.positiveButtonEnabled)
        } else {
            buttonsContainer?.addView(negativeButton)
            addButtonIfEnabled(positiveButton, data.positiveButtonEnabled)
        }
    }

    private fun addButtonIfEnabled(button: ReferenceDrawableButton?, enabled: Boolean) {
        if (enabled) buttonsContainer?.addView(button)
    }

    private fun initPositiveButton(data: DialogSceneData) {
        positiveButton = ReferenceDrawableButton(context)
        positiveButton?.setText(data.positiveButtonText!!)
        positiveButton?.isFocusable = true
        positiveButton?.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        positiveButton?.setDrawable(null)
        positiveButton?.post {
            val params = positiveButton?.layoutParams as LinearLayout.LayoutParams

            if (negativeButton?.visibility == View.VISIBLE) {
                params.marginStart = context.resources.getDimensionPixelSize(R.dimen.custom_dim_25)
            }

            positiveButton?.layoutParams = params
        }
        positiveButton?.setOnClickListener {
            hasUserClicked = true
            (sceneListener as DialogSceneListener).onPositiveButtonClicked()
        }

        positiveButton?.onFocusChangeListener = object : View.OnFocusChangeListener {
            override fun onFocusChange(view: View?, hasFocus: Boolean) {
                if (hasFocus) {
                    try {
                        val color_context =
                            Color.parseColor(ConfigColorManager.getColor("color_background"))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color_context $color_context")
                        positiveButton!!.setTextColor(color_context)

                        (sceneListener as DialogSceneListener).setSpeechText(data.positiveButtonText!!)
                    } catch (ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
                    }
                } else {
                    try {
                        val color_context =
                            Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color_context $color_context")
                        positiveButton!!.setTextColor(color_context)
                    } catch (ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
                    }
                }
            }
        }
    }


    override fun parseConfig(sceneConfig: SceneConfig?) {

    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if(dialogTitle!!.text == ConfigStringsManager.getStringById("timeshift_file") || dialogTitle!!.text == ConfigStringsManager.getStringById("pvr_playback_exit_msg")){
            when(keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (negativeButton!!.hasFocus()) {
                        positiveButton!!.requestFocus()
                    }
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (positiveButton!!.hasFocus()) {
                        negativeButton!!.requestFocus()
                    }
                }
            }
            return true
        }
        if(dialogTitle!!.text ==ConfigStringsManager.getStringById("create_tsfile")){
            return true
        }

        if (dialogTitle!!.text.contains(ConfigStringsManager.getStringById("no_signal_power_off_inactivity_msg"))) {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN || (keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
                (sceneListener as DialogSceneListener).onPositiveButtonClicked()
                return true
            }
        }

        if (defaultInputType?.contains("Composite") == true && (dialogTitle!!.text == ConfigStringsManager.getStringById("rrt5_reset"))) {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
                when(keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (negativeButton?.hasFocus() == true) {
                            buttonsContainer?.getChildAt(1)?.requestFocus()
                        } else {
                            return true
                        }
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (positiveButton?.hasFocus() == true) {
                            buttonsContainer?.getChildAt(0)?.requestFocus()
                        } else {
                            return true
                        }
                    }
                }
            }
        }

        if (type == DialogSceneData.DialogType.SCHEDULED_REMINDER) {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN) {
                when(keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (negativeButton?.hasFocus() == true) {
                            buttonsContainer?.getChildAt(1)?.requestFocus()
                        }
                        return true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (positiveButton?.hasFocus() == true) {
                            buttonsContainer?.getChildAt(0)?.requestFocus()
                        }
                        return true
                    }
                }
            }
        }


        return super.dispatchKeyEvent(keyCode, keyEvent)
    }
}