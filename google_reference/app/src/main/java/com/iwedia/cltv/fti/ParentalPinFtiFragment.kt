package com.iwedia.cltv.fti

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.iwedia.cltv.R
import com.iwedia.cltv.SettingsActivity
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.fti.handlers.ConfigHandler
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import java.io.InputStream


class ParentalPinFtiFragment: Fragment()  {

    companion object {
        fun newInstance() = ParentalPinFtiFragment()
    }

    var textViewPosition = 0
    var buttonConfirmText: TextView? = null
    var buttonConfirm: RelativeLayout? = null
    var textViewPinText: TextView? = null
    var buttonPinEditText: EditText? = null
    var buttonPin:  RelativeLayout? = null
    var viewModel: View? = null
    var pinString: String = "0000"
    var confirmPinString : String = "0000"
    var stateOfPinFragment: Int = 0

    @SuppressLint("ResourceType")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = inflater.inflate(R.layout.fti_pin_layout, container, false)
        viewModel!!.background = ContextCompat.getDrawable(context!!, R.color.fti_left_black)

        val linear_border1: LinearLayout = viewModel!!.findViewById(R.id.linear_border1)
        val linear_border2: LinearLayout = viewModel!!.findViewById(R.id.linear_border2)
        val linear_border3: LinearLayout = viewModel!!.findViewById(R.id.linear_border3)
        val constraint_border1: ConstraintLayout = viewModel!!.findViewById(R.id.constraint_border1)
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            linear_border1.visibility = View.GONE
            linear_border2.visibility = View.GONE
            linear_border3.visibility = View.GONE
            constraint_border1!!.background = ContextCompat.getDrawable(context!!, R.color.fti_left_background)
        }else{
            viewModel!!.background = ContextCompat.getDrawable(context!!, R.drawable.fti_bg_gtv)
        }


        textViewPinText = viewModel!!.findViewById(R.id.textViewPinText)
        textViewPinText!!.text = ConfigStringsManager.getStringById("enter_pin_number")
        var param = textViewPinText!!.layoutParams as ViewGroup.MarginLayoutParams
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param.setMargins(50 * context!!.getResources().getDisplayMetrics().density.toInt(), 150 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 50 * context!!.getResources().getDisplayMetrics().density.toInt())
            textViewPinText!!.layoutParams = param
            textViewPinText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewPinText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewPinText!!.gravity = Gravity.LEFT
            textViewPinText!!.textSize = 36f
            textViewPinText!!.includeFontPadding = false
        }else{
            param.setMargins(0, 124 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewPinText!!.layoutParams = param
            textViewPinText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewPinText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewPinText!!.gravity = Gravity.TOP
            textViewPinText!!.textSize = 32f
        }

        val textViewDescription: TextView = viewModel!!.findViewById(R.id.textViewDescription)
        textViewDescription.text = ConfigStringsManager.getStringById("pin_description")

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param = textViewDescription!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(50 * context!!.getResources().getDisplayMetrics().density.toInt(), 20 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 50 * context!!.getResources().getDisplayMetrics().density.toInt())
            textViewDescription!!.layoutParams = param
            textViewDescription!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescription!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewDescription!!.gravity = Gravity.LEFT
            textViewDescription!!.includeFontPadding = false
            textViewDescription!!.layoutParams
        }else{
            param = textViewDescription.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 20 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewDescription.layoutParams = param
            textViewDescription!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescription!!.layoutParams.height = 96 * context!!.getResources().getDisplayMetrics().density.toInt()
            textViewDescription.gravity = Gravity.TOP
            textViewDescription!!.includeFontPadding = false
            textViewDescription.translationY = -1.98f
            textViewDescription.textSize = 16f
        }

        val imageViewLogo: ImageView = viewModel!!.findViewById(R.id.imageViewLogo)

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = imageViewLogo.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(
                50 * context!!.getResources().getDisplayMetrics().density.toInt(),
                50 * context!!.getResources().getDisplayMetrics().density.toInt(),
                0,
                0
            )
            imageViewLogo.layoutParams = param
            imageViewLogo!!.layoutParams.width =
                40 * context!!.getResources().getDisplayMetrics().density.toInt()
            imageViewLogo!!.layoutParams.height =
                40 * context!!.getResources().getDisplayMetrics().density.toInt()
        }else{
            param = imageViewLogo.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 96 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            imageViewLogo.layoutParams = param
            imageViewLogo!!.layoutParams.width = 20 * context!!.getResources().getDisplayMetrics().density.toInt()
            imageViewLogo!!.layoutParams.height = 20 * context!!.getResources().getDisplayMetrics().density.toInt()
        }

        var imagePath = ConfigHandler.getCompanyLogo()
        if (imagePath.contains("no_image")) {
            imageViewLogo.setBackgroundColor(Color.TRANSPARENT)
        }else if (imagePath.isNotEmpty()) {
            Utils.loadImage(imagePath, imageViewLogo, object: AsyncReceiver {
                override fun onFailed(error: Error?) {}
                override fun onSuccess() {}
            })
        } else {
            val ims: InputStream = (activity as SettingsActivity?)!!.assets.open("company_logo.png")
            val bitmap1 = BitmapFactory.decodeStream(ims)
            imageViewLogo.setImageBitmap(bitmap1)
        }
        setupFirestPinView()

        buttonPinEditText = viewModel!!.findViewById(R.id.buttonPinEditText)
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = buttonPinEditText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 185 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonPinEditText!!.layoutParams = param
            buttonPinEditText!!.gravity = Gravity.CENTER
            buttonPinEditText!!.layoutParams.width = 100 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonPinEditText!!.layoutParams.height = 40 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonPinEditText!!.background = ContextCompat.getDrawable(context!!, R.color.button_not_selected_fti)

            buttonPinEditText!!.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val imm: InputMethodManager
                    imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    return@OnEditorActionListener true
                }
                false
            })

            buttonPinEditText!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    editViewHasFocusNumber(buttonPinEditText!!)
                }else{
                    editViewHasNoFocusNumber(buttonPinEditText!!)
                }
            }
        }else{
            buttonPin = viewModel!!.findViewById(R.id.buttonPin)
            buttonPin!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonPin!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 124 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonPin!!.layoutParams = param
            buttonPin!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_on_gtv)
            param = buttonPinEditText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(16 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonPinEditText!!.layoutParams = param
            buttonPinEditText!!.setTextAppearance(R.style.edit_text_text_style_on_gtv)
            buttonPinEditText!!.gravity = Gravity.CENTER_VERTICAL
            buttonPinEditText!!.layoutParams.width = 236 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonPinEditText!!.layoutParams.height = 22 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonPinEditText!!.setTextColor(ContextCompat.getColor(context!!, R.color.fti_gtv_relative_layout_edit_text_text_color_on))
            buttonPinEditText!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonPin!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_on_gtv)
                    buttonPinEditText!!.setTextAppearance(R.style.edit_text_text_style_on_gtv)
                    buttonPinEditText!!.background = ContextCompat.getDrawable(context!!, R.color.fti_gtv_relative_layout_edit_text_on)
                }else{
                    buttonPin!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_off_gtv)
                    buttonPinEditText!!.setTextAppearance(R.style.edit_text_text_style_off_gtv)
                    buttonPinEditText!!.background = null
                }
            }
        }


        buttonConfirmText = viewModel!!.findViewById(R.id.buttonConfirmText)
        buttonConfirmText!!.setText(ConfigStringsManager.getStringById("confirm_text"))
        buttonConfirm = viewModel!!.findViewById(R.id.buttonConfirm)

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 1) {
            buttonConfirm!!.updateLayoutParams<ConstraintLayout.LayoutParams> { verticalBias = 0f }
            buttonConfirm!!.gravity = Gravity.CENTER_VERTICAL
            param = buttonConfirm!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 196 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            buttonConfirm!!.layoutParams = param
            buttonConfirm!!.setFocusable(true)
            buttonConfirm!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
            buttonConfirm!!.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    buttonConfirm!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_on_gtv)
                    buttonConfirmText!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }else{
                    buttonConfirm!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
                    buttonConfirmText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }
            buttonConfirm!!.setOnClickListener{
                clickConfirm()
            }
        }else{
            buttonConfirm!!.setFocusable(false)
            buttonConfirm!!.updateLayoutParams<ConstraintLayout.LayoutParams> { verticalBias = 1f }
        }

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = buttonConfirmText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(
                0,
                0,
                0,
                50 * context!!.getResources().getDisplayMetrics().density.toInt()
            )
            buttonConfirmText!!.layoutParams = param
            buttonConfirmText!!.isAllCaps = false
            buttonConfirmText!!.textSize = 15f
            buttonConfirmText!!.setTextColor(R.color.fti_text_color)
            buttonConfirmText!!.background =
                ContextCompat.getDrawable(context!!, R.drawable.my_small_action_button)
            buttonConfirmText!!.layoutParams.width =
                120 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonConfirmText!!.layoutParams.height =
                40 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonConfirmText!!.gravity = Gravity.CENTER
        }else{
            buttonConfirmText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            buttonConfirmText!!.layoutParams.width = 250 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonConfirmText!!.layoutParams.height = 22 * context!!.getResources().getDisplayMetrics().density.toInt()
            buttonConfirmText!!.includeFontPadding = false
            buttonConfirmText!!.isAllCaps = false
            buttonConfirmText!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
            param = buttonConfirmText!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(22 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
            buttonConfirmText!!.layoutParams = param
        }
        buttonNotFocused(buttonConfirmText!!)

        buttonConfirmText!!.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                buttonFocused(buttonConfirmText!!)
            }else{
                buttonNotFocused(buttonConfirmText!!)
            }
        }

        buttonConfirmText!!.setOnClickListener {
            clickConfirm()
        }
        return viewModel!!
    }

    fun clickConfirm(){
        buttonPinEditText!!.setFocusable(true)
        buttonPinEditText!!.requestFocus()
        if(buttonPinEditText!!.text.length == 4) {
            if (stateOfPinFragment == 0) {
                pinString = buttonPinEditText!!.text.toString()
                textViewPosition = 0
                stateOfPinFragment = 1
                setupConfirmPinView()
            } else if (stateOfPinFragment == 1) {
                confirmPinString = buttonPinEditText!!.text.toString()
                if (pinString.compareTo(confirmPinString) == 0) {
                    stateOfPinFragment = 2

                    val configUri: Uri =
                        Uri.parse("content://com.iwedia.cltv.platform.model.content_provider.ReferenceContentProvider/config")
                    val cursor: Cursor = context!!.getContentResolver()
                        .query(configUri, null, null, null, null)!!
                    if (cursor.moveToFirst()) {
                        Log.d(Constants.LogTag.CLTV_TAG + "ScanTuneOptionFragment", "Update existing row with new pin")
                        val uri: Uri = Uri.withAppendedPath(configUri, "/1")
                        val cv = ContentValues()
                        cv.put("pin", pinString)
                        val where = "_id" + " =?"
                        val args = arrayOf("1")
                        val result: Int =
                            context!!.getContentResolver().update(uri, cv, where, args)
                    } else {
                        Log.d(Constants.LogTag.CLTV_TAG + "ParentalPinFtiFragment", "Insert new element in config table")
                        val cv = ContentValues()
                        cv.put("pin", pinString)
                        cv.put("lcn", "false")
                        val uri: Uri = context!!.getContentResolver().insert(configUri, cv)!!
                    }
                    cursor.close()
                    (activity as SettingsActivity?)!!.goToCountrySelectFragment()
                } else {
                    buttonPinEditText!!.setText("")
                    textViewPinText!!.setText(ConfigStringsManager.getStringById("not_correct_pins"))
                }
            }
        }
    }

    fun editViewHasFocusNumber(textButton: EditText){
        textButton!!.setTextColor(ContextCompat.getColor(context!!,R.color.button_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_action_button_selected)
    }

    fun editViewHasNoFocusNumber(textButton: EditText){
        textButton!!.setTextColor(ContextCompat.getColor(context!!,R.color.button_not_selected_text_fti))
        textButton!!.setBackgroundResource(R.drawable.my_action_button)
    }

    fun setupFirestPinView() {
        stateOfPinFragment = 0
        textViewPosition = 0
        pinString = "0000"
        confirmPinString = "0000"
        textViewPinText!!.setText(ConfigStringsManager.getStringById("enter_pin_number"))
    }

    fun setupConfirmPinView() {
        buttonPinEditText!!.setText("")
        textViewPinText!!.setText(ConfigStringsManager.getStringById("confirm_pin_number"))
    }

    @SuppressLint("ResourceType")
    fun myOnKeyDown(key_code: Int) {
        if (key_code == KeyEvent.KEYCODE_BACK) {
            if(stateOfPinFragment == 1){
                setupFirestPinView()
            }
        }
    }

    fun buttonFocused(button: TextView){
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            button!!.setTextColor(
                ContextCompat.getColor(
                    context!!,
                    R.color.button_selected_text_fti
                )
            )
            button!!.setBackgroundResource(R.drawable.my_small_action_button_selected)
            button!!.layoutParams.width =
                132 * context!!.getResources().getDisplayMetrics().density.toInt()
            button!!.layoutParams.height =
                44 * context!!.getResources().getDisplayMetrics().density.toInt()
        }
    }

    fun buttonNotFocused(button: TextView){
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            button!!.setTextColor(
                ContextCompat.getColor(
                    context!!,
                    R.color.button_not_selected_text_fti
                )
            )
            button!!.setBackgroundResource(R.drawable.my_small_action_button)
            button!!.layoutParams.width =
                120 * context!!.getResources().getDisplayMetrics().density.toInt()
            button!!.layoutParams.height =
                40 * context!!.getResources().getDisplayMetrics().density.toInt()
        }
    }

}