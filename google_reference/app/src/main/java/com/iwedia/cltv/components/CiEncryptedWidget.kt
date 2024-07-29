package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
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
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import world.widget.GWidget
import world.widget.GWidgetListener

class CiEncryptedWidget :
    GWidget<ConstraintLayout, CiEncryptedWidget.CiEncryptedWidgetListener> {

    lateinit var title: TextView
    lateinit var description: TextView
    lateinit var editText: EditText
    lateinit var icon: ImageView
    lateinit var editLayout: LinearLayout

    @SuppressLint("SetTextI18n")
    constructor(context: Context, listener: CiEncryptedWidget.CiEncryptedWidgetListener) : super(
        ReferenceWorldHandler.WidgetId.CI_ENCRYPTED,
        ReferenceWorldHandler.WidgetId.CI_ENCRYPTED,
        listener
    ) {

        findRefs(context)
        init()
    }

    private fun findRefs(context: Context) {
        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_widget_ci_encrypted, null) as ConstraintLayout

        val channel_list_bg: ConstraintLayout = view!!.findViewById(R.id.channel_list_bg)
        val bg = ConfigColorManager.getColor("color_background") //color_background

        channel_list_bg.setBackgroundColor(Color.parseColor(bg))

        title = view!!.findViewById(R.id.title)
        title.setText("CI Enquire")
        title.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        title.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )

        description = view!!.findViewById(R.id.description)
        description.setText("Select Profile Type between 0 and 1")
        description.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        description.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )

        editText = view!!.findViewById(R.id.edit_text)
        title.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
        editText.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )

        icon = view!!.findViewById(R.id.edit_icon)
        icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_edit_icon))

        editLayout = view!!.findViewById(R.id.edit_layout)
        editLayout.background = ContextCompat.getDrawable(context, R.drawable.bg_search_bar_rounded)
        editText.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

    }

    private fun init() {
        editText.requestFocus()
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
                println("CIE ${editText.text.toString()}") //todo send
                if (editText.text.toString().length > 0) {
                    listener.enquiryAnswer(false, editText.text.toString())
                }
                return@OnEditorActionListener true
            }
            false
        })

        editText!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        println("CIE dispatchKeyEvent KEYCODE_BACK")
                        listener.enquiryAnswer(true, "")
                        return true
                    }
                    if(keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                        listener.onPreviousChannel()
                        return true
                    }

                    if(keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                        listener.onNextChannel()
                        return true
                    }
                }
                return false
            }
        })
    }

    interface CiEncryptedWidgetListener : GWidgetListener {
        fun enquiryAnswer(abort: Boolean, answer: String)
        fun onNextChannel()

        fun onPreviousChannel()
    }

    override fun refresh(data: Any) {
        super.refresh(data)

        println("MMi CiEncryptedWidget refresh $data")
        if (data is CiPlusInterface.Enquiry) {
            title.setText(data.title)
            description.setText(data.inputText)
            if(data.blind) {
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            } else {
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            }
        }
    }
}