package com.iwedia.cltv.scene.postal_code

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication.Companion.applicationContext
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider.Companion.getTypeFace
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager.Companion.getFont
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import com.iwedia.guide.android.widgets.helpers.BaseLinearLayoutManager

/**
 * Postal code scene
 *
 * @author Tanvi Raut
 */
class PostalCodeScene(context: Context, sceneListener: PostalCodeSceneListener) :

    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.POSTAL_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.POSTAL_SCENE),
        sceneListener
    ) {

    private var tvTitle: TextView? = null
    private var tvSubtitle: TextView? = null
    private var tvPressOk: TextView? = null
    private lateinit var postalCodeAdapter: PostalCodeAdapter
    lateinit var postalCodeRecycler: RecyclerView
    private val NUMBER_OF_POSTAL_CODE_ITEMS = 5
    var currentPostalCodePosition = -1

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_scene_postal_code, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                findRefs()
                tvTitle!!.text = ConfigStringsManager.getStringById("postal_code")
                tvSubtitle!!.text = ConfigStringsManager.getStringById("enter_postal_code")
                postalCodeAdapter.registerListener(object : PostalCodeAdapter.PostalCodeListener {

                    override fun onPostalCodeConfirmed(postalCode: String?) {
                        if (postalCode == "00000") {
                            Toast.makeText(
                                context, ConfigStringsManager.getStringById("invalid_input"),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else (sceneListener as PostalCodeSceneListener).onPostalConfirmed(
                            postalCode!!
                        )
                    }

                    override fun getAdapterPosition(position: Int) {
                        currentPostalCodePosition = position
                        if (position == 4) {
                            tvPressOk!!.visibility = View.VISIBLE
                        } else {
                            tvPressOk!!.visibility = View.INVISIBLE
                        }
                    }

                    override fun previous() {}

                    override fun next() {
                        currentPostalCodePosition += 1
                        if (currentPostalCodePosition > NUMBER_OF_POSTAL_CODE_ITEMS - 1) {
                            currentPostalCodePosition = NUMBER_OF_POSTAL_CODE_ITEMS - 1
                        }
                        if (!(sceneListener as PostalCodeSceneListener).isAccessibilityEnabled()) {
                            postalCodeRecycler.getChildAt(currentPostalCodePosition).requestFocus()
                        }
                    }

                    override fun validationEnabled() {
                    }

                    override fun isAccessibilityEnabled(): Boolean {
                        return (sceneListener as PostalCodeSceneListener).isAccessibilityEnabled()
                    }
                })
                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {}

    private fun findRefs() {
        val constraintLayout: ConstraintLayout = view!!.findViewById(R.id.container)
        constraintLayout.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background")))

        //setup title
        tvTitle = view!!.findViewById(R.id.title)
        tvTitle!!.typeface = getTypeFace(applicationContext(), getFont("font_medium"))
        tvTitle!!.text = ConfigStringsManager.getStringById("postal_code")
        tvTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        //setup subtitle
        tvSubtitle = view!!.findViewById(R.id.subtitle)
        tvSubtitle!!.typeface = getTypeFace(applicationContext(), getFont("font_regular"))
        tvSubtitle!!.text = ConfigStringsManager.getStringById("enter_postal_code")
        tvSubtitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        //setup recycler and adapter
        postalCodeRecycler = view!!.findViewById(R.id.postal_code_digit)
        var layoutManager = LinearLayoutManager(context)
        if (!(sceneListener as PostalCodeSceneListener).isAccessibilityEnabled()) {
            layoutManager = BaseLinearLayoutManager(context)
        }
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        postalCodeRecycler.layoutManager = layoutManager
        postalCodeAdapter = PostalCodeAdapter(getPostalCodeItems(NUMBER_OF_POSTAL_CODE_ITEMS))
        postalCodeRecycler.adapter = postalCodeAdapter

        //setup second title
        tvPressOk = view!!.findViewById(R.id.pressOkTv)
        tvPressOk!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        tvPressOk!!.typeface = getTypeFace(applicationContext(), getFont("font_regular"))
        tvPressOk!!.text = ConfigStringsManager.getStringById("press_ok_to_confirm")
        tvPressOk!!.visibility = View.INVISIBLE

        if ((sceneListener as PostalCodeSceneListener).isAccessibilityEnabled()) {
            postalCodeRecycler.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            postalCodeRecycler.isFocusable = false
        }

        if (!(sceneListener as PostalCodeSceneListener).isAccessibilityEnabled()) {
            postalCodeRecycler.requestFocus()
        }
    }

    /**
     * Get postal code items
     *
     * @return postal code items
     */
    private fun getPostalCodeItems(numberOfItems: Int): MutableList<PostalCodeItem> {
        val postalCodeItems = mutableListOf<PostalCodeItem>()
        val postalCode = (sceneListener as PostalCodeSceneListener).getPostalCode()!!
        for (i in 0 until numberOfItems) {
            postalCodeItems.add(
                PostalCodeItem(
                    i, PostalCodeItem.TYPE_CODE, Integer.parseInt(postalCode.substring(i, i + 1))
                )
            )
        }
        return postalCodeItems
    }
}