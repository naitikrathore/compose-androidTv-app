package com.iwedia.cltv.scene.open_source_scene

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.text.style.TypefaceSpan
import android.view.KeyEvent
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

/**
 * Evaluation Scene
 */
class OpenSourceLicenseScene(context: Context, sceneListener: OpenSourceLicenseSceneListener) :
    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.OPEN_SOURCE_LICENSE_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.OPEN_SOURCE_LICENSE_SCENE),
        sceneListener
    ) {

    private val picassoUrl = "https://github.com/square/picasso/blob/master/LICENSE.txt"
    private val firebaseUrl = "https://github.com/square/picasso/blob/master/LICENSE.txt"
    private val gsonUrl = "https://github.com/square/picasso/blob/master/LICENSE.txt"
    private val lottieUrl = "https://github.com/square/picasso/blob/master/LICENSE.txt"

    /**
     * Xml views
     */
    var titleTv: TextView? = null
    var picassoTV: TextView? = null
    var firebaseTV: TextView? = null
    var gsonTV: TextView? = null
    var lottieTV: TextView? = null

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(name, R.layout.layout_scene_open_source_license, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                findViews()
                updateViews()
                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun refresh(data: Any?) {
        super.refresh(data)
        //Update UI
    }


    private fun findViews() {
        titleTv = view!!.findViewById(R.id.open_source_license_title)
        picassoTV = view!!.findViewById(R.id.picasso_txt)
        firebaseTV = view!!.findViewById(R.id.firebase_txt)
        gsonTV = view!!.findViewById(R.id.gson_txt)
        lottieTV = view!!.findViewById(R.id.lottie_txt)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateViews() {
        titleTv?.text = spanLicenseTitle()
        picassoTV?.text =
            convertToSpanText(ConfigStringsManager.getStringById("open_source_license_picasso"), picassoUrl)
        firebaseTV?.text = convertToSpanText(
            ConfigStringsManager.getStringById("open_source_license_firebase"), firebaseUrl
        )
        gsonTV?.text =
            convertToSpanText(ConfigStringsManager.getStringById("open_source_license_gson"), gsonUrl)
        lottieTV?.text =
            convertToSpanText(ConfigStringsManager.getStringById("open_source_license_lottie"), lottieUrl)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun spanLicenseTitle(): CharSequence {
        val spannableStringBuilder = SpannableStringBuilder()
        val spannableTitle: Spannable = SpannableString(ConfigStringsManager.getStringById("open_source_license_title"))
                spannableStringBuilder.append(setSpanStyleMedium(spannableTitle))
        return spannableStringBuilder
    }

    /**
     * To covert a android resource into span.
     */

    @RequiresApi(Build.VERSION_CODES.P)
    private fun convertToSpanText(stringTitle: String, stringDes: String): CharSequence {
        val spannableStringBuilder = SpannableStringBuilder()
        val spannableTitle: Spannable = SpannableString(stringTitle)
        val spannableDes: Spannable = SpannableString("\n" + stringDes)
        spannableStringBuilder.append(setSpanStyleMedium(spannableTitle))
        spannableStringBuilder.append(setSpanStyleRegular(spannableDes))
        return spannableStringBuilder
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setSpanStyleMedium(spannable: Spannable): Spannable {
        var typefaceMedium = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )

        spannable.setSpan(
            TypefaceSpan(typefaceMedium),
            0,
            spannable.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#eeeeee")),
            0,
            spannable.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setSpanStyleRegular(spannable: Spannable): Spannable {
        var typefaceRegular = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )

        spannable.setSpan(
            TypefaceSpan(typefaceRegular),
            0,
            spannable.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#b7b7b7")),
            0,
            spannable.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return spannable
    }



    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            (sceneListener as OpenSourceLicenseSceneListener).onBackPressed()
        }
        return true
    }
}