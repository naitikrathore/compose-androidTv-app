package com.iwedia.cltv.scene.preferences_status

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.*
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
 * Preferences Cam Info Status scene
 *
 * @author Dejan Nadj
 */
class PreferencesStatusScene (context: Context, sceneListener: SceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.PREFERENCES_STATUS_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.PREFERENCES_STATUS_SCENE),
    sceneListener
) {

    // Views
    private var statusTitle: TextView? = null
    private var statusContainer: RelativeLayout? = null
    private var backButton: ReferenceDrawableButton? = null

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_preferences_status_scene, object :
            GAndroidSceneFragmentListener {
            override fun onCreated() {
                view!!.view!!.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor(ConfigColorManager.getColor("color_background"), 0.9)))
                statusTitle = view!!.findViewById(R.id.status_title)
                statusTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")) )
                backButton = view!!.findViewById(R.id.back_button)

                // Setup title text view
                statusTitle!!.setTextColor(
                    Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                statusTitle!!.typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_medium"))
                statusTitle!!.text = ConfigStringsManager.getStringById("subscription_status")

                // Setup back button
                backButton!!.isFocusable = true
                backButton!!.setDrawable(null)
                backButton!!.setText(ConfigStringsManager.getStringById("scan_back"))
                backButton!!.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(p0: View?) {
                        sceneListener.onBackPressed()
                    }
                })

                backButton!!.onFocusChangeListener =
                    View.OnFocusChangeListener { view, hasFocus ->
                        if (hasFocus) {
                            backButton!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_color_background")))
                        } else {
                            backButton!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                        }
                    }
                backButton!!.requestFocus()

                statusContainer = view!!.findViewById<RelativeLayout>(R.id.status_container)

                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    override fun refresh(data: Any?) {
        super.refresh(data)
        if (data is PreferencesStatusSceneData) {
            statusContainer!!.removeAllViews()
            statusTitle!!.text = data.title
            data.items.forEach{statusItem->
                addStatusItems(statusItem.item1, statusItem.item2, statusItem.item3, statusItem.item4, statusItem.mainItem)
            }
        }
    }

    private fun addStatusItems(item1: String, item2: String, item3: String, item4: String, setBackground: Boolean) {
        var view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.preferences_status_item, null) as ConstraintLayout
        var params = ConstraintLayout.LayoutParams(
            Utils.getDimensInPixelSize(R.dimen.custom_dim_841),
            Utils.getDimensInPixelSize(R.dimen.custom_dim_34)
        )
        var topMargin = statusContainer!!.childCount * Utils.getDimensInPixelSize(R.dimen.custom_dim_38)
        params.setMargins(0, topMargin, 0, 0)
        view.layoutParams = params
        if (setBackground) {
            view!!.background = ConfigColorManager.generateBackground("color_main_text", 17f, 0.3)
        }
        val opacity: Double = if (setBackground) 0.8 else 1.0
        if (item1 != null) {
            initStatusItemTextView(view!!.findViewById<TextView>(R.id.status_item_1)!!, item1, opacity)
        }
        if (item2 != null) {
            initStatusItemTextView(view!!.findViewById<TextView>(R.id.status_item_2)!!, item2, opacity)
        }
        if (item3 != null) {
            initStatusItemTextView(view!!.findViewById<TextView>(R.id.status_item_3)!!, item3, opacity)
        }
        if (item4 != null) {
            initStatusItemTextView(view!!.findViewById<TextView>(R.id.status_item_4)!!, item4, opacity)
        }
        statusContainer!!.addView(view)
    }

    private fun initStatusItemTextView(textView: TextView, text: String, opacity: Double) {
        textView.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        textView
            .setTextColor(Color.parseColor(ConfigColorManager.getColor(ConfigColorManager.getColor("color_main_text"), opacity)))

        textView.text = text
    }
}