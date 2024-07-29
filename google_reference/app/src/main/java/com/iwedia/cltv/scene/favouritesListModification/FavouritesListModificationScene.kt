package com.iwedia.cltv.scene.favouritesListModification

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.InputFilter
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import world.SceneListener


/**
 * Favourite list modification scene
 *
 * @author Aleksandar Lazic
 */
class FavouritesListModificationScene(context: Context, sceneListener: SceneListener) :  ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.FAVOURITES_MODIFICATION_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.FAVOURITES_MODIFICATION_SCENE),
    sceneListener
){

    private val MAX_CHARACTERS = 30
    lateinit var title : TextView
    lateinit var editText: EditText
    lateinit var icon : ImageView
    lateinit var editLayout : LinearLayout

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_scene_favourites_modification, object :
            GAndroidSceneFragmentListener {
            override fun onCreated() {
                findRefs()
                init()
                sceneListener.onSceneInitialized()
            }
        })
    }

    private fun findRefs() {
        title = view!!.findViewById(R.id.modification_title)
        title.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        title.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        val background_layout: ConstraintLayout = view!!.findViewById(R.id.background_layout)
        background_layout.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background").replace("#",ConfigColorManager.alfa_light_bg)))

        editText = view!!.findViewById(R.id.favourite_edit_text)
        editText.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
        editText.filters += InputFilter.LengthFilter(MAX_CHARACTERS)
        editText.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        editText.backgroundTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))

        icon =  view!!.findViewById(R.id.edit_icon)
        icon.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_edit_icon))
        icon.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_background")))

        editLayout = view!!.findViewById(R.id.edit_layout)
        editLayout.background = ContextCompat.getDrawable(context, R.drawable.bg_search_bar_rounded)
        editText.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        editLayout.backgroundTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))
    }

    override fun refresh(data: Any?) {
        if (data is Boolean) {
            if (data == true) {
                title.text = ConfigStringsManager.getStringById("new_favorite_list") //com.iwedia.cltv.utils.Utils.getString(R.string.new_favorite_list)
            } else {
                title.text = ConfigStringsManager.getStringById("rename_list")//com.iwedia.cltv.utils.Utils.getString(R.string.rename_list)
            }
        }

        if (data is String) {
            editText.setText(data)
        }

        super.refresh(data)
    }

    private fun init() {
        editText.requestFocus()
        editText.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_background")))

        //Show keyboard
        val imm = ReferenceApplication.get()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)

        //Search button clicked
        editText.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                //Hide keyboard
                val imm = ReferenceApplication.get()
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editText.windowToken, 0)
                (sceneListener as FavouritesListModificationSceneListener).onEnterPressed(editText.text.toString())
                return@OnEditorActionListener true
            }
            false
        })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {

    }

    override fun onDestroy() {
        super.onDestroy()
        val imm = ReferenceApplication.get()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editText.applicationWindowToken, 0)
    }
}