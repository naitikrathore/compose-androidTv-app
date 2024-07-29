package com.iwedia.cltv.scene.evaluation_scene

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.widget.NestedScrollView
import com.iwedia.cltv.*
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

/**
 * Evaluation Scene
 */
class EvaluationScene(context: Context, sceneListener: EvaluationSceneListener) :

    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.EVALUATION_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.EVALUATION_SCENE),
        sceneListener
    ) {

    /**
     * Xml views
     */
    var messageTv: TextView? = null
    var titleTv: TextView? = null
    var accept: ReferenceDrawableButton? = null
    var arrowDownUp: Button? = null
    private var evelScrollView: NestedScrollView? = null


    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(name, R.layout.layout_scene_evaluation, object :
            GAndroidSceneFragmentListener {

            @RequiresApi(Build.VERSION_CODES.P)
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
        titleTv = view!!.findViewById(R.id.evaluation_title)
        messageTv = view!!.findViewById(R.id.evaluation_message)
        accept = view!!.findViewById(R.id.evaluation_accept)
        arrowDownUp = view!!.findViewById(R.id.arrow_up_down)
        evelScrollView = view!!.findViewById(R.id.evaluation_srl_vw)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateViews() {
        titleTv!!.setTextColor(Color.parseColor("#eeeeee"))
        titleTv?.text = spanLicenseTitle()

        messageTv?.text = spanLicenseDescription()

        val isFirstRun = (sceneListener as EvaluationSceneListener).isFirstRun()
        if (isFirstRun) {
            accept?.visibility = View.GONE
        }
        accept?.setText(ConfigStringsManager.getStringById("evaluation_license_accept"))
        accept?.setOnClickListener(View.OnClickListener {
            (sceneListener as EvaluationSceneListener).onAccepted()
        })
        evelScrollView?.setOnScrollChangeListener(View.OnScrollChangeListener { view, i, i2, i3, i4 ->
            if (!evelScrollView?.canScrollVertically(1)!!) {
                // bottom of scroll view
                arrowDownUp?.setBackgroundResource(R.drawable.ic_evl_arrow_up)

            }
            if (!evelScrollView?.canScrollVertically(-1)!!) {
                // top of scroll view
                arrowDownUp?.setBackgroundResource(R.drawable.ic_evl_arrow_down)


            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun spanLicenseTitle(): CharSequence {
        val spannableStringBuilder = SpannableStringBuilder()
        val stringTitle = SpannableString(context.getString(R.string.evaluation_license_title));
        val spannable: Spannable = SpannableString(stringTitle)
        spannableStringBuilder.append(setSpanStyleMedium(spannable))
        return spannableStringBuilder
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun spanLicenseDescription(): CharSequence {
        val desTitle = context.resources.getStringArray(R.array.evaluation_license_dsc_title)
        val desT = context.resources.getStringArray(R.array.evaluation_license_dsc)

        val ia = desTitle.iterator()
        val ib = desT.iterator()

        var androidResourceList = mutableListOf<String>()
        while (ia.hasNext() && ib.hasNext()) {
            val va = ia.next()
            val vb = ib.next()
            androidResourceList.add(va)
            androidResourceList.add(vb)
        }
        return convertToSpanList(androidResourceList)
    }

    /**
     * To covert a android resource array into span list.
     */

    @RequiresApi(Build.VERSION_CODES.P)
    private fun convertToSpanList(stringList: List<String>): CharSequence {
        val spannableStringBuilder = SpannableStringBuilder()
        stringList.forEachIndexed { index, text ->
            val line: CharSequence = text
            val spannable: Spannable = SpannableString(line)
            if (index % 2 == 0) {
                setSpanStyleMedium(spannable)
            } else {
                setSpanStyleRegular(spannable)
            }
            spannableStringBuilder.append(spannable)
        }
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
        return false

    }
}