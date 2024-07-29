package com.iwedia.cltv.fti

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.tv.TvContract
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.iwedia.cltv.R
import com.iwedia.cltv.SettingsActivity
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.fti.data.Channel
import com.iwedia.cltv.fti.handlers.ConfigHandler
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream


class ChooseLcnFragment  : Fragment() {

    val TAG = javaClass.simpleName
    val CHANNELS_URI: Uri = Uri.parse("content://${"com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider"}/${"channels"}")
    companion object {
        fun newInstance() = ChooseLcnFragment()
    }

    var viewModel: View? = null
    private var mContentResolver: ContentResolver? = null

    @SuppressLint("ResourceType", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = inflater.inflate(R.layout.fti_choose_lcn_layout, container, false)
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

        val textViewLcnText: TextView = viewModel!!.findViewById(R.id.textViewLcnText)
        textViewLcnText.text =  "${ConfigStringsManager.getStringById("lcn_conf")} ${(activity as SettingsActivity?)!!.channelKey}"
        var param = textViewLcnText!!.layoutParams as ViewGroup.MarginLayoutParams

        if((activity as SettingsActivity?)!!.atvGtvSwitch == 0){
            param.setMargins(50 * context!!.getResources().getDisplayMetrics().density.toInt(), 150 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 50 * context!!.getResources().getDisplayMetrics().density.toInt())
            textViewLcnText!!.layoutParams = param
            textViewLcnText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewLcnText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewLcnText!!.gravity = Gravity.LEFT
            textViewLcnText!!.textSize = 36f
            textViewLcnText!!.includeFontPadding = false
        }else{
            param.setMargins(0, 124 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
            textViewLcnText!!.layoutParams = param
            textViewLcnText!!.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            textViewLcnText!!.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            textViewLcnText!!.gravity = Gravity.TOP
            textViewLcnText!!.textSize = 32f
        }

        val textViewDescription: TextView = viewModel!!.findViewById(R.id.textViewDescription)
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
            Utils.loadImage(imagePath, imageViewLogo, object : AsyncReceiver {
                override fun onFailed(error: Error?) {}
                override fun onSuccess() {}
            })
        } else {
            val ims: InputStream = (activity as SettingsActivity?)!!.assets.open("company_logo.png")
            val bitmap1 = BitmapFactory.decodeStream(ims)
            imageViewLogo.setImageBitmap(bitmap1)
        }

        ////////////////////////////////////////////////////////////////////
        mContentResolver = (activity as SettingsActivity?)!!.contentResolver;

        val ll_main = viewModel!!.findViewById(R.id.ll_main_layout) as LinearLayout
        ll_main.gravity = Gravity.CENTER
        var i : Int = 0

        var mChannels: MutableList<Channel> = mutableListOf()
        val mChannelsMap: HashMap<String, Channel> = HashMap()

        val duplicateChannels: MutableList<Channel> = (activity as SettingsActivity?)!!.atChannels!!
        duplicateChannels!!.forEach { channel ->
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreateView: duplicateChannels ${channel.mDisplayName}")
            var frequency = 0
            val jsonConfig = channel.getInternalProviderData()
            try {
                val jobj = JSONObject(jsonConfig)
                val transport: JSONObject = jobj.getJSONObject("transport")
                if (transport.has("frequency")) {
                    frequency = transport.getInt("frequency")
                } else {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreateView: No frequency")
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            val button_dynamic = TextView(context)
            val relative_layout_dynamic = RelativeLayout(context)
            button_dynamic!!.isFocusable = true
            button_dynamic.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            button_dynamic.id = i
            button_dynamic.isAllCaps = false
            button_dynamic.text = "${channel.mDisplayName} + (${frequency/1000000}Mhz)"
            var focusSet = 0

            if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                button_dynamic!!.gravity = Gravity.CENTER_VERTICAL
                button_dynamic.width =
                    268 * context!!.getResources().getDisplayMetrics().density.toInt()
                button_dynamic.height =
                    60 * context!!.getResources().getDisplayMetrics().density.toInt()
                button_dynamic.textAlignment = View.TEXT_ALIGNMENT_CENTER
                val param = button_dynamic.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(0, 10, 0, 0)
                button_dynamic.layoutParams = param

                button_dynamic.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        button_dynamic.width = 295 * context!!.getResources().getDisplayMetrics().density.toInt()
                        button_dynamic.height = 66 * context!!.getResources().getDisplayMetrics().density.toInt()
                        button_dynamic!!.setTextColor(
                            ContextCompat.getColor(
                                context!!,
                                R.color.button_selected_text_fti
                            )
                        )
                        button_dynamic!!.setBackgroundResource(R.drawable.my_action_button_selected)
                    } else {
                        button_dynamic.width = 268 * context!!.getResources().getDisplayMetrics().density.toInt()
                        button_dynamic.height = 60 * context!!.getResources().getDisplayMetrics().density.toInt()
                        button_dynamic!!.setTextColor(
                            ContextCompat.getColor(
                                context!!,
                                R.color.button_not_selected_text_fti
                            )
                        )
                        button_dynamic!!.setBackgroundResource(R.drawable.my_action_button)
                    }
                }

                button_dynamic.setOnClickListener {
                    (activity as SettingsActivity?)!!.conflictFixedList[(activity as SettingsActivity?)!!.atLcnConflictNumber] = true
                    val originalLcn = (activity as SettingsActivity?)!!.channelKey

                    updateChannelList(mChannels, mChannelsMap.get("${channel.mId}")!!.mId, originalLcn)
                    updateChannelListDB((activity as SettingsActivity?)!!.lcnConflictFragment.mOriginalLcnMap, (activity as SettingsActivity?)!!.lcnConflictFragment.mUpdatedChannels)

                    (activity as SettingsActivity?)!!.goToLcnConflict()
                }

                ll_main.addView(button_dynamic)
            }
            else{
                button_dynamic!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                button_dynamic!!.layoutParams.width = 250 * context!!.getResources().getDisplayMetrics().density.toInt()
                button_dynamic!!.layoutParams.height = 22 * context!!.getResources().getDisplayMetrics().density.toInt()
                button_dynamic!!.includeFontPadding = false
                button_dynamic!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                param = button_dynamic!!.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(22 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0, 0)
                button_dynamic!!.layoutParams = param

                relative_layout_dynamic.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                param = relative_layout_dynamic!!.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(0, 10 * context!!.getResources().getDisplayMetrics().density.toInt(), 0, 0)
                relative_layout_dynamic!!.layoutParams = param
                relative_layout_dynamic!!.gravity = Gravity.CENTER_VERTICAL
                relative_layout_dynamic!!.setFocusable(true)
                relative_layout_dynamic!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
                relative_layout_dynamic!!.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        relative_layout_dynamic!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_on_gtv)
                        button_dynamic!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                    }else{
                        relative_layout_dynamic!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
                        button_dynamic!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                    }
                }

                relative_layout_dynamic.setOnClickListener {
                    (activity as SettingsActivity?)!!.conflictFixedList[(activity as SettingsActivity?)!!.atLcnConflictNumber] = true
                    val originalLcn = (activity as SettingsActivity?)!!.channelKey

                    updateChannelList(mChannels, mChannelsMap.get("${channel.mId}")!!.mId, originalLcn)
                    updateChannelListDB((activity as SettingsActivity?)!!.lcnConflictFragment.mOriginalLcnMap, (activity as SettingsActivity?)!!.lcnConflictFragment.mUpdatedChannels)

                    (activity as SettingsActivity?)!!.goToLcnConflict()
                }

                relative_layout_dynamic.addView(button_dynamic)
                ll_main.addView(relative_layout_dynamic)
            }

            if (i == 0) {
                if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                    button_dynamic!!.setFocusable(true)
                    button_dynamic!!.requestFocus()
                }else{
                    relative_layout_dynamic!!.setFocusable(true)
                    relative_layout_dynamic!!.requestFocus()
                }
                focusSet = 1
                textViewDescription.text = "${ConfigStringsManager.getStringById("assign")} ${channel.mDisplayName} ${ConfigStringsManager.getStringById("to_number")} ${(activity as SettingsActivity?)!!.channelKey}"

                if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                    if (button_dynamic!!.hasFocus()) {
                        button_dynamic.width =
                            295 * context!!.getResources().getDisplayMetrics().density.toInt()
                        button_dynamic.height =
                            66 * context!!.getResources().getDisplayMetrics().density.toInt()
                        button_dynamic!!.setTextColor(
                            ContextCompat.getColor(
                                context!!,
                                R.color.button_selected_text_fti
                            )
                        )
                        button_dynamic!!.setBackgroundResource(R.drawable.my_action_button_selected)
                    }
                }else{
                    relative_layout_dynamic!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_on_gtv)
                    button_dynamic!!.setTextAppearance(R.style.edit_text_button_text_style_on_gtv)
                }
            } else {
                if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                    button_dynamic!!.setTextColor(
                        ContextCompat.getColor(
                            context!!,
                            R.color.button_not_selected_text_fti
                        )
                    )
                    button_dynamic!!.setBackgroundResource(R.drawable.my_action_button)
                }else{
                    relative_layout_dynamic!!.background = ContextCompat.getDrawable(context!!, R.drawable.edit_text_button_off_gtv)
                    button_dynamic!!.setTextAppearance(R.style.edit_text_button_text_style_off_gtv)
                }
            }

            mChannels.add(channel)
            mChannelsMap.put("${channel.mId}",channel)

            //todo set focus
            try {
                if(focusSet == 0) {
                    if((activity as SettingsActivity?)!!.atvGtvSwitch == 0) {
                        button_dynamic!!.setFocusable(true)
                        button_dynamic!!.requestFocus()
                    }else{
                        relative_layout_dynamic!!.setFocusable(true)
                        relative_layout_dynamic!!.requestFocus()
                    }
                    focusSet = 1
                }
            }catch (E: Exception){
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreateView: ${E.printStackTrace()}")
            }
            i++
        }

        return viewModel!!
    }

    fun updateChannelList(channels: MutableList<Channel>, channelId: Long, originalLcn: Int) {
        (activity as SettingsActivity?)!!.lcnConflictFragment.mOriginalLcnMap.clear()
        val itr: MutableIterator<*> = channels.iterator()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateChannelList: channelId = $channelId")
        while (itr.hasNext()) {
            val c = itr.next() as Channel
            if (c.getId() === channelId) {
                (activity as SettingsActivity?)!!.lcnConflictFragment.mOriginalLcnMap.put(originalLcn, c)
                itr.remove()
            }
        }
        (activity as SettingsActivity?)!!.lcnConflictFragment.mUpdatedChannels.clear()
        (activity as SettingsActivity?)!!.lcnConflictFragment.mUpdatedChannels.addAll(channels)
    }

    fun updateChannelListDB(originalLcnMap: HashMap<Int, Channel>, channels: List<Channel>) {
        var chosenChannel: Channel? = null
        var originalLcn = 0
        originalLcnMap.forEach{ e ->
            val values = ContentValues()
            val selection: String = TvContract.Channels._ID.toString() + " = " + e.value.getId()
            originalLcn = e.key
            chosenChannel = e.value
            values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, e.key)
            mContentResolver!!.update(CHANNELS_URI, values, selection, null)
        }

        channels.forEach { c ->

            if (c.mDisplayNumber!!.toInt() == originalLcn && (originalLcn != 0)) {
                val values = ContentValues()
                val selection: String = TvContract.Channels._ID.toString() + " = " + c.getId()
                values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, chosenChannel!!.mDisplayNumber)
                mContentResolver!!.update(CHANNELS_URI, values, selection, null)
            }
        }
    }
}