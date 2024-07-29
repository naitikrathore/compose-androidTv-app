package com.iwedia.cltv.scene.preferences_info_scene

import android.content.Context
import android.graphics.Color
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import world.SceneListener

/**
 * Preferences info scene
 *
 * @author Dejan Nadj
 */
class PreferencesInfoScene (context: Context, sceneListener: SceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.PREFERENCES_INFO_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.PREFERENCES_INFO_SCENE),
    sceneListener
) {

    // Views
    private var infoTitle: TextView? = null
    private var infoContainer: RelativeLayout? = null
    private var sceneType: Int = -1

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_preferences_info_scene, object :
            GAndroidSceneFragmentListener {
            override fun onCreated() {
                view!!.view!!.setBackgroundColor(
                    Color.parseColor(
                        ConfigColorManager.getColor(
                            ConfigColorManager.getColor("color_background"), 0.9)))
                infoTitle = view!!.findViewById(R.id.info_title)

                // Setup title text view
                infoTitle!!.setTextColor(
                    Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                infoTitle!!.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_medium"))
                infoContainer = view!!.findViewById<RelativeLayout>(R.id.info_container)

                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun refresh(data: Any?) {
        super.refresh(data)

        if (data is PreferencesInfoSceneData) {
            infoContainer!!.removeAllViews()
            infoTitle!!.text = data.title
            sceneType = data.type
            if (data.type == PreferencesInfoSceneData.INFO_ABOUT_TYPE) {
                data.items.forEach{infoItem->
                    addInfoTextItem(infoItem.title, infoItem.content)
                }
            } else if (data.type == PreferencesInfoSceneData.INFO_MESSAGES_TYPE) {
                if (data.items.size > 0) {
                    data.items.forEach{infoItem->
                        addMessageView(infoItem.title, infoItem.content)
                    }

                    infoContainer!!.post{
                        var topMargin = 0
                        for (index in 0 until data.items.size) {
                            var view = infoContainer!!.getChildAt(index)
                            var params = view.layoutParams as RelativeLayout.LayoutParams
                            if (index > 0) {
                                topMargin += Utils.getDimensInPixelSize(R.dimen.custom_dim_15)
                                params.setMargins(0, topMargin, 0, 0)
                                view.layoutParams = params
                            }
                            topMargin += view!!.height
                        }
                    }
                } else {
                    infoTitle!!.visibility = View.GONE
                    addNoMessageText()
                }
            }
        }
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if (sceneType == PreferencesInfoSceneData.INFO_MESSAGES_TYPE) {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN) {
                //Enable scroll for messages scene
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    var location = intArrayOf(0, 0)
                    infoContainer!!.getChildAt(infoContainer!!.childCount - 1).getLocationOnScreen(location)
                    if (location[1] > Utils.getDimensInPixelSize(R.dimen.custom_dim_122)) {
                        infoContainer!!.scrollBy(0, 25)
                    }
                    return true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    var location = intArrayOf(0, 0)
                    infoContainer!!.getChildAt(0).getLocationOnScreen(location)
                    if (location[1] < Utils.getDimensInPixelSize(R.dimen.custom_dim_122)) {
                        infoContainer!!.scrollBy(0, -25)
                    }
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }

    private fun addInfoTextItem(title: String, content: String) {
        var view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.preferences_system_information_text_container, null) as ConstraintLayout
        var params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        var topMargin = if (infoContainer!!.childCount < 4)
            infoContainer!!.childCount * Utils.getDimensInPixelSize(R.dimen.custom_dim_46)
        else
            (infoContainer!!.childCount - 4) * Utils.getDimensInPixelSize(R.dimen.custom_dim_46)
        var startMargin = if (infoContainer!!.childCount < 4) 0 else Utils.getDimensInPixelSize(R.dimen.custom_dim_234)
        params.setMargins(startMargin, topMargin, 0, 0)
        view.layoutParams = params
        view!!.findViewById<TextView>(R.id.title)!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_light")
            )

        view!!.findViewById<TextView>(R.id.title)!!
            .setTextColor(Color.parseColor(ConfigColorManager.getColor(ConfigColorManager.getColor("color_main_text"), 0.8)))

        view!!.findViewById<TextView>(R.id.title)!!.text = title

        view!!.findViewById<TextView>(R.id.content)!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        view!!.findViewById<TextView>(R.id.content)!!
            .setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        view!!.findViewById<TextView>(R.id.content)!!.text = content
        infoContainer!!.addView(view)
    }

    private fun addMessageView(title: String, content: String) {
        var view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.preferences_system_information_text_container, null) as ConstraintLayout
        var params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        view.layoutParams = params

        view!!.findViewById<TextView>(R.id.title)!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_light")
            )

        view!!.findViewById<TextView>(R.id.title)!!
            .setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

        view!!.findViewById<TextView>(R.id.title)!!.text = title

        var contentTextParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        contentTextParams.topToBottom =  view!!.findViewById<TextView>(R.id.title)!!.id
        contentTextParams.setMargins(0, Utils.getDimensInPixelSize(R.dimen.custom_dim_8),0 ,0)
        view!!.findViewById<TextView>(R.id.content)!!.layoutParams = contentTextParams
        view!!.findViewById<TextView>(R.id.content)!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        view!!.findViewById<TextView>(R.id.content)!!
            .setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

        view!!.findViewById<TextView>(R.id.content)!!.text = content
        view!!.requestLayout()
        infoContainer!!.addView(view)
    }

    private fun addNoMessageText() {
        var textView: TextView = TextView(ReferenceApplication.applicationContext())
        var contentTextParams = RelativeLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        contentTextParams.setMargins(Utils.getDimensInPixelSize(R.dimen.custom_dim_295_5), Utils.getDimensInPixelSize(R.dimen.custom_dim_100),0 ,0)
        textView.layoutParams = contentTextParams
        textView.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        textView
            .setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        textView.textSize = 21f

        textView.text = ConfigStringsManager.getStringById("no_messages")

        infoContainer!!.addView(textView)
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
        TODO("Not yet implemented")
    }
}