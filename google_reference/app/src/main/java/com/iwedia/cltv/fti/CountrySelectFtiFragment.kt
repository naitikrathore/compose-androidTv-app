package com.iwedia.cltv.fti

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.iwedia.cltv.R
import com.iwedia.cltv.SettingsActivity
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.fti.handlers.ConfigHandler
import com.iwedia.cltv.fti.scan_models.ConfigParser
import com.iwedia.cltv.fti.scan_models.DataParams
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import java.io.InputStream


class CountrySelectFtiFragment : Fragment() {

    companion object {
        fun newInstance() = CountrySelectFtiFragment()
    }

    var viewModel: View? = null
    private var configParser: ConfigParser? = null
    private var scanConfigList: ArrayList<DataParams>? = null

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ResourceType")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = inflater.inflate(R.layout.fti_country_select_cable_layout, container, false)
        viewModel!!.background = ContextCompat.getDrawable(requireContext(), R.color.fti_left_black)

        val linear_border1: LinearLayout = viewModel!!.findViewById(R.id.linear_border1)
        val linear_border2: LinearLayout = viewModel!!.findViewById(R.id.linear_border2)
        val linear_border3: LinearLayout = viewModel!!.findViewById(R.id.linear_border3)
        val constraint_border1: ConstraintLayout = viewModel!!.findViewById(R.id.constraint_border1)
        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            linear_border1.visibility = View.GONE
            linear_border2.visibility = View.GONE
            linear_border3.visibility = View.GONE
            constraint_border1!!.background = ContextCompat.getDrawable(requireContext(), R.color.fti_left_background)
        }else{
            viewModel!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.fti_bg_gtv)
        }

        val textViewCountryText: TextView = viewModel!!.findViewById(R.id.textViewCountryText)
        textViewCountryText.text = ConfigStringsManager.getStringById("select_country")

        var param = textViewCountryText!!.layoutParams as ViewGroup.MarginLayoutParams

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param.setMargins(50 * requireContext().getResources().getDisplayMetrics().density.toInt(), 150 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 50 * requireContext().getResources().getDisplayMetrics().density.toInt())
            textViewCountryText!!.layoutParams = param
            textViewCountryText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewCountryText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewCountryText!!.gravity = Gravity.LEFT
            textViewCountryText!!.textSize = 36f
            textViewCountryText!!.includeFontPadding = false
        }else{
            param.setMargins(0, 124 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewCountryText!!.layoutParams = param
            textViewCountryText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewCountryText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewCountryText!!.gravity = Gravity.TOP
            textViewCountryText!!.textSize = 32f
        }

        val textViewDescription: TextView = viewModel!!.findViewById(R.id.textViewDescription)
        textViewDescription.text = ConfigStringsManager.getStringById("select_country_description")

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param = textViewDescription!!.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(50 * requireContext().getResources().getDisplayMetrics().density.toInt(), 20 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 50 * requireContext().getResources().getDisplayMetrics().density.toInt())
            textViewDescription!!.layoutParams = param
            textViewDescription!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescription!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewDescription!!.gravity = Gravity.LEFT
            textViewDescription!!.includeFontPadding = false
            textViewDescription!!.layoutParams
        }else{
            param = textViewDescription.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 20 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewDescription.layoutParams = param
            textViewDescription!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewDescription!!.layoutParams.height = 96 * requireContext().getResources().getDisplayMetrics().density.toInt()
            textViewDescription.gravity = Gravity.TOP
            textViewDescription!!.includeFontPadding = false
            textViewDescription.translationY = -1.98f
            textViewDescription.textSize = 16f
        }

        val imageViewLogo: ImageView = viewModel!!.findViewById(R.id.imageViewLogo)

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
            param = imageViewLogo.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(
                50 * requireContext().getResources().getDisplayMetrics().density.toInt(),
                50 * requireContext().getResources().getDisplayMetrics().density.toInt(),
                0,
                0
            )
            imageViewLogo.layoutParams = param
            imageViewLogo!!.layoutParams.width =
                40 * requireContext().getResources().getDisplayMetrics().density.toInt()
            imageViewLogo!!.layoutParams.height =
                40 * requireContext().getResources().getDisplayMetrics().density.toInt()
        }else{
            param = imageViewLogo.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 96 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
            imageViewLogo.layoutParams = param
            imageViewLogo!!.layoutParams.width = 20 * requireContext().getResources().getDisplayMetrics().density.toInt()
            imageViewLogo!!.layoutParams.height = 20 * requireContext().getResources().getDisplayMetrics().density.toInt()
        }
        var imagePath = ConfigHandler.getCompanyLogo()
        if (imagePath.isNotEmpty()) {
            Utils.loadImage(imagePath, imageViewLogo, object : AsyncReceiver {
                override fun onFailed(error: Error?) {}
                override fun onSuccess() {}
            })
        } else {
            val ims: InputStream = (activity as SettingsActivity?)!!.assets.open("company_logo.png")
            val bitmap1 = BitmapFactory.decodeStream(ims)
            imageViewLogo.setImageBitmap(bitmap1)
        }

        configParser = ConfigParser((activity as SettingsActivity?)!!)

        configParser!!.scanConfigList = ArrayList<DataParams>()
        configParser!!.gson = Gson()
        configParser!!.fillList(requireContext())
        scanConfigList = ArrayList<DataParams>(configParser!!.scanConfigList)


        val ll_main = viewModel!!.findViewById(R.id.ll_main_layout) as LinearLayout
        ll_main.gravity = Gravity.CENTER

        val ll_scroll_view: ScrollView = viewModel!!.findViewById(R.id.ll_scroll_view)
        ll_scroll_view.smoothScrollTo(0, ll_main.getTop())
        var i = 0
        scanConfigList!!.forEach { sdp ->
            val button_dynamic = TextView(context)
            val relative_layout_dynamic = RelativeLayout(context)
            button_dynamic!!.isFocusable = true
            button_dynamic.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            var temp = sdp.getCountryName()!!.lowercase().replace(" ", "_")
            button_dynamic.text = ConfigStringsManager.getStringById(temp)
            button_dynamic.id = i
            button_dynamic.isAllCaps = false

            if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                button_dynamic!!.gravity = Gravity.CENTER_VERTICAL
                button_dynamic.width =
                    268 * requireContext().getResources().getDisplayMetrics().density.toInt()
                button_dynamic.height =
                    60 * requireContext().getResources().getDisplayMetrics().density.toInt()
                button_dynamic.textAlignment = View.TEXT_ALIGNMENT_CENTER
                val param = button_dynamic.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(0, 10, 0, 0)
                button_dynamic.layoutParams = param

                button_dynamic.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        button_dynamic.width = 295 * requireContext().getResources().getDisplayMetrics().density.toInt()
                        button_dynamic.height = 66 * requireContext().getResources().getDisplayMetrics().density.toInt()
                        button_dynamic!!.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.button_selected_text_fti
                            )
                        )
                        button_dynamic!!.setBackgroundResource(R.drawable.my_action_button_selected)
                    } else {
                        button_dynamic.width = 268 * requireContext().getResources().getDisplayMetrics().density.toInt()
                        button_dynamic.height = 60 * requireContext().getResources().getDisplayMetrics().density.toInt()
                        button_dynamic!!.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.button_not_selected_text_fti
                            )
                        )
                        button_dynamic!!.setBackgroundResource(R.drawable.my_action_button)
                    }
                }

                button_dynamic.setOnClickListener {
                    ConfigHandler.setCountryValue(sdp.getCountryTag())
                    (activity as SettingsActivity?)!!.countryName = sdp.getCountryName()!!
                    (activity as SettingsActivity?)!!.goToTunerFragment(sdp)
                }

                ll_main.addView(button_dynamic)
            }
            else{
                button_dynamic!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                button_dynamic!!.layoutParams.width = 250 * requireContext().getResources().getDisplayMetrics().density.toInt()
                button_dynamic!!.layoutParams.height = 22 * requireContext().getResources().getDisplayMetrics().density.toInt()
                button_dynamic!!.includeFontPadding = false
                button_dynamic!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                param = button_dynamic!!.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(22 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
                button_dynamic!!.layoutParams = param

                relative_layout_dynamic.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                param = relative_layout_dynamic!!.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(0, 10 * requireContext().getResources().getDisplayMetrics().density.toInt(), 0, 0)
                relative_layout_dynamic!!.layoutParams = param
                relative_layout_dynamic!!.gravity = Gravity.CENTER_VERTICAL
                relative_layout_dynamic!!.setFocusable(true)
                relative_layout_dynamic!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
                relative_layout_dynamic!!.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        relative_layout_dynamic!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_on_gtv)
                        button_dynamic!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                    }else{
                        relative_layout_dynamic!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
                        button_dynamic!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                    }
                }

                relative_layout_dynamic.setOnClickListener {
                    ConfigHandler.setCountryValue(sdp.getCountryTag())
                    (activity as SettingsActivity?)!!.countryName = sdp.getCountryName()!!
                    (activity as SettingsActivity?)!!.goToTunerFragment(sdp)
                }

                relative_layout_dynamic.addView(button_dynamic)
                ll_main.addView(relative_layout_dynamic)
            }

            if (temp.equals((activity as SettingsActivity?)!!.moduleProvider.getGeneralConfigModule().getCountryThatIsSelected().lowercase().replace(" ", "_"))) {
                if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                    button_dynamic!!.setFocusable(true)
                    button_dynamic!!.requestFocus()
                }else{
                    relative_layout_dynamic!!.setFocusable(true)
                    relative_layout_dynamic!!.requestFocus()
                }

                if(i > 4) {
                    if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                        val button_dynamic2: Button = ll_main.findViewById(i - 3)
                        ll_scroll_view.post(Runnable {
                            ll_scroll_view.smoothScrollTo(0, button_dynamic2.y.toInt())
                        })
                    }else{
                        val relative_layout_dynamic2: RelativeLayout = ll_main.findViewById(i - 3)
                        ll_scroll_view.post(Runnable {
                            ll_scroll_view.smoothScrollTo(0, relative_layout_dynamic2.y.toInt())
                        })
                    }
                }

                if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                    if (button_dynamic!!.hasFocus()) {
                        button_dynamic.width =
                            295 * requireContext().getResources().getDisplayMetrics().density.toInt()
                        button_dynamic.height =
                            66 * requireContext().getResources().getDisplayMetrics().density.toInt()
                        button_dynamic!!.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.button_selected_text_fti
                            )
                        )
                        button_dynamic!!.setBackgroundResource(R.drawable.my_action_button_selected)
                    }
                }else{
                    relative_layout_dynamic!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_on_gtv)
                    button_dynamic!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }
            }
            else {
                if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                    button_dynamic!!.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.button_not_selected_text_fti
                        )
                    )
                    button_dynamic!!.setBackgroundResource(R.drawable.my_action_button)
                }else{
                    relative_layout_dynamic!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_text_button_off_gtv)
                    button_dynamic!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }

            i++
        }

        return viewModel!!
    }
}